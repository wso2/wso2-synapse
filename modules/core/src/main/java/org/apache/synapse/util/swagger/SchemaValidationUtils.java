/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.util.swagger;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.API;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.logging.LoggingUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for API schema validation related operations.
 */
public class SchemaValidationUtils {

    private static final Log logger = LogFactory.getLog(SchemaValidationUtils.class);

    /**
     * Utility function to extract collection of String from Map when the key is given.
     *
     * @param map  Map of String, Collection<String>
     * @param name key
     * @return Collection of Strings
     */
    public static Collection<String> getFromMapOrEmptyList(Map<String, Collection<String>> map, String name) {

        if (name != null && map.containsKey(name)) {

            return map.get(name).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Utility function to extract query params from resource path.
     *
     * @param apiResource resource path with query params.
     * @return Map of Query params.
     * @throws UnsupportedEncodingException When failed to decode
     */
    public static Map<String, Collection<String>> getQueryParams(String apiResource)
            throws UnsupportedEncodingException {
        Map<String, String> queryParams = new HashMap<>();
        String queryString = apiResource.split("\\?")[1];
        String[] query = queryString.split("&");
        for (String keyValue : query) {
            int idx = keyValue.indexOf("=");
            queryParams.put(
                    URLDecoder.decode(keyValue.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(keyValue.substring(idx + 1), "UTF-8"));
        }
        return queryParams.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> Collections.singleton(entry.getValue())));
    }

    /**
     * Get the Request/Response messageContent as a JsonObject.
     *
     * @param messageContext Message context
     * @return JsonElement which contains the request/response message content
     */
    public static Optional<String> getMessageContent(MessageContext messageContext) {

        Optional<String> payloadObject = Optional.empty();
        org.apache.axis2.context.MessageContext axis2Context = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        boolean isMessageContextBuilt = isMessageContextBuilt(axis2Context);
        if (!isMessageContextBuilt) {
            // Build Axis2 Message.
            try {
                RelayUtils.buildMessage(axis2Context);
            } catch (IOException | XMLStreamException e) {
                logger.error(" Unable to build axis2 message");
            }
        }

        if (JsonUtil.hasAJsonPayload(axis2Context)) {
            payloadObject = Optional.of(JsonUtil.jsonPayloadToString(axis2Context));
        } else if (messageContext.getEnvelope().getBody() != null) {
            Object objFirstElement = messageContext.getEnvelope().getBody().getFirstElement();
            if (objFirstElement != null) {
                OMElement xmlResponse = messageContext.getEnvelope().getBody().getFirstElement();
                try {
                    payloadObject = Optional.of(JsonUtil.toJsonString(xmlResponse).toString());
                } catch (AxisFault axisFault) {
                    logger.error(" Error occurred while converting the String payload to Json");
                }
            }
        }
        return payloadObject;
    }

    public static boolean isMessageContextBuilt(org.apache.axis2.context.MessageContext axis2MC) {

        boolean isMessageContextBuilt = false;
        Object messageContextBuilt = axis2MC.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED);
        if (messageContextBuilt != null) {
            isMessageContextBuilt = (Boolean) messageContextBuilt;
        }

        return isMessageContextBuilt;
    }

    /**
     * Parse the openAPI schema of the API.
     *
     * @param api API object
     * @param se  SynapseEnvironment
     */
    public static void populateSchema(API api, SynapseEnvironment se) {
        Registry registry = se.getSynapseConfiguration().getRegistry();
        if (registry != null) {
            String schemaPath = registry.getRegistryEntry(api.getSwaggerResourcePath()).getName();
            if (!StringUtils.isEmpty(schemaPath)) {
                File schemaFile = new File(schemaPath);
                if (schemaFile.exists()) {
                    try {
                        api.setOpenAPI(loadOpenAPI(schemaPath));
                    } catch (IOException e) {
                        throw new SynapseException("Error while reading the schema file for the API "
                                + api.getName(), e);
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Schema file not found for the API " + api.getName());
                    }
                }
            }
        }
    }

    /**
     * Load the OpenAPI schema from the file.
     *
     * @param schemaFilePath path of the schema file
     * @return JsonNode of the schema
     * @throws IOException if an error occurs while reading the schema file
     */
    private static OpenAPI loadOpenAPI(String schemaFilePath) throws IOException {
        File schemaFile = new File(schemaFilePath);
        if (schemaFile.exists()) {
            return new OpenAPIV3Parser().read(schemaFilePath);
        }
        throw new IllegalArgumentException("cannot find the schema file in the given path: " + schemaFilePath);
    }

    public static void validateAPIRequest(MessageContext messageContext, OpenAPI openAPI) {
        OpenApiInteractionValidator validator = getOpenAPIValidator(openAPI);
        OpenAPIRequest request = new OpenAPIRequest(messageContext, openAPI);
        ValidationReport validationReport = validator.validateRequest(request);
        if (validationReport.hasErrors()) {
            StringBuilder finalMessage = new StringBuilder();
            for (ValidationReport.Message message : validationReport.getMessages()) {
                finalMessage.append(message.getMessage()).append(", ");
            }
            String errMessage = "Schema validation failed in the Request: ";
            // Setting the state to already validated to avoid validating the response in case
            // we have a Respond mediator in fault sequence.
            messageContext.setProperty(RESTConstants.OPENAPI_VALIDATED, true);
            SynapseException ex = new SynapseException(finalMessage.toString());
            handleException(errMessage, ex, messageContext);
        }
    }

    public static void validateAPIResponse(MessageContext messageContext, OpenAPI openAPI) {
        OpenApiInteractionValidator validator = getOpenAPIValidator(openAPI);
        OpenAPIResponse response = new OpenAPIResponse(messageContext);
        ValidationReport validationReport = validator.validateResponse(response.getPath(), response.getMethod(), response);
        if (validationReport.hasErrors()) {
            StringBuilder finalMessage = new StringBuilder();
            for (ValidationReport.Message message : validationReport.getMessages()) {
                finalMessage.append(message.getMessage()).append(", ");
            }
            String errMessage = "Schema validation failed for the Response: ";
            SynapseException ex = new SynapseException(finalMessage.toString());
            handleException(errMessage, ex, messageContext);
        }
    }

    private static OpenApiInteractionValidator getOpenAPIValidator(OpenAPI openAPI) {

        return OpenApiInteractionValidator
                .createFor(openAPI)
                .withLevelResolver(
                        LevelResolver.create()
                                .withLevel("validation.schema.required", ValidationReport.Level.INFO)
                                .withLevel("validation.response.body.missing", ValidationReport.Level.INFO)
                                .withLevel("validation.schema.additionalProperties", ValidationReport.Level.IGNORE)
                                .build())
                .build();
    }

    /**
     * Trying to find the HTTP status code in cases where the mediation does not have a backend call.
     *
     * @param msgContext Message context
     * @return HTTP status code
     */
    public static int determineHttpStatusCode(org.apache.axis2.context.MessageContext msgContext) {

        int httpStatus = HttpStatus.SC_OK;

        // if this is a dummy message to handle http 202 case with non-blocking IO
        // set the status code to 202
        if (msgContext.isPropertyTrue(PassThroughConstants.SC_ACCEPTED)) {
            httpStatus = HttpStatus.SC_ACCEPTED;
        } else {
            // is this a fault message
            boolean handleFault = msgContext.getEnvelope() != null
                    && (msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault());
            boolean faultsAsHttp200 = false;
            if (msgContext.getProperty(PassThroughConstants.FAULTS_AS_HTTP_200) != null) {
                // shall faults be transmitted with HTTP 200
                faultsAsHttp200 =
                        PassThroughConstants.TRUE.equalsIgnoreCase(
                                msgContext.getProperty(PassThroughConstants.FAULTS_AS_HTTP_200).toString());
            }
            // Set HTTP status code to 500 if this is a fault case, and we shall not use HTTP 200
            if (handleFault && !faultsAsHttp200) {
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }
        }
        return httpStatus;
    }

    private static void handleException(String msg, Exception e, org.apache.synapse.MessageContext msgCtx) {
        String formattedLog = LoggingUtils.getFormattedLog(msgCtx, msg);
        logger.error(formattedLog, e);
        throw new SynapseException(msg, e);
    }
}

/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.util;

import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.unittest.ConfigModifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/*
 *  This class contains the util methods with respect to mediator properties
 */
public class MediatorPropertyUtils {

    private static final String SYNAPSE_TEST = "synapseTest";
    private static final String TRUE = "true";
    private static final String URL_PATH_SEPARATOR = "/";

    /**
     * This method removes the current content-type header value from the Axis2 message context and
     * set the given value.
     * @param propertyName Message type property
     * @param resultValue Value to be set
     * @param axis2MessageCtx Axis2 message context
     */
    public static void handleSpecialProperties(String propertyName, Object resultValue,
                                               org.apache.axis2.context.MessageContext axis2MessageCtx) {
        if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(propertyName)) {
            axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
            Map headers = (Map) axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null) {
                headers.remove(HTTP.CONTENT_TYPE);
                headers.put(HTTP.CONTENT_TYPE, resultValue);
            }
        }
        if (SynapseConstants.SET_ROLLBACK_ONLY.equals(propertyName)) {
            axis2MessageCtx.getOperationContext().setProperty(propertyName, resultValue);
        }
    }

    /**
     * Validate the given name to identify whether it is static or dynamic key
     * If the name is in the {} format then it is dynamic key(XPath)
     * Otherwise just a static name
     *
     * @param nameValue string to validate as a name
     * @return isDynamicName representing name type
     */
    public static boolean isDynamicName(String nameValue) {
        if (nameValue.length() < 2) {
            return false;
        }

        final char startExpression = '{';
        final char endExpression = '}';

        char firstChar = nameValue.charAt(0);
        char lastChar = nameValue.charAt(nameValue.length() - 1);

        return (startExpression == firstChar && endExpression == lastChar);
    }

    /**
     * This method just serializes the OMElement, when setting a message type, we need to serialize to access the
     * inner element.
     *
     * @param msgCtx Synapse MessageContext
     */
    public static void serializeOMElement(MessageContext msgCtx) throws XMLStreamException {

        msgCtx.getEnvelope().toString(); // This is an implemented method in OMElement
    }

    /**
     * This method just serializes the OMElement
     *
     * @param msgCtx Synapse MessageContext
     */
    public static void serializeOMElement(org.apache.axis2.context.MessageContext  msgCtx) throws XMLStreamException {
        msgCtx.getEnvelope().toString(); // This is an implemented method in OMElement
    }

    /**
     * Updates the message context To address with mock URL if mock service exists in unit test mode.
     *
     * @param endpoint endpoint definition
     * @param synapseOutMessageContext synapse message context
     * @param axisOutMsgCtx axis2 message context
     */
    public static void updateSendToUrlForMockServices(EndpointDefinition endpoint,
                                                      MessageContext synapseOutMessageContext,
                                                      org.apache.axis2.context.MessageContext axisOutMsgCtx) {

        if (TRUE.equals(System.getProperty(SYNAPSE_TEST)) && (synapseOutMessageContext.getConfiguration().
                getProperty(org.apache.synapse.unittest.Constants.IS_RUNNING_AS_UNIT_TEST) != null &&
                synapseOutMessageContext.getConfiguration().getProperty
                        (org.apache.synapse.unittest.Constants.IS_RUNNING_AS_UNIT_TEST).equals(TRUE)) &&
                endpoint.leafEndpoint != null &&
                (ConfigModifier.unitTestMockEndpointMap.containsKey(endpoint.leafEndpoint.getName()))) {
            Map<String, String> endpointMockResources =
                    ConfigModifier.unitTestMockEndpointMap.get(endpoint.leafEndpoint.getName());
            String modifiedUrl = modifyEndpointUrlWithMockService(axisOutMsgCtx.getTo().getAddress(),
                    endpointMockResources);
            axisOutMsgCtx.getTo().setAddress(modifiedUrl);
            synapseOutMessageContext.getTo().setAddress(modifiedUrl);
        }
    }

    /**
     * Checks current endpoint URL path contains in the mockServiceResources map.
     *
     * @param endpointUrl endpoint url as a string
     * @param mockServiceResources mock resources urls as a map
     * @return mock service url, if resource path doesn't exists returns endpointURL
     */
    private static String modifyEndpointUrlWithMockService(String endpointUrl,
                                                           Map<String, String> mockServiceResources) {
        try {
            URI endpointURI = new URI (endpointUrl);
            String pathWithContextAndParams = endpointUrl.substring(endpointUrl.indexOf(endpointURI.getPath()));
            if (mockServiceResources.containsKey(pathWithContextAndParams)) {
                return mockServiceResources.get(pathWithContextAndParams);
            } else if (mockServiceResources.containsKey(pathWithContextAndParams + URL_PATH_SEPARATOR)) {
                return mockServiceResources.get(pathWithContextAndParams + URL_PATH_SEPARATOR);
            }
        } catch (URISyntaxException e) {
            // ignore the exception
        }
        return endpointUrl;
    }
}

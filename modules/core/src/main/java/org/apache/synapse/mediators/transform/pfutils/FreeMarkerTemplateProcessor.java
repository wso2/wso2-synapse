/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */


package org.apache.synapse.mediators.transform.pfutils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.transform.ArgumentDetails;
import org.apache.synapse.util.PayloadHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import static org.apache.synapse.mediators.transform.pfutils.Constants.ARGS_INJECTING_NAME;
import static org.apache.synapse.mediators.transform.pfutils.Constants.ARGS_INJECTING_PREFIX;
import static org.apache.synapse.mediators.transform.pfutils.Constants.AXIS2_PROPERTY_INJECTING_NAME;
import static org.apache.synapse.mediators.transform.pfutils.Constants.CTX_PROPERTY_INJECTING_NAME;
import static org.apache.synapse.mediators.transform.pfutils.Constants.JSON_PAYLOAD_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.NOT_SUPPORTING_PAYLOAD_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.PAYLOAD_INJECTING_NAME;
import static org.apache.synapse.mediators.transform.pfutils.Constants.TEXT_PAYLOAD_TYPE;
import static org.apache.synapse.mediators.transform.pfutils.Constants.TRANSPORT_PROPERTY_INJECTING_NAME;
import static org.apache.synapse.mediators.transform.pfutils.Constants.XML_PAYLOAD_TYPE;
import static org.apache.synapse.util.PayloadHelper.TEXTELT;
import static org.apache.synapse.util.PayloadHelper.getXMLPayload;

public class FreeMarkerTemplateProcessor extends TemplateProcessor {

    private final Configuration cfg;
    private final Gson gson;
    private Template freeMarkerTemplate;

    private boolean usingPayload;
    private boolean usingPropertyCtx;
    private boolean usingPropertyAxis2;
    private boolean usingPropertyTransport;
    private boolean usingArgs;

    private static final Log log = LogFactory.getLog(FreeMarkerTemplateProcessor.class);

    public FreeMarkerTemplateProcessor() {

        cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);

        gson = new Gson();
    }

    @Override
    public void init() {

        compileFreeMarkerTemplate(getFormat(), getMediaType());
    }

    @Override
    public String processTemplate(String template, String mediaType, MessageContext messageContext) {

        try {

            Map<String, Object> data = new HashMap<>();
            int payloadType = getPayloadType(messageContext);

            injectPayloadVariables(messageContext, payloadType, data);
            injectArgs(messageContext, mediaType, data);
            injectProperties(messageContext, data);

            Writer out = new StringWriter();
            freeMarkerTemplate.process(data, out);
            return out.toString();
        } catch (IOException e) {
            handleException("Error parsing FreeMarker template");
        } catch (TemplateException e) {
            handleException(generateTemplateErrorMessage(e));
        } catch (SAXException | ParserConfigurationException e) {
            handleException("Error reading payload data");
        }

        return "";
    }

    private String generateTemplateErrorMessage(TemplateException e) {

        if (log.isDebugEnabled()) {
            return e.getMessageWithoutStackTop();
        } else {
            return "Error parsing FreeMarker template, " +
                    "Syntax error or invalid reference : " +
                    e.getBlamedExpressionString() +
                    " At line: " +
                    e.getLineNumber() +
                    " column: " +
                    e.getColumnNumber();
        }

    }

    private void compileFreeMarkerTemplate(String templateString, String mediaType) {

        try {
            if (XML_TYPE.equals(mediaType)) {
                templateString = "<pfPadding>" + templateString + "</pfPadding>";
            }

            freeMarkerTemplate = new Template("synapse-template", templateString, cfg);
            findRequiredInjections(templateString);
        } catch (IOException e) {
            handleException("Error compiling FreeMarking template : " + e.getMessage());
        }
    }

    private void findRequiredInjections(String templateString) {

        usingPayload = templateString.contains(PAYLOAD_INJECTING_NAME);
        usingArgs = templateString.contains(ARGS_INJECTING_NAME);
        usingPropertyCtx = templateString.contains(CTX_PROPERTY_INJECTING_NAME);
        usingPropertyAxis2 = templateString.contains(AXIS2_PROPERTY_INJECTING_NAME);
        usingPropertyTransport = templateString.contains(TRANSPORT_PROPERTY_INJECTING_NAME);
    }

    /**
     * Inject argument values to freemarker
     *
     * @param messageContext Message context
     * @param mediaType      Output media type
     * @param data           Freemarker data input
     */
    private void injectArgs(MessageContext messageContext, String mediaType, Map<String, Object> data) {

        if (usingArgs) {
            Map<String, Object> argsValues = new HashMap<>();
            Map<String, ArgumentDetails>[] argValues = getArgValues(mediaType, messageContext);
            for (int i = 0; i < argValues.length; i++) {
                Map<String, ArgumentDetails> argValue = argValues[i];
                Map.Entry<String, ArgumentDetails> argumentDetailsEntry = argValue.entrySet().iterator().next();
                String replacementValue = prepareReplacementValue(mediaType, messageContext, argumentDetailsEntry);
                argsValues.put(ARGS_INJECTING_PREFIX + (i + 1), replacementValue);
            }
            data.put(ARGS_INJECTING_NAME, argsValues);
        }
    }

    /**
     * Inject  payload in to FreeMarker
     *
     * @param messageContext MessageContext
     * @param payloadType    Input payload type
     * @param data           FreeMarker data input
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void injectPayloadVariables(MessageContext messageContext, int payloadType, Map<String, Object> data)
            throws SAXException, IOException, ParserConfigurationException {

        if (usingPayload) {
            if (payloadType == XML_PAYLOAD_TYPE) {
                injectXmlPayload(messageContext, data);
            } else if (payloadType == JSON_PAYLOAD_TYPE) {
                injectJsonPayload((Axis2MessageContext) messageContext, data);
            } else if (payloadType == TEXT_PAYLOAD_TYPE) {
                injectTextPayload(messageContext, data);
            } else {
                data.put(PAYLOAD_INJECTING_NAME, "");
            }
        }
    }

    /**
     * Inject a JSON payload in the FreeMarker
     *
     * @param messageContext Message context
     * @param data           FreeMarker data input
     */
    private void injectJsonPayload(Axis2MessageContext messageContext, Map<String, Object> data) {

        org.apache.axis2.context.MessageContext axis2MessageContext = messageContext.getAxis2MessageContext();
        String jsonPayloadString = JsonUtil.jsonPayloadToString(axis2MessageContext);
        if (JsonUtil.hasAJsonObject(axis2MessageContext)) {
            injectJsonObject(data, jsonPayloadString);
        } else {
            injectJsonArray(data, jsonPayloadString);
        }
    }

    /**
     * Inject a JSON array in to FreeMarker
     *
     * @param data              FreeMarker data input
     * @param jsonPayloadString JSON payload string
     */
    private void injectJsonArray(Map<String, Object> data, String jsonPayloadString) {

        List<Object> array;
        Type type = new TypeToken<List<String>>() {
        }.getType();
        array = gson.fromJson(jsonPayloadString, type);
        data.put(PAYLOAD_INJECTING_NAME, array);
    }

    /**
     * Inject a JSON object in to FreeMarker
     *
     * @param data              FreeMarker data input
     * @param jsonPayloadString JSON payload string
     */
    private void injectJsonObject(Map<String, Object> data, String jsonPayloadString) {

        Map<String, Object> map;
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        map = gson.fromJson(jsonPayloadString, type);
        data.put(PAYLOAD_INJECTING_NAME, map);
    }

    /**
     * Inject an XML payload in to FreeMarker
     *
     * @param messageContext Message context
     * @param data           FreeMarker data input
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void injectXmlPayload(MessageContext messageContext, Map<String, Object> data)
            throws SAXException, IOException, ParserConfigurationException {

        data.put(PAYLOAD_INJECTING_NAME, freemarker.ext.dom.NodeModel.parse(
                new InputSource(new StringReader(
                        messageContext.getEnvelope().getBody().getFirstElement().toString()))));
    }

    /**
     * Inject an Text payload in to FreeMarker
     *
     * @param messageContext Message context
     * @param data           FreeMarker data input
     */
    private void injectTextPayload(MessageContext messageContext, Map<String, Object> data) {

        String textPayload;
        OMElement el = getXMLPayload(messageContext.getEnvelope());
        if (el == null || !el.getQName().equals(TEXTELT)) {
            textPayload = "";
        } else {
            textPayload = getTextValue(el);
        }

        data.put(PAYLOAD_INJECTING_NAME, textPayload);
    }

    /**
     * Get text value from OMNode
     *
     * @param node OMNode to get text value
     * @return text value of the node
     */
    private String getTextValue(OMNode node) {

        switch (node.getType()) {
            case OMNode.ELEMENT_NODE:
                StringBuilder sb = new StringBuilder();
                Iterator<OMNode> children = ((OMElement) node).getChildren();
                while (children.hasNext()) {
                    sb.append(getTextValue(children.next()));
                }
                return sb.toString();
            case OMNode.TEXT_NODE:
                String text = ((OMText) node).getText();
                return StringEscapeUtils.escapeXml11(text);
            default:
                return "";
        }
    }

    private void injectProperties(MessageContext synCtx, Map<String, Object> data) {

        injectCtxProperties(synCtx, data);
        injectAxis2Properties(synCtx, data);
        injectTransportProperties(synCtx, data);
    }

    private void injectCtxProperties(MessageContext synCtx, Map<String, Object> data) {

        if (usingPropertyCtx) {
            Map<String, String> properties = new HashMap<>();
            Map<String, Object> propertyMap = ((Axis2MessageContext) synCtx).getProperties();

            for (Map.Entry<String, Object> propertyEntry : propertyMap.entrySet()) {
                String propertyKey = propertyEntry.getKey();
                Object propertyValue = propertyEntry.getValue();
                if (propertyValue != null) {
                    properties.put(propertyKey, propertyValue.toString());
                }
            }

            data.put(CTX_PROPERTY_INJECTING_NAME, properties);
        }
    }

    private void injectAxis2Properties(MessageContext synCtx, Map<String, Object> data) {

        if (usingPropertyAxis2) {
            Map<String, String> properties = new HashMap<>();
            org.apache.axis2.context.MessageContext axis2MessageContext
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Iterator<String> propertyNames =
                    axis2MessageContext.getPropertyNames(); // note :: getProperties() in axis2 
            // message context is deprecated 
            while (propertyNames.hasNext()) {
                String propertyName = propertyNames.next();
                Object propertyValue = axis2MessageContext.getProperty(propertyName);
                if (propertyValue != null) {
                    properties.put(propertyName, propertyValue.toString());
                }
            }
            data.put(AXIS2_PROPERTY_INJECTING_NAME, properties);
        }
    }

    private void injectTransportProperties(MessageContext synCtx, Map<String, Object> data) {

        if (usingPropertyTransport) {
            Map<String, String> properties = new HashMap<>();
            org.apache.axis2.context.MessageContext axis2MessageContext
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Object headers = axis2MessageContext.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                for (Object propertyEntry : headersMap.entrySet()) {
                    if (propertyEntry instanceof Map.Entry) {
                        Map.Entry entry = (Map.Entry) propertyEntry;
                        String propertyKey = entry.getKey().toString();
                        Object propertyValue = entry.getValue();
                        if (propertyValue != null) {
                            properties.put(propertyKey, propertyValue.toString());
                        }
                    }

                }
            }
            data.put(TRANSPORT_PROPERTY_INJECTING_NAME, properties);
        }
    }

    /**
     * Get the input payload type
     *
     * @param messageContext Message context
     * @return Type of the input paylaod
     */
    private int getPayloadType(MessageContext messageContext) {

        int payloadType = 0;
        try {
            payloadType = PayloadHelper.getPayloadType(messageContext);
        } catch (NullPointerException e) {
            return NOT_SUPPORTING_PAYLOAD_TYPE;
        }

        if (payloadType == PayloadHelper.XMLPAYLOADTYPE) {
            if (JsonUtil.hasAJsonPayload(((Axis2MessageContext) messageContext).getAxis2MessageContext())) {
                return JSON_PAYLOAD_TYPE;
            }  else {
                return XML_PAYLOAD_TYPE;
            }
        } else if (payloadType == PayloadHelper.TEXTPAYLOADTYPE) {
            return TEXT_PAYLOAD_TYPE;
        }

        return NOT_SUPPORTING_PAYLOAD_TYPE;
    }
}

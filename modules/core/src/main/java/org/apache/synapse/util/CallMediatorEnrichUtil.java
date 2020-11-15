/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.MediatorLog;
import org.apache.synapse.mediators.elementary.EnrichMediator;
import org.apache.synapse.mediators.elementary.Source;
import org.apache.synapse.mediators.elementary.Target;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Map;

public class CallMediatorEnrichUtil {

    public static final String CUSTOM = "custom";
    public static final String PROPERTY = "property";
    public static final String ENVELOPE = "envelope";
    public static final String BODY = "body";
    public static final String INLINE = "inline";

    public static final String JSON_TYPE = "application/json";
    public static final String TEXT_TYPE = "text/plain";
    private final static QName TEXT_ELEMENT = new QName("http://ws.apache.org/commons/ns/payload", "text");

    public static final Log log = LogFactory.getLog(CallMediatorEnrichUtil.class);

    public static int convertTypeToInt(String type) {
        if (type.equals(ENVELOPE)) {
            return EnrichMediator.ENVELOPE;
        } else if (type.equals(BODY)) {
            return EnrichMediator.BODY;
        } else if (type.equals(PROPERTY)) {
            return EnrichMediator.PROPERTY;
        } else if (type.equals(CUSTOM)) {
            return EnrichMediator.CUSTOM;
        } else if (type.equals(INLINE)) {
            return EnrichMediator.INLINE;
        }
        return -1;
    }

    public static void doEnrich(MessageContext synCtx, Source source, Target target,
                                String sourceContentType) {
        Object sourceProperty = synCtx.getProperty(source.getProperty());
        JsonParser jsonParser = new JsonParser();

        //A JsonElement to store the parsed json object, so that it can be used without parsing the string continuously
        JsonElement sourcePropertyJson = getJsonElement(sourceProperty, jsonParser);
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext)synCtx).getAxis2MessageContext();

        if (JSON_TYPE.equals(sourceContentType)) {
            Object sourceNode;
            try {
                sourceNode = source.evaluateJson(synCtx, getLog(synCtx), sourcePropertyJson);
                if (sourceNode == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insertJson(synCtx, sourceNode, getLog(synCtx));
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }

        } else if (TEXT_TYPE.equals(sourceContentType)) {
            String payload = null;
            if (source.getSourceType() == EnrichMediator.CUSTOM) {
                ArrayList<OMNode> sourceNodeList;
                try {
                    sourceNodeList = source.evaluate(synCtx, getLog(synCtx));
                    if (sourceNodeList.get(0) instanceof OMText) {
                        payload = ((OMText) sourceNodeList.get(0)).getText();
                        enrichTextToBody(axis2MessageContext, payload);
                    } else {
                        handleException("Custum path value must be a string for text/plain", synCtx);
                    }
                } catch (JaxenException e) {
                    handleException("Failed to get the source for Enriching", e, synCtx);
                }
            } else if (source.getSourceType() == EnrichMediator.PROPERTY) {
                Object sourcePropertyValue = synCtx.getProperty(source.getProperty());
                if (sourcePropertyValue instanceof String) {
                    enrichTextToBody(axis2MessageContext, (String) sourcePropertyValue);
                } else {
                    handleException("Property value must be a string for text/plain", synCtx);
                }

            } else if (source.getSourceType() == EnrichMediator.INLINE) {
                if (source.getInlineOMNode() instanceof OMText) {
                    payload = ((OMTextImpl) source.getInlineOMNode()).getText();
                    enrichTextToBody(axis2MessageContext, payload);
                } else {
                    handleException("Inline value must be a string for text/plain", synCtx);
                }

            } else {
                ArrayList<OMNode> sourceNodeList;
                String textValue = null;
                try {
                    sourceNodeList = source.evaluate(synCtx, getLog(synCtx));
                    textValue = ((OMElement) sourceNodeList.get(0)).getText();
                } catch (JaxenException e) {
                    handleException("Failed to get the source for Enriching", e, synCtx);
                }
                if (textValue != null) {
                    synCtx.setProperty(target.getProperty(), textValue);
                } else {
                    handleException("text/plain must contain a text value", synCtx);
                }
            }
        } else {
            ArrayList<OMNode> sourceNodeList;
            try {
                sourceNodeList = source.evaluate(synCtx, getLog(synCtx));
                if (sourceNodeList == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insert(synCtx, sourceNodeList, getLog(synCtx));
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        }
    }

    public static void enrichTextToBody(org.apache.axis2.context.MessageContext axis2MessageContext, String content) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement textElement = factory.createOMElement(TEXT_ELEMENT);
        if (content == null) {
            content = "";
        }
        textElement.setText(content);
        SOAPEnvelope env = axis2MessageContext.getEnvelope();
        SOAPBody body = env.getBody();

        OMElement e = body.getFirstElement();
        e.insertSiblingBefore(textElement);
        e.detach();
    }

    public static JsonElement getJsonElement(Object sourceProperty, JsonParser jsonParser) {
        JsonElement sourcePropertyJson = null;
        if (sourceProperty instanceof OMElement) {
        } else if (sourceProperty instanceof ArrayList) {
            for (Object node : (ArrayList) sourceProperty) {
                if (node instanceof OMText) {
                    String propertyString = ((OMTextImpl) node).getText();
                    try {
                        sourcePropertyJson = jsonParser.parse(propertyString);
                        if (!(sourcePropertyJson instanceof JsonObject || sourcePropertyJson instanceof JsonArray)) {
                            break;
                        }
                    } catch (JsonSyntaxException e) {
                    }
                } else if (node instanceof OMElement) {
                    break;
                }
            }
        } else if (sourceProperty instanceof String) {
            try {
                sourcePropertyJson = jsonParser.parse((String) sourceProperty);
                if (!(sourcePropertyJson instanceof JsonObject || sourcePropertyJson instanceof JsonArray
                        || sourcePropertyJson instanceof JsonPrimitive)) {
                }
            } catch (JsonSyntaxException e) {
            }
        }
        return sourcePropertyJson;
    }

    public static void preservetransportHeaders(MessageContext synMsgCtx, Map originalTransportHeaders) {
        ((Axis2MessageContext) synMsgCtx).getAxis2MessageContext()
                                         .removeProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        ((Axis2MessageContext) synMsgCtx).getAxis2MessageContext()
                                         .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                        originalTransportHeaders);
    }

    public static void setContentType(MessageContext synCtx, String targetMessageType, String targetContentType) {
        org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, targetMessageType);
        a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, targetContentType);
        handleTransportHeaders(targetContentType, a2mc);

    }

    public static void handleTransportHeaders(Object resultValue,
                                         org.apache.axis2.context.MessageContext axis2MessageCtx) {
        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map headers = (Map) o;
        if (headers != null) {
            headers.remove(HTTP.CONTENT_TYPE);
            headers.put(HTTP.CONTENT_TYPE, resultValue);
        }
    }

    public static void buildMessage(MessageContext synCtx) {
        try {
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
        } catch (Exception e) {
            handleException("Error while building message. " + e.getMessage(), e, synCtx);
        }
    }

    public static Source createSourceWithProperty(String propertyName) {
        Source source = new Source();
        source.setSourceType(CallMediatorEnrichUtil.convertTypeToInt("property"));
        source.setProperty(propertyName);
        source.setClone(false);
        return source;
    }

    public static Source createSourceWithBody() {
        Source source = new Source();
        source.setClone(false);
        source.setSourceType(CallMediatorEnrichUtil.convertTypeToInt("body"));
        return source;
    }
    public static Target createTargetWithProperty(String propertyName) {
        Target target = new Target();
        target.setTargetType(CallMediatorEnrichUtil.convertTypeToInt("property"));
        target.setProperty(propertyName);
        target.setAction("replace");
        return target;
    }

    public static Target createTargetWithBody() {
        Target target = new Target();
        target.setAction("replace");
        target.setTargetType(CallMediatorEnrichUtil.convertTypeToInt("body"));
        return target;
    }

    public static void handleException(String msg, Exception e, MessageContext msgContext) {
        log.error(msg, e);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg, e);
        }
        throw new SynapseException(msg, e);
    }

    public static void handleException(String msg, MessageContext msgContext) {
        log.error(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        throw new SynapseException(msg);
    }

    public static SynapseLog getLog(MessageContext synCtx) {
        return new MediatorLog(log, false, synCtx);
    }

}

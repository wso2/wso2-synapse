/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.mediators.elementary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.util.InlineExpressionUtil;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;

/**
 * Syntax for EnrichMediator
 * <p/>
 * <enrich>
 * <source [clone=true | false] type=[custom|envelope|body|property] xpath="" property=""/>
 * <target [replace=true | false] type=[custom|envelope|body|property] xpath="" property=""/>
 * </enrich>
 * <p/>
 * This mediator will first get an OMElement from the source. Then put it to the current message
 * according to the target element.
 * <p/>
 * Both target and source can specify a type. These are the types supported
 * <p/>
 * custom : xpath expression should be provided to get the xml
 * envelope : the soap envelope
 * body : first child of the soap body
 * property : synapse property
 * <p/>
 * When specifying the source one can clone the xml by setting the clone to true. The default
 * value for clone is false.
 * <p/>
 * When specifying the target one can replace the existing xml. replace is only valid for custom
 * and body types. By default replace is true.
 */

public class EnrichMediator extends AbstractMediator {
    public static final int CUSTOM = 0;

    public static final int ENVELOPE = 1;

    public static final int BODY = 2;

    public static final int PROPERTY = 3;

    public static final int INLINE = 4;

    public static final int KEY = 5;

    public static final int VARIABLE = 6;

    private Source source = null;

    private Target target = null;

    private boolean isNativeJsonSupportEnabled = false;

    public static final String ACTION_REMOVE = "remove";

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Enrich mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        JsonParser jsonParser = new JsonParser();

        //A JsonElement to store the parsed json object, so that it can be used without parsing the string continuously
        JsonElement sourcePropertyJson = null;

        /*
        * Here we are checking whether the source Property is of XML type,
        * so that we can decide on the flow (Jsonpath or xpath)
        * A value stored in a property can be of type ArrayList, OMElement or String
        */
        boolean isSourcePropertyXML = false;
        Object sourceProperty = synCtx.getProperty(source.getProperty());
        if (sourceProperty instanceof JsonElement) {
            // Handle JSON type property values
            sourcePropertyJson = (JsonElement) sourceProperty;
            isSourcePropertyXML = false;
        } else if (sourceProperty instanceof OMElement) {
            isSourcePropertyXML = true;
        } else if (sourceProperty instanceof ArrayList) {
            for (Object node : (ArrayList) sourceProperty) {
                if (node instanceof OMText) {
                    String propertyString = ((OMTextImpl) node).getText();
                    try {
                        sourcePropertyJson = jsonParser.parse(propertyString);
                        if (!(sourcePropertyJson instanceof JsonObject || sourcePropertyJson instanceof JsonArray)) {
                            isSourcePropertyXML = true;
                            break;
                        }
                    } catch (JsonSyntaxException e) {
                        synLog.traceOrDebug("Source is not a valid json");
                        isSourcePropertyXML = true;
                    }
                } else if (node instanceof OMElement) {
                    isSourcePropertyXML = true;
                    break;
                }
            }
        } else if (sourceProperty instanceof String) {
            try {
                sourcePropertyJson = jsonParser.parse((String) sourceProperty);
                if (!(sourcePropertyJson instanceof JsonObject || sourcePropertyJson instanceof JsonArray
                        || sourcePropertyJson instanceof JsonPrimitive)) {
                    isSourcePropertyXML = true;
                }
            } catch (JsonSyntaxException e) {
                try {
                    // Enclosing using quotes due to the following issue
                    // https://github.com/google/gson/issues/1286
                    String enclosedSourceProperty = "\"" + sourceProperty + "\"";
                    sourcePropertyJson = jsonParser.parse(enclosedSourceProperty);
                    if (!(sourcePropertyJson instanceof JsonObject || sourcePropertyJson instanceof JsonArray
                            || sourcePropertyJson instanceof JsonPrimitive)) {
                        isSourcePropertyXML = true;
                    }
                } catch (JsonSyntaxException ex) {
                    synLog.traceOrDebug("Source string is not a valid json");
                    isSourcePropertyXML = true;
                }
            }
        }

        // If the inline text contains expressions, we need to evaluate the values
        // and decide on the type (whether XML or JSON)
        boolean isInlineTextXML = !isNativeJsonSupportEnabled;
        OMNode inlineOMNodeWithValues = null;
        String inlineString = null;

        if (source.containsInlineExpressions()) {
            OMNode inlineOMNode = source.getInlineOMNode();
            if (inlineOMNode != null) {
                try {
                    if (inlineOMNode instanceof OMText) {
                        // If the node type is text, it can either be a JSON string or an expression
                        // If it is an expression we must check again after resolving the expressions
                        // whether it is XML or JSON
                        inlineString = InlineExpressionUtil.replaceDynamicValues(synCtx,
                                ((OMTextImpl) inlineOMNode).getText());
                        // After the expressions in the inline text is replaced with the value,
                        // the string must be parsed again to identify whether it has changed to a XML
                        inlineOMNodeWithValues = AXIOMUtil.stringToOM(inlineString);
                        isInlineTextXML = true;
                    } else if (inlineOMNode instanceof OMElement) {
                        inlineString = InlineExpressionUtil.replaceDynamicValues(synCtx, inlineOMNode.toString());
                        inlineOMNodeWithValues = AXIOMUtil.stringToOM(inlineString);
                        isInlineTextXML = true;
                    }
                } catch (XMLStreamException | OMException e){
                    // The string is considered as a text / JSON
                    inlineOMNodeWithValues = OMAbstractFactory.getOMFactory().createOMText(inlineString);
                }
            }
        }

        boolean hasJSONPayload = JsonUtil.hasAJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        if (isNativeJsonSupportEnabled && hasJSONPayload && !isSourcePropertyXML && !isInlineTextXML) {
            // handling the remove action separately
            if (target.getAction().equals(ACTION_REMOVE)) {
                try {
                    if (source.getXpath() != null && source.getXpath() instanceof SynapseJsonPath) {
                        if (target.getTargetType() == BODY) {
                            target.removeJsonFromBody(synCtx, source.getXpath());
                        } else if (target.getTargetType() == PROPERTY) {
                            target.removeJsonFromProperty(synCtx, target.getProperty(), source.getXpath());
                        } else {
                            handleException("Target type " + target.getTargetType() + " is invalid for the remove " +
                                    "action", synCtx);
                        }
                    } else {
                        handleException("source Xpath is mandatory for the Remove action", synCtx);
                    }
                } catch (IOException | PathNotFoundException e) {
                    handleException("Error occurred while executing the action : remove", e, synCtx);
                }
            } else {
                Object sourceNode;
                try {
                    sourceNode = source.evaluateJson(synCtx, synLog, sourcePropertyJson, inlineOMNodeWithValues);
                    if (sourceNode == null) {
                        handleException("Failed to get the source for Enriching : ", synCtx);
                    } else if (target.getTargetType() == KEY) {
                        try {
                            if (sourceNode instanceof JsonPrimitive) {
                                String jsonPathString = target.getXpath().toString();
                                // removing "json-eval(" and extract only the expression
                                jsonPathString = jsonPathString.substring(10, jsonPathString.length() - 1);
                                // json-eval expression will contain the key name as the last token in the string
                                // e.g.: $.user.name where name is the key name
                                // and $.user is the json path to locate the key.
                                int lastIndex = jsonPathString.lastIndexOf(".");
                                String jsonPath = jsonPathString.substring(0, lastIndex);
                                String keyName = jsonPathString.substring(lastIndex + 1);
                                target.renameKey(synCtx, jsonPath, keyName, ((JsonPrimitive)sourceNode).getAsString());
                            } else {
                                handleException("Failed to get the new key name from source for Enriching. " +
                                        "Key name must be a string.", synCtx);
                            }
                        } catch (IOException e) {
                            handleException("Failed to rename the key.", e, synCtx);
                        }
                    } else {
                        target.insertJson(synCtx, sourceNode, synLog);
                    }
                } catch (JaxenException e) {
                    handleException("Failed to get the source for Enriching", e, synCtx);
                }
            }
        } else {
            // TODO implement target action "remove" for XML
            ArrayList<OMNode> sourceNodeList;
            try {
                sourceNodeList = source.evaluate(synCtx, synLog, inlineOMNodeWithValues);
                if (sourceNodeList == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insert(synCtx, sourceNodeList, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        }

        //  If we enrich the body or envelope we need to remove the NO_ENTITY_BODY property
        //  Related issue https://github.com/wso2/product-ei/issues/3586
        if (target.getTargetType() == EnrichMediator.BODY || target.getTargetType() == EnrichMediator.ENVELOPE) {
            axis2MsgCtx.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
        }

        synLog.traceOrDebug("End : Enrich mediator");
        return true;
    }

    /**
     * Sets the dynamic value resolved inline text in the source
     *
     * @param messageContext              Message Context
     * @param inlineStringWithExpressions Inline String
     * @return true if the inline text is XML, false otherwise
     */
    private boolean setDynamicValuesInNode(MessageContext messageContext, String inlineStringWithExpressions) {

        boolean isInlineTextXML = false;
        String inlineString = InlineExpressionUtil.replaceDynamicValues(messageContext, inlineStringWithExpressions);
        try {
            // After the expressions in the inline text is replaced with the value, the string must be parsed
            // again to identify whether it has changed to a XML
            OMNode inlineOMNode = AXIOMUtil.stringToOM(inlineString);
            // serialize inlineOMNode
            inlineOMNode.buildWithAttachments();
            source.setInlineOMNode(inlineOMNode);
            isInlineTextXML = true;
        } catch (XMLStreamException | OMException e) {
            // The string is considered as a text / JSON
            source.setInlineOMNode(OMAbstractFactory.getOMFactory().createOMText(inlineString));
        }
        return isInlineTextXML;
    }

    public Source getSource() {
        return source;
    }

    public Target getTarget() {
        return target;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    @Override
    public boolean isContentAltering() {
        return true;
    }

    public void setNativeJsonSupportEnabled(boolean nativeJsonSupportEnabled) {
        isNativeJsonSupportEnabled = nativeJsonSupportEnabled;
    }

}

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
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.Constants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.jaxen.JaxenException;

import java.util.ArrayList;

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

    private Source source = null;

    private Target target = null;

    private boolean isNativeJsonSupportEnabled = false;

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
        if (sourceProperty instanceof OMElement) {
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
                synLog.traceOrDebug("Source string is not a valid json");
                isSourcePropertyXML = true;
            }
        }


        boolean hasJSONPayload = JsonUtil.hasAJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        if (isNativeJsonSupportEnabled && hasJSONPayload && !isSourcePropertyXML) {
            Object sourceNode;
            try {
                sourceNode = source.evaluateJson(synCtx, synLog, sourcePropertyJson);
                if (sourceNode == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insertJson(synCtx, sourceNode, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }
        } else {
            ArrayList<OMNode> sourceNodeList;
            try {
                sourceNodeList = source.evaluate(synCtx, synLog);
                if (sourceNodeList == null) {
                    handleException("Failed to get the source for Enriching : ", synCtx);
                } else {
                    target.insert(synCtx, sourceNodeList, synLog);
                }
            } catch (JaxenException e) {
                handleException("Failed to get the source for Enriching", e, synCtx);
            }

            // Removing the JSON stream since the payload is now updated.
            // Json-eval and other JsonUtil functions now needs to convert XML -> JSON
            // related to wso2/product-ei/issues/1771
            if (target.getTargetType() == EnrichMediator.BODY || target.getTargetType() == EnrichMediator.CUSTOM) {
                axis2MsgCtx.removeProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
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

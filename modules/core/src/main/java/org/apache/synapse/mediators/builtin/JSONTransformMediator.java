/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.parser.JsonProcessor;
import org.apache.synapse.commons.staxon.core.json.JsonXMLOutputFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A mediator to transform XML payload to JSON or JSON to JSON based on a json schema
 * <p/>
 * Via this mediator we can override the global properties which are used in XML to JSON transformations.
 * These properties can be used individually for each artifact.
 */
public class JSONTransformMediator extends AbstractMediator {
    private Value schemaKey = null;
    /**
     * The holder for the custom properties
     */
    private List<MediatorProperty> propertiesArrayList = new ArrayList<>();
    private JsonXMLOutputFactory jsonOutputFactory;

    @Override
    public boolean mediate(MessageContext synCtx) {
        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }
        SynapseLog synLog = getLog(synCtx);
        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : JSONTransform mediator");
            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (!propertiesArrayList.isEmpty()) {
            try {
                //Passing the the custom jsonOutputFactory and converting the Envelope body to JSON
                String jsonPayloadFromOMElement = JsonUtil.
                        toJsonString(((Axis2MessageContext) synCtx).getAxis2MessageContext().
                                getEnvelope().getBody().getFirstElement(), jsonOutputFactory).toString();
                //Update the jsonstream with the newly build payload
                JsonUtil.getNewJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(),
                        jsonPayloadFromOMElement, true, true);
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("JSON stream after converting xml to json : " + jsonPayloadFromOMElement);
                }
            } catch (AxisFault af) {
                handleException("Axisfault occured when updating the " +
                        "JSON stream after applying the properties: ", af, synCtx);
            }
        }
        if (schemaKey != null) {
            // Derive actual key from message context
            String generatedSchemaKey = schemaKey.evaluateValue(synCtx);
            Object jsonSchemaObj = synCtx.getEntry(generatedSchemaKey);
            if (jsonSchemaObj != null) {
                String schema = "";
                if (jsonSchemaObj instanceof OMTextImpl) {
                    schema = ((OMTextImpl) jsonSchemaObj).getText();
                } else if (jsonSchemaObj instanceof String) {
                    schema = (String) jsonSchemaObj;
                } else {
                    handleException("Can not find valid JSON Schema content", synCtx);
                }
                try {
                    String jsonPayload;
                    if (JsonUtil.hasAJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext())) {
                        jsonPayload =
                                JsonUtil.jsonPayloadToString(((Axis2MessageContext) synCtx).getAxis2MessageContext());
                    } else {
                        jsonPayload = JsonUtil.toJsonString(((Axis2MessageContext) synCtx).getAxis2MessageContext()
                                .getEnvelope().getBody().getFirstElement()).toString();
                    }
                    String result = JsonProcessor.parseJson(jsonPayload, schema);
                    JsonUtil.getNewJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(),
                            result, true, true);
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("JSON stream after applying schema : " +
                                ((result != null) ? result : ""));
                    }
                } catch (ValidatorException | ParserException e) {
                    handleException(e.getMessage(), e, synCtx);
                } catch (AxisFault af) {
                    handleException("Axisfault fault occured when updating the " +
                            "JSON stream after applying the JSON schema", af, synCtx);
                }
            } else {
                handleException("Schema does not exist in the specified location : " + generatedSchemaKey, synCtx);
            }
        }
        synLog.traceOrDebug("End : JSON Transform mediator");
        return true;
    }

    public Value getSchemaKey() {
        return schemaKey;
    }

    public void setSchemaKey(Value schemaKey) {
        this.schemaKey = schemaKey;
    }

    public void addAllProperties(List<MediatorProperty> list) {
        this.propertiesArrayList = list;
        Properties properties = new Properties();
        for (MediatorProperty prop : list) {
            properties.setProperty(prop.getName(), prop.getValue());
        }
        this.jsonOutputFactory = JsonUtil.generateJSONOutputFactoryWithOveride(properties);
    }

    public List<MediatorProperty> getProperties() {
        return propertiesArrayList;
    }
}

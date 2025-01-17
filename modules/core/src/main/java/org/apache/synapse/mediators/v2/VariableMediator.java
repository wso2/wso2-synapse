/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.util.JavaUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.Set;

/**
 * The variable mediator save or remove a named variable in the Synapse Message Context.
 */
public class VariableMediator extends AbstractMediator {

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;
    private String name = null;
    private SynapsePath expression = null;
    private Object value = null;
    private String type = null;
    private int action = ACTION_SET;

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Variable mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        String name = this.name;
        if (action == ACTION_SET) {

            Object resultValue = getResultValue(synCtx);

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Setting variable : " + name + " to : " + resultValue);
            }

            if (resultValue instanceof OMElement) {
                ((OMElement) resultValue).build();
            }

            synCtx.setVariable(name, resultValue);

        } else {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Removing variable : " + name);
            }
            Set variableKeySet = synCtx.getVariableKeySet();
            if (variableKeySet != null) {
                variableKeySet.remove(name);
            }
        }
        synLog.traceOrDebug("End : Variable mediator");

        return true;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public Object getValue() {

        return value;
    }

    public void setValue(String value) {

        setValue(value, null);
    }

    /**
     * Set the value to be set by this variable mediator and the data type to be used when setting the value.
     * Accepted type names are defined in XMLConfigConstants.VARIABLE_DATA_TYPES enumeration. Passing null as the type
     * implies that 'STRING' type should be used.
     *
     * @param value the value to be set as a string
     * @param type  the type name
     */
    public void setValue(String value, String type) {

        this.type = type;
        this.value = convertValue(value, type);
    }

    public String getType() {

        return type;
    }

    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {

        CloseEventCollector
                .closeEntryEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR, currentIndex,
                        isContentAltering());
    }

    public int getAction() {

        return action;
    }

    public void setAction(int action) {

        this.action = action;
    }

    public SynapsePath getExpression() {

        return expression;
    }

    public void setExpression(SynapsePath expression, String type) {

        this.expression = expression;
        this.type = type;
    }

    private Object getResultValue(MessageContext synCtx) {

        if (value != null) {
            return value;
        } else {
            if (expression != null) {
                if (isXMLType(type)) {
                    return buildOMElement(expression.stringValueOf(synCtx));
                } else if (isStringType(type)) {
                    return expression.stringValueOf(synCtx);
                }
                return convertExpressionResult(expression.objectValueOf(synCtx), type);
            }
        }
        return null;
    }

    private boolean isXMLType(String type) {

        return type != null && XMLConfigConstants.VARIABLE_DATA_TYPES.XML.equals(XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type));
    }

    private boolean isStringType(String type) {

        return type != null && XMLConfigConstants.VARIABLE_DATA_TYPES.STRING.equals(XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type));
    }

    private Object convertValue(String value, String type) {

        if (type == null) {
            return value;
        }

        try {
            XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
            switch (dataType) {
                case BOOLEAN:
                    return JavaUtils.isTrueExplicitly(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case INTEGER:
                    return Integer.parseInt(value);
                case LONG:
                    return Long.parseLong(value);
                case XML:
                    return buildOMElement(value);
                case JSON:
                    return buildJSONElement(value);
                default:
                    return value;
            }
        } catch (IllegalArgumentException e) {
            String msg = "Unknown type : " + type + " for the variable mediator or the " +
                    "variable value cannot be converted into the specified type.";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    /**
     * Convert the evaluated value to the expected data type.
     *
     * @param evaluatedValue Evaluated value to be converted
     * @param type           Expected data type
     * @return Converted value
     */
    private Object convertExpressionResult(Object evaluatedValue, String type) {

        if (type == null) {
            return evaluatedValue;
        }

        if (evaluatedValue instanceof JsonPrimitive) {
            return convertJsonPrimitive((JsonPrimitive) evaluatedValue, type);
        }

        XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
        switch (dataType) {
            case BOOLEAN:
                if (!(evaluatedValue instanceof Boolean)) {
                    handleDataTypeException("BOOLEAN");
                }
                break;
            case DOUBLE:
                if (!(evaluatedValue instanceof Double)) {
                    handleDataTypeException("DOUBLE");
                }
                break;
            case INTEGER:
                if (!(evaluatedValue instanceof Integer)) {
                    handleDataTypeException("INTEGER");
                }
                break;
            case LONG:
                if (!(evaluatedValue instanceof Long)) {
                    handleDataTypeException("LONG");
                }
                break;
            case XML:
                if (!(evaluatedValue instanceof OMElement)) {
                    handleDataTypeException("XML");
                }
                break;
            case JSON:
                if (!(evaluatedValue instanceof JsonElement)) {
                    handleDataTypeException("JSON");
                }
                break;
            default:
        }
        return evaluatedValue;
    }

    /**
     * Convert the JSON primitive to the expected data type.
     *
     * @param jsonPrimitive JSON primitive to be converted
     * @param type          Expected data type
     * @return Converted JSON primitive
     */
    public Object convertJsonPrimitive(JsonPrimitive jsonPrimitive, String type) {

        XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
        switch (dataType) {
            case BOOLEAN:
                if (jsonPrimitive.isBoolean()) {
                    return jsonPrimitive.getAsBoolean();
                } else {
                    handleDataTypeException("BOOLEAN");
                }
            case DOUBLE:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsDouble();
                } else {
                    handleDataTypeException("DOUBLE");
                }
            case INTEGER:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsInt();
                } else {
                    handleDataTypeException("INTEGER");
                }
            case LONG:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsLong();
                } else {
                    handleDataTypeException("LONG");
                }
            default:
                return jsonPrimitive.getAsString();
        }
    }

    /**
     * This method will throw a SynapseException with a message indicating that the expression result does not match
     * the expected data type.
     *
     * @param dataType Expected data type
     */
    private void handleDataTypeException(String dataType) {

        String msg = "Expression '${" + expression + "}' result does not match the expected data type '" + dataType + "'";
        log.error(msg);
        throw new SynapseException(msg);
    }

    @Override
    public boolean isContentAware() {

        boolean contentAware = false;
        if (expression != null) {
            contentAware = expression.isContentAware();
        }
        return contentAware;
    }

    private OMElement buildOMElement(String xml) {

        if (xml == null) {
            return null;
        }
        OMElement result = SynapseConfigUtils.stringToOM(xml);
        result.buildWithAttachments();
        return result;
    }

    private JsonElement buildJSONElement(String jsonPayload) {

        JsonParser jsonParser = new JsonParser();
        try {
            return jsonParser.parse(jsonPayload);
        } catch (JsonSyntaxException ex) {
            // Enclosing using quotes due to the following issue
            // https://github.com/google/gson/issues/1286
            String enclosed = "\"" + jsonPayload + "\"";
            try {
                return jsonParser.parse(enclosed);
            } catch (JsonSyntaxException e) {
                // log the original exception and discard the new exception
                log.error("Malformed JSON payload : " + jsonPayload, ex);
                return null;
            }
        }
    }

    @Override
    public String getMediatorName() {

        return super.getMediatorName() + ":" + name;
    }
}

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
     * Accepted type names are defined in XMLConfigConstants.DATA_TYPES enumeration. Passing null as the type
     * implies that 'STRING' type should be used.
     *
     * @param value the value to be set as a string
     * @param type  the type name
     */
    public void setValue(String value, String type) {

        this.type = type;
        this.value = convertValue(value, type, false);
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
                return convertValue(expression.stringValueOf(synCtx), type, true);
            }
        }

        return null;
    }

    private Object convertValue(String value, String type, boolean isExpression) {

        if (type == null) {
            return value;
        }

        try {
            XMLConfigConstants.DATA_TYPES dataType = XMLConfigConstants.DATA_TYPES.valueOf(type);
            switch (dataType) {
                case BOOLEAN:
                    return JavaUtils.isTrueExplicitly(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case FLOAT:
                    return Float.parseFloat(value);
                case INTEGER:
                    return parseInteger(value, isExpression);
                case LONG:
                    return Long.parseLong(value);
                case OM:
                    return buildOMElement(value);
                case SHORT:
                    return parseShort(value, isExpression);
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
     * This method will explicitly convert decimals to int since XPAth functions return numbers with decimal.
     *
     * @param value        String value returned from XPAth function
     * @param isExpression Boolean to check whether the value is from XPAth function
     * @return parsed Short value
     */
    private int parseInteger(String value, boolean isExpression) {

        if (isExpression && value.contains(".")) {
            return (int) Double.parseDouble(value);
        }
        return Integer.parseInt(value);
    }

    /**
     * This method will explicitly convert decimals to short since XPAth functions return numbers with decimal.
     *
     * @param value        String value returned from XPAth function
     * @param isExpression Boolean to check whether the value is from XPAth function
     * @return parsed Short value
     */
    private short parseShort(String value, boolean isExpression) {

        if (isExpression && value.contains(".")) {
            return (short) Double.parseDouble(value);
        }
        return Short.parseShort(value);
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

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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.config.xml.SynapsePath;
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
        this.value = Utils.convertValue(value, type, log);
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

        return Utils.getResolvedValue(synCtx, expression, value, type, log);
    }

    @Override
    public boolean isContentAware() {

        boolean contentAware = false;
        if (expression != null) {
            contentAware = expression.isContentAware();
        }
        return contentAware;
    }

    @Override
    public String getMediatorName() {

        return super.getMediatorName() + ":" + name;
    }
}

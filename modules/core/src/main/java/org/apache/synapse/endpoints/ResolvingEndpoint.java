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

package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.context.ConfigurationContext;
import org.json.JSONObject;

/**
 * 
 */
public class ResolvingEndpoint extends AbstractEndpoint {

    private SynapseXPath keyExpression = null;

    public void send(MessageContext synCtx) {
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            Integer currentIndex = null;
            if (getDefinition() != null) {
                currentIndex = OpenEventCollector.reportChildEntryEvent(synCtx, getReportingName(),
                        ComponentType.ENDPOINT, getDefinition().getAspectConfiguration(), true);
            }
            try {
                sendMessage(synCtx);
            } finally {
                if (currentIndex != null) {
                    CloseEventCollector.closeEntryEvent(synCtx, getReportingName(), ComponentType.MEDIATOR,
                            currentIndex, false);
                }
            }
        } else {
            sendMessage(synCtx);
        }
    }

    @Override
    protected void createJsonRepresentation() {
        endpointJson = new JSONObject();
        endpointJson.put(NAME_JSON_ATT, getName());
        endpointJson.put(TYPE_JSON_ATT, "Resolving Endpoint");
    }

    /**
     * Send by calling to the real endpoint
     *
     * @param synCtx the message to send
     */
    public void sendMessage(MessageContext synCtx) {

        String key = keyExpression.stringValueOf(synCtx);
        Endpoint ep = loadAndInitEndpoint(((Axis2MessageContext) synCtx).
                getAxis2MessageContext().getConfigurationContext(), key);

        if (ep != null) {
            ep.send(synCtx);
        } else {
            informFailure(synCtx, SynapseConstants.ENDPOINT_IN_DIRECT_NOT_READY,
                    "Couldn't find the endpoint with the key : " + key);
        }
    }

    public Endpoint loadAndInitEndpoint(ConfigurationContext cc, String key) {
        Parameter parameter = cc.getAxisConfiguration().getParameter(
                SynapseConstants.SYNAPSE_CONFIG);
        Parameter synEnvParameter = cc.getAxisConfiguration().getParameter(
                SynapseConstants.SYNAPSE_ENV);
        if (parameter.getValue() instanceof SynapseConfiguration &&
                synEnvParameter.getValue() instanceof SynapseEnvironment) {

            SynapseConfiguration synCfg = (SynapseConfiguration) parameter.getValue();
            SynapseEnvironment synapseEnvironment = (SynapseEnvironment) synEnvParameter.getValue();

            if (log.isDebugEnabled()) {
                log.debug("Loading real endpoint with key : " + key);
            }

            Endpoint ep = synCfg.getEndpoint(key);
            if (ep != null && !ep.isInitialized()) {
                synchronized (ep) {
                    if (!ep.isInitialized()) {
                        ep.init(synapseEnvironment);
                    }
                }
            }
            return ep;
        }

        return null;
    }

    public SynapseXPath getKeyExpression() {
        return keyExpression;
    }

    public void setKeyExpression(SynapseXPath keyExpression) {
        this.keyExpression = keyExpression;
    }
}

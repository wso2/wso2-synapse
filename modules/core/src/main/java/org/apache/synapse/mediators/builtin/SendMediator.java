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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.MessageHelper;

import java.util.Set;

/**
 * SendMediator sends a message using specified semantics. If it contains an endpoint it will
 * send the message to that endpoint. Once a message is sent to the endpoint further sending
 * behaviors are completely governed by that endpoint. If there is no endpoint available,
 * SendMediator will send the message to the implicitly stated destination.
 */
public class SendMediator extends AbstractMediator implements ManagedLifecycle {

    private Endpoint endpoint = null;

    private Value receivingSequence = null;

    // build the message before sending
    private boolean buildMessage = false;

    /**
     * This will call the send method on the messages with implicit message parameters
     * or else if there is an endpoint, with that endpoint parameters
     *
     * @param synCtx the current message to be sent
     * @return false always as this is a leaf mediator
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            MessageHelper.setWireLogHolderProperties(synCtx, isBreakPoint(), getRegisteredMediationFlowPoint()); //this needs to be set only in mediators where outgoing messages are present
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        synLog.traceOrDebug("Start : Send mediator");
        if (synLog.isTraceTraceEnabled()) {
            synLog.traceTrace("Message : " + synCtx.getEnvelope());
        }

        // Set the last sequence fault handler for future use
        synCtx.setProperty(SynapseConstants.LAST_SEQ_FAULT_HANDLER, getLastSequenceFaultHandler(synCtx));

        if (buildMessage) {
              synCtx.getEnvelope().buildWithAttachments();
        }

        // clear the message context properties related to endpoint in last service invocation
        Set keySet = synCtx.getPropertyKeySet();
        if (keySet != null) {
            keySet.remove(SynapseConstants.RECEIVING_SEQUENCE);
            keySet.remove(SynapseConstants.CONTINUATION_CALL);
            keySet.remove(EndpointDefinition.DYNAMIC_URL_VALUE);
            keySet.remove(SynapseConstants.LAST_ENDPOINT);
            keySet.remove("blockingMsgSender");
        }

        if (receivingSequence != null) {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Receiving sequence is set to: " + receivingSequence);
            }
            synCtx.setProperty(SynapseConstants.RECEIVING_SEQUENCE,
                    receivingSequence.evaluateValue(synCtx));
        }

        // if no endpoints are defined, send where implicitly stated
        if (endpoint == null) {

            if (synLog.isTraceOrDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Sending ").append(synCtx.isResponse() ? "response" : "request")
                        .append(" message using implicit message properties..");
                sb.append("\nSending To: ").append(synCtx.getTo() != null ?
                        synCtx.getTo().getAddress() : "null");
                sb.append("\nSOAPAction: ").append(synCtx.getWSAAction() != null ?
                        synCtx.getWSAAction() : "null");
                synLog.traceOrDebug(sb.toString());
            }

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Envelope : " + synCtx.getEnvelope());
            }
            synCtx.getEnvironment().send(null, synCtx);

        } else {
            endpoint.send(synCtx);
        }

        synLog.traceOrDebug("End : Send mediator");
        //to avoid continuing along the current sequence after send mediator.
        //mediators will not executed after send mediator in the sequence.
        return false;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Value getReceivingSequence() {
        return receivingSequence;
    }

    public void setReceivingSequence(Value receivingSequence) {
        this.receivingSequence = receivingSequence;
    }

    public void setBuildMessage(boolean buildMessage) {
        this.buildMessage = buildMessage;
    }

    public boolean isBuildMessage() {
        return buildMessage;
    }

    public void init(SynapseEnvironment synapseEnvironment) {
        if (endpoint != null) {
            endpoint.init(synapseEnvironment);
        }
    }

    public void destroy() {
        if (endpoint != null) {
            endpoint.destroy();
        }
    }

    @Override
    public boolean isContentAware() {
        return false;
    }

    @Override public void setComponentStatisticsId(ArtifactHolder holder) {
        if (getAspectConfiguration() == null) {
            configure(new AspectConfiguration(getMediatorName()));
        }
        String cloneId = StatisticIdentityGenerator.getIdForComponent(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(cloneId);
        if (endpoint != null) {
            endpoint.setComponentStatisticsId(holder);
        }
        if (receivingSequence != null) {
            String childId = StatisticIdentityGenerator
                    .getIdReferencingComponent(receivingSequence.getName(), ComponentType.SEQUENCE, holder);
            StatisticIdentityGenerator.reportingEndEvent(childId, ComponentType.SEQUENCE, holder);
        }
        StatisticIdentityGenerator.reportingEndEvent(cloneId, ComponentType.MEDIATOR, holder);
    }
}

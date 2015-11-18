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

package org.apache.synapse.mediators.filters;

import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.flowtracer.MessageFlowDataHolder;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;

import java.util.UUID;

/**
 * The In Mediator acts only on "incoming" messages into synapse. This is
 * performed by looking at the result of MessageContext#isResponse()
 *
 * @see org.apache.synapse.MessageContext#isResponse()
 */
public class InMediator extends AbstractListMediator implements org.apache.synapse.mediators.FilterMediator,
                                                                FlowContinuableMediator {

    /**
     * Executes the list of sub/child mediators, if the filter condition is satisfied
     *
     * @param synCtx the current message
     * @return true if filter condition fails. else returns as per List mediator semantics
     */
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : In mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        boolean result = true;
        if (test(synCtx)) {

            String mediatorId = null;
            if(MessageFlowDataHolder.isMessageFlowTraceEnable()) {
                mediatorId = UUID.randomUUID().toString();
                MessageFlowDataHolder.addComponentInfoEntry(synCtx, mediatorId, "In Mediator", true);
                synCtx.addComponentToMessageFlow(mediatorId);
                MessageFlowDataHolder.addFlowInfoEntry(synCtx);
            }

            synLog.traceOrDebug("Current message is incoming - executing child mediators");
            ContinuationStackManager.addReliantContinuationState(synCtx, 0, getMediatorPosition());
            result = super.mediate(synCtx);
            if (result) {
                ContinuationStackManager.removeReliantContinuationState(synCtx);
            }

            if(MessageFlowDataHolder.isMessageFlowTraceEnable()) {
                MessageFlowDataHolder.addComponentInfoEntry(synCtx, mediatorId, "In Mediator", false);
            }

        } else {
            synLog.traceOrDebug("Current message is a response - skipping child mediators");
        }

        synLog.traceOrDebug("End : In mediator");

        return result;
    }

    public boolean mediate(MessageContext synCtx,
                           ContinuationState continuationState) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("In mediator : Mediating from ContinuationState");
        }

        boolean result;
        if (!continuationState.hasChild()) {
            result = super.mediate(synCtx, continuationState.getPosition() + 1);
        } else {
            FlowContinuableMediator mediator =
                    (FlowContinuableMediator) getChild(continuationState.getPosition());
            result = mediator.mediate(synCtx, continuationState.getChildContState());
        }
        return result;
    }

    /**
     * Apply mediation only on request messages
     *
     * @param synCtx the message context
     * @return MessageContext#isResponse()
     */
    public boolean test(MessageContext synCtx) {
        return !synCtx.isResponse();
    }

    @Override
    public boolean isContentAware() {
        return false;
    }

    public void init(SynapseEnvironment se) {
        super.init(se);
    }

}

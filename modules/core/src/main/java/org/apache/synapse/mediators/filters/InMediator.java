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
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.collector.CollectorEnabler;
import org.apache.synapse.mediators.collector.MediatorData;
import org.apache.synapse.mediators.collector.NestedMediator;
import org.apache.synapse.mediators.collector.SuperMediator;
import org.apache.synapse.mediators.collector.TreeNode;

/**
 * The In Mediator acts only on "incoming" messages into synapse. This is
 * performed by looking at the result of MessageContext#isResponse()
 *
 * @see org.apache.synapse.MessageContext#isResponse()
 */
public class InMediator extends AbstractListMediator implements org.apache.synapse.mediators.FilterMediator,
                                                                FlowContinuableMediator {

	private TreeNode current;
    /**
     * Executes the list of sub/child mediators, if the filter condition is satisfied
     *
     * @param synCtx the current message
     * @return true if filter condition fails. else returns as per List mediator semantics
     */
    public boolean mediate(MessageContext synCtx) {


		if (CollectorEnabler.checkCollectorRequired()) {
			current= MediatorData.createNewMediator(synCtx, this);
		}

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : In mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        boolean result = true;
        if (test(synCtx)) {
            synLog.traceOrDebug("Current message is incoming - executing child mediators");
            ContinuationStackManager.addReliantContinuationState(synCtx, 0, getMediatorPosition());
            result = super.mediate(synCtx);

			if (CollectorEnabler.checkCollectorRequired()) {
				MediatorData.setEndingTime(current);
				synCtx.setCurrent(current.getParent());
			}

            if (result) {
                ContinuationStackManager.removeReliantContinuationState(synCtx);
            }
        } else {
            synLog.traceOrDebug("Current message is a response - skipping child mediators");

			//Adding this mediator is of no use since its child mediators are not executed. Therefore
			//change its name and later the change node will be removed from the tree.
			if(CollectorEnabler.checkCollectorRequired()){

				current.getContents().setMediatorName("Skipped");
				synCtx.setCurrent(current.getParent());

        }

		}

        synLog.traceOrDebug("End : In mediator");

        return result;
    }

    public boolean mediate(MessageContext synCtx,
                           ContinuationState continuationState) {

		if(CollectorEnabler.checkCollectorRequired()){
			synCtx.setCurrent(current);
		}

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

		if (CollectorEnabler.checkCollectorRequired()) {
				MediatorData.setEndingTime(current);
				synCtx.setCurrent(current.getParent());
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

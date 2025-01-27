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

package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.flow.statistics.StatisticsCloseEventListener;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.v2.Utils;
import org.apache.synapse.util.logging.LoggingUtils;

/**
 * This class will be used as the executer for the injectAsync method for the
 * sequence mediation
 */
public class MediatorWorker implements Runnable {

    private static final Log log = LogFactory.getLog(MediatorWorker.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /** Mediator to be executed */
    private Mediator seq = null;

    /** MessageContext to be mediated using the mediator */
    private MessageContext synCtx = null;

    /** StatisticsCloseEventListener to be used to close the statistics events at the end of the flow. */
    private StatisticsCloseEventListener statisticsCloseEventListener = new StatisticsCloseEventListener();

    /**
     * Constructor of the MediatorWorker which sets the sequence and the message context
     *
     * @param seq    - Sequence Mediator to be set
     * @param synCtx - Synapse MessageContext to be set
     */
    public MediatorWorker(Mediator seq, MessageContext synCtx) {
        this.seq = seq;
        this.synCtx = synCtx;
    }

    /**
     * Constructor od the MediatorWorker which sets the provided message context and the
     * main sequence as the sequence for mediation
     *
     * @param synCtx - Synapse MessageContext to be set
     */
    public MediatorWorker(MessageContext synCtx) {
        this.synCtx = synCtx;
        seq = synCtx.getMainSequence();
    }

    /**
     * Execution method of the thread. This will just call the mediation of the specified
     * Synapse MessageContext using the specified Sequence Mediator
     */
    public void run() {
        try {
            //Set tenant info when different thread executes the mediation
            TenantInfoConfigurator configurator = synCtx.getEnvironment().getTenantInfoConfigurator();
            if (configurator != null) {
                configurator.applyTenantInfo(synCtx);
            }

            if (synCtx.getEnvironment().isDebuggerEnabled()) {
                SynapseDebugManager debugManager = synCtx.getEnvironment().getSynapseDebugManager();
                debugManager.acquireMediationFlowLock();
                debugManager.advertiseMediationFlowStartPoint(synCtx);
            }

            // If this is a scatter message, then we need to use the clone the continuation state and continue the mediation
            if (Utils.isScatterMessage(synCtx)) {
                SeqContinuationState seqContinuationState = (SeqContinuationState) ContinuationStackManager.peakContinuationStateStack(synCtx);
                if (seqContinuationState == null) {
                    log.error("Sequence Continuation State cannot be found in the stack, hence cannot continue the mediation.");
                    return;
                }
                SeqContinuationState clonedSeqContinuationState = ContinuationStackManager.getClonedSeqContinuationState(seqContinuationState);
                boolean result = seq.mediate(synCtx);
                if (result) {
                    SequenceMediator sequenceMediator = ContinuationStackManager.retrieveSequence(synCtx, clonedSeqContinuationState);
                    synCtx.setProperty(SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER, true);
                    sequenceMediator.mediate(synCtx, clonedSeqContinuationState);
                }
            } else {
                seq.mediate(synCtx);
            }
            //((Axis2MessageContext)synCtx).getAxis2MessageContext().getEnvelope().discard();

        } catch (SynapseException syne) {
            if (!synCtx.getFaultStack().isEmpty()) {
                warn(false, "Executing fault handler due to exception encountered", synCtx);
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, syne);

            } else {
                warn(false, "Exception encountered but no fault handler found - " +
                    "message dropped", synCtx);
            }

        } catch (Exception e) {
            String msg = "Unexpected error executing task/async inject";
            log.error(LoggingUtils.getFormattedLog(synCtx, msg), e);
            if (synCtx.getServiceLog() != null) {
                synCtx.getServiceLog().error(msg, e);
            }
            if (!synCtx.getFaultStack().isEmpty()) {
                warn(false, "Executing fault handler due to exception encountered", synCtx);
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, e);

            } else {
                warn(false, "Exception encountered but no fault handler found - " +
                    "message dropped", synCtx);
            }
        } catch (Throwable e) {
            String msg = "Unexpected error executing task/async inject, message dropped";
            log.error(LoggingUtils.getFormattedLog(synCtx, msg), e);
            if (synCtx.getServiceLog() != null) {
                synCtx.getServiceLog().error(msg, e);
            }
        } finally {
            if (synCtx.getEnvironment().isDebuggerEnabled()) {
                SynapseDebugManager debugManager = synCtx.getEnvironment().getSynapseDebugManager();
                debugManager.advertiseMediationFlowTerminatePoint(synCtx);
                debugManager.releaseMediationFlowLock();
            }
            if (RuntimeStatisticCollector.isStatisticsEnabled() && !Utils.isScatterMessage(synCtx)) {
                this.statisticsCloseEventListener.invokeCloseEventEntry(synCtx);
            }
        }
        synCtx = null;
        seq = null;
    }

    private void warn(boolean traceOn, String msg, MessageContext msgContext) {

        String formattedLog = LoggingUtils.getFormattedLog(msgContext, msg);
        if (traceOn) {
            trace.warn(formattedLog);
        }
        if (log.isDebugEnabled()) {
            log.warn(formattedLog);
        }
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
    }

    public void setStatisticsCloseEventListener(StatisticsCloseEventListener statisticsCloseEventListener) {
        this.statisticsCloseEventListener = statisticsCloseEventListener;
    }
}

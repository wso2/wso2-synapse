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
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryUtil;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.util.MediatorIdLogSetter;
import org.apache.synapse.mediators.util.TracingIdLogSetter;
import org.apache.synapse.mediators.v2.Utils;
import org.apache.synapse.util.logging.LoggingUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class will be used as the executer for the injectAsync method for the
 * sequence mediation
 * <p>
 * A worker continues a message flow in another thread, so it owns one unit of the branch count of the flow's statistics
 * event holder (StatisticsReportingCountHolder): if the message context has a collected event holder when the worker is
 * constructed, the worker adds one unit to it, and releases that unit exactly once, on the same holder, either at the
 * end of {@link #run()} or, when the worker cannot be started, through {@link #releaseBranch()}. Code that creates a
 * worker must call {@link #releaseBranch()} when starting it fails (any exception between construction and a
 * successful execute). A worker created before its flow has an event holder (task, template task, uncollected inbound
 * endpoint) owns no unit.
 * <p>
 * The end of a worker runs, in this order: release of the branch unit; for a worker that starts a flow and whose
 * close listener closes no outer level component, the close of the main branch; the statistics close listener (whose
 * finish check must see the unit released, which is why the release is the first step, at the bottom of run());
 * and {@link RuntimeStatisticCollector#tryFinishEventCollection}, which publishes the statistics if this was the last
 * pending work. The whole sequence runs inside {@code OpenTelemetrySpanHandler.handleBranchEnd}, so a span handler can
 * hold a lock around it and take no decision on the end of the flow between the release and the close events.
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

    /** Event holder whose branch count this worker incremented, or null when it owns no branch unit. */
    private final StatisticsReportingEventHolder branchEventHolder;

    /** Guards the single release of the branch unit. */
    private final AtomicBoolean branchReleased = new AtomicBoolean();

    /**
     * Whether the message context had no statistics event holder when the worker was created: the worker starts a new
     * flow (task, template task, fresh context) rather than continuing one.
     */
    private final boolean flowRoot;

    /**
     * Constructor of the MediatorWorker which sets the sequence and the message context
     *
     * @param seq    - Sequence Mediator to be set
     * @param synCtx - Synapse MessageContext to be set
     */
    public MediatorWorker(Mediator seq, MessageContext synCtx) {
        this.seq = seq;
        this.synCtx = synCtx;
        // Taken before startBranch: whether the worker continues an existing flow or starts a new one
        this.flowRoot = synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) == null;
        // +1 now, in the creating thread: the flow must count this worker from the moment it exists, not from the
        // moment the pool runs it, or the root span could end while the worker waits in the queue
        this.branchEventHolder = TelemetryUtil.startBranch(synCtx);
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
        // See the other constructor
        this.flowRoot = synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) == null;
        this.branchEventHolder = TelemetryUtil.startBranch(synCtx);
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

            // Sync mediator ID from MessageContext to ThreadContext when new thread starts
            MediatorIdLogSetter.getInstance().syncToThreadContext(synCtx);

            // Set the message's trace and span IDs in the ThreadContext
            TracingIdLogSetter.setFromMessage(synCtx);

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
            // Nested finally blocks: a failure in one end step must not skip the next. Above all the branch unit must
            // be released, or the flow's root span never ends and its statistics are never published.
            try {
                if (synCtx.getEnvironment().isDebuggerEnabled()) {
                    SynapseDebugManager debugManager = synCtx.getEnvironment().getSynapseDebugManager();
                    debugManager.advertiseMediationFlowTerminatePoint(synCtx);
                    debugManager.releaseMediationFlowLock();
                }
            } finally {
                try {
                    // Runs finishBranch, through the span handler when tracing is on (it may hold a lock around it)
                    TelemetryUtil.endBranch(synCtx, this::finishBranch);
                } finally {
                    // No-op normally; releases the unit if the span handler failed before running finishBranch
                    releaseBranchUnit();
                    // Clear ThreadContext when thread finishes to prevent context leakage
                    MediatorIdLogSetter.getInstance().clearMediatorId();
                    TracingIdLogSetter.clear();
                }
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

    /**
     * Ends this worker: releases its branch unit first (the finish check of the close listener must see it released),
     * then reports its statistics close events and publishes the statistics if this was the last pending work.
     */
    private void finishBranch() {
        // 1. Release first. The close listener below runs the statistics finish check (branchCount <= 0); if this unit
        //    were still counted, the last worker of a flow would never see the flow finished.
        releaseBranchUnit();
        if (!RuntimeStatisticCollector.isStatisticsEnabled()) {
            return;
        }
        // 2. A worker that starts a flow with the plain listener (a template task: TemplateMessageExecutor injects a
        //    fresh context without TASK_NAME, so its first collected component is the sequence) has no outer level
        //    component whose close would close the main branch. Without this the main unit stays open forever and the
        //    flow's statistics are never published. Task and inbound workers have a TASK / INBOUNDENDPOINT listener,
        //    which closes it in CloseEventCollector instead.
        if (flowRoot && !statisticsCloseEventListener.closesOuterLayerComponent()) {
            TelemetryUtil.closeMainBranch(synCtx);
        }
        // 3. The close events of the component the worker stopped at. Scatter messages (ForEach V2, Scatter-Gather
        //    branches) report none: their branch ends at the aggregation.
        boolean scatterMessage = Utils.isScatterMessage(synCtx);
        if (!scatterMessage) {
            statisticsCloseEventListener.invokeCloseEventEntry(synCtx);
        }
        // 4. Publish the statistics if this worker was the last pending work: after an error close while workers ran,
        //    or, for a scatter worker, the completion check its missing close event would have run.
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, branchEventHolder, scatterMessage);
    }

    /**
     * Gives the unit back to the holder it was taken from (not the holder on the message context now), at most once:
     * run() and releaseBranch() both call it, and a double release would hide another running worker.
     */
    private void releaseBranchUnit() {
        if (branchEventHolder != null && branchReleased.compareAndSet(false, true)) {
            branchEventHolder.countHolder.decrementAndGetBranchCount();
        }
    }

    /**
     * Releases this worker's branch unit, for callers that created the worker but could not start it (any exception
     * between construction and a successful execute). Idempotent with respect to the unit.
     */
    public void releaseBranch() {
        try {
            // Through the span handler, not a bare decrement: if the flow was only waiting for this worker, the
            // handler's completion check after the release ends the root span
            TelemetryUtil.endBranch(synCtx, this::releaseBranchUnit);
        } finally {
            releaseBranchUnit();
        }
    }

    public void setStatisticsCloseEventListener(StatisticsCloseEventListener statisticsCloseEventListener) {
        this.statisticsCloseEventListener = statisticsCloseEventListener;
    }
}

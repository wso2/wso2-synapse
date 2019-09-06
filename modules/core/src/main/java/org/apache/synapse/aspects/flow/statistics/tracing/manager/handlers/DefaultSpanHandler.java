package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.SpanExtendingCounter;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.SpanTagger;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver.DefaultParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.scopemanagement.TracingScope;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.scopemanagement.TracingScopeManager;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.store.StackedSequenceInfo;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.continuation.SeqContinuationState;

import java.util.List;
import java.util.Objects;

public class DefaultSpanHandler implements JaegerTracingSpanHandler {
    private JaegerTracer tracer;
    private TracingScopeManager tracingScopeManager;
//    private final SpanStore spanStore;

    public DefaultSpanHandler(JaegerTracer tracer, TracingScopeManager tracingScopeManager, SpanStore spanStore) {
        this.tracer = tracer;
        this.tracingScopeManager = tracingScopeManager;
//        this.spanStore = spanStore;
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrStackSequence(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                           MessageContext synCtx) {
        startSpanOrStackSequence(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                                MessageContext synCtx) {
        startSpanOrStackSequence(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        startSpanOrStackSequence(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        startSpanOrStackSequence(absoluteId, statisticDataUnit, synCtx);
    }

    private void startSpanOrStackSequence(String absoluteId, StatisticDataUnit statisticDataUnit,
                                          MessageContext synCtx) {
        // TODO lock on tracingScope if stuff like callback counter are going to reside in tracingScope
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isStackableSequence(statisticDataUnit, tracingScope.getSpanStore())) {
                beginSpan(absoluteId, statisticDataUnit, synCtx, tracingScope.getSpanStore());
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit, tracingScope.getSpanStore());
            }
            printStateInfoForDebug(synCtx, tracingScope.getSpanStore());
        }
    }

    @Override
    public void handleOpenFlowAsynchronousEvent(String absoluteId, BasicStatisticDataUnit statisticDataUnit,
                                                 MessageContext synCtx) {
        // TODO
    }

    @Override
    public void handleOpenContinuationEvents(String absoluteId, BasicStatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        // TODO
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isStackableSequence(basicStatisticDataUnit, tracingScope.getSpanStore())) {
                endSpan(basicStatisticDataUnit, synCtx, tracingScope.getSpanStore(), tracingScope);
            }
            // Else: Will end during pop from stack
            printStateInfoForDebug(synCtx, tracingScope.getSpanStore());
        }
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isStackableSequence(basicStatisticDataUnit, tracingScope.getSpanStore())) {
                endSpan(basicStatisticDataUnit, synCtx, tracingScope.getSpanStore(), tracingScope);
            }
            // Else: Will end during pop from stack
            printStateInfoForDebug(synCtx, tracingScope.getSpanStore());
            // TODO for now, ignore if not there. But need to be sophisticated i guess
        }
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb. // TODO confirm
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(messageContext);
        tracingScope.incrementPendingCallbacksCount();
    }

    @Override
    public void handleCallbackCompletionEvent(MessageContext oldMessageContext, String callbackId) {
        handleReportCallbackHandlingCompletion(oldMessageContext, callbackId);
    }

    @Override
    public void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId) {
        // Absorb. Callback handling completion will be reported after handling the specific message.
    }

    @Override
    public void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {
        if (canEndOuterLevelSpan(synapseOutMsgCtx)) {
            synchronized (spanStore) {
                doHackyCleanup(); // TODO remove

                SpanWrapper outerLevelSpanWrapper = tracingScope.getSpanStore().getOuterLevelSpanWrapper();
                tracingScope.getSpanStore().finishActiveSpan(outerLevelSpanWrapper);

                System.out.println("Finished Span - currentIndex: " + outerLevelSpanWrapper.getStatisticDataUnit().getCurrentIndex() +
                        ", statisticsId: " + outerLevelSpanWrapper.getStatisticDataUnit().getStatisticId());
            }
        }
    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            StackedSequenceInfo stackedSequenceInfo =
                    findStackedSequenceInfo(synCtx, seqName, seqType, tracingScope.getSpanStore(),false);
            if (stackedSequenceInfo != null) {
                StatisticDataUnit statisticDataUnit = stackedSequenceInfo.getStatisticDataUnit();
                stackedSequenceInfo.setSpanActive(true);
                beginSpan(stackedSequenceInfo.getSpanReferenceId(), statisticDataUnit, synCtx,
                        tracingScope.getSpanStore());
                printStateInfoForDebug(synCtx, tracingScope.getSpanStore());
            } else {
                // TODO look carefully
                System.out.println("EXCEPTIONAL: Next Unstarted Stacked Sequence Reference is null");
            }
        }
    }

    @Override
    public void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (continuationState instanceof SeqContinuationState) { // No other type will be kept track of // TODO ensure
                StackedSequenceInfo stackedSequenceInfo =
                        findStackedSequenceInfo(
                                synCtx,
                                ((SeqContinuationState)continuationState).getSeqName(),
                                ((SeqContinuationState)continuationState).getSeqType(),
                                tracingScope.getSpanStore(),
                                true);
                if (stackedSequenceInfo != null) {
                    stackedSequenceInfo.setSpanActive(false);
                    finishStackedSequenceSpan(synCtx, stackedSequenceInfo, tracingScope.getSpanStore());
                    tracingScope.getSpanStore().getStackedSequences().remove(stackedSequenceInfo);
                    System.out.println("Finished Span - currentIndex: " +
                            stackedSequenceInfo.getStatisticDataUnit().getCurrentIndex() +
                            ", statisticsId: " + stackedSequenceInfo.getStatisticDataUnit().getStatisticId());
                    printStateInfoForDebug(synCtx, tracingScope.getSpanStore());
                }
            }
        }
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            List<StackedSequenceInfo> stackedSequences = tracingScope.getSpanStore().getStackedSequences();
            for (StackedSequenceInfo stackedSequence : stackedSequences) {
                finishStackedSequenceSpan(synCtx, stackedSequence, tracingScope.getSpanStore());
            }
            stackedSequences.clear();
        }
    }

    // TODO Remove this absoluteIndex story in all the places
    private void beginSpan(String spanId, StatisticDataUnit statisticDataUnit, MessageContext synCtx,
                           SpanStore spanStore) {
        Span parentSpan = DefaultParentResolver.resolveParent(statisticDataUnit, spanStore, synCtx);
        Span span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
        SpanTagger.setDebugSpanTags(span, spanId, statisticDataUnit, synCtx); // TODO remove debug tags
        SpanWrapper spanWrapper = spanStore.addActiveSpan(spanId, span, statisticDataUnit, synCtx);

        if (spanStore.getOuterLevelSpanWrapper() == null && isEligibleForOuterLevelSpan(statisticDataUnit)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }

        System.out.println("Started Span - " + statisticDataUnit.getCurrentIndex() + "(" + spanId + ")" +
                ", componentId: " + statisticDataUnit.getComponentId() +
                ", statisticsId: " + statisticDataUnit.getStatisticId());
        spanStore.printActiveSpans();
    }

    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx, SpanStore spanStore,
                         TracingScope tracingScope) {
        String spanWrapperId = Util.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);

        if (Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
            // An outer level span
            if (isTheMostOuterLevelSpan(tracingScope, spanWrapper)) {
                if (canEndTheMostOuterLevelSpan(synCtx)) {
                    // The most outer level span, and no callbacks are pending
                    spanStore.finishActiveSpan(spanWrapper);
                    System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                            ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
                }
                // Else - Absorb. Will be handled when all the callbacks are received back
            } else {
                // Outer level span, but not the most outer level
                doHackyCleanup(spanStore);
                spanStore.finishActiveSpan(spanWrapper);
                System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                        ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
            }
        } else {
            // A non-outer level span
            spanStore.finishActiveSpan(spanWrapper);
            System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                    ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        }

        spanStore.printActiveSpans();
    }

    private boolean isEligibleForOuterLevelSpan(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType() == ComponentType.PROXYSERVICE ||
                statisticDataUnit.getComponentType() == ComponentType.API; // TODO what about other components
    }

    private boolean isTheMostOuterLevelSpan(TracingScope tracingScope, SpanWrapper spanWrapper) {
        return tracingScope.isOuterLevelScope() &&
                Objects.equals(tracingScope.getSpanStore().getOuterLevelSpanWrapper(), spanWrapper);
    }

    private boolean canEndTheMostOuterLevelSpan(MessageContext synCtx) {
        return SpanExtendingCounter.getValue() == 0 &&
                synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) != null &&
                ((StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY))
                        .isEvenCollectionFinished();
    }

    private boolean isStackableSequence(BasicStatisticDataUnit basicStatisticDataUnit, SpanStore spanStore) {
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
            // Check is performed during an Open Event
            return ((StatisticDataUnit) basicStatisticDataUnit).getComponentType() == ComponentType.SEQUENCE &&
                    (SequenceType.PROXY_INSEQ.toString()
                            .equals(((StatisticDataUnit) basicStatisticDataUnit).getComponentName()) ||
                            SequenceType.PROXY_OUTSEQ.toString()
                                    .equals(((StatisticDataUnit) basicStatisticDataUnit).getComponentName()) ||
                            SequenceType.API_INSEQ.toString()
                                    .equals(((StatisticDataUnit) basicStatisticDataUnit).getComponentName()) ||
                            SequenceType.API_OUTSEQ.toString()
                                    .equals(((StatisticDataUnit) basicStatisticDataUnit).getComponentName())
                    ); // TODO what about FAULTSEQ's?
        }
        // Check is performed during a Close Event
        return spanStore.containsStackedSequenceWithId(
                String.valueOf(basicStatisticDataUnit.getCurrentIndex()));
    }

    private void addStackedSequence(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        StackedSequenceInfo stackedSequenceInfo = new StackedSequenceInfo(statisticDataUnit);
        spanStore.addStackedSequence(stackedSequenceInfo);
    }

    private void finishStackedSequenceSpan(MessageContext synCtx, StackedSequenceInfo stackedSequenceInfo,
                                           SpanStore spanStore) {
        String spanWrapperId = stackedSequenceInfo.getSpanReferenceId();
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);
        spanStore.finishActiveSpan(spanWrapper);

        System.out.println("Finished Stacked Sequence Span - ComponentName: " +
                stackedSequenceInfo.getStatisticDataUnit().getComponentName());
    }

    private StackedSequenceInfo findStackedSequenceInfo(MessageContext synCtx, String seqName, SequenceType seqType,
                                                        SpanStore spanStore, boolean desiredSpanActiveState) {
        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getStackedSequences()) {
            if (seqType.toString().equals(stackedSequenceInfo.getStatisticDataUnit().getComponentName()) &&
                    (stackedSequenceInfo.isSpanActive() == desiredSpanActiveState)) {
                return stackedSequenceInfo;
            }
        }
        return null;
    }

    // TODO Remove. Hacky - to handle non-ending stacked sequences (until fixing stack pop for all mediators)
    private void doHackyCleanup(SpanStore spanStore) {
        if (!spanStore.getStackedSequences().isEmpty()) {
            System.out.println("Doing Hacky Cleanup");
            List<StackedSequenceInfo> stackedSequences = spanStore.getStackedSequences();
            for (StackedSequenceInfo stackedSequence : stackedSequences) {
                finishStackedSequenceSpan(null, stackedSequence, spanStore);
            }
            stackedSequences.clear();
        }
    }

    // TODO remove
    private synchronized void printStateInfoForDebug(MessageContext synCtx, SpanStore spanStore) {
        spanStore.printActiveSpans();
        spanStore.printStackedSequences();
    }
}

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
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.store.StackedSequenceInfo;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.continuation.SeqContinuationState;

import java.util.List;
import java.util.Objects;

public class DefaultSpanHandler implements JaegerTracingSpanHandler {
    private JaegerTracer tracer;
    private final SpanStore spanStore;

    public DefaultSpanHandler(JaegerTracer tracer, SpanStore spanStore) {
        this.tracer = tracer;
        this.spanStore = spanStore;
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(statisticDataUnit)) {
                beginSpan(absoluteId, statisticDataUnit, synCtx);
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
            }
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                           MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(statisticDataUnit)) {
                beginSpan(absoluteId, statisticDataUnit, synCtx);
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
            }
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                                MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(statisticDataUnit)) {
                beginSpan(absoluteId, statisticDataUnit, synCtx);
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
            }
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(statisticDataUnit)) {
                beginSpan(absoluteId, statisticDataUnit, synCtx);
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
            }
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(statisticDataUnit)) {
                beginSpan(absoluteId, statisticDataUnit, synCtx);
            } else {
                // Will begin during addSeqContinuationState
                addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
            }
            printStateInfoForDebug(synCtx);
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
        synchronized (spanStore) {
            if (!isStackableSequence(basicStatisticDataUnit)) {
                endSpan(basicStatisticDataUnit, synCtx);
            }
            // Else: Will end during pop from stack
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        synchronized (spanStore) {
            if (!isStackableSequence(basicStatisticDataUnit)) {
                endSpan(basicStatisticDataUnit, synCtx);
            }
            // Else: Will end during pop from stack
            printStateInfoForDebug(synCtx);
            // TODO for now, ignore if not there. But need to be sophisticated i guess
        }
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb. // TODO confirm
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {
        // Absorb
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

                SpanWrapper outerLevelSpanWrapper = spanStore.getOuterLevelSpanWrapper();
                spanStore.finishActiveSpan(outerLevelSpanWrapper);

                System.out.println("Finished Span - currentIndex: " + outerLevelSpanWrapper.getStatisticDataUnit().getCurrentIndex() +
                        ", statisticsId: " + outerLevelSpanWrapper.getStatisticDataUnit().getStatisticId());
            }
        }
    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType) {
        synchronized (spanStore) {
            StackedSequenceInfo stackedSequenceInfo = findStackedSequenceInfo(synCtx, seqName, seqType, false);
            if (stackedSequenceInfo != null) {
                StatisticDataUnit statisticDataUnit = stackedSequenceInfo.getStatisticDataUnit();
                stackedSequenceInfo.setSpanActive(true);
                beginSpan(stackedSequenceInfo.getSpanReferenceId(), statisticDataUnit, synCtx);
                printStateInfoForDebug(synCtx);
            } else {
                // TODO look carefully
                System.out.println("EXCEPTIONAL: Next Unstarted Call Mediator Sequence Reference is null");
            }
        }
    }

    @Override
    public void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx) {
        synchronized (spanStore) {
            if (continuationState instanceof SeqContinuationState) { // No other type will be kept track of // TODO ensure
                StackedSequenceInfo stackedSequenceInfo =
                        findStackedSequenceInfo(
                                synCtx,
                                ((SeqContinuationState)continuationState).getSeqName(),
                                ((SeqContinuationState)continuationState).getSeqType(),
                                true);
                if (stackedSequenceInfo != null) {
                    stackedSequenceInfo.setSpanActive(false);
                    finishStackedSequenceSpan(synCtx, stackedSequenceInfo);
                    spanStore.getStackedSequences().remove(stackedSequenceInfo);
                    System.out.println("Finished Span - currentIndex: " +
                            stackedSequenceInfo.getStatisticDataUnit().getCurrentIndex() +
                            ", statisticsId: " + stackedSequenceInfo.getStatisticDataUnit().getStatisticId());
                    printStateInfoForDebug(synCtx);
                }
            }
        }
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
        synchronized (spanStore) {
            List<StackedSequenceInfo> stackedSequences = spanStore.getStackedSequences();
            for (StackedSequenceInfo stackedSequence : stackedSequences) {
                finishStackedSequenceSpan(synCtx, stackedSequence);
            }
            stackedSequences.clear();
        }
    }

    // TODO Remove this absoluteIndex story in all the places
    private void beginSpan(String spanId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        Span parentSpan = DefaultParentResolver.resolveParent(statisticDataUnit, spanStore);
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

    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        String spanWrapperId = Util.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getFinishableSpanWrapper(spanWrapperId);
//        if ((spanStore.getOuterLevelSpanWrapper() != null && !spanStore.getOuterLevelSpanWrapper().equals(spanWrapper))
        if (!Objects.equals(spanStore.getOuterLevelSpanWrapper(), spanWrapper) || canEndOuterLevelSpan(synCtx)) {
            spanStore.finishActiveSpan(spanWrapper);

            System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                    ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        }
        // Else - Absorb. Will be handled when all the callbacks are handled/reported

        spanStore.printActiveSpans();
    }

    private boolean isEligibleForOuterLevelSpan(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType() == ComponentType.PROXYSERVICE ||
                statisticDataUnit.getComponentType() == ComponentType.API; // TODO what about other components
    }

    private boolean canEndOuterLevelSpan(MessageContext synCtx) {
        return SpanExtendingCounter.getValue() == 0 &&
                synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) != null &&
                ((StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY))
                        .isEvenCollectionFinished();
    }

    private boolean isStackableSequence(BasicStatisticDataUnit basicStatisticDataUnit) {
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

    private void addStackedSequence(StatisticDataUnit statisticDataUnit) {
        StackedSequenceInfo stackedSequenceInfo = new StackedSequenceInfo(statisticDataUnit);
        spanStore.addStackedSequence(stackedSequenceInfo);
    }

    private void finishStackedSequenceSpan(MessageContext synCtx, StackedSequenceInfo stackedSequenceInfo) {
        String spanWrapperId = stackedSequenceInfo.getSpanReferenceId();
        SpanWrapper spanWrapper = spanStore.getFinishableSpanWrapper(spanWrapperId);
        spanStore.finishActiveSpan(spanWrapper);

        System.out.println("Finished Call Mediator Sequence Span - ComponentName: " +
                stackedSequenceInfo.getStatisticDataUnit().getComponentName());
    }

    private StackedSequenceInfo findStackedSequenceInfo(MessageContext synCtx, String seqName, SequenceType seqType,
                                                        boolean desiredSpanActiveState) {
        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getStackedSequences()) {
            if (seqType.toString().equals(stackedSequenceInfo.getStatisticDataUnit().getComponentName()) &&
                    (stackedSequenceInfo.isSpanActive() == desiredSpanActiveState)) {
                return stackedSequenceInfo;
            }
        }
        return null;
    }

    // TODO Remove. Hacky - to handle non-ending stacked sequences (until fixing stack pop for all mediators)
    private void doHackyCleanup() {
        if (!spanStore.getStackedSequences().isEmpty()) {
            List<StackedSequenceInfo> stackedSequences = spanStore.getStackedSequences();
            for (StackedSequenceInfo stackedSequence : stackedSequences) {
                finishStackedSequenceSpan(null, stackedSequence);
            }
            stackedSequences.clear();
        }
    }

    // TODO remove
    private synchronized void printStateInfoForDebug(MessageContext synCtx) {
        spanStore.printActiveSpans();
        spanStore.printStackedSequences();
    }
}

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
import org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver.DefaultParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.store.StackedSequenceInfo;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.continuation.SeqContinuationState;

import java.util.List;

// Handles everything for now. TODO Add Handlers when special cases are required
public class DefaultSpanHandler implements JaegerTracingSpanHandler {
    private JaegerTracer tracer;
    private SpanStore spanStore;

    public DefaultSpanHandler(JaegerTracer tracer, SpanStore spanStore) {
        this.tracer = tracer;
        this.spanStore = spanStore;
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        if (!isStackableSequence(statisticDataUnit)) {
            beginSpan(absoluteId, statisticDataUnit, synCtx);
        } else {
            // Will begin during addSeqContinuationState
            addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                           MessageContext synCtx) {
        if (!isStackableSequence(statisticDataUnit)) {
            beginSpan(absoluteId, statisticDataUnit, synCtx);
        } else {
            // Will begin during addSeqContinuationState
            addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                                MessageContext synCtx) {
        if (!isStackableSequence(statisticDataUnit)) {
            beginSpan(absoluteId, statisticDataUnit, synCtx);
        } else {
            // Will begin during addSeqContinuationState
            addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        if (!isStackableSequence(statisticDataUnit)) {
            beginSpan(absoluteId, statisticDataUnit, synCtx);
        } else {
            // Will begin during addSeqContinuationState
            addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit,
                                              MessageContext synCtx) {
        if (!isStackableSequence(statisticDataUnit)) {
            beginSpan(absoluteId, statisticDataUnit, synCtx);
        } else {
            // Will begin during addSeqContinuationState
            addStackedSequence(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
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
        if (!isStackableSequence(basicStatisticDataUnit)) {
            endSpan(basicStatisticDataUnit, synCtx);
        }
        // Else: Will end during pop from stack
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        if (!isStackableSequence(basicStatisticDataUnit)) {
            endSpan(basicStatisticDataUnit, synCtx);
        }
        // Else: Will end during pop from stack
        printStateInfoForDebug(synCtx);
        // TODO for now, ignore if not there. But need to be sophisticated i guess
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
        if (SpanExtendingCounter.getValue() == 0 &&
                synapseOutMsgCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) != null &&
                (((StatisticsReportingEventHolder) synapseOutMsgCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY))
                        .isEvenCollectionFinished())) {

            doHackyCleanup(); // TODO remove

            SpanWrapper outerLevelSpanWrapper = spanStore.getOuterLevelSpanWrapper();
            spanStore.finishActiveSpan(outerLevelSpanWrapper.getId(), outerLevelSpanWrapper.getStatisticDataUnit());
            System.out.println("Finished Span - currentIndex: " + outerLevelSpanWrapper.getStatisticDataUnit().getCurrentIndex() +
                    ", statisticsId: " + outerLevelSpanWrapper.getStatisticDataUnit().getStatisticId());


        }
    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType) {
        StackedSequenceInfo stackedSequenceInfo = findStackedSequenceInfo(synCtx, seqName, seqType, false);
        if (stackedSequenceInfo != null) {
            Span parentSpan =
                    DefaultParentResolver.resolveParent(stackedSequenceInfo.getStatisticDataUnit(), spanStore);
            Span span = tracer.buildSpan(stackedSequenceInfo.getStatisticDataUnit().getComponentName()).asChildOf(parentSpan).start();

            StatisticDataUnit statisticDataUnit = stackedSequenceInfo.getStatisticDataUnit();
            setDebugSpanTags(span, "", statisticDataUnit); // TODO remove this absoluteIndex story?

            String spanId = stackedSequenceInfo.getSpanReferenceId();
            SpanWrapper spanWrapper = spanStore.addActiveSpan(spanId, span, statisticDataUnit, synCtx);

            /*
            Mark the relevant span as active,
            so that the same sequence info can not be referred again during stack insertion in cases like clone
             */
            stackedSequenceInfo.setSpanActive(true);

            if (isOuterLevelSpan(statisticDataUnit)) {
                spanStore.assignOuterLevelSpan(spanWrapper);
            }

            System.out.println("Started Stacked Sequence Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
                    ", componentId: " + statisticDataUnit.getComponentId() +
                    ", statisticsId: " + statisticDataUnit.getStatisticId());
        } else {
            // TODO look carefully
            System.out.println("EXCEPTIONAL: Next Unstarted Call Mediator Sequence Reference is null");
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx) {
        if (continuationState instanceof SeqContinuationState) { // No other type will be kept track of // TODO ensure
            StackedSequenceInfo stackedSequenceInfo =
                    findStackedSequenceInfo(
                            synCtx,
                            ((SeqContinuationState)continuationState).getSeqName(),
                            ((SeqContinuationState)continuationState).getSeqType(),
                            true);
            if (stackedSequenceInfo != null) {
                finishStackedSequenceSpan(synCtx, stackedSequenceInfo);
                spanStore.getStackedSequences().remove(stackedSequenceInfo);
                System.out.println("Finished Span - currentIndex: " +
                        stackedSequenceInfo.getStatisticDataUnit().getCurrentIndex() +
                        ", statisticsId: " + stackedSequenceInfo.getStatisticDataUnit().getStatisticId());
            } else {
                // TODO look carefully
                System.out.println("EXCEPTIONAL: Found StackedSequenceInfo is null");
            }
            printStateInfoForDebug(synCtx);
        }
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) { // TODO introduce reentrant lock?
        List<StackedSequenceInfo> stackedSequences = spanStore.getStackedSequences();
        for (StackedSequenceInfo stackedSequence : stackedSequences) {
            finishStackedSequenceSpan(synCtx, stackedSequence);
        }
        stackedSequences.clear();
    }

    private void beginSpan(String absoluteIndex, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        Span parentSpan = DefaultParentResolver.resolveParent(statisticDataUnit, spanStore);
        Span span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
        setDebugSpanTags(span, absoluteIndex, statisticDataUnit);

        SpanWrapper spanWrapper = spanStore.addActiveSpan(absoluteIndex, span, statisticDataUnit, synCtx);

        if (isOuterLevelSpan(statisticDataUnit)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }

        System.out.println("Started Span - " + statisticDataUnit.getCurrentIndex() + "(" + absoluteIndex + ")" +
                ", componentId: " + statisticDataUnit.getComponentId() +
                ", statisticsId: " + statisticDataUnit.getStatisticId());

        spanStore.printActiveSpans();
    }

    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        String spanWrapperId = Util.extractId(basicStatisticDataUnit);

        SpanWrapper spanWrapper = spanStore.getActiveSpans().get(spanWrapperId);
        if (!isOuterLevelSpan(spanWrapper.getStatisticDataUnit()) || canEndOuterLevelSpan(synCtx)) {
            spanStore.finishActiveSpan(spanWrapperId, basicStatisticDataUnit);
            System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                    ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        }
        // Else - Absorb. Will be handled on report callback handling completion

        spanStore.printActiveSpans();
    }

    private boolean isOuterLevelSpan(StatisticDataUnit statisticDataUnit) {
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
        spanStore.finishActiveSpan(spanWrapperId, null);
        System.out.println("");
        System.out.println("Finished Call Mediator Sequence Span - ComponentName: " +
                stackedSequenceInfo.getStatisticDataUnit().getComponentName());
        System.out.println("");
    }

    private StackedSequenceInfo findStackedSequenceInfo(MessageContext synCtx, String seqName, SequenceType seqType,
                                                        boolean isSpanActive) {
        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getStackedSequences()) {
            if (seqType.toString().equals(stackedSequenceInfo.getStatisticDataUnit().getComponentName()) &&
                    (stackedSequenceInfo.isSpanActive() == isSpanActive)) {
                if (isSpanActive) {
                    stackedSequenceInfo.setSpanActive(false);
                }
                return stackedSequenceInfo;
            }
        }
        return null;
    }

    // TODO confirm and remove. old
    private StackedSequenceInfo getNextUnStartedStackedSequence() {
        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getStackedSequences()) {
            if (!stackedSequenceInfo.isSpanActive()) {
                return stackedSequenceInfo;
            }
        }
        return null;
    }

    public void setDebugSpanTags(Span span, String absoluteIndex, StatisticDataUnit statisticDataUnit) {
        if (statisticDataUnit != null) {
            span.setTag("debug.allIds",
                    statisticDataUnit.getCurrentIndex() + "(" + absoluteIndex + ")[" +statisticDataUnit.getParentIndex() + "]");
            span.setTag("debug.absoluteIndex", absoluteIndex);

            // BasicStatisticUnit level (parent)
            span.setTag("debug.currentIndex", statisticDataUnit.getCurrentIndex());
            span.setTag("debug.statisticId", statisticDataUnit.getStatisticId());
            span.setTag("debug.isTracingEnabled", statisticDataUnit.isTracingEnabled());
            span.setTag("debug.isOutOnlyFlow", statisticDataUnit.isOutOnlyFlow());

            // StatisticDataUnit level
            span.setTag("debug.parentIndex", statisticDataUnit.getParentIndex());
            span.setTag("debug.shouldTrackParent", statisticDataUnit.isShouldTrackParent());
            span.setTag("debug.continuationCall", statisticDataUnit.isContinuationCall());
            span.setTag("debug.flowContinuableMediator", statisticDataUnit.isFlowContinuableMediator());
            span.setTag("debug.flowSplittingMediator", statisticDataUnit.isFlowSplittingMediator());
            span.setTag("debug.flowAggregateMediator", statisticDataUnit.isFlowAggregateMediator());
            span.setTag("debug.isIndividualStatisticCollected", statisticDataUnit.isIndividualStatisticCollected());

            if (statisticDataUnit.getParentList() != null) {
                StringBuilder parentList = new StringBuilder("[");
                for (Integer integer : statisticDataUnit.getParentList()) {
                    parentList.append(integer).append(", ");
                }
                parentList.append("]");
                span.setTag("debug.parentList", parentList.toString());
            } else {
                span.setTag("debug.parentList", "null");
            }
        }
        span.setTag("debug.threadId", Thread.currentThread().getId());
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
//        printStack(synCtx);
        spanStore.printActiveSpans();
        spanStore.printStackedSequences();
    }
}

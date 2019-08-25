package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver.DefaultParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.StackedSequenceInfo;
import org.apache.synapse.continuation.ReliantContinuationState;
import org.apache.synapse.continuation.SeqContinuationState;

import java.util.Stack;

// Implemented for Flows with Call Mediator.
@Deprecated // TODO Remove this when no more needed
public class DefaultHandler implements JaegerTracingHandler {
    private JaegerTracer tracer;
    private SpanStore spanStore;

    public DefaultHandler(JaegerTracer tracer, SpanStore spanStore) {
        this.tracer = tracer;
        this.spanStore = spanStore;
    }

    @Override // TODO old approach. Consider removing?
    public void handleOpenEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan) {
        if (!isSequence(statisticDataUnit)) {
            beginSpan(statisticDataUnit, synCtx, parentSpan);
        } else {
            // Will begin during addSeqContinuationState
            markCallMediatorSequenceIn(statisticDataUnit); // TODO fix. Absorb for now
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handleOpenEvent(absoluteId, statisticDataUnit, synCtx, null);
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handleOpenEvent(absoluteId, statisticDataUnit, synCtx, null);
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handleOpenEvent(absoluteId, statisticDataUnit, synCtx, null);
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handleOpenEvent(absoluteId, statisticDataUnit, synCtx, null);
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handleOpenEvent(absoluteId, statisticDataUnit, synCtx, null);
    }

    @Override
    public void handleOpenFlowAsynchronousEvent(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // TODO
    }

    @Override
    public void handleOpenContinuationEvents(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // TODO
    }

    @Override // TODO old. consider removing?
    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        if (!isSequence(basicStatisticDataUnit)) {
            endSpan(basicStatisticDataUnit, synCtx);
        }
        // Else: Will end during pop from stack
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handleCloseEvent(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handleCloseEvent(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb.
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {

    }

    @Override
    public void handleCallbackCompletionEvent(MessageContext oldMssageContext, String callbackId) {

    }

    @Override
    public void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId) {

    }

    @Override
    public void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {

    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx) {
        StackedSequenceInfo stackedSequenceInfo = getNextUnstartedCallMediatorSequenceReference();
        if (stackedSequenceInfo != null) {
            String spanId = stackedSequenceInfo.getSpanReferenceId();
            Span chosenParentSpan = DefaultParentResolver.resolveParent(stackedSequenceInfo.getStatisticDataUnit(),
                    spanStore);
            Span span = tracer.buildSpan(stackedSequenceInfo.getStatisticDataUnit().getComponentName()).asChildOf(chosenParentSpan).start();

            // TODO set span tags
            String parentSpanToString = chosenParentSpan.context().toString();
            span.setTag("chosenParentSpan", parentSpanToString);
            StatisticDataUnit statisticDataUnit = stackedSequenceInfo.getStatisticDataUnit();
            setSpanTags(statisticDataUnit, span);

            spanStore.addActiveSpan(spanId, span, statisticDataUnit, synCtx);
            stackedSequenceInfo.setStarted(true);

            System.out.println("");
            System.out.println("Started Call Mediator Sequence Span - ComponentName: " +
                    stackedSequenceInfo.getStatisticDataUnit().getComponentName());
            System.out.println("");
        } else {
            // TODO look carefully
            System.out.println("EXCEPTIONAL: Next Unstarted Call Mediator Sequence Reference is null");
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleStateStackRemoval(MessageContext synCtx) {
        StackedSequenceInfo stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
        if (stackedSequenceInfo != null) {
            finishSpanForStackedSequenceInfo(synCtx, stackedSequenceInfo);
        } else {
            // TODO look carefully
            System.out.println("EXCEPTIONAL: Popped StackedSequenceInfo is null");
        }
        printStateInfoForDebug(synCtx);
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
        StackedSequenceInfo stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
        while (stackedSequenceInfo != null) {
            finishSpanForStackedSequenceInfo(synCtx, stackedSequenceInfo);

            printStateInfoForDebug(synCtx);

            stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
        }
    }

    @Override
    public void handleCloseOuterLevelSpan() {

    }

    private boolean isSequence(BasicStatisticDataUnit basicStatisticDataUnit) {
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
            // Came in when starting
            return ((StatisticDataUnit) basicStatisticDataUnit).getComponentType() == ComponentType.SEQUENCE;
        }
        // Came in when finishing
        return spanStore.containsActiveCallMediatorSequenceWithId(
                String.valueOf(basicStatisticDataUnit.getCurrentIndex()));
    }

    private void markCallMediatorSequenceIn(StatisticDataUnit statisticDataUnit) {
        StackedSequenceInfo stackedSequenceInfo = new StackedSequenceInfo(statisticDataUnit);
        spanStore.pushToActiveCallMediatorSequences(stackedSequenceInfo);
    }

    private synchronized void finishSpanForStackedSequenceInfo(MessageContext synCtx, StackedSequenceInfo stackedSequenceInfo) {
        String spanWrapperId = stackedSequenceInfo.getSpanReferenceId();
        spanStore.finishActiveSpan(spanWrapperId, null);
        System.out.println("");
        System.out.println("Finished Call Mediator Sequence Span - ComponentName: " +
                stackedSequenceInfo.getStatisticDataUnit().getComponentName());
        System.out.println("");
    }

    private void beginSpan(StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span rootSpan) {
        String spanId = Util.extractId(statisticDataUnit);
        Span parentSpan = DefaultParentResolver.resolveParent(statisticDataUnit, spanStore);
        Span span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
        setSpanTags(statisticDataUnit, span);

        spanStore.addActiveSpan(spanId, span, statisticDataUnit, synCtx);

        System.out.println("");
        System.out.println("Started Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
                ", componentId: " + statisticDataUnit.getComponentId() +
                ", statisticsId: " + statisticDataUnit.getStatisticId());
        System.out.println("");
    }

    public void setSpanTags(StatisticDataUnit statisticDataUnit, Span span) {
        if (statisticDataUnit != null) {
            // BasicStatisticUnit level (parent)
            span.setTag("basicUnit.currentIndex", statisticDataUnit.getCurrentIndex());
            span.setTag("basicUnit.statisticId", statisticDataUnit.getStatisticId());
            span.setTag("basicUnit.isTracingEnabled", statisticDataUnit.isTracingEnabled());
            span.setTag("basicUnit.isOutOnlyFlow", statisticDataUnit.isOutOnlyFlow());

            // StatisticDataUnit level
            span.setTag("unit.parentIndex", statisticDataUnit.getParentIndex());
            span.setTag("unit.shouldTrackParent", statisticDataUnit.isShouldTrackParent());
            span.setTag("unit.continuationCall", statisticDataUnit.isContinuationCall());
            span.setTag("unit.flowContinuableMediator", statisticDataUnit.isFlowContinuableMediator());
            span.setTag("unit.flowSplittingMediator", statisticDataUnit.isFlowSplittingMediator());
            span.setTag("unit.flowAggregateMediator", statisticDataUnit.isFlowAggregateMediator());
            span.setTag("unit.isIndividualStatisticCollected", statisticDataUnit.isIndividualStatisticCollected());
            span.setTag("unit.payload", statisticDataUnit.getPayload());
            span.setTag("unit.componentName", statisticDataUnit.getComponentName());
            span.setTag("unit.componentType", statisticDataUnit.getComponentType().toString());
            span.setTag("unit.componentId", statisticDataUnit.getComponentId());
            span.setTag("unit.hashCode", statisticDataUnit.getHashCode());

            if (statisticDataUnit.getParentList() != null) {
                StringBuilder parentList = new StringBuilder("[");
                for (Integer integer : statisticDataUnit.getParentList()) {
                    parentList.append(integer).append(", ");
                }
                parentList.append("]");
                span.setTag("unit.parentList", parentList.toString());
            } else {
                span.setTag("unit.parentList", "null");
            }
        }
        span.setTag("threadId", Thread.currentThread().getId());
    }

    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        String spanWrapperId = Util.extractId(basicStatisticDataUnit);
        spanStore.finishActiveSpan(spanWrapperId, null);

        System.out.println("");
        System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        System.out.println("");

        System.out.println("\tEnd span event. Not a stack event");
    }

    private synchronized StackedSequenceInfo getNextUnstartedCallMediatorSequenceReference() {
        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getActiveStackedSequences()) {
            if (!stackedSequenceInfo.isStarted()) {
                return stackedSequenceInfo;
            }
        }
        return null;
    }

    // TODO remove
    private static synchronized void printStack(MessageContext synCtx) {
        Stack<ContinuationState> continuationStatesStack = synCtx.getContinuationStateStack();
        if (continuationStatesStack != null) {
            System.out.println("\tStack:");
            System.out.print("\t\t[");
            for (ContinuationState continuationState : continuationStatesStack) {
                if (continuationState instanceof SeqContinuationState) {
                    SeqContinuationState seqContinuationState = (SeqContinuationState) continuationState;
                    System.out.print(seqContinuationState.getPosition() + ". Name:" + seqContinuationState.getSeqName() + "(Type: " + seqContinuationState.getSeqType() + ")");
                } else if (continuationState instanceof ReliantContinuationState) {
                    ReliantContinuationState reliantContinuationState = (ReliantContinuationState) continuationState;
                    System.out.print(reliantContinuationState.getPosition() + ". SubBranch: " + reliantContinuationState.getSubBranch());
                }
            }
            System.out.println("]");
        }
    }

    // TODO remove
    private synchronized void printStateInfoForDebug(MessageContext synCtx) {
//        printStack(synCtx);
        spanStore.printActiveSpans();
//        spanStore.printactiveCallMediatorSequences();
    }
}

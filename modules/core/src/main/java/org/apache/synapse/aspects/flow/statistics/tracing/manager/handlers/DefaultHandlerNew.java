package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.spanextend.SpanExtendingCounter;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver.DefaultParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

// Handles everything for now. TODO Add Handlers when special cases are required
public class DefaultHandlerNew implements JaegerTracingHandler {
    private JaegerTracer tracer;
    private SpanStore spanStore;

    public DefaultHandlerNew(JaegerTracer tracer, SpanStore spanStore) {
        this.tracer = tracer;
        this.spanStore = spanStore;
    }

    @Override // TODO old approach. Consider removing?
    public void handleOpenEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        beginSpan(absoluteId, statisticDataUnit, synCtx);
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
        endSpan(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        endSpan(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        endSpan(basicStatisticDataUnit, synCtx);
        // TODO for now, ignore if not there. But need to be sophisticated i guess
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb. // TODO confirm
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
        if (SpanExtendingCounter.getValue() == 0) {
            SpanWrapper outerLevelSpanWrapper = spanStore.getOuterLevelSpanWrapper();
            spanStore.finishActiveSpan(outerLevelSpanWrapper.getId(), outerLevelSpanWrapper.getStatisticDataUnit());
            System.out.println("Finished Span - currentIndex: " + outerLevelSpanWrapper.getStatisticDataUnit().getCurrentIndex() +
                    ", statisticsId: " + outerLevelSpanWrapper.getStatisticDataUnit().getStatisticId());
        }
    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx) {
    }

    @Override
    public void handleStateStackRemoval(MessageContext synCtx) {
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
    }

    @Override
    public void handleCloseOuterLevelSpan() {
        spanStore.getOuterLevelSpanWrapper();
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

    private boolean isOuterLevelSpan(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType() == ComponentType.PROXYSERVICE; // TODO what about other components
    }

//    public void setSpanTags(Span span, String absoluteIndex, StatisticDataUnit statisticDataUnit) {
//        if (statisticDataUnit != null) {
//            span.setTag("1.allIds", statisticDataUnit.getCurrentIndex() + "(" + absoluteIndex + ")[" +statisticDataUnit.getParentIndex() + "]");
//            span.setTag("absoluteIndex", absoluteIndex);
//
//            // BasicStatisticUnit level (parent)
//            span.setTag("basicUnit.currentIndex", statisticDataUnit.getCurrentIndex());
//            span.setTag("basicUnit.statisticId", statisticDataUnit.getStatisticId());
//            span.setTag("basicUnit.isTracingEnabled", statisticDataUnit.isTracingEnabled());
//            span.setTag("basicUnit.isOutOnlyFlow", statisticDataUnit.isOutOnlyFlow());
//
//            // StatisticDataUnit level
//            span.setTag("unit.parentIndex", statisticDataUnit.getParentIndex());
//            span.setTag("unit.shouldTrackParent", statisticDataUnit.isShouldTrackParent());
//            span.setTag("unit.continuationCall", statisticDataUnit.isContinuationCall());
//            span.setTag("unit.flowContinuableMediator", statisticDataUnit.isFlowContinuableMediator());
//            span.setTag("unit.flowSplittingMediator", statisticDataUnit.isFlowSplittingMediator());
//            span.setTag("unit.flowAggregateMediator", statisticDataUnit.isFlowAggregateMediator());
//            span.setTag("unit.isIndividualStatisticCollected", statisticDataUnit.isIndividualStatisticCollected());
//            span.setTag("unit.payload", statisticDataUnit.getPayload());
//            span.setTag("unit.componentName", statisticDataUnit.getComponentName());
//            span.setTag("unit.componentType", statisticDataUnit.getComponentType().toString());
//            span.setTag("unit.componentId", statisticDataUnit.getComponentId());
//            span.setTag("unit.hashCode", statisticDataUnit.getHashCode());
//
//            if (statisticDataUnit.getParentList() != null) {
//                StringBuilder parentList = new StringBuilder("[");
//                for (Integer integer : statisticDataUnit.getParentList()) {
//                    parentList.append(integer).append(", ");
//                }
//                parentList.append("]");
//                span.setTag("unit.parentList", parentList.toString());
//            } else {
//                span.setTag("unit.parentList", "null");
//            }
//        }
//        span.setTag("threadId", Thread.currentThread().getId());
//    }

    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        String spanWrapperId = Util.extractId(basicStatisticDataUnit);

        SpanWrapper spanWrapper = spanStore.getActiveSpans().get(spanWrapperId);
        if (!isOuterLevelSpan(spanWrapper.getStatisticDataUnit())) {
            spanStore.finishActiveSpan(spanWrapperId, basicStatisticDataUnit);
            System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                    ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        } else if (SpanExtendingCounter.getValue() == 0 && ((StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY)).isEvenCollectionFinished()) {
            spanStore.finishActiveSpan(spanWrapperId, basicStatisticDataUnit);
            System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                    ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        }
        // Else - Absorb. Will be handled on report callback handling completion

        spanStore.printActiveSpans();
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
}

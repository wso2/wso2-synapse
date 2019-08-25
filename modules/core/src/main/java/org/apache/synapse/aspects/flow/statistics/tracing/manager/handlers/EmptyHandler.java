package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

@Deprecated // TODO Remove
public class EmptyHandler implements JaegerTracingHandler {
    @Override
    public void handleOpenEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan) {

    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenFlowAsynchronousEvent(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleOpenContinuationEvents(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {

    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {

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

    }

    @Override
    public void handleStateStackRemoval(MessageContext synCtx) {

    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {

    }

    @Override
    public void handleCloseOuterLevelSpan() {

    }
}

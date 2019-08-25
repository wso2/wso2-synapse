package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

public interface JaegerTracingHandler {
    // Open Events

    // TODO old. Consider removing
    void handleOpenEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan);

    void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    void handleOpenFlowAsynchronousEvent(String absoluteId, BasicStatisticDataUnit statisticDataUnit,
                                         MessageContext synCtx);

    void handleOpenContinuationEvents(String absoluteId, BasicStatisticDataUnit statisticDataUnit,
                                      MessageContext synCtx);


    // Close Events

    // TODO old. Consider removing
    void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);


    // Callback Events

    void handleAddCallback(MessageContext messageContext, String callbackId);

    void handleCallbackCompletionEvent(MessageContext oldMssageContext, String callbackId);

    void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId);

    void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId);


    // Stack Events // TODO Check whether necessary

    void handleStateStackInsertion(MessageContext synCtx);

    void handleStateStackRemoval(MessageContext synCtx);

    void handleStateStackClearance(MessageContext synCtx);

    void handleCloseOuterLevelSpan();
}

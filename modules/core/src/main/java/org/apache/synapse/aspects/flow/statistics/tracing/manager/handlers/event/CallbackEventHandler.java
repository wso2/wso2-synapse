package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event;

import org.apache.synapse.MessageContext;

public interface CallbackEventHandler {
    void handleAddCallback(MessageContext messageContext, String callbackId);

    void handleCallbackCompletionEvent(MessageContext oldMessageContext, String callbackId);

    void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId);

    void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId);
}

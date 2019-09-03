package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event;

import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;

public interface ContinuationStateStackEventHandler {
    void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType);

    void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx);

    void handleStateStackClearance(MessageContext synCtx);
}

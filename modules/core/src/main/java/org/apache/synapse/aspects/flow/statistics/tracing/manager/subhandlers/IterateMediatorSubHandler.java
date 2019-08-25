package org.apache.synapse.aspects.flow.statistics.tracing.manager.subhandlers;

import org.apache.synapse.MessageContext;

public class IterateMediatorSubHandler extends SubHandler {

    public IterateMediatorSubHandler(int totalIterations, MessageContext synCtx, Object referrer) {
        super(totalIterations, synCtx, referrer);
    }
}

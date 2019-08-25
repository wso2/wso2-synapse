package org.apache.synapse.aspects.flow.statistics.tracing.manager.subhandlers;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.tracing.holder.TracingManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.OpenTracingManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles mediators which have to be handled as special cases
 */
public abstract class SubHandler {
    protected AtomicInteger totalIterations;
    protected AtomicInteger finishedIterations;
    protected MessageContext synCtx;

    protected String spanId;
    protected Object referrer;

    public SubHandler(int totalIterations, MessageContext synCtx, Object referrer) {
        this.totalIterations = new AtomicInteger(totalIterations);
        this.finishedIterations = new AtomicInteger(0);
        this.synCtx = synCtx;
        this.referrer = referrer;
    }

    public void finishAnIteration() {
        finishedIterations.incrementAndGet();
    }

    public void cleanupWhenFinished() {
        if (hasMediatorCompleted()) {
            OpenTracingManager openTracingManager = TracingManagerHolder.getOpenTracingManager();
            // TODO finish necessary spans. Keep the Ids inside the subhandler
            openTracingManager.removeSubHandler(referrer);
        }
    }

    protected boolean hasMediatorCompleted() {
        return totalIterations.get() == finishedIterations.get();
    }
}

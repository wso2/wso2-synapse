package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.OpenTracingSpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.subhandlers.SubHandler;

public interface OpenTracingManager {
    void initializeTracer();

    void resolveHandler();

    OpenTracingSpanHandler getHandler();

    @Deprecated
    void addSubHandler(Object referrer, SubHandler subHandler);

    @Deprecated
    void removeSubHandler(Object referrer);

    void closeTracer();
}

package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event.CallbackEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event.CloseEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event.ContinuationStateStackEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.event.OpenEventHandler;

public interface OpenTracingSpanHandler
        extends OpenEventHandler, CloseEventHandler, CallbackEventHandler, ContinuationStateStackEventHandler {
}

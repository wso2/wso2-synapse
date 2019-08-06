package org.apache.synapse.aspects.flow.statistics.tracing.holder;

import org.apache.synapse.aspects.flow.statistics.tracing.manager.JaegerTracingManager;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.OpenTracingManager;

public class TracingManagerHolder {
    private static OpenTracingManager openTracingManager;

    static {
        resolveOpenTracingMananger(); // TODO senthuran re-add this
    }

    private static void resolveOpenTracingMananger() {
        // Only Jaeger Tracing available
        openTracingManager = new JaegerTracingManager();
    }

    public static OpenTracingManager getOpenTracingManager() {
        return openTracingManager;
    }
}

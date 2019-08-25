package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.spanextend;

import java.util.concurrent.atomic.AtomicInteger;

public class SpanExtendingCounter {
    private static AtomicInteger spanExtendingCounter = new AtomicInteger(0);

    public static int incrementAndGetValue() {
        return spanExtendingCounter.incrementAndGet();
    }

    public static int decrementAndGetValue() {
        return spanExtendingCounter.decrementAndGet();
    }

    public static int getValue() {
        return spanExtendingCounter.get();
    }
}

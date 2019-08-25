package org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates absolute Id's for span events. todo fix comment
 */
public class SpanEventCounter {
    private static AtomicInteger absoluteIndex = new AtomicInteger(-1);

    public static int getCurrentAbsoluteIndex() {
        return absoluteIndex.get();
    }

    public static int getNextAbsoluteIndex() {
        return absoluteIndex.incrementAndGet();
    }

    public static void setAbsoluteIndex(int index) {
        absoluteIndex = new AtomicInteger(index);
    }
}

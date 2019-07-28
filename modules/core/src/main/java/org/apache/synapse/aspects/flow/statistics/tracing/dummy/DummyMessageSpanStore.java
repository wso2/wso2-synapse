package org.apache.synapse.aspects.flow.statistics.tracing.dummy;

import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains message spans during Jaeger tracing.
 */
public class DummyMessageSpanStore {
    /**
     * Spans that have been already started
     */
    private Map<String, Span> activeSpans;

    public DummyMessageSpanStore() {
        this.activeSpans = new HashMap<String, Span>();
    }

    public void addActiveSpan(String spanId, Span activeSpan) {
        System.out.println("Start Span");
        activeSpans.put(spanId, activeSpan);
    }

    public void finishActiveSpan(String spanId) {
        System.out.println("End Span");
        Span span = getSpanById(spanId);
        if (span != null) {
            span.finish();
            removeSpan(spanId);
        }
        // TODO: 2019-07-22 Implement.
        // TODO: 2019-07-22 Revise what is the 'key'
    }

    private Span getSpanById(String spanId) {
        for (Map.Entry<String, Span> activeSpan : activeSpans.entrySet()) {
            if (activeSpan.getKey().equals(spanId)) {
                return activeSpan.getValue();
            }
        }
        return null;
    }

    private void removeSpan(String spanId) {
        activeSpans.remove(spanId);
    }

    public boolean isNoActiveSpans() {
        return activeSpans.size() == 0;
    }
}


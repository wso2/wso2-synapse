package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores and managers spans.
 */
public class SpanStore {
    private Span rootSpan;
    private Map<String, Span> activeSpans;

    public SpanStore() {
        this.activeSpans = new HashMap<>();
    }

    public Span getRootSpan() {
        return rootSpan;
    }

    public void setRootSpan(Span rootSpan) {
        this.rootSpan = rootSpan;
    }

    public void addActiveSpan(String spanId, Span activeSpan) {
        activeSpans.put(spanId, activeSpan);
    }

    public void finishActiveSpan(String spanId) {
        Span span = getSpanById(spanId, activeSpans);
        if (span != null) {
            span.finish();
            removeSpan(spanId, activeSpans);
        }
    }

    private Span getSpanById(String spanId, Map<String, Span> spans) {
        for (Map.Entry<String, Span> spanEntry : spans.entrySet()) {
            if (spanEntry.getKey().equals(spanId)) {
                return spanEntry.getValue();
            }
        }
        return null;
    }

    private void removeSpan(String spanId, Map<String, Span> spans) {
        spans.remove(spanId);
    }
}

package org.apache.synapse.aspects.flow.statistics.tracing.old;

import io.opentracing.Span;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains message spans during Jaeger tracing.
 */
public class MessageSpanStore {
    /**
     * Spans that have been already started
     */
    private Map<String, Span> activeSpans;

    public MessageSpanStore() {
        this.activeSpans = new HashMap<>();
    }

    public void addActiveSpan(String spanId, Span activeSpan) {
        activeSpans.put(spanId, activeSpan);
    }

    public void finishActiveSpan(String spanId) {
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

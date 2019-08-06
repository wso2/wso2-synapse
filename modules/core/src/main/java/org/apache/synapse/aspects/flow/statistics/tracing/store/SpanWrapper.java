package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;

public class SpanWrapper {
    private Span span;
    private boolean isCloseable; // Is eligible for closing

    private String uniqueElementId;

    public SpanWrapper(Span span, boolean isCloseable, String uniqueElementId) {
        this.span = span;
        this.isCloseable = isCloseable;
        this.uniqueElementId = uniqueElementId;
    }

    public boolean isCloseable() {
        return isCloseable;
    }

    public void setCloseable(boolean closeable) {
        isCloseable = closeable;
    }

    public SpanWrapper(Span span) {
        this.span = span;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public String getUniqueElementId() {
        return uniqueElementId;
    }

    public void setUniqueElementId(String uniqueElementId) {
        this.uniqueElementId = uniqueElementId;
    }
}

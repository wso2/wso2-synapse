package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

public class SpanWrapper {
    private String id;
    private Span span;
    private StatisticDataUnit statisticDataUnit;
    private boolean isCloseable; // Is eligible for closing

    public SpanWrapper(String id, Span span, StatisticDataUnit statisticDataUnit, boolean isCloseable) {
        this.id = id;
        this.span = span;
        this.statisticDataUnit = statisticDataUnit;
        this.isCloseable = isCloseable;
    }

    public String getId() {
        return id;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public StatisticDataUnit getStatisticDataUnit() {
        return statisticDataUnit;
    }

    public void setStatisticDataUnit(StatisticDataUnit statisticDataUnit) {
        this.statisticDataUnit = statisticDataUnit;
    }

    public boolean isCloseable() {
        return isCloseable;
    }
}

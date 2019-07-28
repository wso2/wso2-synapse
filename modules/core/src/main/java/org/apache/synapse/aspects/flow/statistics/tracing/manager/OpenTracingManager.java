package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

public interface OpenTracingManager {
    void initializeTracer();

    void closeTracer();

    void startSpan(StatisticDataUnit statisticDataUnit, Span parentSpan);

    void setSpanTags(StatisticDataUnit statisticDataUnit, Span span);

    void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit);
}

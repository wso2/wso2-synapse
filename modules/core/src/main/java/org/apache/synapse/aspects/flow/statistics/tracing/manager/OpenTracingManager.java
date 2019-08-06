package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

public interface OpenTracingManager {
    void initializeTracer();

    void closeTracer();

    void startSpan(StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan);

    void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    void startNextCallMediatorSequenceSpan(MessageContext synCtx);

    void endNextCallMediatorSequenceSpan(MessageContext synCtx);
}

package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;

public class JaegerTracingManager implements OpenTracingManager {
    private JaegerTracer tracer;
    private SpanStore spanStore = new SpanStore();

    public JaegerTracingManager() {
        initializeTracer();
    }

    @Override
    public void initializeTracer() {
        String serviceName = "test-synapse-service"; // TODO get from a global place

        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1)
                .withManagerHostPort("localhost:5778");
        Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
                .withAgentHost("localhost")
                .withAgentPort(6831);
        Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration()
                .withLogSpans(false)
                .withSender(sender)
                .withMaxQueueSize(123)
                .withFlushInterval(12345);

        this.tracer = new Configuration(serviceName)
                .withSampler(sampler)
                .withReporter(reporter)
                .getTracer();
    }

    @Override
    public void closeTracer() {
        this.tracer.close();
    }

    @Override
    public void startSpan(StatisticDataUnit statisticDataUnit, Span parentSpan) {
        String spanId = getIdOf(statisticDataUnit);
        Span span;
        if (parentSpan != null) {
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
        } else {
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(spanStore.getRootSpan()).start();
        }
        setSpanTags(statisticDataUnit, span);
        spanStore.addActiveSpan(spanId, span);
    }

    @Override
    public void setSpanTags(StatisticDataUnit statisticDataUnit, Span span) {
        span.setTag("sampleTag", "statisticDataUnit.something"); // TODO all
    }

    @Override
    public void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit) {
        String spanId = getIdOf(basicStatisticDataUnit);
        spanStore.finishActiveSpan(spanId);
    }

    private static String getIdOf(BasicStatisticDataUnit basicStatisticDataUnit) {
        return String.valueOf(basicStatisticDataUnit.getCurrentIndex()); // TODO return as int if works
    }

    public void startRootSpan() {
        Span rootSpan = tracer.buildSpan("rootSpan").start();
        rootSpan.setTag("rootSpan", "rootSpa ");
        spanStore.setRootSpan(rootSpan);
    }

    public void endRootSpan() {
        spanStore.getRootSpan().finish();
        spanStore.setRootSpan(null);
    }
}

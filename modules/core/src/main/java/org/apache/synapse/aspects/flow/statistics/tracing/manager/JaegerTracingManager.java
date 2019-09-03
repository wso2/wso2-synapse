package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.internal.senders.NoopSender;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.DefaultSpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.JaegerTracingSpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.subhandlers.SubHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;

import java.util.HashMap;
import java.util.Map;

public class JaegerTracingManager implements OpenTracingManager {
    private SpanStore spanStore;
    private JaegerTracer tracer;
    private JaegerTracingSpanHandler handler;

    @Deprecated
    private Map<Object, SubHandler> subHandlers;

    public JaegerTracingManager() {
        spanStore = new SpanStore();
        initializeTracer();
        resolveHandler();
        subHandlers = new HashMap<>();
    }

    @Override
    public void initializeTracer() {
        String serviceName = "test-synapse-service"; // TODO get from a global place

        System.out.println("Initialize Tracer");

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
    public void resolveHandler() {
        this.handler = new DefaultSpanHandler(tracer, spanStore); // TODO Make this to resolve programmatically
//        this.handler = new EmptySpanHandler();
    }

    public JaegerTracingSpanHandler getHandler() {
        return this.handler;
    }

    @Override
    public void closeTracer() {
        this.tracer.close();
    }

    @Override
    @Deprecated
    public void addSubHandler(Object referrer, SubHandler subHandler) {
        this.subHandlers.put(referrer, subHandler);
    }

    @Override
    @Deprecated
    public void removeSubHandler(Object referrer) {
        this.subHandlers.remove(referrer);
    }
}

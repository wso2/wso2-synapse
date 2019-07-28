package org.apache.synapse.aspects.flow.statistics.tracing.old;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * Handles message spans in terms of Jaeger.
 */
public class TracingHandler {
    private static JaegerTracer tracer = initializeJaegerTracer("test-synapse-tracing");  // TODO Make this final
    private static MessageSpanStore messageSpanStore = new MessageSpanStore();

//    private static JaegerTracer initializeJaegerTracer(String service) {
//        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
//                .withType(ConstSampler.TYPE)
//                .withParam(1);
//
//        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
//                .withLogSpans(true);
//
//        Configuration config = new Configuration(service)
//                .withSampler(samplerConfig)
//                .withReporter(reporterConfig);
//
//        return config.getTracer();
//    }

    public static JaegerTracer initializeJaegerTracer(String service) {
        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1)
                .withManagerHostPort("localhost:5778");
//        Sender sender = new UdpSender("localhost", 6831, 0);
        Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
                .withAgentHost("localhost")
                .withAgentPort(6831);
        Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration()
                .withLogSpans(false)
                .withSender(sender)
                .withMaxQueueSize(123)
                .withFlushInterval(12345);

        return new Configuration(service)
                .withSampler(sampler)
                .withReporter(reporter)
                .getTracer();
    }

    public static void startSpan(StatisticDataUnit statisticDataUnit, Span parentSpan) {
        // TODO: consider active context and all later
        String spanId = getIdOf(statisticDataUnit);

        Span span;
        if (parentSpan != null) {
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
        } else {
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(getRootSpan()).start();
        }

        span.setTag("sampleTag", "statisticDataUnit.something"); // TODO: Add other setTags
        // TODO: Implement further

        messageSpanStore.addActiveSpan(spanId, span);
    }

    public static void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit) {
        // TODO: 2019-07-22 Implement
        String spanId = getIdOf(basicStatisticDataUnit);
        messageSpanStore.finishActiveSpan(spanId);
        if (messageSpanStore.isNoActiveSpans()) {
            tracer.close();
        }
    }

    private static String getIdOf(BasicStatisticDataUnit basicStatisticDataUnit) {
        // TODO: 2019-07-22 implement for getting a maintainable index
        return String.valueOf(basicStatisticDataUnit.getCurrentIndex()); // TODO have this as int, if this works
    }

    private static Span getRootSpan() {
        return null; // TODO Store data about rootspan somehow. Return when requested
    }
}

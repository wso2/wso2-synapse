package org.apache.synapse.aspects.flow.statistics.tracing.dummy;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.tracing.old.TracingHandler;

/**
 * Handles message spans in terms of Jaeger.
 */
public class DummyTracingHandler {
    private static JaegerTracer tracer = initializeJaegerTracer("test-whether-working");  // TODO Make this final
    private static DummyMessageSpanStore messageSpanStore = new DummyMessageSpanStore();

    private static boolean canProceed = true; // TODO remove this

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

    private static JaegerTracer initializeJaegerTracer(String service) {
        return TracingHandler.initializeJaegerTracer(service);
    }

    public static void startSpan(String name, Span parentSpan) {
        // TODO: consider active context and all later
        if (canProceed) { // TODO remove
            String spanId = getIdOf(name);

            Span span;
            if (parentSpan != null) {
                span = tracer.buildSpan(name).asChildOf(parentSpan).start();
            } else {
                span = tracer.buildSpan(name).asChildOf(getRootSpan()).start();
            }

            span.setTag("sampleTag", "statisticDataUnit.something"); // TODO: Add other setTags
            // TODO: Implement further

            messageSpanStore.addActiveSpan(spanId, span);
        }
    }

    public static void finishSpan(String name) {
        // TODO: 2019-07-22 Implement
        String spanId = getIdOf(name);
        messageSpanStore.finishActiveSpan(spanId);
        if (messageSpanStore.isNoActiveSpans()) {
            tracer.close();
            canProceed = false; // TODO remove
        }
    }

    private static String getIdOf(String name) {
        // TODO: 2019-07-22 implement for getting a maintainable index
        return name;
    }

    private static Span getRootSpan() {
        return null; // TODO Store data about rootspan somehow. Return when requested
    }

    public static void runDummySimulation() {
        // TODO Senthuran added this [START]

        String[] names = {"Trace1", "Trace2", "Trace3"};


        try {
//            DummyTracingHandler.startSpan(names[0], null);
//            Thread.sleep(2500);
//            DummyTracingHandler.finishSpan(names[0]);

            DummyTracingHandler.startSpan(names[0], null);
            Thread.sleep(500);

            DummyTracingHandler.startSpan(names[1], null);
            Thread.sleep(600);
            DummyTracingHandler.finishSpan(names[0]);
            Thread.sleep(900);
            DummyTracingHandler.startSpan(names[2], null);
            Thread.sleep(600);
            DummyTracingHandler.finishSpan(names[1]);

            DummyTracingHandler.finishSpan(names[2]);
        } catch (InterruptedException e) {
            // DO nothing
        }

        // TODO Senthuran added this [END]
    }
}



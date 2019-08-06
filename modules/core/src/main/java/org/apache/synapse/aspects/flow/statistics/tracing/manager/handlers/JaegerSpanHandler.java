package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class JaegerSpanHandler {
    protected JaegerTracer tracer;
    protected SpanStore spanStore;

    public JaegerSpanHandler(JaegerTracer tracer, SpanStore spanStore) {
        this.tracer = tracer;
        this.spanStore = spanStore;
    }

    public abstract void startSpan(StatisticDataUnit statisticDataUnit, Span parentSpan);

    public abstract void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit);

    protected void beginSpan(StatisticDataUnit statisticDataUnit, Span rootSpan) {
        String spanId = getIdOf(statisticDataUnit);
        Span span;
        Span newParentSpan = resolveParentSpan(statisticDataUnit);

        // if (parentSpan != null) {
        if (newParentSpan != null) {
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(newParentSpan).start();
        } else {
            Span chosenParentSpan = getLatestActiveSpanAsParent();
            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(chosenParentSpan).start();
//            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(spanStore.getRootSpan()).start();
        }
        setSpanTags(statisticDataUnit, span);

//        spanStore.addActiveSpan(spanId, span); // TODO revise

        System.out.println("");
        System.out.println("Started Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
                ", componentId: " + statisticDataUnit.getComponentId() +
                ", statisticsId: " + statisticDataUnit.getStatisticId());
        System.out.println("");
    }

    protected void endSpan(BasicStatisticDataUnit basicStatisticDataUnit) {
        String spanId = getIdOf(basicStatisticDataUnit);
        spanStore.finishActiveSpan(spanId);
        System.out.println("");
        System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
                ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
        System.out.println("");
    }

    private static String getIdOf(BasicStatisticDataUnit basicStatisticDataUnit) {
        return String.valueOf(basicStatisticDataUnit.getCurrentIndex()); // TODO return as int if works
    }

    private Span resolveParentSpan(StatisticDataUnit statisticDataUnit) {
        if (statisticDataUnit.getCurrentIndex() == 6) {
            Object o = null;
        }
        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
        if (isSpanActive(parentId)) {
            System.out.println("");
            System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
            System.out.println("");
//            return spanStore.getActiveSpans().get(parentId); // TODO revise
            return null;
        }
        return null;
    }

    private boolean isSpanActive(String spanId) {
        return spanStore.getActiveSpans().containsKey(spanId);
    }

    private Span getLatestActiveSpanAsParent() {
        List<Integer> ids = new ArrayList<>();
        for (String id : spanStore.getActiveSpans().keySet()) {
            ids.add(Integer.valueOf(id));
        }
        if (!ids.isEmpty()) {
            Collections.sort(ids);
            int spanId = ids.get(ids.size() - 1);
//            return spanStore.getActiveSpans().get(String.valueOf(spanId)); // TODO revise
            return null;
        }
        return null;
    }

    public void setSpanTags(StatisticDataUnit statisticDataUnit, Span span) {
        span.setBaggageItem("componentType", statisticDataUnit.getComponentType().toString());
        span.setBaggageItem("componentId", statisticDataUnit.getComponentId());
        span.setTag("sampleTag", "statisticDataUnit.something"); // TODO add all tags
    }
}

package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;

import java.util.*;

/**
 * Stores and managers spans.
 */
public class SpanStore {
    private Map<String, SpanWrapper> activeSpans;
    private Stack<StackedSequenceInfo> activeCallMediatorSequences = new Stack<>();

    public SpanStore() {
        this.activeSpans = new HashMap<>();
    }

    public Map<String, SpanWrapper> getActiveSpans() {
        return activeSpans;
    }

    public Stack<StackedSequenceInfo> getActiveCallMediatorSequences() {
        return activeCallMediatorSequences;
    }

    public void addActiveSpan(String spanId, Span activeSpan, MessageContext synCtx) {
        SpanWrapper spanWrapper = createActiveSpanWrapper(activeSpan, synCtx);
        activeSpans.put(spanId, spanWrapper);
    }

    private SpanWrapper createActiveSpanWrapper(Span activeSpan, MessageContext synCtx) {
        return new SpanWrapper(activeSpan, true, ""); // TODO implement properly
    }

    public void finishActiveSpan(String spanWrapperId) {
        SpanWrapper spanWrapper = getSpanWrapperById(spanWrapperId);
        if (spanWrapper != null && spanWrapper.isCloseable()) {
            spanWrapper.getSpan().finish();
            removeSpanWrapper(spanWrapperId);
        }
    }

    private SpanWrapper getSpanWrapperById(String spanWrapperId) {
        for (Map.Entry<String, SpanWrapper> spanWrapperEntry : activeSpans.entrySet()) {
            if (spanWrapperEntry.getKey().equals(spanWrapperId)) {
                return spanWrapperEntry.getValue();
            }
        }
        return null;
    }

    private void removeSpanWrapper(String spanWrapperId) {
        activeSpans.remove(spanWrapperId);
    }

    public void pushToActiveCallMediatorSequences(StackedSequenceInfo stackedSequenceInfo) {
        activeCallMediatorSequences.push(stackedSequenceInfo);
    }

    public boolean containsActiveCallMediatorSequenceWithId(String id) {
        for (StackedSequenceInfo activeCallMediatorSequence : activeCallMediatorSequences) {
            if (Objects.equals(id, activeCallMediatorSequence.getStatisticDataUnitId())) {
                return true;
            }
        }
        return false;
    }

    public StackedSequenceInfo popActiveCallMediatorSequences() {
        return activeCallMediatorSequences.pop();
    }
}


























    // TODO Old Below. Remove when finalized
// public class SpanStore {

//    public Span getRootSpan() {
//        return rootSpan;
//    }
//
//    public void setRootSpan(Span rootSpan) {
//        this.rootSpan = rootSpan;
//    }
//
//    public Map<String, Span> getActiveSpans() {
//        return activeSpans;
//    }
//
//    public void addActiveSpan(String spanId, Span activeSpan) {
//        activeSpans.put(spanId, activeSpan);
//    }
//
//    public void finishActiveSpan(String spanId) {
//        Span span = getSpanById(spanId, activeSpans);
//        if (span != null) {
//            span.finish();
//            removeSpan(spanId, activeSpans);
//        }
//    }
//
//    public void finishActiveSpanWithEndTime(String spanId, Long endTime) {
//        Span span = getSpanById(spanId, activeSpans);
//        if (span != null) {
//            Long startTime = Long.parseLong(span.getBaggageItem("startTime"));
//            Long collectedDuration = endTime - startTime;
//            span.setTag("startTime", String.valueOf(startTime));
//            span.setTag("endTime", String.valueOf(endTime));
//            span.setTag("collectedDuration", String.valueOf(collectedDuration));
//            span.finish();
//            removeSpan(spanId, activeSpans);
//        }
//    }
//
//    public void setDuration(String spanId, Long endTime) {
//        Span span = getSpanById(spanId, activeSpans);
//        Long startTime = Long.parseLong(span.getBaggageItem("startTime"));
//        Long duration = endTime - startTime;
//        span.setBaggageItem("endTime", String.valueOf(endTime));
//        span.setBaggageItem("collectedDuration", String.valueOf(duration));
//    }
//
//    private Span getSpanById(String spanId, Map<String, Span> spans) {
//        for (Map.Entry<String, Span> spanEntry : spans.entrySet()) {
//            if (spanEntry.getKey().equals(spanId)) {
//                return spanEntry.getValue();
//            }
//        }
//        return null;
//    }
//
//    private void removeSpan(String spanId, Map<String, Span> spans) {
//        spans.remove(spanId);
//    }
// }

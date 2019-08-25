package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores and managers spans.
 */
public class SpanStore {
    private Map<String, SpanWrapper> activeSpans;
    private Stack<StackedSequenceInfo> activeStackedSequences = new Stack<>(); // TODO confirm and remove

    private Stack<SpanWrapper> eligibleAlternativeParents = new Stack<>();

    private SpanWrapper outerLevelSpan = null;

    public void assignOuterLevelSpan(SpanWrapper spanWrapper) {
        LOCK.lock();
        try {
            outerLevelSpan = spanWrapper;
        } finally {
            LOCK.unlock();
        }
    }

    public SpanWrapper getOuterLevelSpanWrapper() {
        return this.outerLevelSpan;
    }

    private final ReentrantLock LOCK = new ReentrantLock(true);

    public SpanStore() {
        this.activeSpans = new HashMap<>();
    }

    private void addEligibleAlternativeParent(SpanWrapper spanWrapper) {
        eligibleAlternativeParents.push(spanWrapper);
    }

    public SpanWrapper getAlternativeParent() {
        LOCK.lock();
        try {
            if (eligibleAlternativeParents.isEmpty()) {
                return null;
            }
            return eligibleAlternativeParents.peek();
        } finally {
            LOCK.unlock();
        }
    }

    public Map<String, SpanWrapper> getActiveSpans() {
        return activeSpans;
    }

    public Stack<StackedSequenceInfo> getActiveStackedSequences() {
        return activeStackedSequences;
    }

    public SpanWrapper addActiveSpan(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit,
                            MessageContext synCtx) {
        LOCK.lock();
        try {
            SpanWrapper spanWrapper = createActiveSpanWrapper(spanId, activeSpan, statisticDataUnit, synCtx);
            activeSpans.put(spanId, spanWrapper);

            // TODO experimental
            if (spanWrapper.getStatisticDataUnit().isFlowContinuableMediator()) {
                addEligibleAlternativeParent(spanWrapper);
            }
            return spanWrapper;
        } finally {
            LOCK.unlock();
        }
    }

    private SpanWrapper createActiveSpanWrapper(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // TODO Implement properly. Revise "isCloseable"
        return new SpanWrapper(spanId, activeSpan, statisticDataUnit, true);
    }

    public void finishActiveSpan(String spanWrapperId, BasicStatisticDataUnit basicStatisticDataUnit) {
        LOCK.lock();
        try {
            SpanWrapper spanWrapper = getSpanWrapperById(spanWrapperId);
            if (spanWrapper != null && spanWrapper.isCloseable() && spanWrapper.getSpan() != null) {
                if (spanWrapper.getStatisticDataUnit() != null) {
                    setSpanTags(spanWrapper, basicStatisticDataUnit);
                }
                spanWrapper.getSpan().finish();
//                removeSpanWrapper(spanWrapperId); TODO This should be removed i guess
            }
        } finally {
            LOCK.unlock();
        }
    }

    private void setSpanTags(SpanWrapper spanWrapper, BasicStatisticDataUnit basicStatisticDataUnit) {
        StatisticsLog statisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        StatisticDataUnit endEventDataUnit = (StatisticDataUnit) basicStatisticDataUnit;
        statisticsLog.setAfterPayload(endEventDataUnit.getPayload());
        span.setTag("noOfFaults", statisticsLog.getNoOfFaults());
        span.setTag("componentName", statisticsLog.getComponentName());
        span.setTag("componentType", statisticsLog.getComponentTypeToString());
        span.setTag("componentId", statisticsLog.getComponentId());
        span.setTag("hashcode", statisticsLog.getHashCode());
        span.setTag("beforePayload", statisticsLog.getBeforePayload());
        span.setTag("afterPayload", statisticsLog.getAfterPayload());
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
        LOCK.lock();
        try{
            activeStackedSequences.push(stackedSequenceInfo);
        } finally {
            LOCK.unlock();
        }
    }

    public boolean containsActiveCallMediatorSequenceWithId(String id) {
        for (StackedSequenceInfo activeCallMediatorSequence : activeStackedSequences) {
            if (Objects.equals(id, activeCallMediatorSequence.getSpanReferenceId())) {
                return true;
            }
        }
        return false;
    }

    public StackedSequenceInfo popActiveCallMediatorSequences() {
        LOCK.lock();
        try {
            if (activeStackedSequences == null || activeStackedSequences.isEmpty()) {
                return null;
            }
            return activeStackedSequences.pop();
        } finally {
            LOCK.unlock();
        }
    }

    public void printActiveSpans() {
        LOCK.lock();
        try {
            System.out.println("");
            System.out.print("\t\tActive Spans (Keys): [");
            for (String key : activeSpans.keySet()) {
                System.out.print(key + ", ");
            }
            System.out.println("]");
            System.out.println("");
        } finally {
            LOCK.unlock();
        }
    }

//    public void printactiveCallMediatorSequences() {
//        LOCK.lock();
//        try{
//            System.out.println("");
//            System.out.print("\t\tActive Call Mediator Sequences: [");
//            for (StackedSequenceInfo activeCallMediatorSequence : activeStackedSequences) {
//                System.out.print(activeCallMediatorSequence.getSpanReferenceId() + ". " +
//                        activeCallMediatorSequence.getStatisticDataUnit().getComponentName() + ", ");
//            }
//            System.out.println("]");
//            System.out.println("");
//        } finally {
//            LOCK.unlock();
//        }
//    }

    public void printactiveCallMediatorSequences() {
        LOCK.lock();
        try{
            System.out.println("");
            System.out.println("\t\tActive Call Mediator Sequences:");
            for (StackedSequenceInfo activeCallMediatorSequence : activeStackedSequences) {
                System.out.print(activeCallMediatorSequence.getSpanReferenceId() + ". " +
                        activeCallMediatorSequence.getStatisticDataUnit().getComponentName() + ", ");
            }
            System.out.println("]");
            System.out.println("");
        } finally {
            LOCK.unlock();
        }
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

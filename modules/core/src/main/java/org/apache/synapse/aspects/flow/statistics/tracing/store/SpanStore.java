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
    private final ReentrantLock LOCK = new ReentrantLock(true);

    private SpanWrapper outerLevelSpan;
    private Map<String, SpanWrapper> activeSpans;
    private List<StackedSequenceInfo> stackedSequences;
    private Stack<SpanWrapper> eligibleAlternativeParents;

    public SpanStore() {
        this.activeSpans = new HashMap<>();
        this.stackedSequences = new Stack<>();
        this.eligibleAlternativeParents = new Stack<>();
        this.outerLevelSpan = null;
    }


    // Active spans related

    public Map<String, SpanWrapper> getActiveSpans() {
        return activeSpans;
    }

    public synchronized SpanWrapper addActiveSpan(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit,
                                                  MessageContext synCtx) {
        SpanWrapper spanWrapper = createActiveSpanWrapper(spanId, activeSpan, statisticDataUnit, synCtx);
        activeSpans.put(spanId, spanWrapper);

        if (spanWrapper.getStatisticDataUnit().isFlowContinuableMediator()) {
            addEligibleAlternativeParent(spanWrapper);
        }
        return spanWrapper;
    }

    private SpanWrapper createActiveSpanWrapper(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // TODO Implement properly. Revise "isCloseable"
        return new SpanWrapper(spanId, activeSpan, statisticDataUnit, true);
    }

    public synchronized void finishActiveSpan(String spanWrapperId, BasicStatisticDataUnit basicStatisticDataUnit) {
        SpanWrapper spanWrapper = getSpanWrapperById(spanWrapperId);
        if (spanWrapper != null && spanWrapper.isCloseable() && spanWrapper.getSpan() != null) {
            if (spanWrapper.getStatisticDataUnit() != null) {
                setSpanTags(spanWrapper, basicStatisticDataUnit);
            }
            spanWrapper.getSpan().finish();
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


    // Stacked sequences related

    public List<StackedSequenceInfo> getStackedSequences() {
        return stackedSequences;
    }

    public synchronized void addStackedSequence(StackedSequenceInfo stackedSequenceInfo) {
        stackedSequences.add(stackedSequenceInfo);
    }

    public boolean containsStackedSequenceWithId(String id) {
        for (StackedSequenceInfo activeCallMediatorSequence : stackedSequences) {
            if (Objects.equals(id, activeCallMediatorSequence.getSpanReferenceId())) {
                return true;
            }
        }
        return false;
    }

    @Deprecated // TODO remove
    public synchronized StackedSequenceInfo popStackedSequences() {
        if (stackedSequences == null || stackedSequences.isEmpty()) {
            return null;
        }
        return null;
    }


    // Alternative parents related

    private void addEligibleAlternativeParent(SpanWrapper spanWrapper) {
        eligibleAlternativeParents.push(spanWrapper);
    }

    public synchronized SpanWrapper getAlternativeParent() {
        if (eligibleAlternativeParents.isEmpty()) {
            return null;
        }
        return eligibleAlternativeParents.peek();
    }


    // Outer level span related

    public synchronized void assignOuterLevelSpan(SpanWrapper spanWrapper) {
        outerLevelSpan = spanWrapper;
    }

    public SpanWrapper getOuterLevelSpanWrapper() {
        return this.outerLevelSpan;
    }


    // Others

    private void setSpanTags(SpanWrapper spanWrapper, BasicStatisticDataUnit basicStatisticDataUnit) {
        StatisticsLog statisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
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
    }

    // TODO remove
    public synchronized void printActiveSpans() {
        System.out.println("");
        System.out.print("\t\tActive Spans (Keys): [");
        for (String key : activeSpans.keySet()) {
            System.out.print(key + ", ");
        }
        System.out.println("]");
        System.out.println("");
    }

    // TODO remove
    public synchronized void printStackedSequences() {
        System.out.println("");
        System.out.println("\t\tActive Call Mediator Sequences:");
        for (StackedSequenceInfo activeCallMediatorSequence : stackedSequences) {
            if (activeCallMediatorSequence.isSpanActive()) {
                System.out.print("*" + activeCallMediatorSequence.getSpanReferenceId() + ". " +
                        activeCallMediatorSequence.getStatisticDataUnit().getComponentName() + "* , ");
            } else {
                System.out.print(activeCallMediatorSequence.getSpanReferenceId() + ". " +
                        activeCallMediatorSequence.getStatisticDataUnit().getComponentName() + " , ");
            }
        }
        System.out.println("]");
        System.out.println("");
    }
}
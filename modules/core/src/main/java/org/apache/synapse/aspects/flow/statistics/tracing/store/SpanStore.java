package org.apache.synapse.aspects.flow.statistics.tracing.store;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.SpanTagger;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores and managers spans.
 */
public class SpanStore {
    private SpanWrapper outerLevelSpan;
    private Map<String, SpanWrapper> activeSpans;
    private List<StackedSequenceInfo> stackedSequences;
    private Stack<SpanWrapper> eligibleAlternativeParents;

    private List<SpanWrapper> spanWrappersByInsertionOrder; // TODO under construction

    public SpanStore() {
        this.activeSpans = new HashMap<>();
        this.stackedSequences = new Stack<>();
        this.eligibleAlternativeParents = new Stack<>();
        this.outerLevelSpan = null;
        spanWrappersByInsertionOrder = new ArrayList<>();
    }

    // Active spans related

    public Map<String, SpanWrapper> getActiveSpans() {
        return activeSpans;
    }

    public SpanWrapper addActiveSpan(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit,
                                                  MessageContext synCtx) {
        SpanWrapper spanWrapper = createActiveSpanWrapper(spanId, activeSpan, statisticDataUnit, synCtx);
        activeSpans.put(spanId, spanWrapper);
        spanWrappersByInsertionOrder.add(spanWrapper);

        if (spanWrapper.getStatisticDataUnit().isFlowContinuableMediator()) {
            addEligibleAlternativeParent(spanWrapper);
        }
        return spanWrapper;
    }

    private SpanWrapper createActiveSpanWrapper(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit,
                                                MessageContext synCtx) {
        // TODO Implement properly. Revise "isCloseable"
        return new SpanWrapper(spanId, activeSpan, statisticDataUnit, true);
    }

    public void finishActiveSpan(SpanWrapper spanWrapper) {
        if (spanWrapper != null && spanWrapper.isCloseable() && spanWrapper.getSpan() != null) { // TODO revise closable
            if (spanWrapper.getStatisticDataUnit() != null) {
                SpanTagger.setSpanTags(spanWrapper, spanWrapper.getStatisticDataUnit());
            }
            spanWrapper.getSpan().finish();
        }
    }

    public SpanWrapper getSpanWrapper(String spanWrapperId) {
        return activeSpans.get(spanWrapperId);
    }

    // Stacked sequences related

    public List<StackedSequenceInfo> getStackedSequences() {
        return stackedSequences;
    }

    public void addStackedSequence(StackedSequenceInfo stackedSequenceInfo) {
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


    // Alternative parents related


    public List<SpanWrapper> getSpanWrappersByInsertionOrder() {
        return spanWrappersByInsertionOrder;
    }

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

    public void assignOuterLevelSpan(SpanWrapper spanWrapper) {
        outerLevelSpan = spanWrapper;
    }

    public SpanWrapper getOuterLevelSpanWrapper() {
        return this.outerLevelSpan;
    }


    // Others

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
        System.out.println("\t\tActive Stacked Sequences:");
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
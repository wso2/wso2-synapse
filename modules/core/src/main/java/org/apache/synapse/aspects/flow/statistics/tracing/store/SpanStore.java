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
    private final ReentrantLock LOCK = new ReentrantLock(true);

    private SpanWrapper outerLevelSpan;
    private Map<String, Stack<SpanWrapper>> activeSpans;
    private List<StackedSequenceInfo> stackedSequences;
    private Stack<SpanWrapper> eligibleAlternativeParents;

    public SpanStore() {
        this.activeSpans = new HashMap<>();
        this.stackedSequences = new Stack<>();
        this.eligibleAlternativeParents = new Stack<>();
        this.outerLevelSpan = null;
    }

    // Active spans related

    public Map<String, Stack<SpanWrapper>> getActiveSpans() {
        return activeSpans;
    }

    public SpanWrapper addActiveSpan(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit,
                                                  MessageContext synCtx) {
        SpanWrapper spanWrapper = createActiveSpanWrapper(spanId, activeSpan, statisticDataUnit, synCtx);
        if (!activeSpans.containsKey(spanId)) {
            Stack<SpanWrapper> spanWrappers = new Stack<>();
            spanWrappers.push(spanWrapper);
            activeSpans.put(spanId, spanWrappers);
        } else {
            activeSpans.get(spanId).add(spanWrapper);
        }

        if (spanWrapper.getStatisticDataUnit().isFlowContinuableMediator()) {
            addEligibleAlternativeParent(spanWrapper);
        }
        return spanWrapper;
    }

    private SpanWrapper createActiveSpanWrapper(String spanId, Span activeSpan, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
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

    public SpanWrapper getFinishableSpanWrapper(String spanWrapperId) {
        Stack<SpanWrapper> spanWrappers = activeSpans.get(spanWrapperId);
        if (spanWrappers != null && !spanWrappers.isEmpty()) {
            if (spanWrappers.size() == 1) {
                // Last in the stack. Further elements will refer this as parent
                return spanWrappers.peek();
            } else {
                // Not the last in the stack. No further element will refer this as parent
                return spanWrappers.pop();
            }
        }
        return null;
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
package org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver;

import io.opentracing.Span;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;

import java.util.Stack;

public class DefaultParentResolver implements ParentResolver {
    private static final String BEGINNING_INDEX = "0"; // TODO make them int
    private static final String BEGINNING_INDEX_PARENT = "-1";

    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
        SpanWrapper parent = getLatestActiveSpanWithId(parentId, spanStore);
        if (parent != null) {
            if (isParentAcceptable(statisticDataUnit, parent.getStatisticDataUnit())) {
                System.out.println("");
                System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
                System.out.println("");
                return parent.getSpan();
            }
            return resolveAlternativeParent(spanStore);
        }
        return null;
    }

    // Such as an API which is inside a PROXY_SERVICE
    private static boolean isAnAdditionalEntryPoint(StatisticDataUnit statisticDataUnit, String parentId,
                                                 SpanStore spanStore) {
        return BEGINNING_INDEX_PARENT.equals(parentId) && BEGINNING_INDEX.equals(Util.extractId(statisticDataUnit)) &&
                isAlreadyABeginningIndexExists(spanStore);
    }

    private static boolean isAlreadyABeginningIndexExists(SpanStore spanStore) {
        return spanStore.getActiveSpans().get(BEGINNING_INDEX) != null &&
                !spanStore.getActiveSpans().get(BEGINNING_INDEX).isEmpty() ;
    }

    private static SpanWrapper getLatestActiveSpanWithId(String spanId, SpanStore spanStore) {
        Stack<SpanWrapper> spanWrappers = spanStore.getActiveSpans().get(spanId);
        if (spanWrappers != null && !spanWrappers.isEmpty()) {
            return spanWrappers.peek();
        }
        return null;
    }

    private static boolean isParentAcceptable(StatisticDataUnit child, StatisticDataUnit parent) {
        return parent.isFlowContinuableMediator() || isForEachMediator(parent) || isEndpointOrInboundEndpoint(parent) ||
                (isEndpointOrInboundEndpoint(child) && isCallMediatorOrSendMediator(parent));
    }

    private static boolean isForEachMediator(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.MEDIATOR) &&
                statisticDataUnit.getComponentName().equalsIgnoreCase("foreachmediator");
    }

    private static boolean isEndpointOrInboundEndpoint(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.ENDPOINT) ||
                statisticDataUnit.getComponentType().equals(ComponentType.INBOUNDENDPOINT);
    }

    private static boolean isCallMediatorOrSendMediator(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.MEDIATOR) &&
                (statisticDataUnit.getComponentName().equalsIgnoreCase("sendmediator") ||
                        statisticDataUnit.getComponentName().equalsIgnoreCase("callmediator"));
    }

    public static Span resolveAlternativeParent(SpanStore spanStore) {
        SpanWrapper parent = spanStore.getAlternativeParent();
        if (parent != null) {
            return parent.getSpan();
        }
        return null;
    }

    // TODO on hold for now
//    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
//        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
//        if (isSpanActive(parentId, spanStore)) {
//            System.out.println("");
//            System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
//            System.out.println("");
//            return spanStore.getActiveSpans().get(parentId).getSpan();
//        }
//        return null;
//    }

    private static boolean isSpanActive(String spanId, SpanStore spanStore) {
        return spanStore.getActiveSpans().containsKey(spanId);
    }
}

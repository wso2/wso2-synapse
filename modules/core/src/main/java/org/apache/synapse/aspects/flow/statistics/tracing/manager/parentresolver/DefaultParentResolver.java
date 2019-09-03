package org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver;

import io.opentracing.Span;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;

public class DefaultParentResolver implements ParentResolver {
    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
        SpanWrapper parent = spanStore.getActiveSpans().get(parentId);
        if (parent == null) {
            return null;
        }
        if (isParentAcceptable(statisticDataUnit, parent.getStatisticDataUnit())) {
            System.out.println("");
            System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
            System.out.println("");
            return parent.getSpan();
        }
        return resolveAlternativeParent(spanStore);
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

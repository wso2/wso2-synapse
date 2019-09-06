package org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver;

import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;

import java.util.List;

public class DefaultParentResolver implements ParentResolver {
//    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore, MessageContext synCtx) {
//        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
//        SpanWrapper parent = spanStore.getSpanWrapper(parentId);
//        if (parent != null) {
//            if (isParentAcceptable(statisticDataUnit, parent.getStatisticDataUnit())) {
//                System.out.println("");
//                System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
//                System.out.println("");
//                return parent.getSpan();
//            }
//            return resolveAlternativeParent(spanStore);
//        }
//        return null;
//    }

    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore, MessageContext synCtx) {
        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
        SpanWrapper parent = spanStore.getSpanWrapper(parentId);
        if (parent != null) {
            if (isLoopBackMediator(statisticDataUnit)) {
                return resolveLatestDesiredParent(ComponentType.SEQUENCE, null, true, spanStore).getSpan();
            }
            if (isParentAcceptable(statisticDataUnit, parent.getStatisticDataUnit())) {
                System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
                return parent.getSpan();
            }
            return resolveAlternativeParent(spanStore);
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

    private static boolean isLoopBackMediator(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType().equals(ComponentType.MEDIATOR) &&
                statisticDataUnit.getComponentName().equalsIgnoreCase("loopbackmediator");
    }

    public static Span resolveAlternativeParent(SpanStore spanStore) {
        SpanWrapper parent = spanStore.getAlternativeParent();
        if (parent != null) {
            return parent.getSpan();
        }
        return null;
    }

    public static synchronized SpanWrapper resolveLatestDesiredParent(ComponentType desiredComponentType,
                                                                      String desiredComponentName,
                                                                      boolean shouldIgnoreComponentNameCase,
                                                                      SpanStore spanStore) {
        List<SpanWrapper> spanWrappers = spanStore.getSpanWrappersByInsertionOrder();
        if (!spanWrappers.isEmpty()) {
            boolean doesComponentTypeMatch = desiredComponentType == null;
            boolean doesComponentNameMatch = desiredComponentName == null;

            for (int i = spanWrappers.size() - 1; i >= 0; i--) {
                SpanWrapper spanWrapper = spanWrappers.get(i);

                if (!doesComponentTypeMatch) {
                    doesComponentTypeMatch = spanWrapper.getStatisticDataUnit().getComponentType().equals(desiredComponentType);
                }

                if (!doesComponentNameMatch) {
                    if (shouldIgnoreComponentNameCase) {
                        doesComponentNameMatch =
                                spanWrapper.getStatisticDataUnit().getComponentName().equalsIgnoreCase(desiredComponentName);
                    } else {
                        doesComponentNameMatch =
                                spanWrapper.getStatisticDataUnit().getComponentName().equals(desiredComponentName);
                    }
                }

                if (doesComponentTypeMatch && doesComponentNameMatch) {
                    return spanWrapper;
                }
            }
        }
        return null;
    }
}

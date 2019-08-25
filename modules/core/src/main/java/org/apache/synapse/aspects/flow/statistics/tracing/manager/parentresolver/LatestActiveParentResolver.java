package org.apache.synapse.aspects.flow.statistics.tracing.manager.parentresolver;

import io.opentracing.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Deprecated
public class LatestActiveParentResolver implements ParentResolver {
    public static Span resolveParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        return getLatestActiveSpanAsParent(statisticDataUnit, spanStore);
    }

    private static Span getLatestActiveSpanAsParent(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        List<Integer> ids = new ArrayList<>();
        for (String id : spanStore.getActiveSpans().keySet()) {
            ids.add(Integer.valueOf(id));
        }
        if (!ids.isEmpty()) {
            Collections.sort(ids);
            int spanId = ids.get(ids.size() - 1);
            return spanStore.getActiveSpans().get(String.valueOf(spanId)).getSpan();
        }
        return null;
    }

    private static Span getLatestFlowContinuableAsParent(SpanStore spanStore) {
        List<Integer> ids = new ArrayList<>();

        for (Map.Entry<String, SpanWrapper> activeSpanEntry : spanStore.getActiveSpans().entrySet()) {
            if (activeSpanEntry.getValue().getStatisticDataUnit().isFlowContinuableMediator()) {
                ids.add(Integer.valueOf(activeSpanEntry.getKey()));
            }
        }

        if (!ids.isEmpty()) {
            Collections.sort(ids);
            int spanId = ids.get(ids.size() - 1);
            return spanStore.getActiveSpans().get(String.valueOf(spanId)).getSpan();
        }
        return null;
    }
}

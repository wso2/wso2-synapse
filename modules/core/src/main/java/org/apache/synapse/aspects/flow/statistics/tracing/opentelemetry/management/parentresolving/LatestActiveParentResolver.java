/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;

import java.util.List;

/**
 * Resolves the latest active span, as the parent.
 */
public class LatestActiveParentResolver extends AbstractParentResolver {
    /**
     * Resolves the latest active span as the parent.
     * @param spanStore The span store object.
     * @return          Resolved parent span wrapper.
     */
    public static SpanWrapper resolveParent(SpanStore spanStore) {
        return resolveLatestActiveSpanWrapper(spanStore);
    }

    /**
     * Resolves the latest active span wrapper - which is either a Call mediator, a Send mediator,
     * or a flow continuable mediator, as the parent span wrapper for endpoints or inbound endpoints.
     * @param spanStore The span store object.
     * @return          Resolved parent span wrapper.
     */
    public static SpanWrapper resolveParentForEndpointOrInboundEndpoint(SpanStore spanStore) {
        List<SpanWrapper> parentableSpans = spanStore.getActiveSpanWrappers();
        for (int i = parentableSpans.size() - 1; i >= 0; i--) {
            SpanWrapper spanWrapper = parentableSpans.get(i);
            StatisticDataUnit statisticDataUnit = spanWrapper.getStatisticDataUnit();
            if (isCallMediator(statisticDataUnit) ||
                    isSendMediator(statisticDataUnit) ||
                    isFlowContinuableMediator(statisticDataUnit)) {
                return spanWrapper;
            }
        }
        return null;
    }

    /**
     * Resolves the latest active span wrapper as the parent, regardless of its type.
     * @param spanStore The span store object.
     * @return          Resolved parent span wrapper.
     */
    private static SpanWrapper resolveLatestActiveSpanWrapper(SpanStore spanStore) {
        List<SpanWrapper> activeSpanWrappers = spanStore.getActiveSpanWrappers();
        if (activeSpanWrappers.isEmpty()) {
            return null;
        }
        return activeSpanWrappers.get(activeSpanWrappers.size() - 1);
    }
}

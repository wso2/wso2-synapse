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
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;

/**
 * Resolves the parent based on message flow index, that is explicitly contained by the statistic data unit.
 */
public class MessageFlowRepresentationBasedParentResolver extends AbstractParentResolver {
    /**
     * Resolves the parent span wrapper based on the parent index, that is explicitly reported by the child statistic
     * data unit.
     * @param child     Child statistic data unit.
     * @param spanStore The span store object.
     * @return          Resolved parent span wrapper.
     */
    public static SpanWrapper resolveParent(StatisticDataUnit child, SpanStore spanStore) {
        String parentId = String.valueOf(child.getParentIndex());
        SpanWrapper parent = spanStore.getSpanWrapper(parentId);
        if (parent != null) {
            if (isEndpointOrInboundEndpoint(child)) {
                /*
                An endpoint can be only parented by either a Call mediator or a Send mediator.
                 */
                if (isCallMediator(parent.getStatisticDataUnit()) || isSendMediator(parent.getStatisticDataUnit())) {
                    return parent;
                }
                /*
                Else:
                The parent will be chosen by the latest active parent resolver.
                Endpoints won't have information in artifact holder structuring element stack.
                 */
                return LatestActiveParentResolver.resolveParentForEndpointOrInboundEndpoint(spanStore);
            }
            if (TracingUtils.isAnonymousSequence(parent.getStatisticDataUnit()) ||
                    TracingUtils.isAnonymousSequence(child)) {
                if (isFlowContinuableMediator(parent.getStatisticDataUnit()) ||
                        isForeachMediator(parent.getStatisticDataUnit())) {
                    return parent;
                }
                return getLatestEligibleParent(spanStore);
            }
        }
        return null;
    }

    /**
     * Gets the latest eligible span wrapper,
     * which is either a flow continuable mediator, or a For Each mediator.
     * Unlike in the LatestActiveParentResolver, the resolved span wrapper doesn't have to be active.
     * @param spanStore Span store object.
     * @return          Resolved parent span wrapper object.
     */
    private static SpanWrapper getLatestEligibleParent(SpanStore spanStore) {
        Object[] spanWrapperKeys = spanStore.getSpanWrappers().keySet().toArray();
        for (int i = spanWrapperKeys.length - 1; i >= 0; i--) {
            String key = (String)spanWrapperKeys[i];
            SpanWrapper spanWrapper = spanStore.getSpanWrapper(key);
            if (isFlowContinuableMediator(spanWrapper.getStatisticDataUnit()) ||
                    isForeachMediator(spanWrapper.getStatisticDataUnit())) {
                // Only a flow continuable mediator, or a for each mediator can be the parent
                return spanWrapper;
            }
        }
        return null;
    }
}

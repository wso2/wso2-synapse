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

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;

/**
 * Resolves parent spans before a child span starts.
 */
public class ParentResolver {
    /**
     * Prevents instantiation.
     */
    private ParentResolver() {}

    /**
     * Resolves parent spans before a child span starts.
     *
     * The message flow parent index which is explicitly reported by the statistic data unit is checked first.
     * The parent is finalized if it gives an acceptable parent as implemented inside.
     *
     * During an unacceptable parent in the above case, the component's unique id will be examined in the
     * artifact holder's structuring elements stack, in order to track the parent holder.
     *
     * In case if both of the above fail, the most recent active span will be chosen as the parent.
     *
     * The very first span of the scope will always return null as the parent, which creates a new trace.
     *
     * @param child     Statistic data unit of the child.
     * @param spanStore Span store from where, existing spans are referred.
     * @param synCtx    Message context.
     * @return          Resolved parent span wrapper. Null if no parent.
     */
    public static SpanWrapper resolveParent(StatisticDataUnit child,
                                            SpanStore spanStore,
                                            MessageContext synCtx) {
        // Try resolving based on statistic data unit message flow representation
        SpanWrapper parent = MessageFlowRepresentationBasedParentResolver.resolveParent(child, spanStore);
        if (parent != null && parent.getSpan() != null) {
            return parent;
        }

        // Try resolving based on the structuring element stack of the artifact holder
        parent = ArtifactHolderBasedParentResolver.resolveParent(child, spanStore, synCtx);
        if (parent != null && parent.getSpan() != null) {
            return parent;
        }

        // Resolve based on latest active span
        return LatestActiveParentResolver.resolveParent(spanStore);
    }
}

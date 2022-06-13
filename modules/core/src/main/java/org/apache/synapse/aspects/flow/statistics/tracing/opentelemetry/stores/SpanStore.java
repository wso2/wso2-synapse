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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores;

import io.opentelemetry.api.trace.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.SpanTagger;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.ContinuationStateSequenceInfo;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

/**
 * Stores information about the Spans.
 */
public class SpanStore {
    /**
     * Contains the span which is in the most outer level, of a scope.
     * This can be either a Proxy Service, or an API.
     */
    private SpanWrapper outerLevelSpan;

    /**
     * Contains all the Span Wrappers that have been started so far, in the order they have been started.
     * Each Span Wrapper value is identified by its it Statistic Data Unit Component Index.
     * Span Wrappers are not removed when Spans finish.
     */
    private Map<String, SpanWrapper> spanWrappers;

    /**
     * Contains references to Span Wrappers that have been started, and not yet finished.
     * Reference to the appropriate Span Wrapper is removed from this list, when its Span has finished.
     * This is used in searching for parents with respect to latest active spans.
     */
    private List<SpanWrapper> activeSpanWrappers;

    /**
     * Contains references to Span Wrappers, with respect to their Statistic Data Unit Component Unique Id.
     * This is used in searching for parents with respect to the aspect configuration,
     * since aspect configuration holders maintain naming in this way.
     */
    private Map<String, SpanWrapper> componentUniqueIdWiseSpanWrappers;

    /**
     * Contains information about stacked sequences (only applicable for sequences other than anonymous sequences).
     * This is used to start/finish the span of such a sequence based on state continuation state addition or removal,
     * not based on an open event or close event.
     */
    private List<ContinuationStateSequenceInfo> continuationStateSequenceInfos;

    public SpanStore() {
        this.outerLevelSpan = null;
        this.spanWrappers = new LinkedHashMap<>();
        this.activeSpanWrappers = new ArrayList<>();
        this.componentUniqueIdWiseSpanWrappers = new HashMap<>();
        this.continuationStateSequenceInfos = new Stack<>();
    }

    /**
     * Denotes the beginning of a span. Adds appropriate elements to necessary data structures.
     * @param spanId            Index of the span wrapper
     * @param activeSpan        Reference to the span object, that have been started
     * @param statisticDataUnit The statistic data unit object
     * @param parentSpanWrapper Parent span wrapper of the created span wrapper
     * @param synCtx            Message Context that is reported during the open event
     * @return                  Created span wrapper object
     */
    public SpanWrapper addSpanWrapper(String spanId,
                                      Span activeSpan,
                                      StatisticDataUnit statisticDataUnit,
                                      SpanWrapper parentSpanWrapper,
                                      MessageContext synCtx) {
        SpanWrapper spanWrapper = new SpanWrapper(spanId, activeSpan, statisticDataUnit, parentSpanWrapper);
        spanWrappers.put(spanId, spanWrapper);
        spanWrapper.addKnownSynCtxHashCodeToAllParents(TracingUtils.getSystemIdentityHashCode(synCtx));
        if (parentSpanWrapper != null) {
            parentSpanWrapper.addChildComponentUniqueId(statisticDataUnit.getComponentId());
            if (TracingUtils.isAnonymousSequence(spanWrapper.getStatisticDataUnit())) {
                /*
                Add this anonymous sequence to the parent.
                Note that, anonymous sequences are not pushed to the continuation stack
                */
                parentSpanWrapper.addAnonymousSequence(spanId, spanWrapper);
            }
        }
        componentUniqueIdWiseSpanWrappers.put(statisticDataUnit.getComponentId(), spanWrapper);
        activeSpanWrappers.add(spanWrapper);
        return spanWrapper;
    }

    /**
     * Denotes the end of a span.
     * Adds tags to the span and removes reference to the appropriate span wrapper in activeSpanWrappers.
     * @param spanWrapper   Span wrapper object, which has been already created
     */
    public void finishSpan(SpanWrapper spanWrapper) {
        if (spanWrapper != null && spanWrapper.getSpan() != null) {
            if (spanWrapper.getStatisticDataUnit() != null) {
                SpanTagger.setSpanTags(spanWrapper);
            }
            spanWrapper.getSpan().end();
            activeSpanWrappers.remove(spanWrapper);
        }
    }

    public void assignOuterLevelSpan(SpanWrapper spanWrapper) {
        outerLevelSpan = spanWrapper;
    }

    public SpanWrapper getOuterLevelSpanWrapper() {
        return this.outerLevelSpan;
    }

    public Map<String, SpanWrapper> getSpanWrappers() {
        return spanWrappers;
    }

    public SpanWrapper getSpanWrapper(String spanWrapperId) {
        return spanWrappers.get(spanWrapperId);
    }

    public List<SpanWrapper> getActiveSpanWrappers() {
        return activeSpanWrappers;
    }

    public SpanWrapper getSpanWrapperByComponentUniqueId(String componentUniqueId) {
        return componentUniqueIdWiseSpanWrappers.get(componentUniqueId);
    }

    public void addContinuationStateSequenceInfo(ContinuationStateSequenceInfo continuationStateSequenceInfo) {
        continuationStateSequenceInfos.add(continuationStateSequenceInfo);
    }

    public List<ContinuationStateSequenceInfo> getContinuationStateSequenceInfos() {
        return continuationStateSequenceInfos;
    }

    public boolean hasContinuationStateSequenceInfoWithId(String id) {
        for (ContinuationStateSequenceInfo activeCallMediatorSequence : continuationStateSequenceInfos) {
            if (Objects.equals(id, activeCallMediatorSequence.getSpanReferenceId())) {
                return true;
            }
        }
        return false;
    }
}

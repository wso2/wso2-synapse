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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models;

import io.opentelemetry.api.trace.Span;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * Wraps an OpenTelemetry span, and additional information that are needed to identify and correlate with it later.
 */
public class SpanWrapper {
    private String id;

    /**
     * Span object which represents the life line of a mediator, sequence and etc.
     */
    private Span span;

    /**
     * Statistic data unit that has been collected during an open event, which carries data related to the span.
     */
    private StatisticDataUnit openEventStatisticDataUnit;

    /**
     * Statistic data unit that has been collected during an closing event, which carries data related to the span.
     */
    private StatisticDataUnit closeEventStatisticDataUnit;

    /**
     * Parent span wrapper for this span wrapper.
     */
    private SpanWrapper parentSpanWrapper;

    /**
     * Anonymous sequences that are contained within this span wrapper.
     * Storing this information is used in resolving artifact holder based parent.
     * This is applicable in cases where a mediator such as Iterate or Clone contains anonymous sequences,
     * and children hold that particular mediator as the parent in their artifact holder stacks.
     */
    private final Map<String, SpanWrapper> anonymousSequences;

    /**
     * System Identity HashCodes of message contexts, that this element has gone through.
     * This is used to identify parent span wrappers, that have been branched at the time of execution.
     * Mediators branch to message contexts in several cases, and such new message contexts are updated here every time.
     * In such cases, the change is updated all the way towards the super parent span wrapper.
     */
    private Set<String> knownSynCtxHashCodes;

    /**
     * Structuring element ids (or statistic data unit component unique ids) of children,
     * contained by this span wrapper.
     *
     * Used to identify the correct parent, when multiple copies of the element exist with the same name,
     * in cases like where an Iterate mediator creates multiple Proxy Out sequences.
     *
     * A child will always refer structuring element id, with respect to its artifact holder.
     * But in cases like where an Iterate mediator creates multiple Proxy Out sequences,
     * the child won't say 'which span wrapper', that has a particular structuring element id.
     *
     * In such cases, whenever a child reports the structuring element id, it is checked whether such child structured
     * element already exists in the found parent.
     * If so, the next step is to search for a span wrapper which doesn't have the current structuring element id,
     * under that.
     */
    private Set<String> childStructuredElementIds;

    public SpanWrapper(String id, Span span, StatisticDataUnit openEventStatisticDataUnit, SpanWrapper parentSpanWrapper) {
        this.id = id;
        this.span = span;
        this.openEventStatisticDataUnit = openEventStatisticDataUnit;
        this.anonymousSequences = new LinkedHashMap<>();
        this.parentSpanWrapper = parentSpanWrapper;
        this.childStructuredElementIds = new HashSet<>();
        this.knownSynCtxHashCodes = new HashSet<>();
    }

    public Span getSpan() {
        return span;
    }

    public StatisticDataUnit getStatisticDataUnit() {
        return openEventStatisticDataUnit;
    }

    public void setStatisticDataUnit(StatisticDataUnit statisticDataUnit) {
        this.openEventStatisticDataUnit = statisticDataUnit;
    }

    public StatisticDataUnit getCloseEventStatisticDataUnit() {
        return closeEventStatisticDataUnit;
    }

    public void setCloseEventStatisticDataUnit(StatisticDataUnit closeEventStatisticDataUnit) {
        this.closeEventStatisticDataUnit = closeEventStatisticDataUnit;
    }

    public void addAnonymousSequence(String id, SpanWrapper anonymousSequenceSpanWrapper) {
        synchronized (anonymousSequences) {
            anonymousSequences.put(id, anonymousSequenceSpanWrapper);
        }
    }

    public Map<String, SpanWrapper> getAnonymousSequences() {
        return anonymousSequences;
    }

    public SpanWrapper getLatestAnonymousSequence() {
        synchronized (anonymousSequences) {
            if (!anonymousSequences.isEmpty()) {
                String[] keys = anonymousSequences.keySet().toArray(new String[0]);
                return anonymousSequences.get(keys[keys.length - 1]);
            }
            return null;
        }
    }

    public void addChildComponentUniqueId(String childStructuredElementId) {
        childStructuredElementIds.add(childStructuredElementId);
    }

    public Set<String> getChildStructuredElementIds() {
        return childStructuredElementIds;
    }

    public Set<String> getKnownSynCtxHashCodes() {
        return knownSynCtxHashCodes;
    }

    public void addKnownSynCtxHashCodeToAllParents(String synCtxHashCode) {
        this.knownSynCtxHashCodes.add(synCtxHashCode);
        if (parentSpanWrapper != null) {
            parentSpanWrapper.addKnownSynCtxHashCodeToAllParents(synCtxHashCode);
        }
    }
}

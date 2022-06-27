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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * Represents a scope, which is represented as a separate trace.
 * A scope carries information about a message flow that happens across mediators,
 * within a particular Proxy Service or an API.
 */
public class TracingScope {
    private String tracingScopeId;

    /**
     * The scope from which, this scope was created.
     * For example, if an API is called inside a Proxy Service, Proxy Service's scope will be the parent scope
     * of the API's scope.
     */
    private TracingScope parentScope;

    /**
     * Contains information about spans within this scope.
     */
    private final SpanStore spanStore;

    /**
     * Denotes the number of pending callbacks.
     * This is incremented when a callback has been sent from this scope, and
     * decremented when a callback has been received by this scope.
     *
     * This is used to sustain the outer level span of the scope.
     * Otherwise, just closing it during a close event will finish the span before all the contained mediators end,
     * which will give a wrong representation.
     *
     * An increment/decrement in a child scope's pending callbacks count will reflect in all its parents.
     * But such a change in a parent scope's pending callbacks count will not reflect in any of its children.
     */
    private Integer pendingCallbacksCount;

    public TracingScope(String tracingScopeId) {
        this.tracingScopeId = tracingScopeId;
        this.spanStore = new SpanStore();
        this.pendingCallbacksCount = 0;
    }

    public SpanStore getSpanStore() {
        return spanStore;
    }

    /**
     * Increments pending callbacks count in this scope, till its super parent.
     */
    public void addCallback() {
        this.incrementPendingCallbacksCount();
    }

    /**
     * Decrements pending callbacks count in this scope, till its super parent.
     */
    public void removeCallback() {
        this.decrementPendingCallbacksCount();
    }

    private synchronized void incrementPendingCallbacksCount() {
        pendingCallbacksCount++;
    }

    private synchronized void decrementPendingCallbacksCount() {
        if (pendingCallbacksCount > 0) {
            pendingCallbacksCount--;
        }
    }

    /**
     * Denotes whether event collection has been finished in this scope,
     * so that the outer level span of this scope can be ended.
     * @param synCtx    Message context.
     * @return          Whether event collection has been finished or not.
     */
    public boolean isEventCollectionFinished(MessageContext synCtx) {
        return pendingCallbacksCount == 0 &&
                synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY) != null &&
                ((StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY))
                        .isEvenCollectionFinished();
    }

    public String getTracingScopeId() {
        return tracingScopeId;
    }
}

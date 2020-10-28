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

package org.apache.synapse.aspects.flow.statistics.opentracing.management.scoping;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages tracing scopes.
 */
public class TracingScopeManager {
    /**
     * Contains all tracing scopes, denoted by their flow statistics id.
     */
    private final Map<String, TracingScope> tracingScopes;

    public TracingScopeManager() {
        tracingScopes = new LinkedHashMap<>();
    }

    /**
     * Returns a tracing scope object for the provided message context.
     * Returns the reference to the existing object when the tracing scope is known (has been already created).
     * Otherwise creates a new scope, stores it, and returns its reference.
     * @param synCtx    Message context.
     * @return          Tracing scope object.
     */
    public TracingScope getTracingScope(MessageContext synCtx) {
        synchronized (tracingScopes) {
            String tracingScopeId = extractTracingScopeId(synCtx);
            if (tracingScopes.containsKey(tracingScopeId)) {
                // Already existing scope. Return its reference
                return tracingScopes.get(tracingScopeId);
            } else {
                // Create scope and return its reference
                TracingScope parent = getLatestTracingScope();
                TracingScope tracingScope = new TracingScope(parent, tracingScopeId);
                tracingScopes.put(tracingScopeId, tracingScope);
                return tracingScope;
            }
        }
    }

    /**
     * Gets the tracing scope id for the provided message context.
     * @param synCtx    Message context.
     * @return          Tracing scope id.
     */
    private String extractTracingScopeId(MessageContext synCtx) {
        return (String) synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
    }

    /**
     * Gets the latest tracing scope object.
     * @return  Latest tracing scope object.
     */
    private TracingScope getLatestTracingScope() {
        if (!tracingScopes.isEmpty()) {
            String[] keys = tracingScopes.keySet().toArray(new String[0]);
            return tracingScopes.get(keys[keys.length - 1]);
        }
        return null;
    }

    public void cleanupTracingScope(String tracingScopeId) {
        tracingScopes.remove(tracingScopeId);
    }
}

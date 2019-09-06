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

package org.apache.synapse.aspects.flow.statistics.tracing.manager.scopemanagement;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

import java.util.*;

public class TracingScopeManager {
    private final Map<String, TracingScope> tracingScopes;

    public Map<String, TracingScope> getTracingScopes() {
        return tracingScopes;
    }

    public TracingScopeManager() {
        tracingScopes = new LinkedHashMap<>();
    }

    public TracingScope getTracingScope(MessageContext synCtx) {
        synchronized (tracingScopes) {
            String tracingScopeId = findTracingScopeId(synCtx);
            if (tracingScopes.containsKey(tracingScopeId)) {
                // Already existing scope. Return its reference
                return tracingScopes.get(tracingScopeId);
            } else {
                // Create scope and return its reference
                TracingScope parent = getLatestTracingScope();
                TracingScope tracingScope = new TracingScope(tracingScopeId, parent);
                tracingScopes.put(tracingScopeId, tracingScope);
                // Set the first ever tracing scope as the outer level scope
                if (tracingScopes.size() == 1) {
                    tracingScope.setOuterLevelScope(true);
                }
                return tracingScope;
            }
        }
    }

    private String findTracingScopeId(MessageContext synCtx) {
        return (String) synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
    }

    private TracingScope getLatestTracingScope() {
        if (!tracingScopes.isEmpty()) {
            String[] keys = tracingScopes.keySet().toArray(new String[0]);
            return tracingScopes.get(keys[keys.length - 1]);
        }
        return null;
    }
}

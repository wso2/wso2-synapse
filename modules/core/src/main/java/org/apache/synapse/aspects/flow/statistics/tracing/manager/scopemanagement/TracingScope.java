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
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;

import java.util.concurrent.atomic.AtomicInteger;

public class TracingScope {
    private String tracingScopeId;
    private TracingScope parentScope; // The scope from (inside) which, this scope was created
    private final SpanStore spanStore;
    private Integer pendingCallbacksCount;
    private boolean isOuterLevelScope;

    public TracingScope(String tracingScopeId, TracingScope parentScope) {
        this.tracingScopeId = tracingScopeId;
        this.spanStore = new SpanStore();
        this.parentScope = parentScope;
        pendingCallbacksCount = 0;
    }

    public SpanStore getSpanStore() {
        return spanStore;
    }

    public boolean isOuterLevelScope() {
        return isOuterLevelScope;
    }

    public void setOuterLevelScope(boolean outerLevelScope) {
        isOuterLevelScope = outerLevelScope;
    }

    public int incrementPendingCallbacksCount() {
        this.incrementAndGetPendingCallbacksCount();
        TracingScope parent = this.parentScope;
        while (parent != null) {
            parent.incrementAndGetPendingCallbacksCount();
            parent = parent.parentScope;
        }
        return pendingCallbacksCount;
    }

    public int decrementPendingCallbacksCount() {
        this.decrementAndGetPendingCallbacksCount();
        TracingScope parent = this.parentScope;
        while (parent != null) {
            parent.decrementAndGetPendingCallbacksCount();
            parent = parent.parentScope;
        }
        return pendingCallbacksCount;
    }

    private synchronized int incrementAndGetPendingCallbacksCount() {
        return ++pendingCallbacksCount;
    }

    private synchronized int decrementAndGetPendingCallbacksCount() {
        if (pendingCallbacksCount > 0) {
            return --pendingCallbacksCount;
        }
        return 0;
    }
}

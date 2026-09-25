/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.util;

import org.apache.logging.log4j.ThreadContext;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;

/**
 * Sets and clears the OpenTelemetry trace and span IDs that log4j2 reads from the ThreadContext.
 * <p>
 * The span handler puts the IDs on a thread only when a span opens. When mediation continues on another
 * thread, anything logged there before the next span opens would otherwise show the IDs that thread last
 * held, or none. Both methods do nothing when OpenTelemetry is disabled.
 */
public final class TracingIdLogSetter {

    private TracingIdLogSetter() {
    }

    /**
     * Replaces the trace and span IDs on this thread with the ones recorded on the message.
     *
     * @param synCtx message whose IDs should be logged
     */
    public static void setFromMessage(MessageContext synCtx) {
        if (!RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            return;
        }
        ThreadContext.remove(SynapseConstants.TRACE_ID);
        ThreadContext.remove(SynapseConstants.SPAN_ID);
        Object traceId = synCtx.getProperty(SynapseConstants.JAEGER_TRACE_ID);
        if (traceId instanceof String) {
            ThreadContext.put(SynapseConstants.TRACE_ID, (String) traceId);
        }
        Object spanId = synCtx.getProperty(SynapseConstants.JAEGER_SPAN_ID);
        if (spanId instanceof String) {
            ThreadContext.put(SynapseConstants.SPAN_ID, (String) spanId);
        }
    }

    /**
     * Removes the trace and span IDs from this thread.
     */
    public static void clear() {
        if (!RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            return;
        }
        ThreadContext.remove(SynapseConstants.TRACE_ID);
        ThreadContext.remove(SynapseConstants.SPAN_ID);
    }
}

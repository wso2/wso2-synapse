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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.event.CallbackEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.event.CloseEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.event.ContinuationStateStackEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.event.OpenEventHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;

/**
 * Interface for the controller which handles OpenTelemetry compatible spans, with respect to various events received
 * during Synapse message flow.
 */
public interface OpenTelemetrySpanHandler
        extends OpenEventHandler, CloseEventHandler, CallbackEventHandler, ContinuationStateStackEventHandler {

    /**
     * Gets the span store that holds the spans of the message flow the given context belongs to.
     *
     * @param messageContext Message context.
     * @return Span store of that message flow.
     */
    SpanStore getSpanStore(MessageContext messageContext);

    /**
     * Ends a MediatorWorker of the flow of the given context: called once per worker in the worker's thread at the end of
     * run(), and in the creating thread when the worker cannot be started. The handler must run branchEnd exactly once.
     * branchEnd releases the worker's branch unit on the statistics event holder and reports the worker's statistics
     * close events (which call this handler's close methods). A handler may hold a lock around it so that no decision on
     * the end of the flow is taken between the release and those events. Default: runs it.
     *
     * @param synCtx    Message context the worker mediated.
     * @param branchEnd End sequence of the worker.
     */
    default void handleBranchEnd(MessageContext synCtx, Runnable branchEnd) {
        branchEnd.run();
    }
}

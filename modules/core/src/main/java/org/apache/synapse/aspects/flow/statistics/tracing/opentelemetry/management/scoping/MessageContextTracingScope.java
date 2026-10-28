/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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

import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingCountHolder;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;

/**
 * Tracing scope of one message flow, kept on the message context by the message context based span handler.
 * <p>
 * The scope is shared by reference by every message context of the flow (clones, iterated messages and callback
 * response contexts copy the property), and is released together with them. Nothing outside the flow refers to it.
 * <p>
 * The scope tracks no work itself. Whether the flow still has work left, which decides when the outer level span can
 * end, is read from the statistics event holder of the flow captured when the scope was created: its callback count
 * and its branch count (the MediatorWorkers of the flow that have not ended). The scope only records the close of the
 * outer level component and whether the outer level span has ended.
 */
public class MessageContextTracingScope extends TracingScope {

    /**
     * Statistics event holder of the flow when the scope was created; null for flows without one (data services).
     * Captured once, not read from the message context each time: every context of the flow (clones, callback
     * responses) shares this holder by reference, and a context that gets a new holder (a message processor
     * dispatching a stored message) gets a new scope as well.
     */
    private final StatisticsReportingEventHolder eventHolder;

    /**
     * Whether the close event of the outer level component has been received.
     */
    private volatile boolean outerLevelCloseReceived;

    /**
     * Whether a mediator reported that the active spans must be ended together with the outer level span: an
     * aggregation (ForEach, Scatter-Gather) completed, or a cloned branch ended.
     */
    private volatile boolean endActiveSpansWithOuterLevelSpan;

    /**
     * Whether the outer level span has ended.
     */
    private volatile boolean finished;

    public MessageContextTracingScope(String tracingScopeId, StatisticsReportingEventHolder eventHolder) {
        super(tracingScopeId);
        this.eventHolder = eventHolder;
    }

    public StatisticsReportingEventHolder getEventHolder() {
        return eventHolder;
    }

    public void setOuterLevelCloseReceived() {
        this.outerLevelCloseReceived = true;
    }

    public boolean isOuterLevelCloseReceived() {
        return outerLevelCloseReceived;
    }

    public void setEndActiveSpansWithOuterLevelSpan() {
        this.endActiveSpansWithOuterLevelSpan = true;
    }

    public boolean isEndActiveSpansWithOuterLevelSpan() {
        return endActiveSpansWithOuterLevelSpan;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished() {
        this.finished = true;
    }

    /**
     * Whether the flow has no work left: the outer level component has closed, and the statistics event holder shows no
     * pending callback and no running MediatorWorker (branchCount without the main branch). Without an event holder the
     * flow is completed when the outer level component closes. Callback counts are kept after the statistics collection
     * finished, so this holds for the whole flow. The callback count is read before and after the branch count, because
     * work is handed over from a unit that is still counted. Must be called while holding the span store lock.
     * <p>
     * Why the main branch is left out: the main unit is closed only by the close of an outer layer component (proxy,
     * API, inbound endpoint, task, message processor), an error, or a root worker. A {@code main} sequence root closes
     * none of these, so its raw branchCount would stay at 1 and the root would never end. The end of the root's own
     * work is tracked by {@link #outerLevelCloseReceived} instead, which covers every kind of root.
     * <p>
     * Why the callback count is read twice. The counters are separate atomics, so the three reads are not one snapshot.
     * Work moves between them in two directions, always in the order "add the new unit, then drop the old one":
     * <ul>
     * <li>callback to worker: the response of a callback that is still counted starts a MediatorWorker
     * (branchCount +1), and only then the callback is completed (callBackCount -1). The first read covers this: if
     * it sees 0 callbacks, the worker's +1 already happened, so the branch read sees it.</li>
     * <li>worker to callback: a running worker makes a non-blocking call (callBackCount +1) and only then releases its
     * unit. The last read covers this: if the branch read saw the unit released, the call's +1 already happened.</li>
     * </ul>
     *
     * @return Whether the outer level span can end.
     */
    public boolean isFlowCompleted() {
        if (!outerLevelCloseReceived) {
            return false;
        }
        if (eventHolder == null) {
            // Data services report with an Axis2 message context: no event holder, no asynchronous parts
            return true;
        }
        StatisticsReportingCountHolder counts = eventHolder.countHolder;
        // Read order matters: callbacks, pending branches, callbacks again (see above)
        return counts.getCallBackCount() <= 0 && counts.getPendingBranchCount() <= 0 && counts.getCallBackCount() <= 0;
    }

    @Override
    public String toString() {
        StatisticsReportingCountHolder counts = eventHolder != null ? eventHolder.countHolder : null;
        return "MessageContextTracingScope{id=" + getTracingScopeId() + ", outerLevelCloseReceived="
                + outerLevelCloseReceived + ", callbacks=" + (counts != null ? counts.getCallBackCount() : "-")
                + ", pendingBranches=" + (counts != null ? counts.getPendingBranchCount() : "-")
                + ", mainBranchOpen=" + (counts != null ? counts.isMainBranchOpen() : "-")
                + ", finished=" + finished + "}";
    }
}

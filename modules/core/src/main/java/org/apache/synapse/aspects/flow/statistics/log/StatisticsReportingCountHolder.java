/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.aspects.flow.statistics.log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is to hold stat count information(stat count, callback count) inside event holder.
 */
public class StatisticsReportingCountHolder {
    /**
     * Which holds count of open events.
     */
    private AtomicInteger statCount = new AtomicInteger(0);

    /**
     * Which holds count of call backs registered.
     */
    private AtomicInteger callBackCount = new AtomicInteger(0);

    /**
     * Which holds count of branches of the message flow, to make sure all the branches are closed before the flow is
     * considered finished. Initial value is 1 since the main branch is also considered as a branch; it is closed once,
     * through {@link #closeMainBranch()}. Every other unit belongs to a MediatorWorker of the flow: +1 when the worker
     * is constructed, -1 when it ends or when it cannot be started.
     */
    private AtomicInteger branchCount = new AtomicInteger(1);

    /** Whether the main branch of the flow (the initial unit of branchCount) is still open. */
    private final AtomicBoolean mainBranchOpen = new AtomicBoolean(true);

    public void incrementStatCount() {
        this.statCount.incrementAndGet();
    }

    public int decrementAndGetStatCount() {
        return this.statCount.decrementAndGet();
    }

    public int getStatCount() {
        return this.statCount.get();
    }

    public void incrementCallBackCount() {
        this.callBackCount.incrementAndGet();
    }

    public int decrementAndGetCallbackCount() {
        return this.callBackCount.decrementAndGet();
    }

    public int getCallBackCount() {
        return this.callBackCount.get();
    }

    public void incrementBranchCount() {
        this.branchCount.incrementAndGet();
    }

    public int decrementAndGetBranchCount() {
        return this.branchCount.decrementAndGet();
    }

    public int getBranchCount() {
        return this.branchCount.get();
    }

    /**
     * Closes the main branch of the flow: decrements branchCount the first time only.
     * <p>
     * A flow can report the end of its main branch more than once: a proxy fault closes the flow with an error and
     * then the proxy closes again; an API that sends a response reports several outer layer closes; an inbound
     * dispatch closes both the inbound endpoint and the API. A plain decrement per close drove branchCount below zero,
     * which hid running MediatorWorkers from every "branchCount <= 0" check.
     *
     * @return whether this call closed it
     */
    public boolean closeMainBranch() {
        if (mainBranchOpen.compareAndSet(true, false)) {
            branchCount.decrementAndGet();
            return true;
        }
        return false;
    }

    public boolean isMainBranchOpen() {
        return mainBranchOpen.get();
    }

    /**
     * Number of MediatorWorkers of the flow that have not ended: branchCount without the main branch. branchCount is read
     * before the main branch flag, so a concurrent closeMainBranch can only make the result too high, never too low.
     * closeMainBranch clears the flag first and decrements second. Reading the count first and the flag second can
     * observe (count with the main unit, flag closed), which returns one too many: the caller waits, which is safe.
     * The other order could observe (count without the main unit, flag open) and return one too few: a running worker
     * would be missed and the root span ended under it.
     */
    public int getPendingBranchCount() {
        int count = branchCount.get();
        return mainBranchOpen.get() ? count - 1 : count;
    }

}

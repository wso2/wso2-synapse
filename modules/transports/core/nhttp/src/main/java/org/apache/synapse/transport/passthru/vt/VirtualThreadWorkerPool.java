/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.vt;

import org.apache.axis2.transport.base.threads.WorkerPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A {@link WorkerPool} implementation backed by a virtual-thread-per-task
 * {@link ExecutorService}. This allows Axis2 engine internals (mediators,
 * message dispatchers, etc.) to use the same virtual-thread executor as the
 * VT transport listener, eliminating the platform thread pool entirely.
 *
 * <p>Each call to {@link #execute(Runnable)} spawns a new, lightweight virtual
 * thread from the shared executor. This is cheap (sub-microsecond) compared to
 * platform threads and avoids the fixed-size thread pool bottleneck.</p>
 */
public class VirtualThreadWorkerPool implements WorkerPool {

    private final ExecutorService executor;

    /**
     * Create a VirtualThreadWorkerPool wrapping the given executor.
     *
     * @param executor a virtual-thread-per-task executor
     *                 (e.g. {@code Executors.newThreadPerTaskExecutor(...)})
     */
    public VirtualThreadWorkerPool(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        executor.submit(task);
    }

    @Override
    public int getActiveCount() {
        // Virtual threads do not expose an active count
        return -1;
    }

    @Override
    public int getQueueSize() {
        // No queue — each task gets its own virtual thread immediately
        return 0;
    }

    @Override
    public void shutdown(int timeout) {
        executor.shutdown();
        try {
            executor.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}

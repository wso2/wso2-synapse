/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Executor wrapper that limits concurrently running tasks while preserving the
 * thread factory and behavior of the wrapped executor.
 */
public class SemaphoreLimitedExecutor extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final Semaphore semaphore;

    public SemaphoreLimitedExecutor(ExecutorService delegate, int maxConcurrentTasks) {
        if (maxConcurrentTasks <= 0) {
            throw new IllegalArgumentException("maxConcurrentTasks must be greater than zero");
        }
        this.delegate = delegate;
        this.semaphore = new Semaphore(maxConcurrentTasks);
    }

    @Override
    public void execute(Runnable command) {
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting for virtual-thread permit", e);
        } catch (RejectedExecutionException e) {
            if (acquired) {
                semaphore.release();
            }
            throw e;
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}

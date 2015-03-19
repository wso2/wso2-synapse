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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is the executor service that will be returned by the env
 */
public class InboundThreadPool extends ThreadPoolExecutor {

    // default values
    public static final int INBOUND_CORE_THREADS = 20;
    public static final int INBOUND_MAX_THREADS = 100;
    public static final int INBOUND_KEEP_ALIVE = 1;
    public static final int INBOUND_THREAD_QLEN = 1;
    public static final String INBOUND_THREAD_GROUP = "inbound-thread-group";
    public static final String INBOUND_THREAD_ID_PREFIX = "InboundWorker";

    // property keys
    public static final String IB_THREAD_CORE = "inbound.threads.core";
    public static final String IB_THREAD_MAX = "inbound.threads.max";


    /**
     * Constructor for the Inbound thread poll
     *
     * @param corePoolSize    - number of threads to keep in the pool, even if they are idle
     * @param maximumPoolSize - the maximum number of threads to allow in the pool
     * @param keepAliveTime   - this is the maximum time that excess idle threads will wait
     *                        for new tasks before terminating.
     * @param unit            - the time unit for the keepAliveTime argument.
     * @param workQueue       - the queue to use for holding tasks before they are executed.
     */
    public InboundThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                             TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                new InboundThreadFactory(
                        new ThreadGroup(INBOUND_THREAD_GROUP), INBOUND_THREAD_ID_PREFIX));
    }

    /**
     * Default Constructor for the thread pool and will use all the values as default
     */
    public InboundThreadPool() {
        this(INBOUND_CORE_THREADS, INBOUND_MAX_THREADS, INBOUND_KEEP_ALIVE,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Constructor for the InboundThreadPool
     *
     * @param corePoolSize   - number of threads to keep in the pool, even if they are idle
     * @param maxPoolSize    - the maximum number of threads to allow in the pool
     * @param keepAliveTime  - this is the maximum time that excess idle threads will wait
     *                       for new tasks before terminating.
     * @param qlen           - Thread Blocking Queue length
     * @param threadGroup    - ThreadGroup name
     * @param threadIdPrefix - Thread id prefix
     */
    public InboundThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime, int qlen,
                             String threadGroup, String threadIdPrefix) {
        super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                qlen > 0 ? new LinkedBlockingQueue<Runnable>(qlen) : new LinkedBlockingQueue<Runnable>(),
                new SynapseThreadFactory(new ThreadGroup(threadGroup), threadIdPrefix));
    }
}

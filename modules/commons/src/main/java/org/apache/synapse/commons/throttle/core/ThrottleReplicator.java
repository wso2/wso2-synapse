/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.commons.throttle.core;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Set;
import java.util.concurrent.*;

/* Runs a scheduled task, which replicates CallerContexts through the cluster.
 * Frequency of the job can be controlled
 */

public class ThrottleReplicator {
    private static final Log log = LogFactory.getLog(ThrottleReplicator.class);
    private static final int MAX_KEYS_TO_REPLICATE = 1000;
    private static int keysToReplicate = MAX_KEYS_TO_REPLICATE;
    private static final int REPLICATOR_THREAD_POOL_SIZE = 1;
    private static int replicatorPoolSize = REPLICATOR_THREAD_POOL_SIZE;

    private volatile ConfigurationContext configContext;
    private ThrottleProperties throttleProperties;
    private int replicatorCount;

    private Set<String> set = new ConcurrentSkipListSet<String>();
    private ConcurrentHashMap<String, Boolean> processingKeys = new ConcurrentHashMap<>();

    public ThrottleReplicator() {
        throttleProperties = ThrottleServiceDataHolder.getInstance().getThrottleProperties();
        replicatorPoolSize = Integer.parseInt(throttleProperties.getThrottlingPoolSize());

        if (log.isDebugEnabled()) {
            log.debug("Replicator pool size set to " + replicatorPoolSize);
        }
        if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Throttle Sync Async Hybrid Mode is enabled. So throttle replicator task will not be scheduled.");
            }
            return;
        }
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(replicatorPoolSize,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(
                            Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("Throttle Replicator - " + replicatorCount++);
                        return t;
                    }
                });
        String throttleFrequency =throttleProperties.getThrottlingReplicationFrequency();

        log.debug("Throttling Frequency set to " + throttleFrequency);
            keysToReplicate = Integer.parseInt(throttleProperties.getThrottlingKeysToReplicates());
        log.debug("Max keys to Replicate " + keysToReplicate);
        for (int i = 0; i < replicatorPoolSize; i++) {
            executor.scheduleAtFixedRate(new ReplicatorTask(), Integer.parseInt(throttleFrequency),
                    Integer.parseInt(throttleFrequency), TimeUnit.MILLISECONDS);
        }
    }

    public void setConfigContext(ConfigurationContext configContext) {
        if (this.configContext == null) {
            this.configContext = configContext;
        }
    }

    public void add(String key) {
        if (configContext == null) {
            throw new IllegalStateException("ConfigurationContext has not been set");
        }
        set.add(key);
        if (log.isDebugEnabled()) {
            log.trace("Adding key " + key + " to replication list");
        }
    }

    private class ReplicatorTask implements Runnable {
        public void run() {
            log.debug("Start running ThrottleReplicatorTask.");
            if (!set.isEmpty()) {
                for (String key : set) {
                    // Lock-free deduplication: Claim key for processing to prevent concurrent replicator threads
                    // from processing same key (pool > 1 safety)
                    boolean claimed = processingKeys.putIfAbsent(key, Boolean.TRUE) == null;
                    if (!claimed) {
                        if (log.isDebugEnabled()) {
                            log.debug("Key " + key + " already being processed by another replicator thread - skipping");
                        }
                        continue;
                    }

                    // Error recovery state: Track snapshot values to restore on failure
                    long snapshotToRestore = 0;
                    CallerContext contextForRestore = null;
                    long windowAtSnapshot = 0;
                    try {
                        // Get distributed map instance and update counters
                        ThrottleDataHolder dataHolder = (ThrottleDataHolder)
                                configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                        CallerContext callerContext = dataHolder.getCallerContext(key);
                        // If callerContext is null, cleanup caller from replication set
                        if (callerContext != null) {
                            contextForRestore = callerContext;
                            windowAtSnapshot = callerContext.getNextTimeWindow();
                            // Only replicate if time window is not expired. If expired, cleanup without replication.
                            if (windowAtSnapshot > System.currentTimeMillis()) {
                                String id = callerContext.getId();
                                // Atomically snapshot and reset local counter. If incoming requests arrive after this,
                                // they increment localCounter from 0, preventing loss of concurrent request counts.
                                long snapshotCounter = callerContext.getAndSetLocalCounter(0);
                                snapshotToRestore = snapshotCounter;

                                // Skip replication if snapshotCounter is 0 (no local activity since last replication)
                                if (snapshotCounter == 0) {
                                    snapshotToRestore = 0;
                                    set.remove(key);
                                    continue;
                                }
                                // Window-change guard 1: Detect if window rolled over between snapshot and here.
                                // If window changed, discard snapshotCounter to prevent old-window counts contaminating new window.
                                if (callerContext.getNextTimeWindow() != windowAtSnapshot) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Window changed from " + windowAtSnapshot
                                                + " to " + callerContext.getNextTimeWindow()
                                                + " during snapshot for key: " + key
                                                + " — discarding " + snapshotCounter
                                                + " counts to prevent old-window contamination (Trade-off T3).");
                                    }
                                    set.remove(key);
                                    snapshotToRestore = 0;
                                    continue;
                                }

                                try {
                                    // Replicate snapshotCounter to Redis distributed counter and get previous value
                                    Long distributedCounter = SharedParamManager.asyncGetAndAddDistributedCounter(id, snapshotCounter);
                                    // Update instance global counter with distributed counter under lock for consistency
                                    synchronized (id.intern()) {
                                        // Window-change guard 2: Verify window didn't change during Redis call.
                                        // If changed, skip globalCounter update to prevent new-window inflation.
                                        if (callerContext.getNextTimeWindow() == windowAtSnapshot) {
                                            callerContext.updateGlobalCounterIfHigher(distributedCounter + snapshotCounter);
                                        } else {
                                            if (log.isDebugEnabled()) {
                                                log.debug("Window changed during Redis flush for key: " + key
                                                        + " — skipping globalCount update to prevent new-window inflation.");
                                            }
                                        }
                                    }
                                    snapshotToRestore = 0;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Replicated counter for context:" + callerContext.getId()
                                                + " distributedCounter=" + distributedCounter
                                                + " snapshotCounter=" + snapshotCounter
                                                + " total=" + (distributedCounter + snapshotCounter));
                                    }
                                } catch (Exception e) {
                                    // Redis failure: Restore snapshotCounter to localCounter so counts aren't lost.
                                    // Key stays in set for retry on next tick.
                                    callerContext.incrementLocalCounterBy(snapshotCounter);
                                    snapshotToRestore = 0;
                                    log.error("Could not replicate throttle counter for key: " + id
                                            + ". Restored " + snapshotCounter + " counts to local counter for retry.", e);
                                    continue;
                                }
                                try {
                                    set.remove(key);
                                } catch (Throwable removeEx) {
                                    log.warn("Failed to remove key from replication set: " + key
                                            + ". Key will be retried on next tick with zero-count early exit.", removeEx);
                                }
                            } else {
                                set.remove(key);
                            }
                        } else {
                            set.remove(key);
                        }
                    } catch (Throwable t) {
                        // Outer error recovery: Restore snapshot if failure occurred after snapshot was taken.
                        // Window-change guard 3: Only restore if window hasn't changed (same-window counts).
                        if (snapshotToRestore > 0 && contextForRestore != null) {
                            if (contextForRestore.getNextTimeWindow() == windowAtSnapshot) {
                                contextForRestore.incrementLocalCounterBy(snapshotToRestore);
                                log.error("Could not replicate throttle data for key: " + key
                                        + ". Restored " + snapshotToRestore + " counts for retry.", t);
                            } else {
                                log.error("Could not replicate throttle data for key: " + key
                                        + ". Window changed during error - discarding " + snapshotToRestore
                                        + " old-window counts to prevent contamination.", t);
                            }
                        } else {
                            log.error("Could not replicate throttle data for key: " + key
                                    + ". Will retry on next tick.", t);
                        }
                    } finally {
                        processingKeys.remove(key);
                    }
                }
            }
        }
    }

}

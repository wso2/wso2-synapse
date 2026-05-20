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

    private ConfigurationContext configContext;
    private ThrottleProperties throttleProperties;
    private int replicatorCount;

    private Set<String> set = new ConcurrentSkipListSet<String>();

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
        synchronized (key.intern()) {
            set.add(key);
        }
        if (log.isDebugEnabled()) {
            log.trace("Adding key " + key + " to replication list");
        }
    }

    private class ReplicatorTask implements Runnable {
        public void run() {
            log.debug("Start running ThrottleReplicatorTask.");
            try {
                if (!set.isEmpty()) {
                    for (String key : set) {
                        synchronized (key.intern()) {
                            ThrottleDataHolder dataHolder = (ThrottleDataHolder)
                                    configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                            CallerContext callerContext = dataHolder.getCallerContext(key);
                            //get distributed map instance and update counters
                            //If both global and local counters are 0 then that means cleanup caller
                            if (callerContext != null) {
                                if (callerContext.getLocalCounter() > 0 &&
                                        callerContext.getNextTimeWindow() > System.currentTimeMillis()) {
                                    String id = callerContext.getId();
                                    // Read current local count and reset to 0 in one atomic step.
                                    // Any request that increments localCounter after this line will be picked up on the next tick.
                                    long snapshotCounter = callerContext.getAndSetLocalCounter(0);
                                    boolean redisSuccess = false;
                                    try {
                                        Long distributedCounter = SharedParamManager.asyncGetAndAddDistributedCounter(id, snapshotCounter);
                                        callerContext.setGlobalCounter(distributedCounter + snapshotCounter);
                                        redisSuccess = true;
                                        if (log.isDebugEnabled()) {
                                            log.debug("Replicated counter for context:" + callerContext.getId()
                                                    + " distributedCounter=" + distributedCounter
                                                    + " snapshotCounter=" + snapshotCounter
                                                    + " total=" + (distributedCounter + snapshotCounter));
                                        }
                                    } catch (Exception e) {
                                        // Restore snapshot to local counter so the counts are retried on the next tick.
                                        callerContext.incrementLocalCounterBy(snapshotCounter);
                                        log.error("Could not replicate throttle counter for key: " + id
                                                + ". Restored " + snapshotCounter + " counts to local counter for retry.", e);
                                    }
                                    if (redisSuccess) {
                                        set.remove(key);
                                    }
                                } else {
                                    set.remove(key);
                                }
                            } else {
                                set.remove(key);
                            }
                        }

                    }
                }
            } catch (Throwable t) {
                log.error("Could not replicate throttle data", t);
            }
        }
    }

}

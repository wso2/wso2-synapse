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

    private int replicatorCount;

    private Set<String> set = new ConcurrentSkipListSet<String>();

    public ThrottleReplicator() {

        String replicatorThreads = System.getProperty("throttling.pool.size");
        if (replicatorThreads != null) {
            replicatorPoolSize = Integer.parseInt(replicatorThreads);
        }
        log.debug("Replicator pool size set to " + replicatorPoolSize);
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
        String throttleFrequency = System.getProperty("throttling.replication.frequency");
        if (throttleFrequency == null) {
            throttleFrequency = "50";
        }
        log.debug("Throttling Frequency set to " + throttleFrequency);
        String maxKeysToReplicate = System.getProperty("throttling.keys.to.replicate");
        if (maxKeysToReplicate != null) {
            keysToReplicate = Integer.parseInt(maxKeysToReplicate);
        }
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
            try {
                if (!set.isEmpty()) {
                    for (String key : set) {
                        synchronized (key.intern()) {
                            ThrottleDataHolder dataHolder = (ThrottleDataHolder)
                                    configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                            CallerContext callerContext = dataHolder.getCallerContext(key);
                            //get hazlecast instance and update counters
                            //If both global and local counters are 0 then that means cleanup caller
                            if (callerContext != null) {
                                //If local counter > 0 and time window is not expired then only we have to replicate counters.
                                //Otherwise we do not need to do replication.
                                if (callerContext.getLocalCounter() > 0 &&
                                        callerContext.getNextTimeWindow() > System.currentTimeMillis()) {
	                                String id = callerContext.getId();
	                                //First put local counter to variable and reset it just after it because
	                                //if there are incoming requests coming. the local counter will be updated
	                                //if that happen, reset will cause to miss the additional requests come after
	                                //local counter value taken into the consideration
	                                long localCounter = callerContext.getLocalCounter();
	                                callerContext.resetLocalCounter();
	                                Long distributedCounter = SharedParamManager.asyncGetAndAddDistributedCounter(id, localCounter);
	                                //Update instance global counter with distributed counter
                                    callerContext.setGlobalCounter(distributedCounter + localCounter);
                                    if(log.isDebugEnabled()) {
                                        log.debug("Increasing counters of context :" + callerContext.getId() + " "
                                                  + "Replicated Count After  Update : distributedCounter =" +distributedCounter
                                                  + " localCounter=" + localCounter + " total=" + (distributedCounter + localCounter));
                                    }
                                }
                            }
	                        set.remove(key);
                        }

                    }
                }
            } catch (Throwable t) {
                log.error("Could not replicate throttle data", t);
            }
        }
    }

}

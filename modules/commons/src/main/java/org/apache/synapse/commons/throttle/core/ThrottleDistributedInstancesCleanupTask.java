/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.hazelcast.concurrent.atomiclong.AtomicLongService;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This task is responsible for cleanup callers which has expired along with hazelcast shared params
 */
public class ThrottleDistributedInstancesCleanupTask {

    private static final Log log = LogFactory.getLog(ThrottleDistributedInstancesCleanupTask.class);


    private static final int CLEANUP_THREAD_POOL_SIZE = 1;
    private static int cleanUpPoolSize = CLEANUP_THREAD_POOL_SIZE;
    private boolean distributedCleanupEnabled = true;
    private int maxNonAssociatedCounterCountToClear = 50000;

    private long distributedInstanceExpiryMillis;
    private long noOfTimestampObjectToBeCleared;
private ThrottleProperties throttleProperties;
    public ThrottleDistributedInstancesCleanupTask() {
        throttleProperties = ThrottleServiceDataHolder.getInstance().getThrottleProperties();
        cleanUpPoolSize = Integer.parseInt(throttleProperties.getThrottleDistributedCleanupPoolSize());

            noOfTimestampObjectToBeCleared = Long.parseLong(throttleProperties.getThrottleDistributedCleanupAmount());


            distributedCleanupEnabled = Boolean.parseBoolean(throttleProperties.getThrottleDistributedCleanupTaskEnable());


            maxNonAssociatedCounterCountToClear = Integer.parseInt(throttleProperties.getMaxNonAssociatedCounterCleanupAmount());

        if (log.isDebugEnabled()) {
            log.debug("Throttle window replicator pool size set to " + cleanUpPoolSize);
        }

        if (distributedCleanupEnabled) {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(cleanUpPoolSize,
                    new ThreadFactory() {

                        public Thread newThread(
                                Runnable r) {
                            Thread t = new Thread(r);
                            t.setName(
                                    "Throttle " +
                                            "Distributed Cleanup" +
                                            " Task");
                            return t;
                        }
                    });

            String throttleFrequency = throttleProperties.getThrottleContextDistributedCleanupTaskFrequency();

            String distributedInstanceExpiry = throttleProperties.getThrottleContextDistributedExpiredInstanceTime();

            if (log.isDebugEnabled()) {
                log.debug("Throttling Cleanup Task Frequency set to " + throttleFrequency);
            }

            executor.scheduleAtFixedRate(new CleanupTask(), Integer.parseInt(throttleFrequency),
                    Integer.parseInt(throttleFrequency), TimeUnit.MILLISECONDS);
            distributedInstanceExpiryMillis = Long.parseLong(distributedInstanceExpiry);
        }
    }

    private class CleanupTask implements Runnable {

        public void run() {
            Map<String, String> timestamps = new HashMap<String, String>();
            Map<String, String> removedTimestamps = new HashMap<String, String>();
            List<String> counters = new ArrayList<String>();
            long removedCounterCount = 0;
            long atomicLongCount = 0;
            long removedAtomicTimestampCount = 0;
            long start = 0;
            long end;
            if (log.isDebugEnabled()) {
                log.debug("Running the distributed counter cleanup task");
                start = System.currentTimeMillis();
            }
            String serviceName;
            String name;
            String counterKey;
            HazelcastInstance hazelcastInstance = getHazelcastInstance();
            if (hazelcastInstance != null && hazelcastInstance.getCluster().getMembers().iterator().next().localMember()) {
                    Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();
                    if (log.isDebugEnabled()) {
                        log.debug("TOTAL NUMBER OF DISTRIBUTED OBJECTS BEFORE CLEAN-UP " + distributedObjects.size());
                    }
                    long currentTime = System.currentTimeMillis();
                    long timestamp;
                    for (DistributedObject distributedObject : distributedObjects) {
                        serviceName = distributedObject.getServiceName();
                        if (AtomicLongService.SERVICE_NAME.equals(serviceName)) {
                            name = distributedObject.getName();
                            if (name.contains(ThrottleConstants.THROTTLE_TIMESTAMP_KEY)) {
                                counterKey = name.split(ThrottleConstants.THROTTLE_TIMESTAMP_KEY)[1];
                                timestamp = SharedParamManager.getSharedTimestampWithFullId(name);
                                timestamps.put(counterKey, counterKey);
                                log.debug("ADDING TIMESTAMP:============" + counterKey);
                                log.debug("TIMESTAMP VALUE:============" + timestamp);
                                if (timestamp < (currentTime - distributedInstanceExpiryMillis) &&
                                        (removedAtomicTimestampCount < noOfTimestampObjectToBeCleared)) {
                                    log.debug("REMOVING TIMESTAMP:============" + counterKey);
                                    SharedParamManager.removeTimestampWithFullId(name);
                                    removedTimestamps.put(counterKey, counterKey);
                                    removedAtomicTimestampCount++;
                                }
                            } else {
                                if (name.contains(ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY)) {
                                    log.debug("ADDING COUNTER:============" + name.split(ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY)[1]);
                                    counters.add(name.split(ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY)[1]);
                                }
                            }
                            atomicLongCount++;
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("TOTAL NUMBER OF DISTRIBUTED TIMESTAMP OBJECTS CLEARED " + removedTimestamps.size());
                        log.debug("TOTAL NUMBER OF DISTRIBUTED TIMESTAMP OBJECTS " + timestamps.size());
                        log.debug("TOTAL NUMBER OF DISTRIBUTED COUNTER OBJECTS " + counters.size());
                    }

                    int nonTimestampAssociatedCountersCount = 0;
                    for (String key : counters) {
                        if (timestamps.containsKey(key)) {
                            if (removedTimestamps.containsKey(key)) {
                                log.debug("REMOVING COUNTER:============" + key);
                                SharedParamManager.removeCounter(key);
                                removedCounterCount++;
                            }
                        } else {
                            if (nonTimestampAssociatedCountersCount < maxNonAssociatedCounterCountToClear) {
                                if (key.contains(ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY)) {
                                    SharedParamManager.removeCounter(key);
                                    log.debug("NON MATCHING COUNTER:============" + key);
                                    nonTimestampAssociatedCountersCount++;
                                    removedCounterCount++;
                                }
                            }
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("TOTAL NUMBER OF ATOMIC LONGS BEFORE CLEANUP" + atomicLongCount);
                        log.debug("TOTAL NUMBER OF DISTRIBUTED COUNTER OBJECTS CLEARED " + removedCounterCount);
                        log.debug("TOTAL NUMBER OF DISTRIBUTED TIMESTAMP OBJECTS CLEARED " + removedAtomicTimestampCount);
                        log.debug("TOTAL NUMBER OF DISTRIBUTED OBJECTS AFTER CLEAN-UP" + hazelcastInstance.getDistributedObjects().size());
                        log.debug("TOTAL NUMBER OF NON ASSOCIATED OBJECTS CLEANED" + nonTimestampAssociatedCountersCount);
                        end = System.currentTimeMillis();
                        log.debug("TIME TAKEN FOR CLEANUP" + (end - start));
                    }

            }
        }
    }

    private static HazelcastInstance getHazelcastInstance() {
        return ThrottleServiceDataHolder.getInstance().getHazelCastInstance();
    }
}

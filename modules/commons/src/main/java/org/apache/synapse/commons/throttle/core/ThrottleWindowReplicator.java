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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/* Runs a scheduled task, which replicates CallerContexts through the cluster.
 * Frequency of the job can be controlled
 */

public class ThrottleWindowReplicator {

	private static final Log log = LogFactory.getLog(ThrottleWindowReplicator.class);
	private static int replicatorPoolSize ;
	private volatile ConfigurationContext configContext;
	private ThrottleProperties throttleProperties;
	private int replicatorCount;

	private Set<String> set = new ConcurrentSkipListSet<String>();
	private ConcurrentHashMap<String, Boolean> processingKeys = new ConcurrentHashMap<>();

	public ThrottleWindowReplicator() {

		throttleProperties = ThrottleServiceDataHolder.getInstance().getThrottleProperties();
		replicatorPoolSize = Integer.parseInt(throttleProperties.getWindowReplicatorPoolSize());

		if (log.isDebugEnabled()) {
			log.debug("Throttle window replicator pool size set to " + replicatorPoolSize);
		}

		if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug("Throttle Sync Async Hybrid Mode is enabled. So throttle window replicator task will not be "
						+ "scheduled.");
			}
			return;
		}

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(replicatorPoolSize,
				new ThreadFactory() {
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("Throttle window replicator - " + replicatorCount++);
						return t;
					}
				});

		String windowReplicationFrequency = throttleProperties.getWindowReplicatorFrequency();

		if (log.isDebugEnabled()) {
			log.debug("Throttling window replication frequency set to " + windowReplicationFrequency);
		}

		for (int i = 0; i < replicatorPoolSize; i++) {
			executor.scheduleAtFixedRate(new ReplicatorTask(), Integer.parseInt(windowReplicationFrequency),
					Integer.parseInt(windowReplicationFrequency), TimeUnit.MILLISECONDS);
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
			log.debug("Start running ThrottleWindowReplicatorTask.");
			if (!set.isEmpty()) {
				for (String key : set) {
					boolean claimed = processingKeys.putIfAbsent(key, Boolean.TRUE) == null;
					if (!claimed) {
						if (log.isDebugEnabled()) {
							log.debug("Key " + key + " already being processed by another window replicator thread - skipping");
						}
						continue;
					}

					try {
						ThrottleDataHolder dataHolder = (ThrottleDataHolder)
								configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
						CallerContext callerContext = dataHolder.getCallerContext(key);
						if (callerContext != null) {
							String callerId = callerContext.getId();
							long sharedTimestamp = SharedParamManager.getSharedTimestamp(callerContext.getId());
							long sharedNextWindow = sharedTimestamp + callerContext.getUnitTime();
							long localFirstAccessTime = callerContext.getFirstAccessTime();
							// Branch A: Sync local window to cluster if cluster has active window
							if (localFirstAccessTime < sharedNextWindow && sharedTimestamp > 0) {
								long distributedCounter = SharedParamManager.getDistributedCounter(callerId);
								synchronized (callerId.intern()) {
									// Re-read FAT inside lock to detect concurrent window changes
									long currentFirstAccessTime = callerContext.getFirstAccessTime();
									// If cluster is ahead, reset local window to match cluster
									if (sharedTimestamp > currentFirstAccessTime) {
										callerContext.resetGlobalCounter();
										callerContext.resetLocalCounter();
									}
									// Apply distCounter only if firstAccessTime unchanged (detects concurrent changes).
									// Note: After reset above, localFirstAccessTime==currentFirstAccessTime still holds,
									// so node-behind case correctly applies cluster's distCounter.
									if (localFirstAccessTime == currentFirstAccessTime) {
										callerContext.updateGlobalCounterIfHigher(distributedCounter);
									}
									
									// Update window boundaries atomically (prevents half-updated state)
									callerContext.setFirstAccessTime(sharedTimestamp);
									callerContext.setNextTimeWindow(sharedNextWindow);
								}
								if (log.isDebugEnabled()) {
									log.debug("Setting time windows for callerId: " + callerId);
								}
							} else {
								// Branch B: Node is ahead or cluster has no window — create new window in Redis.
								// Write order matters: setDistributedCounter(0) MUST happen before setSharedTimestamp.
								// If reversed and setDistributedCounter fails, next tick would enter Branch A (timestamp matches)
								// and apply stale counter from old window → false throttling for entire window duration.
								SharedParamManager.setDistributedCounter(callerId, 0);
								SharedParamManager.setSharedTimestamp(callerId, localFirstAccessTime);
								SharedParamManager.setExpiryTime(callerId,
										callerContext.getUnitTime() + localFirstAccessTime);
								synchronized (callerId.intern()) {
									// Reset to clear any dirty counter from previous window
									callerContext.resetGlobalCounter();
								}
								if (log.isDebugEnabled()) {
									log.debug("Complete resetting time window of=" + callerId);
								}
							}
						}
						set.remove(key);
					} catch (Throwable t) {

						log.error("Could not replicate window data for key: " + key
								+ ". Will retry on next tick.", t);
					} finally {
						processingKeys.remove(key);
					}
				}
			}
		}
	}

}

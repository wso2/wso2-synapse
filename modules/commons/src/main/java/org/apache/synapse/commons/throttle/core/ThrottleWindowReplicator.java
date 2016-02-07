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

import java.util.Set;
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
	private static final int REPLICATOR_THREAD_POOL_SIZE = 1;
	private static int replicatorPoolSize = REPLICATOR_THREAD_POOL_SIZE;
	private static final String WINDOW_REPLICATOR_POOL_SIZE = "throttlingWindowReplicator.pool.size";
	private static final String WINDOW_REPLICATOR_FREQUENCY = "throttlingWindowReplicator.replication.frequency";
	private ConfigurationContext configContext;

	private int replicatorCount;

	private Set<String> set = new ConcurrentSkipListSet<String>();

	public ThrottleWindowReplicator() {

		String replicatorThreads = System.getProperty(WINDOW_REPLICATOR_POOL_SIZE);

		if (replicatorThreads != null) {
			replicatorPoolSize = Integer.parseInt(replicatorThreads);
		}

		if (log.isDebugEnabled()) {
			log.debug("Throttle window replicator pool size set to " + replicatorPoolSize);
		}

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(replicatorPoolSize,
				new ThreadFactory() {
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("Throttle window replicator - " + replicatorCount++);
						return t;
					}
				});

		String windowReplicationFrequency = System.getProperty(WINDOW_REPLICATOR_FREQUENCY);
		if (windowReplicationFrequency == null) {
			windowReplicationFrequency = "50";
		}

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
						String callerId;
						long localFirstAccessTime;
						synchronized (key.intern()) {
							ThrottleDataHolder dataHolder = (ThrottleDataHolder)
									configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
							CallerContext callerContext = dataHolder.getCallerContext(key);
							if (callerContext != null) {
								callerId = callerContext.getId();
								long sharedTimestamp = SharedParamManager.getSharedTimestamp(callerContext.getId());
								long sharedNextWindow = sharedTimestamp + callerContext.getUnitTime();
								localFirstAccessTime = callerContext.getFirstAccessTime();
								//First if statement check whether local first access time is lower than the current
								// global counter if so it will adjust the local first access time to global time to
								// adjust the time window
								if (localFirstAccessTime < sharedTimestamp) {
									callerContext.setFirstAccessTime(sharedTimestamp);
									callerContext.setNextTimeWindow(sharedNextWindow);
									callerContext.setGlobalCounter(SharedParamManager.getDistributedCounter(callerId));
									if(log.isDebugEnabled()) {
										log.debug("Setting time windows of caller context when window already set=" + callerId);
									}
									//If some request comes to a nodes after some node set the shared timestamp then this
									// check whether the first access time of local is in between the global time window
									// if so this will set local caller context time window to global
								} else if (localFirstAccessTime > sharedTimestamp
								           && localFirstAccessTime < sharedNextWindow) {
									callerContext.setFirstAccessTime(sharedTimestamp);
									callerContext.setNextTimeWindow(sharedNextWindow);
									callerContext.setGlobalCounter(SharedParamManager.getDistributedCounter(callerId));
									if (log.isDebugEnabled()) {
										log.debug("Setting time windows of caller context in intermediate interval=" +
										         callerId);
									}
									//If above two statements not meets, this is the place where node set new window if
									// global first access time is 0, then it will be the beginning of the throttle time time
									// window so present node will set shared timestamp and the distributed counter. Also if time
									// window expired this will be the node who set the next time window starting time
								} else {
									SharedParamManager.setSharedTimestamp(callerId, localFirstAccessTime);
									SharedParamManager.setDistributedCounter(callerId, 0);
									//Reset global counter here as throttle replicator task may have updated global counter
									//with dirty value
									callerContext.resetGlobalCounter();
									callerContext.setLocalCounter(1);//Local counter will be set to one as new time window starts
									if (log.isDebugEnabled()) {
										log.debug("Complete resetting time window of=" + callerId);
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

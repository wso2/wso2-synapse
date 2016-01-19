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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This task is responsible for cleanup callers which has expired along with hazelcast shared params
 *
 */
public class ThrottleContextCleanupTask {

	private static final Log log = LogFactory.getLog(ThrottleContextCleanupTask.class);

	private List<ThrottleContext> throttleContexts = new ArrayList<ThrottleContext>();

	public ThrottleContextCleanupTask() {

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
				new ThreadFactory() {

					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName("Throttle Cleanup Task");
						return t;
					}
				});

		String throttleFrequency = ThrottleServiceDataHolder.getInstance().getThrottleProperties().getThrottleFrequency();


		if (log.isDebugEnabled()) {
			log.debug("Throttling Cleanup Task Frequency set to " + throttleFrequency);
		}

		executor.scheduleAtFixedRate(new CleanupTask(), Integer.parseInt(throttleFrequency),
				Integer.parseInt(throttleFrequency), TimeUnit.MILLISECONDS);

	}

	public void addThrottleContext(ThrottleContext throttleContext) {
		throttleContexts.add(throttleContext);
	}

	private class CleanupTask implements Runnable {

		public void run() {
			if(log.isDebugEnabled()) {
				log.debug("Running the cleanup task");
			}
			for(ThrottleContext throttleContext : throttleContexts) {
				throttleContext.cleanupCallers(System.currentTimeMillis());
			}
		}
	}

}

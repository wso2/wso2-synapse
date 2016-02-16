/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.store;

import org.apache.synapse.aspects.flow.statistics.data.raw.EndpointStatisticLog;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This class will hold completed statistic entries till they are collected for storage.
 */
public class CompletedStatisticStore {

	private final BlockingQueue<PublishingFlow> completedStatisticEntries;

	private final List<EndpointStatisticLog> completedEndpointStatisticEntries = new LinkedList<>();

//	public List<PublishingFlow> getCompletedStatisticEntries() {
//		List<PublishingFlow> cloneOfCompletedStatisticEntries = new LinkedList<>();
//		synchronized (completedStatisticEntries) {
//			cloneOfCompletedStatisticEntries.addAll(completedStatisticEntries);
//			completedStatisticEntries.clear();
//		}
//		return cloneOfCompletedStatisticEntries;
//	}


	public CompletedStatisticStore() {
		int queueSize = Integer.parseInt(SynapsePropertiesLoader
				                                 .getPropertyValue(StatisticsConstants.FLOW_STATISTICS_QUEUE_SIZE,
				                                                   StatisticsConstants.FLOW_STATISTICS_DEFAULT_QUEUE_SIZE));

		completedStatisticEntries = new ArrayBlockingQueue<PublishingFlow>(queueSize);

	}

	public void enqueue(PublishingFlow statisticsLogs) {
		synchronized (completedStatisticEntries) {
			completedStatisticEntries.add(statisticsLogs);
		}
	}

	public PublishingFlow dequeue() throws Exception {
		try {
			return completedStatisticEntries.take();
		} catch (InterruptedException exception) {
			String errorMsg = "Error consuming statistic data queue";
			throw new Exception(errorMsg, exception);
		}
	}


	public boolean isEmpty() {
		return completedStatisticEntries.isEmpty();
	}



//	public List<EndpointStatisticLog> getCompletedEndpointStatisticEntries() {
//		List<EndpointStatisticLog> cloneOfCompletedEndpointEntries = new LinkedList<>();
//		synchronized (completedEndpointStatisticEntries) {
//			cloneOfCompletedEndpointEntries.addAll(completedEndpointStatisticEntries);
//			completedEndpointStatisticEntries.clear();
//		}
//		return cloneOfCompletedEndpointEntries;
//	}

	public void putCompletedEndpointStatisticEntry(EndpointStatisticLog endpointStatisticLog) {
		synchronized (completedEndpointStatisticEntries) {
			completedEndpointStatisticEntries.add(endpointStatisticLog);
		}
	}
}

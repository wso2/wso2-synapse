/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class will hold completed statistic entries till they are collected for by carbon mediation.
 */
public class CompletedStatisticStore {

	private static final Log log = LogFactory.getLog(CompletedStatisticStore.class);

	/**
	 * Completed statistics entries for message flows.
	 */
	private final Queue<PublishingFlow> completedStatisticEntries;

	/**
	 * Queue size to keep
	 */
	private int queueSize;

	public CompletedStatisticStore() {

		completedStatisticEntries = new LinkedList<>();
		queueSize = Integer.parseInt(SynapsePropertiesLoader.getPropertyValue(
				StatisticsConstants.FLOW_STATISTICS_COMPLETED_STORE_QUEUE_SIZE,
				StatisticsConstants.FLOW_STATISTICS_COMPLETED_STORE_DEFAULT_QUEUE_SIZE));
	}

	public void putCompletedStatisticEntry(PublishingFlow publishingFlow) {
		if (queueSize > completedStatisticEntries.size()) {
			completedStatisticEntries.add(publishingFlow);
		} else {
			log.info("Completed statistic store queue has reached maximum size. Dropping incoming completed statistic" +
			         " entries.");
		}
	}

	public Queue<PublishingFlow> getCompletedStatisticEntries() {
		return completedStatisticEntries;
	}
}

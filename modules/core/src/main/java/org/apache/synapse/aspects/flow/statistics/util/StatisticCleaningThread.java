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

package org.apache.synapse.aspects.flow.statistics.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;

import java.util.Map;

/**
 * This class is responsible for cleaning stale statistics entries if they are not updated for global timeout period.
 */
public class StatisticCleaningThread implements Runnable {
	private static final Log log = LogFactory.getLog(StatisticCleaningThread.class);
	private Map<String, StatisticsEntry> runtimeStatisticsMap;

	public StatisticCleaningThread(Map<String, StatisticsEntry> runtimeStatisticsMap) {
		this.runtimeStatisticsMap = runtimeStatisticsMap;
	}

	@Override public void run() {
		for (Map.Entry<String, StatisticsEntry> entry : runtimeStatisticsMap.entrySet()) {
			if (log.isDebugEnabled()) {
				log.debug("Statistics cleaning started.");
			}
			StatisticsEntry statisticsEntry = entry.getValue();
			PublishingFlow publishingFlow = statisticsEntry.getMessageFlowLogs();
			if (statisticsEntry.isEventExpired()) {
				runtimeStatisticsMap.remove(entry.getKey());
				statisticsEntry.getSynapseEnvironment().getCompletedStatisticStore()
				               .putCompletedStatisticEntry(publishingFlow);
				log.error("Cleaned Statistics for component: " + publishingFlow.getEvent(0).getEntryPoint());
			}
		}
	}
}

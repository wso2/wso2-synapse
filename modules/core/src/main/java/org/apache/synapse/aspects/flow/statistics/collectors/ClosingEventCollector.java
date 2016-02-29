/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndFlowEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.FinalizedFlowEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.StatisticsCloseEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class ClosingEventCollector extends RuntimeStatisticCollector {
	public static Integer closeEntryEvent(MessageContext messageContext, String componentName,
	                                      ComponentType componentType, Integer currentIndex,
	                                      boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			Boolean isCollectingTracing =
					(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);

			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			statisticDataUnit.setComponentId(componentName);
			statisticDataUnit.setComponentType(componentType);
			if (currentIndex == null) {
				statisticDataUnit.setShouldBackpackParent(true);
				statisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentIndex(messageContext, null));
			} else {
				statisticDataUnit.setCurrentIndex(currentIndex);
			}
			StatisticDataCollectionHelper
					.collectData(messageContext, isContentAltering, isCollectingTracing, statisticDataUnit);

			StatisticsCloseEvent closeEvent = new StatisticsCloseEvent(statisticDataUnit);
			messageDataStore.enqueue(closeEvent);
		}
		return null;
	}

	public static void createEndFlowEvent(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {

			BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(messageContext.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentIndex(messageContext, null));

			EndFlowEvent endFlowEvent = new EndFlowEvent(dataUnit);
			messageDataStore.enqueue(endFlowEvent);
		}
	}

	public static void createFinalizeFlowEvent(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {

			BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(messageContext.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentIndex(messageContext, null));

			FinalizedFlowEvent endFlowEvent = new FinalizedFlowEvent(dataUnit);
			messageDataStore.enqueue(endFlowEvent);
		}
	}
}

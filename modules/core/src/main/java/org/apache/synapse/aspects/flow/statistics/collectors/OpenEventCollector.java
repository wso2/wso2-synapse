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
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.StatisticsOpenEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class OpenEventCollector extends RuntimeStatisticCollector {

	public static Integer reportEntryEvent(MessageContext messageContext, String componentName,
	                                       AspectConfiguration aspectConfiguration, ComponentType componentType) {
		if (isStatisticsEnabled()) {
			boolean isCollectingStatistics = (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());
			boolean isCollectingTracing = (aspectConfiguration != null && aspectConfiguration.isTracingEnabled());

			Boolean isFlowStatisticEnabled =
					(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);

			if (isCollectingStatistics) {
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
				setStatisticsTraceId(messageContext);
				if (isCollectingTracing) {
					messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, true);
				}
			} else if (isFlowStatisticEnabled == null) {
				//To signal lower levels that statistics was disabled in upper component in the flow
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
			}

			if (shouldReportStatistic(messageContext)) {

				StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
				statisticDataUnit.setComponentId(componentName);
				statisticDataUnit.setComponentType(componentType);
				statisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getNextIndex(messageContext));
				int parentIndex = StatisticDataCollectionHelper
						.getParentIndex(messageContext, statisticDataUnit.getCurrentIndex());
				statisticDataUnit.setParentIndex(parentIndex);
				if (statisticDataUnit.getComponentType() != ComponentType.ENDPOINT) {
					statisticDataUnit.setFlowContinuableMediator(true);
				}

				if (isFlowStatisticEnabled == null) {
					statisticDataUnit.setIsIndividualStatisticCollected(true);
				}

				StatisticDataCollectionHelper.collectData(messageContext, true, isCollectingTracing, statisticDataUnit);

				StatisticsOpenEvent openEvent = new StatisticsOpenEvent(statisticDataUnit);
				messageDataStore.enqueue(openEvent);

				return statisticDataUnit.getCurrentIndex();
			}
		}
		return null;
	}

	public static Integer reportChildEntryEvent(MessageContext messageContext, String componentName,
	                                            ComponentType componentType, boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			getMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit);
			return statisticDataUnit.getCurrentIndex();
		}
		return null;
	}

	public static Integer reportFlowContinuableEvent(MessageContext messageContext, String componentName,
	                                                 ComponentType componentType, boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();

			statisticDataUnit.setFlowContinuableMediator(true);
			getMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit);
			return statisticDataUnit.getCurrentIndex();
		}
		return null;

	}

	public static Integer reportFlowSplittingEvent(MessageContext messageContext, String componentName,
	                                               ComponentType componentType, boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();

			statisticDataUnit.setFlowContinuableMediator(true);
			statisticDataUnit.setFlowSplittingMediator(true);
			getMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit);
			return statisticDataUnit.getCurrentIndex();
		}
		return null;

	}

	public static Integer reportFlowAggregateEvent(MessageContext messageContext, String componentName,
	                                               ComponentType componentType, boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();

			statisticDataUnit.setFlowContinuableMediator(true);
			statisticDataUnit.setFlowAggregateMediator(true);
			getMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit);
			return statisticDataUnit.getCurrentIndex();
		}
		return null;

	}

	private static void getMediatorStatistics(MessageContext messageContext, String componentName,
	                                          ComponentType componentType, boolean isContentAltering,
	                                          StatisticDataUnit statisticDataUnit) {
		Boolean isCollectingTracing = (Boolean) messageContext.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);
		statisticDataUnit.setComponentId(componentName);
		statisticDataUnit.setComponentType(componentType);
		statisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getNextIndex(messageContext));
		int parentIndex =
				StatisticDataCollectionHelper.getParentIndex(messageContext, statisticDataUnit.getCurrentIndex());
		statisticDataUnit.setParentIndex(parentIndex);

		StatisticDataCollectionHelper
				.collectData(messageContext, isContentAltering, isCollectingTracing, statisticDataUnit);

		StatisticsOpenEvent openEvent = new StatisticsOpenEvent(statisticDataUnit);
		messageDataStore.enqueue(openEvent);
	}
}

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

package org.apache.synapse.aspects.flow.statistics.log.templates;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * Handling Open Event
 */
public class StatisticsOpenEvent implements StatisticsReportingEvent {

	private StatisticDataUnit statisticDataUnit;

	public StatisticsOpenEvent(MessageContext messageContext, String componentId, ComponentType componentType,
	                           String parentId, boolean isAlteringContent) {
		String statisticId = (String) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		int cloneId;
		if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID) != null) {
			cloneId = (Integer) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID);
		} else {
			cloneId = 0;
		}
		statisticDataUnit = new StatisticDataUnit(statisticId, componentId, componentType, parentId, cloneId,
		                                          messageContext.isResponse(), messageContext, isAlteringContent);

	}

	public StatisticsOpenEvent(MessageContext messageContext, String componentId, ComponentType componentType,
	                           String parentId, boolean isCloneLog, boolean isAggregateLog, boolean isAlteringContent) {
		this(messageContext, componentId, componentType, parentId, isAlteringContent);
		statisticDataUnit.setAggregatePoint(isAggregateLog);
		statisticDataUnit.setClonePoint(isCloneLog);
	}

	public StatisticsOpenEvent(MessageContext messageContext, String componentId, ComponentType componentType,
	                           String parentId, boolean isCloneLog, boolean isAggregateLog, boolean isAlteringContent,
	                           boolean isIndividualStatistic) {
		this(messageContext, componentId, componentType, parentId, isAlteringContent);
		statisticDataUnit.setAggregatePoint(isAggregateLog);
		statisticDataUnit.setClonePoint(isCloneLog);
		statisticDataUnit.setIsIndividualStatisticCollected(isIndividualStatistic);
	}

	@Override
	public void process() {
		RuntimeStatisticCollector.recordStatisticsOpenEvent(statisticDataUnit);
	}
}

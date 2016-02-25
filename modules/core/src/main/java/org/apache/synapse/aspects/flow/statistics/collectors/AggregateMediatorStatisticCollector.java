/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.util.StatisticMessageCountHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class AggregateMediatorStatisticCollector extends MediatorStatisticCollector {

	private static final Log log = LogFactory.getLog(AggregateMediatorStatisticCollector.class);

	public static void setAggregateProperties(MessageContext oldMessageContext, MessageContext newMessageContext) {
		if (shouldReportStatistic(oldMessageContext)) {
			StatisticMessageCountHolder cloneCount = (StatisticMessageCountHolder) oldMessageContext
					.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER);
			int parentMsgId =
					(Integer) oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID, parentMsgId);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
		}
	}

	/**
	 * Reports statistics for aggregation.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param componentName       Component name.
	 * @param componentType       Component type of the component.
	 * @param parentName          Parent of the component.
	 * @param isCreateLog         It is statistic flow start or end.
	 * @param isAggregateComplete Whether aggregate completed.
	 */
	public static void reportStatisticForAggregateMediator(MessageContext messageContext, String reportingId,
	                                                       String componentName, ComponentType componentType,
	                                                       String parentName, boolean isCreateLog,
	                                                       boolean isAggregateComplete) {
		if (shouldReportStatistic(messageContext)) {
			if (isCreateLog) {
				createLogForMessageCheckpoint(messageContext, reportingId, componentName, componentType, parentName,
				                              true, false, true, true);
			} else {
				createLogForMessageCheckpoint(messageContext, reportingId, componentName, componentType, parentName,
				                              false, false, isAggregateComplete, true);
			}
		}
	}

}

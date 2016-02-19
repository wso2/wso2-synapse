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
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * Handling Close Event
 */
public class StatisticsCloseEvent implements StatisticsReportingEvent {

	private StatisticDataUnit statisticDataUnit;

	public StatisticsCloseEvent(MessageContext messageContext, String componentId, String parentId) {
		String statisticId = (String) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		int msgId;
		if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID) != null) {
			msgId = (Integer) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID);
		} else {
			msgId = 0;
		}

		statisticDataUnit =
				new StatisticDataUnit(statisticId, componentId, parentId, msgId, messageContext.isResponse(),
				                      messageContext.getEnvironment(), messageContext);
	}

	public StatisticsCloseEvent(MessageContext messageContext, String componentId, String parentId,
	                            boolean isCloneLog, boolean isAggregateLog, boolean isAlteringContent) {
		this(messageContext, componentId, parentId);
		if (isAggregateLog) {
			statisticDataUnit.setAggregatePoint();
		}

		if (isCloneLog) {
			statisticDataUnit.setClonePoint();
		}

		if (isAlteringContent) {
			statisticDataUnit.setPayload(messageContext.getEnvelope().toString());
		}
	}

	@Override
	public void process() {
		RuntimeStatisticCollector.closeStatisticEntry(statisticDataUnit, StatisticsConstants.GRACEFULLY_CLOSE);
	}
}

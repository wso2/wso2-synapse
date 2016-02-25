/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class SequenceStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(SequenceStatisticCollector.class);

	/**
	 * Reports statistics for the Sequence.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param sequenceName        Sequence name.
	 * @param aspectConfiguration Aspect Configuration for the Sequence.
	 * @param isCreateLog         It is statistic flow start or end.
	 */
	public static void reportStatisticForSequence(MessageContext messageContext, String sequenceName,
	                                              AspectConfiguration aspectConfiguration, boolean isCreateLog) {
		if (isStatisticsEnabled()) {
			Boolean isStatsAlreadyCollected =
					(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);

			if (isStatsAlreadyCollected == null || !isStatsAlreadyCollected) {
				boolean isCollectingStatistics =
						(aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());
				boolean isCollectingTracing = (aspectConfiguration != null && aspectConfiguration.isTracingEnabled());

				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, isCollectingStatistics);
				messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, isCollectingTracing);

				if (isCollectingStatistics) {
					if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null && !isCreateLog) {
						log.error("Trying close statistic entry without Statistic ID");
						return;
					}
					setStatisticsTraceId(messageContext);
					createStatisticForSequence(messageContext, aspectConfiguration, sequenceName, isCreateLog, true);
				}
			} else {
				if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) {
					createStatisticForSequence(messageContext, aspectConfiguration, sequenceName, isCreateLog, false);
				}
			}
		}
	}

	private static void createStatisticForSequence(MessageContext messageContext,
	                                               AspectConfiguration aspectConfiguration, String sequenceName,
	                                               boolean isCreateLog, boolean individualStatisticCollected) {
		if (isCreateLog) {
			String reportingId = (aspectConfiguration == null) ? null : aspectConfiguration.getUniqueId();
			createLogForMessageCheckpoint(messageContext, reportingId, sequenceName, ComponentType.SEQUENCE, null, true,
			                              false, false, true, individualStatisticCollected);
		} else {
			createLogForMessageCheckpoint(messageContext, null, sequenceName, ComponentType.SEQUENCE, null, false,
			                              false, false, true);
		}
	}
}

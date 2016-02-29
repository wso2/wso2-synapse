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
import org.apache.synapse.aspects.flow.statistics.util.StatisticMessageCountHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

@Deprecated public class SplittingMediatorsStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(SplittingMediatorsStatisticCollector.class);

	public static void setCloneProperties(MessageContext oldMessageContext, MessageContext newMessageContext) {
		if (shouldReportStatistic(oldMessageContext)) {
			StatisticMessageCountHolder cloneCount;
			int parentMsgId;
			if (oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER) != null) {
				cloneCount = (StatisticMessageCountHolder) oldMessageContext
						.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER);
				parentMsgId = (Integer) oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID);
			} else {
				parentMsgId = 0;
				cloneCount = new StatisticMessageCountHolder();
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID, 0);
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID, null);
			}
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID,
			                              cloneCount.incrementAndGetCloneCount());
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID, parentMsgId);
		}
	}

}

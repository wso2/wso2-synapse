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

package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.FaultEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * FaultStatisticCollector receives fault events from synapse mediation engine.
 */
public class FaultStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(FaultStatisticCollector.class);

	/**
	 * Enqueue Fault Event to the event queue indicating that fault has occurred in the message flow.
	 *
	 * @param messageContext messageContext of the message flow.
	 */
	public static void reportFault(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isFaultCreated = isFaultAlreadyReported(messageContext);
			if (!isFaultCreated) {
				BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
				dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
				dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(messageContext, null));

				FaultEvent faultEvent = new FaultEvent(dataUnit);
				statisticEventQueue.enqueue(faultEvent);
			}
		}
	}

	/**
	 * Report to the StatisticsEntry that message flow encountered a Fault During Mediation.
	 *
	 * @param basicStatisticDataUnit raw statistic unit carrying statistic data
	 */
	public static void reportFault(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (runtimeStatistics.containsKey(basicStatisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(basicStatisticDataUnit.getStatisticId()).reportFault(basicStatisticDataUnit);
		}
	}

	private static boolean isFaultAlreadyReported(MessageContext synCtx) {
		Boolean faultReported = (Boolean) synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_FAULT_REPORTED);
		return (faultReported != null && faultReported);
	}
}

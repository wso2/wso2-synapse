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

public class EndpointStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(EndpointStatisticCollector.class);

	/**
	 * Reports Endpoint Statistics.
	 *
	 * @param messageContext               Current MessageContext of the flow.
	 * @param endpointName                 Endpoint name.
	 * @param individualStatisticCollected Whether individual statistic of this endpoint is collected.
	 * @param isCreateLog                  It is statistic flow start or end.
	 */
	public static void reportStatisticForEndpoint(MessageContext messageContext, String endpointName,
	                                              boolean individualStatisticCollected, boolean isCreateLog) {
		if (RuntimeStatisticCollector.isStatisticsEnabled()) {
			if (individualStatisticCollected) {
				if (isCreateLog) {
					setStatisticsTraceId(messageContext);
				}
			}
			if (shouldReportStatistic(messageContext) || individualStatisticCollected) {
				createLogForMessageCheckpoint(messageContext, endpointName, ComponentType.ENDPOINT, null, isCreateLog,
				                              false, false, false, individualStatisticCollected);
			}
		}
	}
}

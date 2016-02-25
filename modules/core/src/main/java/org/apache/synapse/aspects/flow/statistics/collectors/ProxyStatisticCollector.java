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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class ProxyStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(ProxyStatisticCollector.class);

	/**
	 * Reports statistics for the Proxy.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param proxyName           Proxy name.
	 * @param aspectConfiguration Aspect Configuration for the Proxy.
	 */
	public static void reportStatisticsForProxy(MessageContext messageContext, String proxyName,
	                                            AspectConfiguration aspectConfiguration) {
		if (isStatisticsEnabled()) {
			boolean isCollectingStatistics = (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());
			boolean isCollectingTracing = (aspectConfiguration != null && aspectConfiguration.isTracingEnabled());

			messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, isCollectingStatistics);
			messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, isCollectingTracing);

			if (isCollectingStatistics) {
				setStatisticsTraceId(messageContext);
				createLogForMessageCheckpoint(messageContext, aspectConfiguration.getUniqueId(), proxyName,
				                              ComponentType.PROXYSERVICE, null, true, false, false, true);
			}
		}
	}

	/**
	 * End Statistic Flow for Proxy if Message Flow is Out_Only.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 */
	public static void reportEndProxy(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isOutOnly =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
			if (!isOutOnly) {
				isOutOnly = (!Boolean
						.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) && !messageContext.isResponse());
			}
			if (isOutOnly) {
				createLogForFinalize(messageContext);
			}
		}
	}

	/**
	 * End Statistic Flow for Proxy if Message Flow is Out_Only and there is no Sending Fault.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 */
	public static void reportEndSynapseMessageReceiver(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isOutOnly =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
			boolean isFault =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_FAULT)));
			if (!isOutOnly && !isFault) {
				isOutOnly = !Boolean.parseBoolean(
						String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) &&
				            !messageContext.isResponse();
			}
			if (isOutOnly && !isFault) {
				createLogForFinalize(messageContext);
			}
		}
	}
}

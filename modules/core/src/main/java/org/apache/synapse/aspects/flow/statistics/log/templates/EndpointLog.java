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
import org.apache.synapse.aspects.flow.statistics.log.StatisticReportingLog;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.core.SynapseEnvironment;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EndpointLog implements StatisticReportingLog {

	private String endpointId;
	private String endpointName;
	private long time;
	private boolean isCreateLog;
	private String statisticId;
	private SynapseEnvironment synapseEnvironment;
	private String timeStamp;
	private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");



	public EndpointLog(MessageContext messageContext, String endpointId, String endpointName, boolean isCreateLog) {
		this.endpointId = endpointId;
		this.endpointName = endpointName;
		this.isCreateLog = isCreateLog;
		this.time = System.currentTimeMillis();
		this.statisticId = (String) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		this.synapseEnvironment = messageContext.getEnvironment();
		this.timeStamp = dateFormatter.format(new Date());
	}

	@Override public void process() {
		RuntimeStatisticCollector
				.createEndpointStatistics(statisticId, timeStamp, endpointId, endpointName, synapseEnvironment, time,
				                          isCreateLog);
	}
}

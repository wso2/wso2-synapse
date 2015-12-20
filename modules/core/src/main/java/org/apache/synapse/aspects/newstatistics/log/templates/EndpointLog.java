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

package org.apache.synapse.aspects.newstatistics.log.templates;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.newstatistics.RuntimeStatisticCollector;
import org.apache.synapse.core.SynapseEnvironment;

public class EndpointLog implements StatisticReportingLog {

	private String endpointUuid;
	private String endpointName;
	private long time;
	private boolean isCreateLog;
	private String statisticId;
	private SynapseEnvironment synapseEnvironment;

	public EndpointLog(MessageContext messageContext, String endpointUuid, String endpointName, boolean isCreateLog) {
		this.endpointUuid = endpointUuid;
		this.endpointName = endpointName;
		this.isCreateLog = isCreateLog;
		this.time = System.currentTimeMillis();
		this.statisticId = (String) messageContext.getProperty(SynapseConstants.NEW_STATISTICS_ID);
		this.synapseEnvironment = messageContext.getEnvironment();
	}

	@Override public void process() {
		RuntimeStatisticCollector.createEndpointstatistics(statisticId, endpointUuid, endpointName, isCreateLog, time,
		                                                   synapseEnvironment);
	}
}

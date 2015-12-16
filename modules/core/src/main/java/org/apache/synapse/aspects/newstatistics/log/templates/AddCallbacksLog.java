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

public class AddCallbacksLog implements StatisticReportingLog {

	private final String statisticId;
	private final Integer cloneId;
	private String callbackId;

	public AddCallbacksLog(MessageContext messageContext, String callbackId) {
		statisticId = (String) messageContext.getProperty(SynapseConstants.NEW_STATISTICS_ID);
		if (messageContext.getProperty(SynapseConstants.NEW_STATISTICS_MESSAGE_ID) != null) {
			cloneId = (Integer) messageContext.getProperty(SynapseConstants.NEW_STATISTICS_MESSAGE_ID);
		} else {
			cloneId = -1;
		}
		this.callbackId = callbackId;
	}

	@Override public void process() {
		RuntimeStatisticCollector.addCallbacks(statisticId, callbackId, cloneId);
	}
}

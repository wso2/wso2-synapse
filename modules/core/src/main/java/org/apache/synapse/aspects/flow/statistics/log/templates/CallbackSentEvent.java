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

package org.apache.synapse.aspects.flow.statistics.log.templates;

import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.MessageFlowProcessorInterface;
import org.apache.synapse.aspects.flow.statistics.log.StatisticEventProcessor3;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;

/**
 * Event to represent callback sending.
 */
public class CallbackSentEvent extends AbstractStatisticEvent {

	private CallbackDataUnit callbackDataUnit;

	public CallbackSentEvent(CallbackDataUnit callbackDataUnit) {
		this.callbackDataUnit = callbackDataUnit;
		this.eventType = EventType.CALLBACK_SENT_EVENT;
	}

	@Override
	public void process() {
		StatisticEventProcessor3.addCallbacks(callbackDataUnit);
	}

    @Override
    public void processEvents(MessageFlowProcessorInterface messageFlowProcessor) {
        messageFlowProcessor.addCallbacks(callbackDataUnit);
    }

	@Override
	public BasicStatisticDataUnit getDataUnit() {
		return callbackDataUnit;
	}
}

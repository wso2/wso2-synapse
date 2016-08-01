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

/**
 * Event to represent end of the callback handling.
 */
public class CallbackHandledEvent extends AbstractStatisticEvent {

	private CallbackDataUnit callbackDataUnit;

	public CallbackHandledEvent(CallbackDataUnit callbackDataUnit) {
		this.callbackDataUnit = callbackDataUnit;
		this.eventType = EventType.CALLBACK_HANDLED_EVENT;
	}

	@Override
	public BasicStatisticDataUnit getDataUnit() {
		return callbackDataUnit;
	}
}

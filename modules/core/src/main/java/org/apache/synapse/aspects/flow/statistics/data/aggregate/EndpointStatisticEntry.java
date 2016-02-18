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

package org.apache.synapse.aspects.flow.statistics.data.aggregate;

import java.util.HashMap;

import org.apache.synapse.aspects.flow.statistics.data.raw.EndpointStatisticLog;

import java.util.Map;

public class EndpointStatisticEntry {

	private Map<String, EndpointStatisticLog> endpointMap;

	private Map<String, String> callbackMap;

	private String lastEndpointUuid;

	public EndpointStatisticEntry() {
		this.endpointMap = new HashMap<>();
		this.callbackMap = new HashMap<>();
	}

	public void createEndpointLog(String endpointId, String uuid, String timestamp, String endpointName,
	                              long startTime) {
		EndpointStatisticLog endpointstatisticLog = new EndpointStatisticLog(uuid, timestamp, endpointName, startTime);
		endpointMap.put(endpointId, endpointstatisticLog);
		lastEndpointUuid = endpointId;
	}

	public EndpointStatisticLog closeEndpointLog(String endpointId, String name, long endTime) {
		EndpointStatisticLog endpointStatisticLog = endpointMap.get(endpointId);
		if (endpointStatisticLog != null) {
			endpointStatisticLog.setEndTime(endTime);
			if (!endpointStatisticLog.isCallbackRegistered()) {
				endpointMap.remove(endpointId);
				return endpointStatisticLog;
			}
		}
		return null;
	}

	public void registerCallback(String callbackId) {
		if (lastEndpointUuid != null) {
			callbackMap.put(callbackId, lastEndpointUuid);
			endpointMap.get(lastEndpointUuid).setIsCallbackRegistered();
			lastEndpointUuid = null;
		}
	}

	public EndpointStatisticLog unregisterCallback(String callbackId, long endTime, boolean isOutOnlyFlow) {
		if (callbackMap.containsKey(callbackId)) {
			String uuid = callbackMap.remove(callbackId);
			EndpointStatisticLog endpointStatisticLog = endpointMap.remove(uuid);
			if (endpointStatisticLog != null) {
				if (!isOutOnlyFlow) {
					endpointStatisticLog.setEndTime(endTime);
				}
				return endpointStatisticLog;
			}
		}
		return null;
	}

	public int getSize() {
		return endpointMap.size();
	}
}

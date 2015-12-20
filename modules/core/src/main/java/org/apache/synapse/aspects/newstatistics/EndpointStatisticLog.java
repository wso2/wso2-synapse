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

package org.apache.synapse.aspects.newstatistics;

public class EndpointStatisticLog {

	private final String componentId;

	private boolean hasFault;

	private long startTime;

	private long endTime;

	private boolean isCallbackRegistered;

	public EndpointStatisticLog(String componentId, long startTime) {
		this.componentId = componentId;
		this.startTime = startTime;
		this.isCallbackRegistered = false;
	}

	public String getComponentId() {
		return componentId;
	}

	public boolean isHasFault() {
		return hasFault;
	}

	public void setHasFault() {
		this.hasFault = true;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public boolean isCallbackRegistered() {
		return isCallbackRegistered;
	}

	public void setIsCallbackRegistered() {
		this.isCallbackRegistered = true;
	}
}

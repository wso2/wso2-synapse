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

import org.apache.synapse.aspects.ComponentType;

/**
 * StatisticsLog holds statistics logs during statistic collection. When message passes through each
 * mediator,API,sequence etc. StatisticsLog will be created for each and every component.
 */

public class StatisticsLog {

	private final int parentLevel;

	private final int parentMsgId;

	private final ComponentType componentType;

	private final String componentId;

	private final String parent;

	private final int msgId;

	private boolean hasChildren = false;

	private int noOfChildren = 0;

	private int noOfFaults = 0;

	private long startTime = -1;

	private long endTime = -1;

	private boolean isResponse;

	public StatisticsLog(String componentId, ComponentType componentType, int msgId, int parentLevel, int parentMsgId,
	                     String parent, long startTime) {

		this.parentLevel = parentLevel;
		this.startTime = startTime;
		this.componentType = componentType;
		this.componentId = componentId;
		this.parent = parent;
		this.msgId = msgId;
		this.parentMsgId = parentMsgId;
	}

	public int getParentMsgId() {
		return parentMsgId;
	}

	public void setHasChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getParent() {
		return parent;
	}

	public boolean isHasChildren() {
		return hasChildren;
	}

	public int getParentLevel() {
		return parentLevel;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public int getNoOfChildren() {
		return noOfChildren;
	}

	public long getStartTime() {
		return startTime;
	}

	public String getComponentId() {
		return componentId;
	}

	public long getEndTime() {
		return endTime;
	}

	public void incrementNoOfChildren() {
		noOfChildren += 1;
	}

	public int getNoOfFaults() {
		return noOfFaults;
	}

	public void incrementNoOfFaults() {
		this.noOfFaults += 1;
	}

	public int getMsgId() {
		return msgId;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public void setIsResponse(boolean isResponse) {
		this.isResponse = isResponse;
	}
}
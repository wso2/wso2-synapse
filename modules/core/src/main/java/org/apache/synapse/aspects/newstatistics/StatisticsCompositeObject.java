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

import java.beans.ConstructorProperties;

/**
 * Composite data object which is passed to the JMX Agent
 */
public class StatisticsCompositeObject {

	private final String componentId;

	private final String componentType;

	private final String parentId;

	private final int parentMsgId;

	private final int msgId;

	private long maxProcessingTime = 0;

	private long minProcessingTime = Long.MAX_VALUE;

	private long avgProcessingTime = 0;

	private boolean inResponsePath;

	private int numberOfInvocations = 0;

	private int faultCount;

	@ConstructorProperties({ "componentId", "componentType", "parentId", "parentMsgId", "msgId", "maxProcessingTime",
	                         "maxProcessingTime", "minProcessingTime", "avgProcessingTime", "inResponsePath",
	                         "faultCount" }) public StatisticsCompositeObject(String componentId, String componentType,
	                                                                          String parentId, int parentMsgId,
	                                                                          int msgId, long maxProcessingTime,
	                                                                          long minProcessingTime,
	                                                                          long avgProcessingTime,
	                                                                          boolean inResponsePath,
	                                                                          int numberOfInvocations, int faultCount) {
		this.faultCount = faultCount;
		this.componentId = componentId;
		this.componentType = componentType;
		this.parentId = parentId;
		this.parentMsgId = parentMsgId;
		this.msgId = msgId;
		this.maxProcessingTime = maxProcessingTime;
		this.minProcessingTime = minProcessingTime;
		this.avgProcessingTime = avgProcessingTime;
		this.inResponsePath = inResponsePath;
		this.numberOfInvocations = numberOfInvocations;
	}

	public int getFaultCount() {
		return faultCount;
	}

	public String getComponentId() {
		return componentId;
	}

	public String getComponentType() {
		return componentType;
	}

	public String getParentId() {
		return parentId;
	}

	public int getParentMsgId() {
		return parentMsgId;
	}

	public int getMsgId() {
		return msgId;
	}

	public long getMaxProcessingTime() {
		return maxProcessingTime;
	}

	public long getMinProcessingTime() {
		return minProcessingTime;
	}

	public long getAvgProcessingTime() {
		return avgProcessingTime;
	}

	public boolean isInResponsePath() {
		return inResponsePath;
	}

	public int getNumberOfInvocations() {
		return numberOfInvocations;
	}
}

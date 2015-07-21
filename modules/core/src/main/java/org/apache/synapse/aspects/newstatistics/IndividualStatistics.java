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
 *
 */

package org.apache.synapse.aspects.newstatistics;

import org.apache.synapse.aspects.ComponentType;

import java.util.ArrayList;

/**
 * IndividualStatistics represent a node in the statistics tree in the statistics store. It is
 * responsible for maintaining statistics about a component and updating statistics of the
 * component when new data comes
 */
public class IndividualStatistics {

	/**
	 * holds references to the branches that starts from its node
	 */
	private final ArrayList<IndividualStatistics> children;

	/**
	 * statistic owners component Id
	 */
	private final String componentId;

	/**
	 * statistic owners component Id Component Type
	 */
	private final ComponentType componentType;

	/**
	 * parentId of this Individual Statistics Node
	 */
	private final String parentId;

	/**
	 * message Identification number of this Individual Statistics Node's parentId
	 */
	private final int parentMsgId;

	/**
	 * message identification number in the message flow
	 */
	private final int msgId;

	/**
	 * Maximum processing time for the component
	 */
	private long maxProcessingTime = 0;

	/**
	 * Minimum processing time for the component
	 */
	private long minProcessingTime = Long.MAX_VALUE;

	/**
	 * Average processing time for the component
	 */
	private long avgProcessingTime = 0;

	/**
	 * component is in the response path or not
	 */
	private boolean isResponse;

	/**
	 * The number of access count this component is invoked in the message flow
	 */
	private int count = 0;

	/**
	 * The number of fault count for this component. This is a combination its own fault count
	 * and children fault count
	 */
	private int faultCount = 0;

	/**
	 * Overloaded constructor to set variables for the node
	 *
	 * @param componentId   Node owner's component
	 * @param componentType Node component type
	 * @param msgId         message Id of the statistic reported component
	 * @param parentId      statistics tree parents component Id
	 * @param parentMsgId   statistics tree parents message Id
	 * @param duration      Execution time for this statistics reporting component
	 * @param faultCount    Number of faults encountered during execution
	 */
	public IndividualStatistics(String componentId, ComponentType componentType, int msgId,
	                            String parentId, int parentMsgId, long duration, int faultCount) {
		children = new ArrayList<IndividualStatistics>();
		this.componentType = componentType;
		this.componentId = componentId;
		this.faultCount = faultCount;
		this.parentId = parentId;
		this.msgId = msgId;
		this.parentMsgId = parentMsgId;
		setDuration(duration);
	}

	/**
	 * Updates statistics record for new statistics information
	 *
	 * @param faultCount fault count for the component
	 * @param duration   execution time for the component
	 */
	public void update(int faultCount, long duration) {
		this.faultCount += faultCount;
		setDuration(duration);
	}

	/**
	 * Updates variables relating to execution time
	 *
	 * @param duration execution time for the component
	 */
	private void setDuration(long duration) {
		avgProcessingTime = (avgProcessingTime * count + duration) / (count + 1);
		if (maxProcessingTime < duration) {
			maxProcessingTime = duration;
		}
		if (minProcessingTime > duration) {
			minProcessingTime = duration;
		}
		count += 1;
	}

	public int getParentMsgId() {
		return parentMsgId;
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

	public int getCount() {
		return count;
	}

	public int getFaultCount() {
		return faultCount;
	}

	public String getComponentId() {
		return componentId;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public int getMsgId() {
		return msgId;
	}

	public ArrayList<IndividualStatistics> getChildren() {
		return children;
	}

	public String getParentId() {
		return parentId;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public void setIsResponse(boolean isResponse) {
		this.isResponse = isResponse;
	}
}
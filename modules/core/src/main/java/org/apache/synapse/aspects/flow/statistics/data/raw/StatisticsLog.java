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

package org.apache.synapse.aspects.flow.statistics.data.raw;

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * StatisticsLog holds statistics logs during statistic collection. When message passes through each
 * mediator,API,sequence etc. StatisticsLog will be created for each and every component.
 */

public class StatisticsLog {

	private final int parentLevel;

	private final int parentMsgId;

	private long timestamp;

	private String messageFlowId;

	private final ComponentType componentType;

	private final String componentId;

	private final String parent;

	private int msgId;

	List<Integer> children = new LinkedList<>();

	private int noOfFaults = 0;

	private long startTime = -1;

	private long endTime = -1;

	private long duration = 0;

	private boolean isResponse;

	private boolean cloneLog;

	private boolean aggregateLog;

	private Integer immediateChild = null;

	private Integer treeMapping = null;

	private boolean isOpenedByContinuation;

	private Map<String, Object> contextPropertyMap;

	private Map<String, Object> transportPropertyMap;

	private String beforePayload;

	private String afterPayload;

	public StatisticsLog(StatisticDataUnit statisticDataUnit, int parentMsgId, int parentLevel) {
		this.startTime = statisticDataUnit.getTime();
		this.componentType = statisticDataUnit.getComponentType();
		this.componentId = statisticDataUnit.getComponentId();
		this.parent = statisticDataUnit.getParentId();
		this.msgId = statisticDataUnit.getCloneId();
		this.isResponse = statisticDataUnit.isResponse();
		this.parentLevel = parentLevel;
		this.parentMsgId = parentMsgId;
		this.aggregateLog = statisticDataUnit.isAggregatePoint();
		this.cloneLog = statisticDataUnit.isClonePoint();
		this.immediateChild = null;
		this.contextPropertyMap = statisticDataUnit.getContextPropertyMap();
		this.transportPropertyMap = statisticDataUnit.getTransportPropertyMap();
		this.beforePayload = statisticDataUnit.getPayload();
	}

	public int getParentMsgId() {
		return parentMsgId;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
		this.duration = this.endTime - this.startTime;
	}

	public String getParent() {
		return parent;
	}

	public int getParentLevel() {
		return parentLevel;
	}

	public ComponentType getComponentType() {
		return componentType;
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

	public long getDuration() {
		return duration;
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

	public void setMsgId(int msgId) {
		this.msgId = msgId;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public void setIsResponse(boolean isResponse) {
		this.isResponse = isResponse;
	}

	public boolean isCloneLog() {
		return cloneLog;
	}

	public boolean isAggregateLog() {
		return aggregateLog;
	}

	public void setCloneLog(boolean cloneLog) {
		this.cloneLog = cloneLog;
	}

	public void setImmediateChild(Integer immediateChild) {
		this.immediateChild = immediateChild;
	}

	public void setChildren(Integer childrenIndex) {
		this.children.add(childrenIndex);
	}

	public List<Integer> getChildren() {
		return children;
	}

	public Integer getImmediateChild() {
		return immediateChild;
	}

	public Integer getTreeMapping() {
		return treeMapping;
	}

	public void setTreeMapping(int treeMapping) {
		this.treeMapping = treeMapping;
	}

	public boolean isOpenedByContinuation() {
		return isOpenedByContinuation;
	}

	public void setIsOpenedByContinuation(boolean isOpenedByContinuation) {
		this.isOpenedByContinuation = isOpenedByContinuation;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getMessageFlowId() {
		return messageFlowId;
	}

	public void setMessageFlowId(String messageFlowId) {
		this.messageFlowId = messageFlowId;
	}

	public String getBeforePayload() {
		return beforePayload;
	}

	public void setBeforePayload(String beforePayload) {
		this.beforePayload = beforePayload;
	}

	public Map<String, Object> getContextPropertyMap() {
		return contextPropertyMap;
	}

	public void setContextPropertyMap(Map<String, Object> contextPropertyMap) {
		this.contextPropertyMap = contextPropertyMap;
	}

	public Map<String, Object> getTransportPropertyMap() {
		return transportPropertyMap;
	}

	public void setTransportPropertyMap(Map<String, Object> transportPropertyMap) {
		this.transportPropertyMap = transportPropertyMap;
	}

	public void setAfterPayload(String afterPayload) {
		this.afterPayload = afterPayload;
	}

	public String getAfterPayload() {
		return afterPayload;
	}

	public String getComponentTypeToString() {
		switch (componentType) {
			case PROXYSERVICE:
				return StatisticsConstants.FLOW_STATISTICS_PROXYSERVICE;
			case ENDPOINT:
				return StatisticsConstants.FLOW_STATISTICS_ENDPOINT;
			case INBOUNDENDPOINT:
				return StatisticsConstants.FLOW_STATISTICS_INBOUNDENDPOINT;
			case SEQUENCE:
				return StatisticsConstants.FLOW_STATISTICS_SEQUENCE;
			case MEDIATOR:
				return StatisticsConstants.FLOW_STATISTICS_MEDIATOR;
			case API:
				return StatisticsConstants.FLOW_STATISTICS_API;
			case RESOURCE:
				return StatisticsConstants.FLOW_STATISTICS_RESOURCE;
			default:
				return StatisticsConstants.FLOW_STATISTICS_ANY;
		}
	}
}
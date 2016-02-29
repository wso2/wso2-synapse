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

	private final ComponentType componentType;
	private final String componentId;
	private final String parent;
	private final int parentMsgId = 0;
	private String messageFlowId;
	private String beforePayload;
	private String afterPayload;
	private List<Integer> children = new LinkedList<>();
	private Integer immediateChild = null;
	private Integer treeMapping = null;
	private Map<String, Object> contextPropertyMap;
	private Map<String, Object> transportPropertyMap;
	private int parentLevel;
	private int msgId = 0;
	private int noOfFaults = 0;
	private long startTime = -1;
	private long endTime = -1;
	private long timestamp;
	private boolean isResponse;
	private boolean isOpenedByContinuation;
	private int numberOpenTimes;
	private int currentIndex;
	private boolean isFlowContinuable;
	private boolean isFlowSplittingMediator;
	private boolean isFlowAggregateMediator;

	public StatisticsLog(StatisticDataUnit statisticDataUnit) {
		this.startTime = statisticDataUnit.getTime();
		this.componentType = statisticDataUnit.getComponentType();
		this.componentId = statisticDataUnit.getComponentId();
		this.parent = null;
		this.msgId = statisticDataUnit.getFlowId();
		parentLevel = statisticDataUnit.getParentIndex();
		this.immediateChild = null;
		this.contextPropertyMap = statisticDataUnit.getContextPropertyMap();
		this.transportPropertyMap = statisticDataUnit.getTransportPropertyMap();
		this.beforePayload = statisticDataUnit.getPayload();
		this.currentIndex = statisticDataUnit.getCurrentIndex();
		this.numberOpenTimes = 1;
		timestamp = System.currentTimeMillis();
		this.messageFlowId = statisticDataUnit.getStatisticId();
		this.isFlowContinuable = statisticDataUnit.isFlowContinuableMediator();
		this.isFlowSplittingMediator = statisticDataUnit.isFlowSplittingMediator();
		this.isFlowAggregateMediator = statisticDataUnit.isFlowAggregateMediator();
	}

	public StatisticsLog(ComponentType componentType, String componentId, int parentMsgId, int parentLevel) {
		this.componentType = componentType;
		this.componentId = componentId;
		this.parent = null;
		this.msgId = StatisticsConstants.DEFAULT_MSG_ID;
		this.parentLevel = parentLevel;
		this.immediateChild = null;
	}

	public void decrementParentLevel() {
		this.parentLevel--;
	}

	public void decrementChildren() {
		if (immediateChild != null) {
			immediateChild--;
		}

		if (children.size() > 0) {
			for (Integer child : children) {
				child -= 1;
			}
		}
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

	public int getParentMsgId() {
		return parentMsgId;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
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

	public boolean isOpenLog() {
		return numberOpenTimes > 0;
	}

	public void incrementOpenTimes() {
		this.numberOpenTimes++;
	}

	public void decrementOpenTimes() {
		this.numberOpenTimes--;
	}

	public void setParentLevel(int parentLevel) {
		this.parentLevel = parentLevel;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	public void setChildren(List<Integer> children) {
		this.children = children;
	}

	public void setTreeMapping(Integer treeMapping) {
		this.treeMapping = treeMapping;
	}

	public void setNoOfFaults(int noOfFaults) {
		this.noOfFaults = noOfFaults;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public boolean isFlowContinuable() {
		return isFlowContinuable;
	}

	public void setIsFlowContinuable(boolean isFlowContinuable) {
		this.isFlowContinuable = isFlowContinuable;
	}

	public boolean isFlowSplittingMediator() {
		return isFlowSplittingMediator;
	}

	public void setIsFlowSplittingMediator(boolean isFlowSplittingMediator) {
		this.isFlowSplittingMediator = isFlowSplittingMediator;
	}

	public boolean isFlowAggregateMediator() {
		return isFlowAggregateMediator;
	}

	public void setIsFlowAggregateMediator(boolean isFlowAggregateMediator) {
		this.isFlowAggregateMediator = isFlowAggregateMediator;
	}
}
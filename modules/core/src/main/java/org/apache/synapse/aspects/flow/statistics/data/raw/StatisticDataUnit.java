/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.util.List;
import java.util.Map;

public class StatisticDataUnit extends BasicStatisticDataUnit {
	private String payload;
	private String componentId;
	private ComponentType componentType;
	private Map<String, Object> contextPropertyMap;
	private Map<String, Object> transportPropertyMap;
	private boolean flowContinuableMediator = false;
	private boolean flowSplittingMediator = false;
	private boolean flowAggregateMediator = false;
	private boolean isIndividualStatisticCollected = false;
	private int flowId;
	private int parentIndex;
	private boolean shouldBackpackParent;
	private List<Integer> parentList;

	public StatisticDataUnit() {
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public void setComponentType(ComponentType componentType) {
		this.componentType = componentType;
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

	public boolean isFlowContinuableMediator() {
		return flowContinuableMediator;
	}

	public void setFlowContinuableMediator(boolean flowContinuableMediator) {
		this.flowContinuableMediator = flowContinuableMediator;
	}

	public boolean isIndividualStatisticCollected() {
		return isIndividualStatisticCollected;
	}

	public void setIsIndividualStatisticCollected(boolean isIndividualStatisticCollected) {
		this.isIndividualStatisticCollected = isIndividualStatisticCollected;
	}

	public int getFlowId() {
		return flowId;
	}

	public void setFlowId(int flowId) {
		this.flowId = flowId;
	}

	public int getParentIndex() {
		return parentIndex;
	}

	public void setParentIndex(int parentIndex) {
		this.parentIndex = parentIndex;
	}

	public boolean isShouldBackpackParent() {
		return shouldBackpackParent;
	}

	public void setShouldBackpackParent(boolean shouldBackpackParent) {
		this.shouldBackpackParent = shouldBackpackParent;
	}

	public void setFlowSplittingMediator(boolean isSplitting) {
		flowSplittingMediator = isSplitting;
	}

	public boolean isFlowSplittingMediator() {
		return flowSplittingMediator;
	}

	public List<Integer> getParentList() {
		return parentList;
	}

	public void setParentList(List<Integer> parentList) {
		this.parentList = parentList;
	}

	public boolean isFlowAggregateMediator() {
		return flowAggregateMediator;
	}

	public void setFlowAggregateMediator(boolean flowAggregateMediator) {
		this.flowAggregateMediator = flowAggregateMediator;
	}
}

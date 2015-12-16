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
import org.apache.synapse.core.SynapseEnvironment;

public class StatisticDataUnit {
	private String statisticId;
	private Long time;
	private ComponentType componentType;
	private String parentId;
	private String componentId;
	private int cloneId;
	private boolean isResponse;
	private SynapseEnvironment synapseEnvironment;

	public StatisticDataUnit(String statisticId, String componentId, ComponentType componentType, String parentId,
	                         int cloneId, Long time, boolean isResponse) {
		this.statisticId = statisticId;
		this.time = time;
		this.componentType = componentType;
		this.parentId = parentId;
		this.componentId = componentId;
		this.cloneId = cloneId;
		this.isResponse = isResponse;
	}

	public StatisticDataUnit(String statisticId, String componentId, String parentId, int cloneId, Long time,
	                         boolean isResponse) {
		this.statisticId = statisticId;
		this.time = time;
		this.parentId = parentId;
		this.componentId = componentId;
		this.cloneId = cloneId;
		this.isResponse = isResponse;
	}

	public StatisticDataUnit(String statisticId, SynapseEnvironment synapseEnvironment, Long time) {
		this.statisticId = statisticId;
		this.synapseEnvironment = synapseEnvironment;
		this.time = time;
	}

	public StatisticDataUnit(String statisticId, String componentId, String parentId, int cloneId, Long time,
	                         boolean isResponse, SynapseEnvironment synapseEnvironment) {
		this.statisticId = statisticId;
		this.time = time;
		this.parentId = parentId;
		this.componentId = componentId;
		this.cloneId = cloneId;
		this.isResponse = isResponse;
		this.synapseEnvironment = synapseEnvironment;
	}

	public String getStatisticId() {
		return statisticId;
	}

	public Long getTime() {
		return time;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public String getParentId() {
		return parentId;
	}

	public String getComponentId() {
		return componentId;
	}

	public int getCloneId() {
		return cloneId;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public SynapseEnvironment getSynapseEnvironment() {
		return synapseEnvironment;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
}

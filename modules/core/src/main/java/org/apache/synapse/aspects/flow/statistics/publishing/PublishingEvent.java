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

package org.apache.synapse.aspects.flow.statistics.publishing;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;

import java.util.Map;

public class PublishingEvent {

	private String componentType;
	private String componentName;

	private long startTime;
	private long endTime;
	private long duration;

	private String beforePayload;
	private String afterPayload;

	private Map contextPropertyMap;
	private Map transportPropertyMap;

	private Integer[] children;
	private String entryPoint;

	private String ComponentId;
	private Integer hashCode;

	private int faultCount;

	public PublishingEvent(StatisticsLog statisticsLog, String entryPoint) {
		this.componentType = statisticsLog.getComponentTypeToString();
		this.componentName = statisticsLog.getComponentName();

		this.startTime = statisticsLog.getStartTime();
		this.endTime = statisticsLog.getEndTime();
		this.duration = this.endTime - this.startTime;

		this.contextPropertyMap = statisticsLog.getContextPropertyMap();
		this.transportPropertyMap = statisticsLog.getTransportPropertyMap();

		if (statisticsLog.getChildren().size() > 0) {
			this.children = new Integer[statisticsLog.getChildren().size()];
			this.children = statisticsLog.getChildren().toArray(this.children);
		}

		this.entryPoint = entryPoint;

		this.faultCount = statisticsLog.getNoOfFaults();
	}

	public String getComponentType() {
		return componentType;
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getBeforePayload() {
		return beforePayload;
	}

	public void setBeforePayload(String beforePayload) {
		this.beforePayload = beforePayload;
	}

	public String getAfterPayload() {
		return afterPayload;
	}

	public void setAfterPayload(String afterPayload) {
		this.afterPayload = afterPayload;
	}

	public Map getContextPropertyMap() {
		return contextPropertyMap;
	}

	public void setContextPropertyMap(Map contextPropertyMap) {
		this.contextPropertyMap = contextPropertyMap;
	}

	public Map getTransportPropertyMap() {
		return transportPropertyMap;
	}

	public void setTransportPropertyMap(Map transportPropertyMap) {
		this.transportPropertyMap = transportPropertyMap;
	}

	public Integer[] getChildren() {
		return children;
	}

	public void setChildren(Integer[] children) {
		this.children = children;
	}

	public String getEntryPoint() {
		return entryPoint;
	}

	public void setEntryPoint(String entryPoint) {
		this.entryPoint = entryPoint;
	}

	public int getFaultCount() {
		return faultCount;
	}

	public void setFaultCount(int faultCount) {
		this.faultCount = faultCount;
	}

	public String getComponentId() {
		return ComponentId;
	}

	public void setComponentId(String componentId) {
		ComponentId = componentId;
	}

	public Integer getHashCode() {
		return hashCode;
	}

	public void setHashCode(Integer hashCode) {
		this.hashCode = hashCode;
	}

	@Override public String toString() {
		return "Component Type " + componentType + " , Component Name " +
		       componentName;
	}
}

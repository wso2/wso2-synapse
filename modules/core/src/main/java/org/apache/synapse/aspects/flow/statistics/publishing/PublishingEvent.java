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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PublishingEvent {

	private String componentType;
	private String componentName;
	private String componentId;

	private long startTime;
	private long endTime;
	private long duration;

	private String beforePayload;
	private String afterPayload;

	private Map contextPropertyMap;
	private Map transportPropertyMap;

	private Integer[] children;

	private String entryPoint;
	private Integer entryPointHashcode;
	private int faultCount;
	private Integer hashCode;

	public  PublishingEvent(){}

	public PublishingEvent(StatisticsLog statisticsLog, String entryPoint, Integer entryPointHashcode) {
		this.componentType = statisticsLog.getComponentTypeToString();
		this.componentName = statisticsLog.getComponentName();
		this.componentId = statisticsLog.getComponentId();

		this.startTime = statisticsLog.getStartTime();
		this.endTime = statisticsLog.getEndTime();
		this.duration = this.endTime - this.startTime;

		this.contextPropertyMap = extractProperties(statisticsLog.getContextPropertyMap());
		this.transportPropertyMap = extractProperties(statisticsLog.getTransportPropertyMap());

		if (statisticsLog.getChildren().size() > 0) {
			this.children = new Integer[statisticsLog.getChildren().size()];
			this.children = statisticsLog.getChildren().toArray(this.children);
		}

		this.entryPoint = entryPoint;
		this.entryPointHashcode = entryPointHashcode;
		this.faultCount = statisticsLog.getNoOfFaults();
		this.hashCode = statisticsLog.getHashCode();
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
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public Integer getHashCode() {
		return hashCode;
	}

	public void setHashCode(Integer hashCode) {
		this.hashCode = hashCode;
	}

	public Integer getEntryPointHashcode() {
		return entryPointHashcode;
	}

	public void setEntryPointHashcode(Integer entryPointHashcode) {
		this.entryPointHashcode = entryPointHashcode;
	}

	@Override
	public String toString() {
		return "Component Type " + componentType + " , Component Name " +
		       componentName;
	}

	/**
	 * Need to convert complex objects in properties to string, before converting to JSON
	 */
	private Map<String, Object> extractProperties(Map<String, Object> originalMap) {
		Map<String, Object> copy = new HashMap<String, Object>();

		if (originalMap == null) {
			return null;
		}

		for (Map.Entry<String, Object> anEntry : originalMap.entrySet()){
			Object entryValue = anEntry.getValue();

			// Directly set if it's a known serializable type
			if (entryValue instanceof String
			    || entryValue instanceof Boolean
			    || entryValue instanceof Integer
			    || entryValue instanceof Double
			    || entryValue instanceof Character) {
				copy.put(anEntry.getKey(), entryValue);
			} else {
				// Else, try to serialize
				try {
					copy.put(anEntry.getKey(), anEntry.getValue().toString());
				} catch (Exception ignore) {
					copy.put(anEntry.getKey(), "Value cannot be serialized");
				}
			}

		}
		return copy;
	}

	public Map<String, Object> getObjectAsMap() {
		Map<String, Object> objectMap = new HashMap<String, Object>();

		objectMap.put("componentType", this.componentType);
		objectMap.put("componentName", this.componentName);
		objectMap.put("componentId", this.componentId);

		objectMap.put("startTime", this.startTime);
		objectMap.put("endTime", this.endTime);
		objectMap.put("duration", this.duration);

		objectMap.put("beforePayload", this.beforePayload);
		objectMap.put("afterPayload", this.afterPayload);

		if (this.contextPropertyMap == null) {
			objectMap.put("contextPropertyMap", null);
		} else {
			objectMap.put("contextPropertyMap", this.contextPropertyMap.toString());
		}

		if (this.transportPropertyMap == null) {
			objectMap.put("transportPropertyMap", null);
		} else {
			objectMap.put("transportPropertyMap", this.transportPropertyMap.toString());
		}

		objectMap.put("children", Arrays.toString(this.children));

		objectMap.put("entryPoint", this.entryPoint);
		objectMap.put("entryPointHashcode", String.valueOf(this.entryPointHashcode));
		objectMap.put("faultCount", this.faultCount);
		objectMap.put("hashCode", String.valueOf(this.hashCode));

		return objectMap;
	}
}

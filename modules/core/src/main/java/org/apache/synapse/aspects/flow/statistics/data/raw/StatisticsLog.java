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

package org.apache.synapse.aspects.flow.statistics.data.raw;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.elasticsearch.ElasticMetadata;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.endpoints.Endpoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * StatisticsLog holds statistics logs during statistic collection. When message passes through each
 * mediator,API,sequence etc. StatisticsLog will be created for each and every component.
 */

public class StatisticsLog {

	/**
	 * Parent Index of this statistic Log.
	 */
	private int parentIndex;

	/**
	 * Number of faults in the component.
	 */
	private int noOfFaults = 0;

	/**
	 * number of branches currently referring this component as a parent.
	 */
	private int numberOpenTimes = 1;

	/**
	 * Index of the component position in the message flow.
	 */
	private int currentIndex;

	/**
	 * Start time of the event.
	 */
	private long startTime = -1;

	/**
	 * endTime of the Event.
	 */
	private long endTime = 0;

	/**
	 * Is this component is a FlowContinuable Mediator.
	 */
	private boolean isFlowContinuable;

	/**
	 * Is this component is a Splitting Mediator (Clone or Iterate).
	 */
	private boolean isFlowSplittingMediator;

	/**
	 * Is this component is a Aggregate Mediator.
	 */
	private boolean isFlowAggregateMediator;

	/**
	 * Name of the component.
	 */
	private String componentName;

	/**
	 * Statistic tracing id for the message flow.
	 */
	private String messageFlowId;

	/**
	 * Payload before mediation by the component.
	 */
	private String beforePayload;

	/**
	 * Payload after mediation by the component.
	 */
	private String afterPayload;

	/**
	 * Unique Id of the reporting component.
	 */
	private String componentId;

	/**
	 * Value of the property which is in trace scope..
	 */
	private String propertyValue;

	/**
	 * HashCode of the reporting component.
	 */
	private Integer hashCode;

	/**
	 * Children List for the component.
	 */
	private List<Integer> children = new LinkedList<>();

	/**
	 * Synapse Message context properties for the component.
	 */
	private Map<String, Object> contextPropertyMap;

	/**
	 * Synapse Message context variables for the component.
	 */
	private Map<String, Object> contextVariableMap;

	/**
	 * Transport properties for the component.
	 */
	private Map<String, Object> transportPropertyMap;

	/**
	 * Component type of the component.
	 */
	private ComponentType componentType;

	/**
	 * Reference to immediate parent of a node
	 */
	private int immediateParent;

	/**
	 * Flag indicates whether tracing enabled for the component
	 */
	private boolean isTracingEnabled;

	/**
	 * Endpoint of the sequence
	 */
	private Endpoint endpoint;

	/**
	 * Transport headers for the component.
	 */
	private Map<String, Object> transportHeaderMap;

	/**
	 * Status code of the response.
	 */
	private String statusCode;

	/**
	 * Status description of the response.
	 */
	private String statusDescription;

    private Map<String, Object> customProperties;

    /**
	 * Elastic analytics metadata holder.
	 */
	private ElasticMetadata elasticMetadata;

	public StatisticsLog(StatisticDataUnit statisticDataUnit) {
		this.parentIndex = statisticDataUnit.getParentIndex();
		this.currentIndex = statisticDataUnit.getCurrentIndex();
		this.startTime = statisticDataUnit.getTime();
		this.isFlowContinuable = statisticDataUnit.isFlowContinuableMediator();
		this.isFlowSplittingMediator = statisticDataUnit.isFlowSplittingMediator();
		this.isFlowAggregateMediator = statisticDataUnit.isFlowAggregateMediator();
		this.componentName = statisticDataUnit.getComponentName();
		this.messageFlowId = statisticDataUnit.getStatisticId();
		this.beforePayload = statisticDataUnit.getPayload();
		this.contextPropertyMap = statisticDataUnit.getContextPropertyMap();
		this.contextVariableMap = statisticDataUnit.getContextVariableMap();
		this.transportPropertyMap = statisticDataUnit.getTransportPropertyMap();
		this.componentType = statisticDataUnit.getComponentType();
		this.hashCode = statisticDataUnit.getHashCode();
		this.propertyValue = statisticDataUnit.getPropertyValue();
		if (statisticDataUnit.getComponentId() == null) {
			this.componentId = StatisticsConstants.HASH_CODE_NULL_COMPONENT;
		} else {
			this.componentId = statisticDataUnit.getComponentId();
		}
		this.isTracingEnabled = statisticDataUnit.isTracingEnabled();
		this.endpoint = statisticDataUnit.getEndpoint();
		this.transportHeaderMap = statisticDataUnit.getTransportHeaderMap();
		this.statusCode = statisticDataUnit.getStatusCode();
		this.statusDescription = statisticDataUnit.getStatusDescription();
        this.customProperties = statisticDataUnit.getCustomProperties();
	}

	public StatisticsLog(ComponentType componentType, String componentName, int parentIndex) {
		this.componentType = componentType;
		this.componentName = componentName;
		this.parentIndex = parentIndex;
	}

	public void decrementParentLevel() {
		this.parentIndex--;
	}

	public void decrementChildren() {
		if (children.size() > 0) {
			for (Integer child : children) {
				child -= 1;
			}
		}
	}

	public String getComponentTypeToString() {
		return StatisticsConstants.getComponentTypeToString(componentType);
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getParentIndex() {
		return parentIndex;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public long getStartTime() {
		return startTime;
	}

	public String getComponentName() {
		return componentName;
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

	public void setChildren(Integer childrenIndex) {
		this.children.add(childrenIndex);
	}

	public List<Integer> getChildren() {
		return children;
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

	public Map<String, Object> getContextVariableMap() {
		return contextVariableMap;
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

	public void setParentIndex(int parentIndex) {
		this.parentIndex = parentIndex;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public Map<String, Object> getTransportPropertyMap() {
		return transportPropertyMap;
	}

	public void setChildren(List<Integer> children) {
		this.children = children;
	}

	public boolean isFlowContinuable() {
		return isFlowContinuable;
	}

	public boolean isFlowSplittingMediator() {
		return isFlowSplittingMediator;
	}

	public boolean isFlowAggregateMediator() {
		return isFlowAggregateMediator;
	}

	public int getImmediateParent() {
		return immediateParent;
	}

	public void setImmediateParent(int immediateParent) {
		this.immediateParent = immediateParent;
	}

	public String getComponentId() {
		return componentId;
	}

	public Integer getHashCode() {
		return hashCode;
	}

	public void setHashCode(Integer hashCode) {
		this.hashCode = hashCode;
	}

	public boolean isTracingEnabled() {
		return isTracingEnabled;
	}

	public void setTracingEnabled(boolean tracingEnabled) {
		isTracingEnabled = tracingEnabled;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public void setComponentType(ComponentType componentType) {
		this.componentType = componentType;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public Map<String, Object> getTransportHeaderMap() {
		return transportHeaderMap;
	}

	public void setTransportHeaderMap(Map<String, Object> transportHeaderMap) {
		this.transportHeaderMap = transportHeaderMap;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusDescription() {
		return statusDescription;
	}

	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}

	public ElasticMetadata getElasticMetadata() {
		return elasticMetadata;
	}

	public void setElasticMetadata(ElasticMetadata elasticMetadata) {
		this.elasticMetadata = elasticMetadata;
	}

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
}

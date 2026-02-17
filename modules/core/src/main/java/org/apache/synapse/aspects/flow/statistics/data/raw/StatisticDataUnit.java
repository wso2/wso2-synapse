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

import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.endpoints.Endpoint;

import java.util.List;
import java.util.Map;

/**
 * This data unit carries raw statistic data for open and close Events.
 */
public class StatisticDataUnit extends BasicStatisticDataUnit {

	/**
	 * Parent Index for this event.
	 */
	private int parentIndex;

	/**
	 * Should retrieve parent when closing the event.
	 */
	private boolean shouldTrackParent;

	/**
	 * Is this a event from Continuation Call.
	 */
	private boolean continuationCall = false;

	/**
	 * Is this a event from FlowContinuableMediator.
	 */
	private boolean flowContinuableMediator = false;

	/**
	 * Is this a event from Splitting Mediator (clone or iterate).
	 */
	private boolean flowSplittingMediator = false;

	/**
	 * Is this a event from Aggregate Mediator.
	 */
	private boolean flowAggregateMediator = false;

	/**
	 * Is statistic enabled in aspect configuration.
	 */
	private boolean isIndividualStatisticCollected = false;

	/**
	 * Payload of the message context.
	 */
	private String payload;

	/**
	 * Name of the event reporting component.
	 */
	private String componentName;

	/**
	 * Component Type of the reporting component.
	 */
	private ComponentType componentType;

    /**
     * Component Type String of the reporting component.
     */
    private String componentTypeString;

    /**
	 * Unique Id of the reporting component.
	 */
	private String componentId;

	private String propertyValue;

	/**
	 * HashCode of the reporting component.
	 */
	private Integer hashCode;

	/**
	 * Parent list for this event.
	 */
	private List<Integer> parentList;

	/**
	 * Message context property map.
	 */
	private Map<String, Object> contextPropertyMap;

	/**
	 * Message context variable map.
	 */
	private Map<String, Object> contextVariableMap;

	/**
	 * Transport property map.
	 */
	private Map<String, Object> transportPropertyMap;

	/**
	 * Endpoint of the sequence.
	 */
	private Endpoint endpoint;

	/**
	 * Transport headers map for the component.
	 */
	private Map transportHeaderMap;

	/**
	 * Status code of the response. Optional
	 */
	private String statusCode;

    /**
     * Error code if the statistic event is reported due to an error
     */
    private String errorCode;

    /**
     * Error message if the statistic event is reported due to an error
     */
    private String errorMessage;

    /**
     * Denotes whether this span is an outer layer span in a tracing scope.
     * ex: Apis, Proxies, Tasks, or any top level artifact.
     */
    private boolean isOuterLayerSpan = false;

    /**
	 * Status description of the response. Optional
	 */
	private String statusDescription;

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public Map getTransportHeaderMap() {
		return transportHeaderMap;
	}

	public void setTransportHeaderMap(Map transportHeaderMap) {
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


	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getComponentName() {
		return componentName;
	}

	public void setComponentName(String componentName) {
		this.componentName = componentName;
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

	public void setContextVariableMap(Map<String, Object> contextVariableMap) {
		this.contextVariableMap = contextVariableMap;
	}

	public Map<String, Object> getContextVariableMap() {
		return contextVariableMap;
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

	public int getParentIndex() {
		return parentIndex;
	}

	public void setParentIndex(int parentIndex) {
		this.parentIndex = parentIndex;
	}

	public boolean isShouldTrackParent() {
		return shouldTrackParent;
	}

	public void setShouldTrackParent(boolean shouldTrackParent) {
		this.shouldTrackParent = shouldTrackParent;
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

	public boolean isContinuationCall() {
		return continuationCall;
	}

	public void setContinuationCall(boolean continuationCall) {
		this.continuationCall = continuationCall;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

    public String getComponentTypeString() {
        return componentTypeString;
    }

    public void setComponentTypeString(String componentTypeString) {
        this.componentTypeString = componentTypeString;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isOuterLayerSpan() {
        return isOuterLayerSpan;
    }

    public void setOuterLayerSpan(boolean isOuterLayerSpan) {
        this.isOuterLayerSpan = isOuterLayerSpan;
    }
}

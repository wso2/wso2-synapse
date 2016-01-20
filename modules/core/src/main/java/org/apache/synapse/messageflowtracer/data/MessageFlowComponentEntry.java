/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.messageflowtracer.data;

import org.apache.synapse.core.SynapseEnvironment;

import java.util.Map;

/**
 * This class represents state of each stage in mediation flow. It stores all the synapse properties and payload at
 * each collecting point
 */
public class MessageFlowComponentEntry implements MessageFlowDataEntry {

    private String messageId;
    private String componentId;
    private String componentName;
    private boolean response;
    private boolean start;
    private String timestamp;
    private String payload;
    private SynapseEnvironment synapseEnvironment;
    private Map<String, Object> propertyMap;
    private Map<String, Object> transportPropertyMap;

    public MessageFlowComponentEntry(String messageId, String componentId, String componentName, boolean response,
                                     boolean start, String timestamp, Map<String, Object> propertyMap, Map<String,
            Object> transportPropertyMap, String payload, SynapseEnvironment synapseEnvironment) {
        this.messageId = messageId;
        this.componentId = componentId;
        this.componentName = componentName;
        this.response = response;
        this.start = start;
        this.timestamp = timestamp;
        this.propertyMap = propertyMap;
        this.transportPropertyMap = transportPropertyMap;
        this.payload = payload;
        this.synapseEnvironment = synapseEnvironment;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, Object> getPropertyMap() {
        return propertyMap;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getComponentId() {
        return componentId;
    }

    public boolean isResponse() {
        return response;
    }

    public boolean isStart() {
        return start;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getTransportPropertyMap() {
        return transportPropertyMap;
    }

    @Override
    public String toString() {
        return ", componentName='" + componentName + '\'' +
                ", response=" + response +
                ", start=" + start +
                ", timestamp='" + timestamp ;
    }

    /**
     * This adds current instance to the MessageFlowDataHolder data store for publishing
     */
    public void process(){
        this.synapseEnvironment.getMessageFlowDataHolder().addComponentInfoEntry(this);
        this.synapseEnvironment.getMessageDataCollector().enQueue(this);

    }
}

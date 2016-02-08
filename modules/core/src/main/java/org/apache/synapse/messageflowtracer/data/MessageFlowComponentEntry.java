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

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.messageflowtracer.util.MessageFlowTracerConstants;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class represents state of each stage in mediation flow. It stores all the synapse properties and payload at
 * each collecting point
 */
public class MessageFlowComponentEntry implements MessageFlowDataEntry {

    private String messageId;
    private String componentId;
    private String componentName;
    private long timestamp;
    private String payload;
    private String parent;
    private String entryType;
    private Map<String, Object> propertyMap;
    private Map<String, Object> transportPropertyMap;

    public MessageFlowComponentEntry(MessageContext synCtx, String componentId, String componentName) {
        this.messageId = synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString();
        this.componentId = componentId;
        this.componentName = componentName;
        this.timestamp = new Date().getTime();
        this.propertyMap = this.extractContextProperties(synCtx);
        this.transportPropertyMap = this.extractTransportProperties(synCtx);
        this.payload = synCtx.getEnvelope().toString();
        this.parent = (String) synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_PARENT);
        this.entryType = synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE).toString();
    }

    public String getPayload() {
        return payload;
    }

    public String getParent() {
        return parent;
    }

    public String getEntryType() {
        return entryType;
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

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getTransportPropertyMap() {
        return transportPropertyMap;
    }

    @Override
    public String toString() {
        return ", componentName='" + componentName + '\'' +
                ", timestamp='" + timestamp ;
    }

    /**
     * Extract message context properties
     *
     * @param synCtx
     * @return
     */
    private Map<String, Object> extractContextProperties(MessageContext synCtx) {
        Set<String> propertySet = synCtx.getPropertyKeySet();
        Map<String, Object> propertyMap = new TreeMap<>();

        for (String property : propertySet) {
            Object propertyValue = synCtx.getProperty(property);
            propertyMap.put(property, propertyValue);
        }

        // Remove message-flow-tracer properties
        propertyMap.remove(MessageFlowTracerConstants.MESSAGE_FLOW_PARENT);
        propertyMap.remove(MessageFlowTracerConstants.MESSAGE_FLOW_INCREMENT_ID);
        propertyMap.remove(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE);

        return propertyMap;
    }

    /**
     * Extract transport headers from context
     *
     * @param synCtx
     * @return
     */
    private Map<String, Object> extractTransportProperties(MessageContext synCtx) {
        Map<String, Object> transportPropertyMap = new TreeMap<>();

        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            Set<String> axis2PropertySet = headersMap.keySet();
            for (String entry : axis2PropertySet) {
                transportPropertyMap.put(entry, headersMap.get(entry));
            }
        }

        // Adding important transport related properties
        if (axis2MessageCtx.getTo() != null) {
            transportPropertyMap.put("To", axis2MessageCtx.getTo().getAddress());
        }

        if (axis2MessageCtx.getFrom() != null) {
            transportPropertyMap.put("From", axis2MessageCtx.getFrom().getAddress());
        }

        if (axis2MessageCtx.getWSAAction() != null) {
            transportPropertyMap.put("WSAction", axis2MessageCtx.getWSAAction());
        }

        if (axis2MessageCtx.getSoapAction() != null) {
            transportPropertyMap.put("SOAPAction", axis2MessageCtx.getSoapAction());
        }

        if (axis2MessageCtx.getReplyTo() != null) {
            transportPropertyMap.put("ReplyTo", axis2MessageCtx.getReplyTo().getAddress());
        }

        if (axis2MessageCtx.getMessageID() != null) {
            transportPropertyMap.put("MessageID", axis2MessageCtx.getMessageID());
        }

        // Remove unnecessary properties
        if (transportPropertyMap.get("Cookie") != null) {
            transportPropertyMap.remove("Cookie");
        }

        return transportPropertyMap;
    }
}

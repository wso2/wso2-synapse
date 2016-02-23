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

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StatisticDataUnit {
    private String statisticId;
    private Long time;
    private ComponentType componentType;
    private String parentId;
    private String componentId;
    private int cloneId;
    private boolean isResponse;
    private SynapseEnvironment synapseEnvironment;
    private boolean aggregatePoint;
    private boolean clonePoint;
    private long timestamp;
    private Map<String, Object> contextPropertyMap;
    private Map<String, Object> transportPropertyMap;
    private String payload;
    private boolean aspectConfigStatsEnabled = false;
    private boolean aspectConfigTraceEnabled = false;

    public StatisticDataUnit(String statisticId, String componentId, ComponentType componentType, String parentId,
                             int cloneId, boolean isResponse, MessageContext messageContext,
                             boolean isAlteringContent) {
        this(statisticId, componentId, componentType, parentId, cloneId, isResponse, messageContext);

        if (RuntimeStatisticCollector.isCollectingPayloads() && shouldCaptureTracing(messageContext) && isAlteringContent) {
            try {
                payload = messageContext.getEnvelope().toString();
            } catch (Exception e) {
                // SOAP envelop is not created yet
                payload = "NONE";
            }

        }
    }

    public StatisticDataUnit(String statisticId, String componentId, ComponentType componentType, String parentId,
                             int cloneId, boolean isResponse, MessageContext messageContext) {
        this(statisticId, componentId, parentId, cloneId, isResponse, null, messageContext);
        this.componentType = componentType;
    }

    public StatisticDataUnit(String statisticId, String componentId, String parentId, int cloneId,
                             boolean isResponse, SynapseEnvironment synapseEnvironment, MessageContext messageContext) {
        this.statisticId = statisticId;
        this.time = System.currentTimeMillis();
        this.parentId = parentId;
        this.componentId = componentId;
        this.cloneId = cloneId;
        this.isResponse = isResponse;
        this.synapseEnvironment = synapseEnvironment;
        this.aggregatePoint = false;
        this.clonePoint = false;
        this.timestamp = System.currentTimeMillis();

        if (RuntimeStatisticCollector.isCollectingProperties() && shouldCaptureTracing(messageContext)) {
            this.contextPropertyMap = this.extractContextProperties(messageContext);
            this.transportPropertyMap = this.extractTransportProperties(messageContext);
        }

        this.aspectConfigTraceEnabled = shouldCaptureTracing(messageContext);
        this.aspectConfigStatsEnabled = shouldCaptureStats(messageContext);
    }

    public StatisticDataUnit(String statisticId, SynapseEnvironment synapseEnvironment, Long time) {
        this.statisticId = statisticId;
        this.synapseEnvironment = synapseEnvironment;
        this.time = time;
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

    public void setCloneId(int cloneId) {
        this.cloneId = cloneId;
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

    public boolean isAggregatePoint() {
        return aggregatePoint;
    }

    public void setAggregatePoint() {
        this.aggregatePoint = true;
    }

    public boolean isClonePoint() {
        return clonePoint;
    }

    public void setClonePoint() {
        this.clonePoint = true;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getContextPropertyMap() {
        return contextPropertyMap;
    }

    public Map<String, Object> getTransportPropertyMap() {
        return transportPropertyMap;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isAspectConfigTraceEnabled() {
        return aspectConfigTraceEnabled;
    }

    public void setAspectConfigTraceEnabled(boolean aspectConfigTraceEnabled) {
        this.aspectConfigTraceEnabled = aspectConfigTraceEnabled;
    }

    public boolean isAspectConfigStatsEnabled() {
        return aspectConfigStatsEnabled;
    }

    public void setAspectConfigStatsEnabled(boolean aspectConfigStatsEnabled) {
        this.aspectConfigStatsEnabled = aspectConfigStatsEnabled;
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
        propertyMap.remove(SynapseConstants.STATISTICS_STACK);

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
            transportPropertyMap.put(SynapseConstants.HEADER_TO, axis2MessageCtx.getTo().getAddress());
        }

        if (axis2MessageCtx.getFrom() != null) {
            transportPropertyMap.put(SynapseConstants.HEADER_FROM, axis2MessageCtx.getFrom().getAddress());
        }

        if (axis2MessageCtx.getWSAAction() != null) {
            transportPropertyMap.put("WSAction", axis2MessageCtx.getWSAAction());
        }

        if (axis2MessageCtx.getSoapAction() != null) {
            transportPropertyMap.put("SOAPAction", axis2MessageCtx.getSoapAction());
        }

        if (axis2MessageCtx.getReplyTo() != null) {
            transportPropertyMap.put(SynapseConstants.HEADER_REPLY_TO, axis2MessageCtx.getReplyTo().getAddress());
        }

        if (axis2MessageCtx.getMessageID() != null) {
            transportPropertyMap.put(SynapseConstants.HEADER_MESSAGE_ID, axis2MessageCtx.getMessageID());
        }

        // Remove unnecessary properties
        if (transportPropertyMap.get("Cookie") != null) {
            transportPropertyMap.remove("Cookie");
        }

        return transportPropertyMap;
    }

    /**
     * Decide whether to collect tracing data per message flow
     *
     * @param synCtx
     * @return true, if need to collect tracing data.
     */
    private boolean shouldCaptureTracing(MessageContext synCtx) {
        Boolean isCollectingTraces = (Boolean) synCtx.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);

        if (isCollectingTraces == null) {
            return false;
        }
        else {
            return isCollectingTraces;
        }
    }


    private boolean shouldCaptureStats(MessageContext synCtx) {
        Boolean isCollectingStats = (Boolean) synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);

        if (isCollectingStats == null) {
            return false;
        }
        else {
            return isCollectingStats;
        }
    }
}

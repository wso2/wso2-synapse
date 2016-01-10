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
package org.apache.synapse.messageflowtracer.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.messageflowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.messageflowtracer.data.MessageFlowComponentId;
import org.apache.synapse.messageflowtracer.data.MessageFlowDataEntry;
import org.apache.synapse.messageflowtracer.data.MessageFlowTraceEntry;
import org.apache.synapse.messageflowtracer.util.MessageFlowTracerConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Tracing Data Collector class which has a blocking queue to store collected data temporarily until been analyzed
 */
public class MessageFlowTracingDataCollector {

    private static final Log log = LogFactory.getLog(MessageFlowTracingDataCollector.class.getName());
    private static boolean isMessageFlowTracingEnabled;
    private static Date date;
    private static MessageDataCollector tracingDataCollector;
    private static MessageFlowTracingDataConsumer tracingDataConsumer;
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    public static void init() {
        int queueSize;
        isMessageFlowTracingEnabled = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue
                (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_ENABLED, MessageFlowTracerConstants
                        .DEFAULT_TRACE_ENABLED));
        if (isMessageFlowTracingEnabled()) {
            queueSize = Integer.parseInt(SynapsePropertiesLoader.getPropertyValue
                    (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_QUEUE_SIZE, MessageFlowTracerConstants.DEFAULT_QUEUE_SIZE));

            tracingDataCollector = new MessageDataCollector(queueSize);
            tracingDataConsumer = new MessageFlowTracingDataConsumer();
            //Thread to consume queue and update data structures for publishing
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName("Mediation Tracing Data consumer Task");
                            return t;
                        }
                    });
            executor.scheduleAtFixedRate(tracingDataConsumer, 0, 1000, TimeUnit.MILLISECONDS);
        }
        date = new java.util.Date();
    }

    /**
     * Removes and return MediationFlowDataEntry from the queue
     *
     * @return MediationFlowDataEntry instance
     * @throws Exception
     */
    public static MessageFlowDataEntry deQueue() throws Exception {
        return tracingDataCollector.deQueue();
    }

    /**
     * Checks whether the queue is empty
     *
     * @return Tru if empty/false otherwise
     */
    public static boolean isEmpty() {
        return tracingDataCollector.isEmpty();
    }

    /**
     * Collect message flow information from mediation and store it as a MessageFlowTraceEntry instance in the queue
     *
     * @param synCtx Synapse Message Context
     */
    private static void addFlowInfoEntry(MessageContext synCtx) {
        java.util.Date date = new java.util.Date();
        List<String> messageFlowTrace = (List<String>) synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW);

        for (String flow : messageFlowTrace) {
            if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) != null) {
                tracingDataCollector.enQueue(new MessageFlowTraceEntry(synCtx.getProperty
                        (MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(), flow, synCtx.getProperty
                        (MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE).toString(), dateFormatter.format(new Date()), synCtx.getEnvironment()));
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Message Flow ID is null, Flow Entry Data will not be updated for " + synCtx.getMessageID
                            ());
                }
            }
        }
    }

    /**
     * Collect component information from mediation and store it as a MessageFlowComponentEntry instance in the queue
     *
     * @param synCtx        Synapse Message Context
     * @param componentId   Unique ID for the Component/Mediator
     * @param componentName Name od the component/Mediator
     * @param start         True if it is a start of the flow for the particular component/mediator, False otherwise
     */
    private static void addComponentInfoEntry(MessageContext synCtx, String componentId, String componentName,
                                              boolean start) {
        Set<String> propertySet = synCtx.getPropertyKeySet();
        Map<String, Object> propertyMap = new HashMap<>();
        Map<String, Object> transportPropertyMap = new HashMap<>();


        for (String property : propertySet) {
            Object propertyValue = synCtx.getProperty(property);
            propertyMap.put(property, propertyValue);
        }

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

        String payload = synCtx.getMessageString();
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) != null) {
            tracingDataCollector.enQueue(new MessageFlowComponentEntry(synCtx.getProperty
                    (MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(), componentId, componentName, synCtx
                    .isResponse(), start, dateFormatter.format(new Date()), propertyMap,
                                                                       transportPropertyMap, payload, synCtx
                    .getEnvironment()));
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Message Flow ID is null, Component Entry Data will not be updated for " + synCtx.getMessageID
                        ());
            }
        }
    }

    /**
     * Update mediation tracing information
     *
     * @param msgCtx       Synapse message context
     * @param componentId  Component/Mediator ID
     * @param mediatorName Component/Mediator Name
     * @param isStart      True if it is a start of the flow for the particular component/mediator, False otherwise
     * @return String ID for the mediator/component generated
     */
    public static String setTraceFlowEvent(MessageContext msgCtx, String componentId, String mediatorName,
                                           boolean isStart) {
        String updatedComponentId = null;
        if (MessageFlowTracingDataCollector.isMessageFlowTracingEnabled(msgCtx)) {
            if (isStart) {
                MessageFlowComponentId messageFlowComponentId = (MessageFlowComponentId) (msgCtx.getProperty
                        (MessageFlowTracerConstants.MESSAGE_FLOW_INCREMENT_ID));
                updatedComponentId = messageFlowComponentId.getUpdatedId();
                addComponentInfoEntry(msgCtx, updatedComponentId, mediatorName, true);
                addComponentToMessageFlow(updatedComponentId, msgCtx);
                addFlowInfoEntry(msgCtx);
            } else {
                addComponentInfoEntry(msgCtx, componentId, mediatorName, false);
            }
        }
        return updatedComponentId;
    }

    /**
     * Set properties of the entry point for the mediation flow such as API, Proxy, Sequence etc.
     *
     * @param synCtx    Synapse Message Context
     * @param entryType Type of the entry (API/Proxy etc)
     * @param messageID Unique ID for the message
     */
    public static void setEntryPoint(MessageContext synCtx, String entryType, String messageID) {
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) == null) {
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID, messageID);
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE, entryType);
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_INCREMENT_ID,
                               new MessageFlowComponentId(MessageFlowTracerConstants.INITIAL_FLOW_INCREMENT_ID));
            synCtx.setMessageFlowTracingState(SynapseConstants.TRACING_ON);
        }
    }

    /**
     * States whether message flow tracing is enabled
     *
     * @return True if enabled, False otherwise
     */
    public static boolean isMessageFlowTracingEnabled() {
        return isMessageFlowTracingEnabled;
    }

    /**
     * States whether message flow tracing is enabled in given message context
     *
     * @param msgCtx Message Context to validate
     * @return True if enabled, False otherwise
     */
    public static boolean isMessageFlowTracingEnabled(MessageContext msgCtx) {
        return (msgCtx.getMessageFlowTracingState() == SynapseConstants.TRACING_ON);
    }

    /**
     * Adds new component to the existing component list in message context.
     * @param componentId Component/Mediator ID
     * @param msgCtx Synapse Message Context
     */
    private static void addComponentToMessageFlow(String componentId, MessageContext msgCtx) {
        if (msgCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW) != null) {
            List<String> messageFlowTrace = (List<String>) msgCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW);
            List<String> newMessageFlow = new ArrayList<String>();
            for (String flowTrace : messageFlowTrace) {
                newMessageFlow.add(flowTrace + componentId + MessageFlowTracerConstants.FLOW_PATH_SEPARATOR);
            }
            msgCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW, newMessageFlow);
        } else {
            List<String> messageFlowTrace = new ArrayList<String>();
            messageFlowTrace.add(componentId + MessageFlowTracerConstants.FLOW_PATH_SEPARATOR);
            msgCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW, messageFlowTrace);
        }
    }

    /**
     * Stop data consumer
     */
    public static void stopConsumer() {
        tracingDataConsumer.setStopped(true);
    }
}

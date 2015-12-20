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

import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.messageflowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.messageflowtracer.data.MessageFlowDataEntry;
import org.apache.synapse.messageflowtracer.data.MessageFlowTraceEntry;
import org.apache.synapse.messageflowtracer.util.MessageFlowTracerConstants;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Tracing Data Collector class which has a blocking queue to store collected data temporarily until been analyzed
 */
public class MessageFlowTracingDataCollector {


    private static int queueSize;
    private static boolean isMessageFlowTracingEnabled;
    private static Date date;
    private static MessageDataCollector tracingDataCollector;

    public static void init() {
        isMessageFlowTracingEnabled = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue
                (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_ENABLED, MessageFlowTracerConstants
                        .DEFAULT_TRACE_ENABLED));
        if (isMessageFlowTracingEnabled()) {
            queueSize = Integer.parseInt(SynapsePropertiesLoader.getPropertyValue
                    (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_QUEUE_SIZE, MessageFlowTracerConstants.DEFAULT_QUEUE_SIZE));

            tracingDataCollector = new MessageDataCollector(queueSize);
            //Thread to consume queue and update data structures for publishing
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName("Mediation Tracing Data consumer Task");
                            return t;
                        }
                    });
            executor.scheduleAtFixedRate(new MessageFlowTracingDataConsumer(), 0, 1000, TimeUnit.MILLISECONDS);
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
     * Collect message flow information from mediation and store it as a MessageFlowTraceEntry instance in the queue
     *
     * @param synCtx Synapse Message Context
     */
    private static void addFlowInfoEntry(MessageContext synCtx) {
        java.util.Date date = new java.util.Date();
        List<String> messageFlowTrace = (List<String>) synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW);

        for (String flow : messageFlowTrace) {
            tracingDataCollector.enQueue(new MessageFlowTraceEntry(synCtx.getProperty(MessageFlowTracerConstants
                                                                                              .MESSAGE_FLOW_ID)
                                                                           .toString(),
                                                                   flow, synCtx.getProperty(MessageFlowTracerConstants
                                                                                                    .MESSAGE_FLOW_ENTRY_TYPE).toString(),
                                                                   new Timestamp(date.getTime()).toString(), synCtx.getEnvironment()));
        }
    }

    /**
     * Collect component information from mediation and store it as a MessageFlowComponentEntry instance in the queue
     * @param synCtx Synapse Message Context
     * @param componentId Unique ID for the Component/Mediator
     * @param componentName Name od the component/Mediator
     * @param start True if it is a start of the flow for the particular component/mediator, False otherwise
     */
    private static void addComponentInfoEntry(MessageContext synCtx, String componentId, String componentName,
                                              boolean start) {
        Set<String> propertySet = synCtx.getPropertyKeySet();
        Map<String,String> propertyMap = new HashMap<String, String>();

        for (String property : propertySet) {
            Object propertyValue = synCtx.getProperty(property);
            if (propertyValue instanceof String) {
                propertyMap.put(property, (String)propertyValue);
            }
        }

        String payload = synCtx.getMessageString();
        tracingDataCollector.enQueue(new MessageFlowComponentEntry(synCtx.getProperty(MessageFlowTracerConstants
                                                                                              .MESSAGE_FLOW_ID)
                                                                           .toString(), componentId, componentName, synCtx.isResponse(),
                                                                   start, new Timestamp(date.getTime()).toString(), propertyMap,
                                                                   payload, synCtx.getEnvironment()));
    }

    /**
     * Update mediation tracing information
     * @param msgCtx Synapse message context
     * @param componentId Component/Mediator ID
     * @param mediatorName Component/Mediator Name
     * @param isStart True if it is a start of the flow for the particular component/mediator, False otherwise
     * @return String ID for the mediator/component generated
     */
    public static String setTraceFlowEvent(MessageContext msgCtx, String componentId, String mediatorName,
                                                      boolean isStart) {
        String newComponentId = MessageFlowTracerConstants.DEFAULT_COMPONENT_ID;
        if (isStart) {
            newComponentId = UUID.randomUUID().toString();
            addComponentInfoEntry(msgCtx, newComponentId, mediatorName, true);
            addComponentToMessageFlow(newComponentId, msgCtx);
            addFlowInfoEntry(msgCtx);
        } else {
            addComponentInfoEntry(msgCtx, componentId, mediatorName, false);
        }
        return newComponentId;
    }

    /**
     * Set properties of the entry point for the mediation flow such as API, Proxy, Sequence etc.
     * @param synCtx Synapse Message Context
     * @param entryType Type of the entry (API/Proxy etc)
     * @param messageID Unique ID for the message
     */
    public static void setEntryPoint(MessageContext synCtx, String entryType, String messageID){
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) == null) {
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID, messageID);
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE, entryType);
        }
    }

    /**
     * States whether message flow tracing is enabled
     * @return True if enabled, False otherwise
     */
    public static boolean isMessageFlowTracingEnabled(){
        return isMessageFlowTracingEnabled;
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
                newMessageFlow.add(flowTrace + componentId + " -> ");
            }
            msgCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW, newMessageFlow);
        } else {
            List<String> messageFlowTrace = new ArrayList<String>();
            messageFlowTrace.add(componentId + " -> ");
            msgCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW, messageFlowTrace);
        }
    }

}

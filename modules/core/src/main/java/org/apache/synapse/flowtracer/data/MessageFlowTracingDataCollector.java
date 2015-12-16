package org.apache.synapse.flowtracer.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.flowtracer.MessageFlowTracerConstants;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class MessageFlowTracingDataCollector {

    private static Log log = LogFactory.getLog(MessageFlowTracingDataCollector.class);
    private static BlockingQueue<MessageFlowDataEntry> queue;
    private static int defaultQueueSize;
    private static boolean isMessageFlowTracingEnabled;
    private static Date date;

    public static void init() {
        defaultQueueSize = Integer.parseInt(SynapsePropertiesLoader.getPropertyValue
                (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_QUEUE_SIZE, MessageFlowTracerConstants.DEFAULT_QUEUE_SIZE));
        isMessageFlowTracingEnabled = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue
                (MessageFlowTracerConstants.MESSAGE_FLOW_TRACE_ENABLED, MessageFlowTracerConstants
                        .DEFAULT_TRACE_ENABLED));
        queue = new ArrayBlockingQueue<MessageFlowDataEntry>(defaultQueueSize);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("Mediation Tracing " +
                                  "Data consumer Task");
                        return t;
                    }
                });
        executor.scheduleAtFixedRate(new MessageFlowTracingDataConsumer(), 0, 1000, TimeUnit.MILLISECONDS);
        date = new java.util.Date();
    }

    public static void enQueue(MessageFlowDataEntry mediationDataEntry){
            queue.add(mediationDataEntry);
    }

    public static MessageFlowDataEntry deQueue() throws Exception {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            String errorMsg = "Error consuming tracing data queue";
            throw new Exception(errorMsg, e);
        }
    }

    private static void addFlowInfoEntry(MessageContext synCtx) {
        java.util.Date date = new java.util.Date();
        List<String> messageFlowTrace = (List<String>) synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW);

        for (String flow : messageFlowTrace) {
            enQueue(new MessageFlowTraceEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID)
                                                      .toString(),
                                              flow, synCtx.getProperty(MessageFlowTracerConstants
                                                                               .MESSAGE_FLOW_ENTRY_TYPE).toString(),
                                              new Timestamp(date.getTime()).toString(), synCtx.getEnvironment()));
        }
    }

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
        enQueue(new MessageFlowComponentEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID)
                                                      .toString(), componentId, componentName, synCtx.isResponse(),
                                              start, new Timestamp(date.getTime()).toString(), propertyMap,
                                              payload, synCtx.getEnvironment()));
    }

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

    public static void setEntryPoint(MessageContext synCtx, String entryType, String messageID){
        if (synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID) == null) {
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID, messageID);
            synCtx.setProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE, entryType);
        }
    }

    public static boolean isMessageFlowTracingEnabled(){
        return isMessageFlowTracingEnabled;
    }

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

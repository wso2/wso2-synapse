package org.apache.synapse.flowtracer;

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MessageFlowDataHolder {

    private static List<MessageFlowComponentEntry> componentInfo = new ArrayList<MessageFlowComponentEntry>();
    private static List<MessageFlowTraceEntry> flowInfo = new ArrayList<MessageFlowTraceEntry>();

    private static boolean messageFlowTraceEnable = false;

    private static void addComponentInfoEntry(MessageContext synCtx, String componentId, String
            componentName, boolean start){
        java.util.Date date= new java.util.Date();
        Set<String> propertySet = synCtx.getPropertyKeySet();
        String propertyString = "";

        for(String property:propertySet){
            Object propertyValue = synCtx.getProperty(property);
            if(propertyValue instanceof String) {
                propertyString += property + "=" + propertyValue + ",";
            }
        }

        String payload = synCtx.getMessageString();
        componentInfo.add(new MessageFlowComponentEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(), componentId, componentName, synCtx.isResponse(), start, new Timestamp(date.getTime()).toString(), propertyString, payload));
    }

    public static synchronized MessageFlowComponentEntry getComponentInfoEntry() {
        if(componentInfo.size()>0){
            return componentInfo.remove(0);
        }
        return null;
    }

    private static void addFlowInfoEntry(MessageContext synCtx){
        java.util.Date date= new java.util.Date();
        List<String> messageFlowTrace = (List<String>) synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW);

        for(String flow:messageFlowTrace){
            flowInfo.add(new MessageFlowTraceEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(),flow,synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE).toString(),new Timestamp(date.getTime()).toString()));
        }
    }

    public static synchronized MessageFlowTraceEntry getFlowInfoEntry() {
        if(flowInfo.size()>0){
            return flowInfo.remove(0);
        }

        return null;
    }

    public static void setMessageFlowTraceEnable(boolean messageFlowTraceEnable) {
        MessageFlowDataHolder.messageFlowTraceEnable = messageFlowTraceEnable;
    }

    public static boolean isMessageFlowTraceEnable() {
        return messageFlowTraceEnable;
    }

    public static synchronized void setTraceFlowEvent(MessageContext msgCtx, String mediatorId, String mediatorName,
                                                       boolean isStart) {
        if (isStart) {
            MessageFlowDataHolder.addComponentInfoEntry(msgCtx, mediatorId, mediatorName, true);
            msgCtx.addComponentToMessageFlow(mediatorId);
            MessageFlowDataHolder.addFlowInfoEntry(msgCtx);
        } else {
            MessageFlowDataHolder.addComponentInfoEntry(msgCtx, mediatorId, mediatorName, false);
        }
    }
}

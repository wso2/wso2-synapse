package org.apache.synapse.flowtracer;

import org.apache.synapse.MessageContext;
import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Set;

public class MessageFlowDataHolder {

    private static ArrayList<MessageFlowComponentEntry> componentInfo = new ArrayList<>();
    private static ArrayList<MessageFlowTraceEntry> flowInfo = new ArrayList<>();

    private static boolean messageFlowTraceEnable = false;

    public static synchronized void addComponentInfoEntry(MessageContext synCtx, String componentId, String componentName, boolean start){
        java.util.Date date= new java.util.Date();

        Set<String> propertySet = synCtx.getPropertyKeySet();

        String propertyString = "";

        for(String property:propertySet){

            propertyString+=property+"="+synCtx.getProperty(property)+",";

        }

        componentInfo.add(new MessageFlowComponentEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(), componentId, componentName, synCtx.isResponse(), start, new Timestamp(date.getTime()).toString(), propertyString, synCtx.getEnvelope().toString()));
    }

    public static synchronized MessageFlowComponentEntry getComponentInfoEntry() {
        if(componentInfo.size()>0){
            return componentInfo.remove(0);
        }

        return null;
    }

    public static synchronized void addFlowInfoEntry(MessageContext synCtx){
        java.util.Date date= new java.util.Date();

        flowInfo.add(new MessageFlowTraceEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(),synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW).toString(),synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE).toString(),new Timestamp(date.getTime()).toString()));
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
}

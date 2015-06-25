package org.apache.synapse.flowtracer;

import org.apache.synapse.MessageContext;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Set;

public class MessageFlowDataHolder {

    private static ArrayList<MessageFlowEntry> flowInfo = new ArrayList<>();

    public static synchronized void addEntry(MessageContext synCtx, String componentId, String componentName, boolean start){
        java.util.Date date= new java.util.Date();

        Set<String> propertySet = synCtx.getPropertyKeySet();

        String propertyString = "";

        for(String property:propertySet){

            propertyString+=property+"="+synCtx.getProperty(property)+",";

        }

        flowInfo.add(new MessageFlowEntry(synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString(),componentId,componentName,synCtx.isResponse(),start,new Timestamp(date.getTime()).toString(),propertyString,synCtx.getEnvelope().toString()));
    }

    public static synchronized MessageFlowEntry getEntry() {
        if(flowInfo.size()>0){
            return flowInfo.remove(0);
        }

        return null;
    }
}

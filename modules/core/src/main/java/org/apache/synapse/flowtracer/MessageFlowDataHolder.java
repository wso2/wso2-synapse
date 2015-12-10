package org.apache.synapse.flowtracer;

import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MessageFlowDataHolder {

    private static Map<String, List<MessageFlowComponentEntry>> componentInfo = new HashMap<String,
            List<MessageFlowComponentEntry>>();
    private static Map<String, List<MessageFlowTraceEntry>> flowInfo = new HashMap<String,
            List<MessageFlowTraceEntry>>();

    private static boolean messageFlowTraceEnable = false;

    public static void setMessageFlowTraceEnable(boolean messageFlowTraceEnable) {
        MessageFlowDataHolder.messageFlowTraceEnable = messageFlowTraceEnable;
    }

    public static boolean isMessageFlowTraceEnable() {
        return messageFlowTraceEnable;
    }

    public static void addFlowInfoEntry(MessageFlowTraceEntry messageFlowTraceEntry) {
        List<MessageFlowTraceEntry> traceList = flowInfo.get(messageFlowTraceEntry.getMessageId());
        if (traceList == null) {
            traceList = new ArrayList<MessageFlowTraceEntry>();
            flowInfo.put(messageFlowTraceEntry.getMessageId(), traceList);
        }
        traceList.add(messageFlowTraceEntry);
    }

    public static void addComponentInfoEntry(MessageFlowComponentEntry messageFlowComponentEntry) {
        List<MessageFlowComponentEntry> componentList = componentInfo.get(messageFlowComponentEntry.getMessageId());
        if (componentList == null) {
            componentList = new ArrayList<MessageFlowComponentEntry>();
            componentInfo.put(messageFlowComponentEntry.getMessageId(), componentList);
        }
        componentList.add(messageFlowComponentEntry);
        componentInfo.put(messageFlowComponentEntry.getMessageId(), componentList);
    }

    public static Map<String, List<MessageFlowTraceEntry>> getMessageFlows() {
        return flowInfo;
    }


    public static String[] getMessageFlowTrace(String messageId) {
        List<MessageFlowTraceEntry> traceList = flowInfo.get(messageId);

        if (traceList != null && traceList.size() > 0) {
            int position = 0;
            String[] messageFlows = new String[traceList.size()];
            for (MessageFlowTraceEntry messageFlowTraceEntry : traceList) {
                messageFlows[position] = messageFlowTraceEntry.getMessageFlow();
                position++;
            }
            return messageFlows;
        }
        return null;
    }

    public static MessageFlowComponentEntry[] getComponentInfo(String messageId) {
        List<MessageFlowComponentEntry> componentList = componentInfo.get(messageId);
        return componentList.toArray(new MessageFlowComponentEntry[componentList.size()]);
    }

    public static void clearDataStores() {
        componentInfo.clear();
        flowInfo.clear();
    }
}

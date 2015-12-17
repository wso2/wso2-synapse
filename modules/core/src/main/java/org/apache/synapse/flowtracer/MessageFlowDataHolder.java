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
package org.apache.synapse.flowtracer;

import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data repository for message flow tracing which are ready to be published
 */
public class MessageFlowDataHolder {

    private static Map<String, List<MessageFlowComponentEntry>> componentInfo = new HashMap<String,
            List<MessageFlowComponentEntry>>();
    private static Map<String, List<MessageFlowTraceEntry>> flowInfo = new HashMap<String,
            List<MessageFlowTraceEntry>>();

    /**
     * Adds MessageFlowTraceEntry to the store
     * @param messageFlowTraceEntry
     */
    public void addFlowInfoEntry(MessageFlowTraceEntry messageFlowTraceEntry) {
        List<MessageFlowTraceEntry> traceList = flowInfo.get(messageFlowTraceEntry.getMessageId());
        if (traceList == null) {
            traceList = new ArrayList<MessageFlowTraceEntry>();
            flowInfo.put(messageFlowTraceEntry.getMessageId(), traceList);
        }
        traceList.add(messageFlowTraceEntry);
    }

    /**
     * Adds MessageFlowComponentEntry to the store
     * @param messageFlowComponentEntry
     */
    public void addComponentInfoEntry(MessageFlowComponentEntry messageFlowComponentEntry) {
        List<MessageFlowComponentEntry> componentList = componentInfo.get(messageFlowComponentEntry.getMessageId());
        if (componentList == null) {
            componentList = new ArrayList<MessageFlowComponentEntry>();
            componentInfo.put(messageFlowComponentEntry.getMessageId(), componentList);
        }
        componentList.add(messageFlowComponentEntry);
        componentInfo.put(messageFlowComponentEntry.getMessageId(), componentList);
    }

    /**
     * Returns all message flow entries available in the store
     * @return Map of strings containing message flow entries
     */
    public Map<String, List<MessageFlowTraceEntry>> getMessageFlows() {
        return flowInfo;
    }

    /**
     * Get list of MessageFlowTraceEntry for a particular message ID
     * @param messageId Message ID for the data to be received
     * @return String array containing MessageFlowTraceEntry instances
     */
    public String[] getMessageFlowTrace(String messageId) {
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

    /**
     * Get list of MessageFlowComponentEntry for a particular message ID
     * @param messageId Message ID for the data to be received
     * @return Array containing MessageFlowComponentEntry instances
     */
    public MessageFlowComponentEntry[] getComponentInfo(String messageId) {
        List<MessageFlowComponentEntry> componentList = componentInfo.get(messageId);
        return componentList.toArray(new MessageFlowComponentEntry[componentList.size()]);
    }

    /**
     * Clears all data stores
     */
    public void clearDataStores() {
        componentInfo.clear();
        flowInfo.clear();
    }
}

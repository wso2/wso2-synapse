/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.flowtracer;

import org.apache.synapse.MessageContext;
import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MessageFlowDataHolder {

    private static List<MessageFlowComponentEntry> componentInfo = new ArrayList<>();
    private static List<MessageFlowTraceEntry> flowInfo = new ArrayList<>();

    private static boolean messageFlowTraceEnable = false;

    public static synchronized void addComponentInfoEntry(MessageContext synCtx, String componentId, String componentName, boolean start){
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

    public static synchronized void addFlowInfoEntry(MessageContext synCtx){
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
}

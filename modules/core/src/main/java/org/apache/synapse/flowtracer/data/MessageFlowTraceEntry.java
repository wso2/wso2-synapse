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
package org.apache.synapse.flowtracer.data;

import org.apache.synapse.core.SynapseEnvironment;

/**
 * This class represents message flow entry in mediation flow
 */
public class MessageFlowTraceEntry implements MessageFlowDataEntry {

    private String messageId;
    private String entryType;
    private String messageFlow;
    private String timeStamp;
    private SynapseEnvironment synapseEnvironment;

    public MessageFlowTraceEntry(String messageId, String entryType, String timeStamp, SynapseEnvironment
            synapseEnvironment) {
        this.messageId = messageId;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
        this.synapseEnvironment = synapseEnvironment;
    }

    public MessageFlowTraceEntry(String messageId, String messageFlow, String entryType, String timeStamp,
                                 SynapseEnvironment synapseEnvironment) {
        this.messageId = messageId;
        this.messageFlow = messageFlow;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
        this.synapseEnvironment = synapseEnvironment;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getMessageFlow() {
        return messageFlow;
    }

    /**
     * This adds current instance to the MessageFlowDataHolder data store for publishing
     */
    public void process(){
        this.synapseEnvironment.getMessageFlowDataHolder().addFlowInfoEntry(this);
    }
}

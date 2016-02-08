/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.messageflowtracer.data;

import org.apache.synapse.MessageContext;
import org.apache.synapse.messageflowtracer.util.MessageFlowTracerConstants;

import java.util.Date;

/**
 * This class represents state of each stage in mediation flow. It stores all the synapse properties and payload at
 * each collecting point
 */
public class MessageFlowTraceEntry implements MessageFlowDataEntry {

    private String messageId;
    private long timestamp;
    private String entryType;

    public MessageFlowTraceEntry(MessageContext synCtx) {
        this.messageId = synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ID).toString();
        this.timestamp = new Date().getTime();
        this.entryType = synCtx.getProperty(MessageFlowTracerConstants.MESSAGE_FLOW_ENTRY_TYPE).toString();
    }

    public String getEntryType() {
        return entryType;
    }

    public String getMessageId() {
        return messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }


    @Override
    public String toString() {
        return ", messageId='" + messageId + '\'' +
                ", timestamp='" + timestamp ;
    }
}

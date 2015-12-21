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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.messageflowtracer.data.MessageFlowDataEntry;

/**
 * Data consumer which process entries from the queue
 */
public class MessageFlowTracingDataConsumer implements Runnable {
    private static Log log = LogFactory.getLog(MessageFlowTracingDataConsumer.class);
    private boolean isStopped = false;

    public void run(){
        MessageFlowDataEntry mediationDataEntry;
        while (!isStopped && MessageFlowTracingDataCollector.isEmpty()) {
            try {
                mediationDataEntry = MessageFlowTracingDataCollector.deQueue();
                mediationDataEntry.process();
            } catch (Exception exception) {
                log.error("Error in Mediation Tracing data consumer while consuming data", exception);
            }
        }
    }

    public void setStopped(boolean isStopped) {
        this.isStopped = isStopped;
    }
}

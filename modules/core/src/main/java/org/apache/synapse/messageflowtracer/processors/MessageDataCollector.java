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

import org.apache.synapse.messageflowtracer.data.MessageFlowDataEntry;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * MessageDataCollector contains the non-blocking queue and utility methods to store and retrieve elements from the
 * queue.
 */
public class MessageDataCollector {

    private BlockingQueue<MessageFlowDataEntry> queue;

    public MessageDataCollector(int queueSize) {
        queue = new ArrayBlockingQueue<MessageFlowDataEntry>(queueSize);
    }

    /**
     * Add MediationFlowDataEntry instance to the queue
     *
     * @param mediationDataEntry MediationFlowDataEntry to be stored in the queue
     */
    public void enQueue(MessageFlowDataEntry mediationDataEntry) {
        queue.add(mediationDataEntry);
    }

    /**
     * Removes and return MediationFlowDataEntry from the queue
     *
     * @return MediationFlowDataEntry instance
     * @throws Exception
     */
    public MessageFlowDataEntry deQueue() throws Exception {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            String errorMsg = "Error consuming tracing data queue";
            throw new Exception(errorMsg, e);
        }
    }
}

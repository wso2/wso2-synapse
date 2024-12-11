/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.eip.aggregator;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.eip.EIPConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An instance of this class is created to manage each aggregation group, and it holds
 * the aggregation properties and the messages collected during aggregation. This class also
 * times out itself after the timeout expires it
 */
public class ForEachAggregate {

    private final String forLoopMediatorId;
    private final ReentrantLock lock = new ReentrantLock();
    private List<MessageContext> messages = new ArrayList<>();
    private boolean completed = false;
    private String correlation = null;

    public ForEachAggregate(String correlation, String forLoopMediatorId) {

        this.correlation = correlation;
        this.forLoopMediatorId = forLoopMediatorId;
    }

    /**
     * Add a message to the aggregate's message list
     *
     * @param synCtx message to be added into this aggregation group
     * @return true if the message was added or false if not
     */
    public synchronized boolean addMessage(MessageContext synCtx) {

        if (messages == null) {
            return false;
        }
        messages.add(synCtx);
        return true;
    }

    /**
     * Has this aggregation group completed?
     *
     * @param synLog the Synapse log to use
     * @return boolean true if aggregation is complete
     */
    public synchronized boolean isComplete(SynapseLog synLog) {

        if (!completed) {
            // if any messages have been collected, check if the completion criteria is met
            if (!messages.isEmpty()) {
                // get total messages for this group, from the first message we have collected
                MessageContext mc = messages.get(0);
                Object prop = mc.getProperty(EIPConstants.MESSAGE_SEQUENCE + "." + forLoopMediatorId);

                if (prop instanceof String) {
                    String[] msgSequence = prop.toString().split(
                            EIPConstants.MESSAGE_SEQUENCE_DELEMITER);
                    int total = Integer.parseInt(msgSequence[1]);

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug(messages.size() +
                                " messages of " + total + " collected in current foreach aggregation");
                    }
                    if (messages.size() >= total) {
                        synLog.traceOrDebug("Foreach iterations complete");
                        return true;
                    }
                }
            } else {
                synLog.traceOrDebug("No messages collected in current foreach aggregation");
            }
        } else {
            synLog.traceOrDebug(
                    "Foreach iteration already completed - this message will not be processed in aggregation");
        }
        return false;
    }

    public MessageContext getLastMessage() {

        return messages.get(messages.size() - 1);
    }

    public synchronized List<MessageContext> getMessages() {

        return new ArrayList<>(messages);
    }

    public void setMessages(List<MessageContext> messages) {

        this.messages = messages;
    }

    public String getCorrelation() {

        return correlation;
    }

    public void clear() {

        messages = null;
    }

    public synchronized boolean getLock() {

        return lock.tryLock();
    }

    public void releaseLock() {

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public boolean isCompleted() {

        return completed;
    }

    public void setCompleted(boolean completed) {

        this.completed = completed;
    }
}

/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.message.processor.impl.sampler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessor;
import org.apache.synapse.task.Task;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * This {@link Task} injects a message to a given sequence. It also supports
 * Throttling scenarios.
 */
public class SamplingService implements Task, ManagedLifecycle {
    private static Log log = LogFactory.getLog(SamplingService.class);

    // The consumer that is associated with the particular message store
    private MessageConsumer messageConsumer;

    // Owner of the this job
    private MessageProcessor messageProcessor;

    // Determines how many messages at a time it should execute
    private int concurrency = 1;

    // Represents the send sequence of a message
    private String sequence;

    private SynapseEnvironment synapseEnvironment;

    private boolean initialized = false;

    private final String concurrencyPropName;
    private final String sequencePropName;

    public SamplingService(MessageProcessor messageProcessor,
                           SynapseEnvironment synapseEnvironment, String concurrencyPropName,
                           String sequencePropName) {
        super();
        this.messageProcessor = messageProcessor;
        this.synapseEnvironment = synapseEnvironment;
        this.concurrencyPropName = concurrencyPropName;
        this.sequencePropName = sequencePropName;
    }

    /**
     * Starts the execution of this task which grabs a message from the message
     * queue and inject it to a given sequence.
     */
    public void execute() {

        try {
            /*
			 * Initialize only if it is NOT already done. This will make sure
			 * that the initialization is done only once.
			 */
            if (!initialized) {
                this.init(synapseEnvironment);
            }

            if (!this.messageProcessor.isDeactivated()) {
                for (int i = 0; i < concurrency; i++) {

                    final MessageContext messageContext = fetch(messageConsumer);

                    if (messageContext != null) {
                        dispatch(messageContext);
                    } else {
                        // either the connection is broken or there are no new
                        // massages.
                        if (log.isDebugEnabled()) {
                            log.debug("No messages were received for message processor [" +
                                    messageProcessor.getName() + "]");
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Exiting service since the message processor is deactivated");
                }
            }
        } catch (Throwable t) {
            // All the possible recoverable exceptions are handles case by case
            // and yet if it comes this
            // we have to shutdown the processor
            log.fatal("Deactivating the message processor [" + this.messageProcessor.getName() +
                    "]", t);

            this.messageProcessor.stop();
        }

        if (log.isDebugEnabled()) {
            log.debug("Exiting service thread of message processor [" +
                    this.messageProcessor.getName() + "]");
        }
    }


    public void init(SynapseEnvironment se) {
        // Setting up the JMS consumer here.
        setMessageConsumer();

        Map<String, Object> parameterMap = messageProcessor.getParameters();
        sequence = (String) parameterMap.get(sequencePropName);
        String conc = (String) parameterMap.get(concurrencyPropName);
        if (conc != null) {

            try {
                concurrency = Integer.parseInt(conc);
            } catch (NumberFormatException e) {
                parameterMap.remove(concurrencyPropName);
                log.error("Invalid value for concurrency switching back to default value", e);
            }
        }

		/*
		 * Make sure to set the isInitialized flag too TRUE in order to avoid
		 * re-initialization.
		 */
        initialized = true;
    }

    /**
     * Receives the next message from the message store.
     *
     * @param msgConsumer message consumer
     * @return {@link MessageContext} of the last message received from the
     * store.
     */
    public MessageContext fetch(MessageConsumer msgConsumer) {
        MessageContext newMsg = messageConsumer.receive();
        if (newMsg != null) {
            messageConsumer.ack();
        }

        return newMsg;
    }

    /**
     * Sends the message to a given sequence.
     *
     * @param messageContext message to be injected.
     */
    public void dispatch(final MessageContext messageContext) {

        final ExecutorService executor = messageContext.getEnvironment().
                getExecutorService();
        executor.submit(new Runnable() {
            public void run() {
                try {
                    Mediator processingSequence = messageContext.getSequence(sequence);
                    if (processingSequence != null) {
                        processingSequence.mediate(messageContext);
                    }
                } catch (SynapseException syne) {
                    if (!messageContext.getFaultStack().isEmpty()) {
                        (messageContext.getFaultStack().pop()).handleFault(messageContext, syne);
                    }
                    log.error("Error occurred while executing the message", syne);
                } catch (Throwable t) {
                    log.error("Error occurred while executing the message", t);
                }
            }
        });
    }

    /**
     * Terminates the job of the message processor.
     *
     * @return <code>true</code> if the job is terminated successfully,
     * <code>false</code> otherwise.
     */
    public boolean terminate() {
        messageConsumer.cleanup();
        return true;
    }

    private boolean setMessageConsumer() {
        final String messageStore = messageProcessor.getMessageStoreName();
        messageConsumer =
                synapseEnvironment.getSynapseConfiguration()
                        .getMessageStore(messageStore).getConsumer();
		/*
		 * Make sure to set the same message consumer in the message processor
		 * since it is used by life-cycle management methods. Specially by the
		 * deactivate method to cleanup the connection before the deactivation.
		 */
        return messageProcessor.setMessageConsumer(messageConsumer);

    }

    /**
     * Checks whether this TaskService is properly initialized or not.
     *
     * @return <code>true</code> if this TaskService is properly initialized.
     * <code>false</code> otherwise.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public void destroy() {
        terminate();

    }
}

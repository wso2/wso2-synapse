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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessor;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.task.Task;

/**
 * This {@link Task} injects a message to a given sequence. It also supports
 * Throttling scenarios.
 *
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

    /*
     * Number of connection attempts to store before shutting down the processor.
     * -1 value to indicate that retry needs to happen forever.
     */
	private int maxConnectionAttemptsToStore = -1;

    /*
     * Time (in milliseconds) between two attempts to connect to the store.
     */
	private int storeConnectionAttemptDelay = 1000;

    /**
     * Specifies whether the service should be started as deactivated or not
     */
    private boolean isDeactivatedAtStartup = false;

	public SamplingService(MessageProcessor messageProcessor,
	                       SynapseEnvironment synapseEnvironment, String concurrencyPropName,
	                       String sequencePropName) {
		super();
		this.messageProcessor = messageProcessor;
		this.synapseEnvironment = synapseEnvironment;
		this.concurrencyPropName = concurrencyPropName;
		this.sequencePropName = sequencePropName;
	}

    public SamplingService(MessageProcessor messageProcessor,
                           SynapseEnvironment synapseEnvironment, String concurrencyPropName,
                           String sequencePropName, boolean isDeactivatedAtStartup) {
        super();
        this.messageProcessor = messageProcessor;
        this.synapseEnvironment = synapseEnvironment;
        this.concurrencyPropName = concurrencyPropName;
        this.sequencePropName = sequencePropName;
        this.isDeactivatedAtStartup = isDeactivatedAtStartup;
    }


    /**
	 * Starts the execution of this task which grabs a message from the message
	 * queue and inject it to a given sequence.
	 */
	public void execute() {

        if(isDeactivatedAtStartup){
            //This delay is required until tasks are paused from ScheduledMessageProcessor since message processor is
            // inactive
            try {
                TimeUnit.MILLISECONDS.sleep(MessageProcessorConstants.INITIAL_EXECUTION_DELAY);
            } catch (InterruptedException exception) {
                log.warn("Initial delay interrupted when Sampling service started as inactive " , exception);
            }
            isDeactivatedAtStartup = false;
        }

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

					final MessageContext messageContext = fetch();

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
		String maxConnectionAttemptsToStore =
				(String) parameterMap.get(MessageProcessorConstants.MAX_STORE_CONNECT_ATTEMPTS);
		String storeConnectionAttemptDelay =
				(String) parameterMap.get(MessageProcessorConstants.STORE_CONNECTION_RETRY_INTERVAL);

		if (null != maxConnectionAttemptsToStore) {
			this.maxConnectionAttemptsToStore = Integer.parseInt(maxConnectionAttemptsToStore);
		}

		if (null != storeConnectionAttemptDelay) {
			this.storeConnectionAttemptDelay = Integer.parseInt(storeConnectionAttemptDelay);
		}
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
	 * @return {@link MessageContext} of the last message received from the
	 *         store.
	 */
	public MessageContext fetch() {
		MessageContext newMsg = null;
		for (int connAttempt = 0;
			 connAttempt < maxConnectionAttemptsToStore || maxConnectionAttemptsToStore == -1;
			 connAttempt++) {
			try {
				newMsg = messageConsumer.receive();
				if (newMsg != null) {
					messageConsumer.ack();
				}
				break;
			} catch (SynapseException synapseException) {
                                /*used message in the exception to keep Interface MessageConsumer unchanged.
                                 If it is a connection exception retry, otherwise throw  as it is
                                */
				if (synapseException.getMessage().contains(MessageProcessorConstants.STORE_CONNECTION_ERROR)) {
					try {
						//on last try to connect throw the exception
						if (connAttempt == maxConnectionAttemptsToStore - 1) {
							throw new SynapseException("Error while connecting to message store "
									+ messageProcessor.getName(), synapseException);
						}
						Thread.sleep(storeConnectionAttemptDelay);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
					}
				} else {
					throw new SynapseException("Error while fetching message from "
							+ messageProcessor.getName(), synapseException);
				}
			}
		}
		return newMsg;
	}

	/**
	 * Sends the message to a given sequence.
	 * 
	 * @param messageContext
	 *            message to be injected.
	 */
    public void dispatch(final MessageContext messageContext) {

		setSoapHeaderBlock(messageContext);

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
	 *         <code>false</code> otherwise.
	 */
	public boolean terminate() {
        if (messageConsumer != null) {
            messageConsumer.cleanup();
        }
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
	 *         <code>false</code> otherwise.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	public void destroy() {
		terminate();

	}

	private void setSoapHeaderBlock(MessageContext synCtx) {
		// Send the SOAP Header Blocks to support WS-Addressing
		if (synCtx.getEnvelope().getHeader() != null) {
			Iterator iHeader = synCtx.getEnvelope().getHeader().getChildren();
			SOAPFactory fac;
			if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(synCtx.getEnvelope().getBody()
					.getNamespace().getNamespaceURI())) {
				fac = OMAbstractFactory.getSOAP11Factory();
			} else {
				fac = OMAbstractFactory.getSOAP12Factory();
			}
			List<OMNode> newHeaderNodes = new ArrayList<OMNode>();
			while (iHeader.hasNext()) {
				try {
					Object element = iHeader.next();
					if (!(element instanceof SOAPHeaderBlock)) {
						if (element instanceof OMElement) {
							newHeaderNodes.add(ElementHelper.toSOAPHeaderBlock((OMElement) element, fac));
						}
						iHeader.remove();
					}
				} catch (OMException e) {
					log.error("Unable to convert to SoapHeader Block", e);
				} catch (Exception e) {
					log.error("Unable to convert to SoapHeader Block", e);
				}
			}
			for (OMNode newHeaderNode : newHeaderNodes) {
				synCtx.getEnvelope().getHeader().addChild(newHeaderNode);
			}
		}
	}
}

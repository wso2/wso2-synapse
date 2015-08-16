/**
 *  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.message.processor.impl.failover;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.processor.MessageProcessor;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.task.Task;
import org.apache.synapse.util.MessageHelper;

/**
 * This task is responsible for forwarding a request to a given message store.
 */
public class FailoverForwardingService implements Task, ManagedLifecycle {

	private static final Log log = LogFactory.getLog(FailoverForwardingService.class);

	// The consumer that is associated with the particular message store
	private MessageConsumer messageConsumer;

	// Target message store producer
	private MessageProducer targetMessageProducer;

	// Target message store name
	private String targetMessageStoreName;

	// Owner of the this job
	private MessageProcessor messageProcessor;

    /*
	 * Interval between two retries to the client. This only come to affect only
	 * if the client is un-reachable
	 */
	private int retryInterval = 1000;

	// Sequence to invoke in a failure
	private String faultSeq = null;

	// Sequence to invoke in a message processor deactivation
	private String deactivateSeq = null;

	/*
	 * The cron expression under which the message processor runs.
	 */
	private String cronExpression = null;

	/*
	 * These two maintain the state of service. For each iteration these should
	 * be reset
	 */
	private boolean isSuccessful = false;
	private volatile boolean isTerminated = false;

	/*
	 * Number of retries before shutting-down the processor. -1 default value
	 * indicates that
	 * retry should happen forever
	 */
	private int maxDeliverAttempts = -1;
	private int attemptCount = 0;

    private boolean isThrottling = true;

    /**
     * Throttling-interval is the forwarding interval when cron scheduling is enabled.
     */
    private long throttlingInterval = -1;

	// Message Queue polling interval value.
	private long interval;

	/*
	 * Configuration to continue the message processor even without stopping
	 * the message processor after maximum number of delivery
	 */
	private boolean isMaxDeliveryAttemptDropEnabled = false;

	/**
	 * If false, the MessageProcessor will process every single message in the queue regardless of their origin
	 * If true, it will only process messages that were processed by a MessageStore running on the same server
	 * Default value is set to true
	 */
	private SynapseEnvironment synapseEnvironment;

	private boolean initialized = false;

    /**
     * Specifies whether the service should be started as deactivated or not
     */
    private boolean isDeactivatedAtStartup = false;

	public FailoverForwardingService(MessageProcessor messageProcessor,
	                                 SynapseEnvironment synapseEnvironment, long threshouldInterval) {
		this.messageProcessor = messageProcessor;
		this.synapseEnvironment = synapseEnvironment;
		// Initializes the interval to the Threshould interval value.
		this.interval = threshouldInterval;
	}

    public FailoverForwardingService(MessageProcessor messageProcessor,
                                     SynapseEnvironment synapseEnvironment, long threshouldInterval,
                                     boolean isDeactivatedAtStartup ) {
        this.messageProcessor = messageProcessor;
        this.synapseEnvironment = synapseEnvironment;
        this.interval = threshouldInterval;
        this.isDeactivatedAtStartup = isDeactivatedAtStartup;
    }

	/**
	 * Starts the execution of this task which grabs a message from the message
	 * queue and dispatch it to a given endpoint.
	 */
	public void execute() {
		final long startTime = new Date().getTime();

        if(isDeactivatedAtStartup){
            //This delay is required until tasks are paused from ScheduledMessageProcessor since message processor is
            // inactive
            try {
                TimeUnit.MILLISECONDS.sleep(MessageProcessorConstants.INITIAL_EXECUTION_DELAY);
            } catch (InterruptedException exception) {
                log.warn("Initial delay interrupted when Failover Forwarding service started as inactive ", exception);
            }
            isDeactivatedAtStartup = false;
        }

		/*
		 * Initialize only if it is NOT already done. This will make sure that
		 * the initialization is done only once.
		 */
		if (!initialized) {
			this.init(synapseEnvironment);
		}
		do {
			resetService();
			MessageContext messageContext = null;
			try {
				if (!this.messageProcessor.isDeactivated()) {
					messageContext = fetch(messageConsumer);
					if (messageContext != null) {
						// Now it is NOT terminated anymore.
						isTerminated = messageProcessor.isDeactivated();
						dispatch(messageContext);

					} else {
						// either the connection is broken or there are no new
						// massages.
						if (log.isDebugEnabled()) {
							log.debug("No messages were received for message processor [" +
							          messageProcessor.getName() + "]");
						}

						// this means we have consumed all the messages
						if (isRunningUnderCronExpression()) {
							break;
						}
					}
				} else {
					/*
					 * we need this because when start the server while the
					 * processors in deactivated mode
					 * the deactivation may not come in to play because the
					 * service may not be running.
					 */
					isTerminated = true;

					if (log.isDebugEnabled()) {
						log.debug("Exiting service since the message processor is deactivated");
					}
				}
			} catch (Throwable e) {
				/*
				 * All the possible recoverable exceptions are handles case by
				 * case and yet if it comes this
				 * we have to shutdown the processor
				 */
				log.fatal("Deactivating the message processor [" + this.messageProcessor.getName() +
				          "]", e);
				deactivateMessageProcessor(messageContext);
			}

			if (log.isDebugEnabled()) {
				log.debug("Exiting the iteration of message processor [" +
				          this.messageProcessor.getName() + "]");
			}
			/*
			 * This code wrote handle scenarios in which cron expressions are
			 * used for scheduling task
			 */
			if (isRunningUnderCronExpression()) {
				try {
					Thread.sleep(throttlingInterval);
				} catch (InterruptedException e) {
					// no need to worry. it does have any serious consequences
					log.debug("Current Thread was interrupted while it is sleeping.");
				}
			}
			/*
			 * If the interval is less than 1000 ms, then the scheduling is done
			 * using the while loop since ntask rejects any intervals whose
			 * value is less then 1000 ms.
			 */
			if (interval > 0 && interval < MessageProcessorConstants.THRESHOULD_INTERVAL) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					log.debug("Current Thread was interrupted while it is sleeping.");
				}
			}
			/*
			 * Gives the control back to Quartz scheduler. This needs to be done
			 * only if the interval value is less than the Threshould interval
			 * value of 1000 ms, where the scheduling is done outside of Quartz
			 * via the while loop. Otherwise the schedular will get blocked.
			 * For cron expressions this scenario is already
			 * handled above.
			 */
			if (isThrottling && new Date().getTime() - startTime > 1000) {
				break;
			}
		} while ((isThrottling || isRunningUnderCronExpression()) && !isTerminated);

		if (log.isDebugEnabled()) {
			log.debug("Exiting service thread of message processor [" +
			          this.messageProcessor.getName() + "]");
		}
	}

    /**
     * Helper method to get a value of a parameters in the AxisConfiguration
     *
     * @param axisConfiguration AxisConfiguration instance
     * @param paramKey The name / key of the parameter
     * @return The value of the parameter
     */
    private static String getAxis2ParameterValue(AxisConfiguration axisConfiguration,
                                                 String paramKey) {
        Parameter parameter = axisConfiguration.getParameter(paramKey);
        if (parameter == null) {
            return null;
        }
        Object value = parameter.getValue();
        if (value != null && value instanceof String) {
            return (String) parameter.getValue();
        } else {
            return null;
        }
    }

	public void init(SynapseEnvironment se) {
		// Setting up the JMS consumer/Producer here.
		setMessageConsumerAndProducer();

		// Defaults to -1.
		Map<String, Object> parametersMap = messageProcessor.getParameters();
		if (parametersMap.get(MessageProcessorConstants.MAX_DELIVER_ATTEMPTS) != null) {
			maxDeliverAttempts =
			                     Integer.parseInt((String) parametersMap.get(MessageProcessorConstants.MAX_DELIVER_ATTEMPTS));
		}

		if (parametersMap.get(MessageProcessorConstants.RETRY_INTERVAL) != null) {
			retryInterval =
			                Integer.parseInt((String) parametersMap.get(MessageProcessorConstants.RETRY_INTERVAL));
		}

		faultSeq = (String) parametersMap.get(FailoverForwardingProcessorConstants.FAULT_SEQUENCE);


		deactivateSeq = (String) parametersMap.get(FailoverForwardingProcessorConstants.DEACTIVATE_SEQUENCE);

		// Default value should be true.
		if (parametersMap.get(FailoverForwardingProcessorConstants.THROTTLE) != null) {
			isThrottling =
			               Boolean.parseBoolean((String) parametersMap.get(FailoverForwardingProcessorConstants.THROTTLE));
		}

		if (parametersMap.get(FailoverForwardingProcessorConstants.CRON_EXPRESSION) != null) {
			cronExpression =
			                 String.valueOf(parametersMap.get(FailoverForwardingProcessorConstants.CRON_EXPRESSION));
		}

		// Default Value should be -1.
		if (cronExpression != null &&
		    parametersMap.get(FailoverForwardingProcessorConstants.THROTTLE_INTERVAL) != null) {
			throttlingInterval =
			                     Long.parseLong((String) parametersMap.get(FailoverForwardingProcessorConstants
					                                                               .THROTTLE_INTERVAL));
		}

		// Default to FALSE.
		if (parametersMap.get(FailoverForwardingProcessorConstants.MAX_DELIVERY_DROP) != null &&
		    parametersMap.get(FailoverForwardingProcessorConstants.MAX_DELIVERY_DROP).toString()
		                 .equals("Enabled") && maxDeliverAttempts > 0) {
			isMaxDeliveryAttemptDropEnabled = true;
		}

		// Setting the interval value.
		interval = Long.parseLong((String) parametersMap.get(MessageProcessorConstants.INTERVAL));

		/*
		 * Make sure to set the isInitialized flag to TRUE in order to avoid
		 * re-initialization.
		 */
		initialized = true;
	}


	/**
	 * Receives the next message from the message store.
	 *
	 * @param msgConsumer
	 *            message consumer
	 * @return {@link MessageContext} of the last message received from the
	 *         store.
	 */
	public MessageContext fetch(MessageConsumer msgConsumer) {
		return messageConsumer.receive();
	}

	/**
	 * Sends the message to a given message store.
	 *
	 * @param messageContext
	 *            synapse {@link MessageContext} to be sent
	 */
	public void dispatch(MessageContext messageContext) {
		if (log.isDebugEnabled()) {
			log.debug("Sending the message to client with message processor [" +
			          messageProcessor.getName() + "]");
		}


		SOAPEnvelope originalEnvelop = messageContext.getEnvelope();

		if (targetMessageStoreName != null) {

			try {
				// Send message to the client
				while (!isSuccessful && !isTerminated) {
					try {
						// For each retry we need to have a fresh copy of the
						// actual message. otherwise retry may not
						// work as expected.
						messageContext.setEnvelope(MessageHelper.cloneSOAPEnvelope(originalEnvelop));
						OMElement firstChild = null; //
						org.apache.axis2.context.MessageContext origAxis2Ctx = ((Axis2MessageContext) messageContext)
								.getAxis2MessageContext();
						if (JsonUtil.hasAJsonPayload(origAxis2Ctx)) {
							firstChild = origAxis2Ctx.getEnvelope().getBody().getFirstElement();
						} // Had to do this because
						  // MessageHelper#cloneSOAPEnvelope does not clone
						  // OMSourcedElemImpl correctly.
						if (JsonUtil.hasAJsonPayload(firstChild)) { //
							OMElement clonedFirstElement =
							                               messageContext.getEnvelope().getBody()
							                                             .getFirstElement();
							if (clonedFirstElement != null) {
								clonedFirstElement.detach();
								messageContext.getEnvelope().getBody().addChild(firstChild);
							}
						}

						if (messageConsumer != null && messageConsumer.isAlive() && targetMessageStoreName != null) {

							targetMessageProducer = synapseEnvironment.getSynapseConfiguration().getMessageStore
									(targetMessageStoreName).getProducer();
							if(targetMessageProducer != null) {
								isSuccessful = targetMessageProducer.storeMessage(messageContext);
							} else {
								isSuccessful = false;
							}
						}

					} catch (Exception e) {

						log.error("Message store messageSender of message processor [" +
						          this.messageProcessor.getName() +
						          "] failed to send message to the target message store");

						sendThroughFaultSeq(messageContext);
					}

					if (isSuccessful) {

						messageConsumer.ack();
						attemptCount = 0;

						if (log.isDebugEnabled()) {
							log.debug("Successfully sent the message to message store [" +
							          targetMessageStoreName + "]" + " with message processor [" +
							          messageProcessor.getName() + "]");
						}

						if (messageProcessor.isPaused()) {
							this.messageProcessor.resumeService();
							log.info("Resuming the service of message processor [" +
							         messageProcessor.getName() + "]");
						}

					} else {

						// Then we have to retry sending the message to the target
						// store.
						prepareToRetry(messageContext);

					}

				}

			} catch (Exception e) {
				log.error("Message processor [" + messageProcessor.getName() +
				          "] failed to send the message to target store" , e);
			}
		} else {
			/*
			 * No Target message store defined for the Message So we do not have a
			 * place to deliver.
			 * Here we log a warning and remove the message
			 * this by implementing a target inferring
			 * mechanism.
			 */

			log.warn("Property " + FailoverForwardingProcessorConstants.TARGET_MESSAGE_STORE +
			         " not found in the message context , Hence removing the message ");
			messageConsumer.ack();
		}
		return;
	}

	/**
	 * Sending the out message through the fault sequence.
	 *
	 * @param msgCtx
	 *            Synapse {@link MessageContext} to be sent through the fault
	 *            sequence.
	 */
	public void sendThroughFaultSeq(MessageContext msgCtx) {
		if (faultSeq == null) {
			log.warn("Failed to send the message through the fault sequence. Sequence name does not Exist.");
			return;
		}
		Mediator mediator = msgCtx.getSequence(faultSeq);

		if (mediator == null) {
			log.warn("Failed to send the message through the fault sequence. Sequence [" +
			         faultSeq + "] does not Exist.");
			return;
		}

		mediator.mediate(msgCtx);
	}

	/**
	 * Sending the out message through the deactivate sequence.
	 *
	 * @param msgCtx Synapse {@link MessageContext} to be sent through the deactivate
	 *               sequence.
	 */
	public void sendThroughDeactivateSeq(MessageContext msgCtx) {
		if (deactivateSeq == null) {
			log.warn("Failed to send the message through the deactivate sequence. Sequence name does not Exist.");
			return;
		}
		Mediator mediator = msgCtx.getSequence(deactivateSeq);

		if (mediator == null) {
			log.warn("Failed to send the message through the deactivate sequence. Sequence [" +
			         deactivateSeq + "] does not Exist.");
			return;
		}

		mediator.mediate(msgCtx);
	}

	/**
	 * Terminates the job of the message processor.
	 *
	 * @return <code>true</code> if the job is terminated successfully,
	 *         <code>false</code> otherwise.
	 */
	public boolean terminate() {
		try {
			isTerminated = true;
			// Thread.currentThread().interrupt();

			if (log.isDebugEnabled()) {
				log.debug("Successfully terminated job of message processor [" +
				          messageProcessor.getName() + "]");
			}
			return true;
		} catch (Exception e) {
			log.error("Failed to terminate the job of message processor [" +
			          messageProcessor.getName() + "]");
			return false;
		}
	}

	/*
	 * If the max delivery attempt is reached, this will deactivate the message
	 * processor. If the MaxDeliveryAttemptDrop is Enabled, then the message is
	 * dropped and the message processor continues.
	 */
	private void checkAndDeactivateProcessor(MessageContext msgCtx) {
		if (maxDeliverAttempts > 0) {
            this.attemptCount++;
            if (attemptCount >= maxDeliverAttempts) {

                if (this.isMaxDeliveryAttemptDropEnabled) {
                    dropMessageAndContinueMessageProcessor();
                    if (log.isDebugEnabled()) {
                        log.debug("Message processor [" + messageProcessor.getName() +
                                "] Dropped the failed message and continue due to reach of max attempts");
                    }
                } else {
	                terminate();
	                deactivateMessageProcessor(msgCtx);

                    if (log.isDebugEnabled()) {
                        log.debug("Message processor [" + messageProcessor.getName() +
                                "] stopped due to reach of max attempts");
                    }
                }
            }
        }
    }


	/*
	 * Prepares the message processor for the next retry of delivery.
	 */
	private void prepareToRetry(MessageContext msgCtx) {
		if (!isTerminated) {
			checkAndDeactivateProcessor(msgCtx);

			if (log.isDebugEnabled()) {
				log.debug("Failed to send to target store retrying after " + retryInterval +
				          "s with attempt count - " + attemptCount);
			}

			try {
				// wait for some time before retrying
				Thread.sleep(retryInterval);
			} catch (InterruptedException ignore) {
				// No harm even it gets interrupted. So nothing to handle.
			}
		}
	}

	private void deactivateMessageProcessor(MessageContext messageContext) {
		sendThroughDeactivateSeq(messageContext);
		this.messageProcessor.deactivate();
	}

    private void resetService() {
        isSuccessful = false;
        attemptCount = 0;
    }

	private boolean isRunningUnderCronExpression() {
		return (cronExpression != null) && (throttlingInterval > -1);
	}

	private void dropMessageAndContinueMessageProcessor() {
		messageConsumer.ack();
		attemptCount = 0;
		isSuccessful = true;
		if (this.messageProcessor.isPaused()) {
			this.messageProcessor.resumeService();
		}
		log.info("Removed failed message and continue the message processor [" +
		         this.messageProcessor.getName() + "]");
	}

	private boolean setMessageConsumerAndProducer() {
		final String messageStoreName = messageProcessor.getMessageStoreName();
		MessageStore messageStore = synapseEnvironment.getSynapseConfiguration().getMessageStore(messageStoreName);
		messageConsumer = messageStore.getConsumer();

		if (messageProcessor.getParameters().get(FailoverForwardingProcessorConstants.TARGET_MESSAGE_STORE) != null) {
			targetMessageStoreName = (String) messageProcessor.getParameters().get
					(FailoverForwardingProcessorConstants.TARGET_MESSAGE_STORE);
		}

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

}

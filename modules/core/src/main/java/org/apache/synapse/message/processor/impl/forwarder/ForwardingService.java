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

package org.apache.synapse.message.processor.impl.forwarder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessor;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.processor.impl.ScheduledMessageProcessor;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.task.Task;
import org.apache.synapse.util.MessageHelper;

/**
 * This task is responsible for forwarding a request to a given endpoint. This
 * is based on a blocking implementation and can send only one message at a
 * time. Also this supports Throttling and reliable messaging.
 * 
 */
public class ForwardingService implements Task, ManagedLifecycle {
    private static final Log log = LogFactory.getLog(ForwardingService.class);

	// The consumer that is associated with the particular message store
	private MessageConsumer messageConsumer;

	// Owner of the this job
	private MessageProcessor messageProcessor;

    // This is the client which sends messages to the end point
    private BlockingMsgSender sender;

	/*
	 * Interval between two retries to the client. This only come to affect only
	 * if the client is un-reachable
	 */
	private int retryInterval = 1000;

	// Sequence to invoke in a failure
	private String faultSeq = null;

	// Sequence to reply on success
	private String replySeq = null;

	// Sequence to invoke in a message processor deactivation
	private String deactivateSeq = null;

    private String targetEndpoint = null;

	/*
	 * The cron expression under which the message processor runs.
	 */
	private String cronExpression = null;

	/*
	 * This is specially used for REST scenarios where http status codes can
	 * take semantics in a RESTful architecture.
	 */
	private String[] nonRetryStatusCodes = null;

	/*
	 * These two maintain the state of service. For each iteration these should
	 * be reset
	 */
	private boolean isSuccessful = false;
	private volatile boolean isTerminated = false;

	/*
	 * Number of retries before shutting-down the processor. -1 value
	 * indicates that
	 * retry should happen forever
	 */
	private int maxDeliverAttempts = 4;
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
    
	private SynapseEnvironment synapseEnvironment;

	private boolean initialized = false;

    /**
     * Specifies whether the service should be started as deactivated or not
     */
    private boolean isDeactivatedAtStartup= false;

    /**
     * Specifies whether we should consider the response of the message in determining the success of message forwarding
     */
    private boolean isResponseValidationNotRequired = false;
    
    Pattern httpPattern = Pattern.compile("^(http|https|hl7):");
	
	public ForwardingService(MessageProcessor messageProcessor, BlockingMsgSender sender,
	                         SynapseEnvironment synapseEnvironment, long threshouldInterval) {
		this.messageProcessor = messageProcessor;
		this.sender = sender;
		this.synapseEnvironment = synapseEnvironment;
		// Initializes the interval to the Threshould interval value.
		this.interval = threshouldInterval;
	}

    public ForwardingService(MessageProcessor messageProcessor, BlockingMsgSender sender,
                             SynapseEnvironment synapseEnvironment, long threshouldInterval,
                             boolean isDeactivatedAtStartup ) {
        this.messageProcessor = messageProcessor;
        this.sender = sender;
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
                log.warn("Initial delay interrupted when Forwarding service started as inactive ", exception);
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

						Set proSet = messageContext.getPropertyKeySet();
						if (proSet != null) {
							if (proSet.contains(ForwardingProcessorConstants.BLOCKING_SENDER_ERROR)) {
								proSet.remove(ForwardingProcessorConstants.BLOCKING_SENDER_ERROR);
							}
						}
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
             * value is less then 1000 ms. Cron expressions are handled above so
             * we need to skip it here. Otherwise the cron expression is kept
             * sleeping twice as the forwarding interval.
             */
            if (interval > 0 && interval < MessageProcessorConstants.THRESHOULD_INTERVAL &&
                !isRunningUnderCronExpression()) {
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
             * For cron expressions with interval < 1000ms this scenario is not
             * applicable hence skipping it here. For cron expressions, all the
             * messages in the queue at the moment are sent to the backend. If
             * you give control back to the Quartz that behavior can not be
             * achieved, only a portion of the messages will get dispatched
             * while other messages will remain in the queue.
             */
            if (isThrottling && new Date().getTime() - startTime > 1000 &&
                !isRunningUnderCronExpression()) {
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
		// Setting up the JMS consumer here.
		setMessageConsumer();

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

		replySeq = (String) parametersMap.get(ForwardingProcessorConstants.REPLY_SEQUENCE);

		faultSeq = (String) parametersMap.get(ForwardingProcessorConstants.FAULT_SEQUENCE);

		deactivateSeq = (String) parametersMap.get(ForwardingProcessorConstants.DEACTIVATE_SEQUENCE);

		targetEndpoint = (String) parametersMap.get(ForwardingProcessorConstants.TARGET_ENDPOINT);

		// Default value should be true.
		if (parametersMap.get(ForwardingProcessorConstants.THROTTLE) != null) {
			isThrottling =
			               Boolean.parseBoolean((String) parametersMap.get(ForwardingProcessorConstants.THROTTLE));
		}
		
		if (parametersMap.get(ForwardingProcessorConstants.CRON_EXPRESSION) != null) {
			cronExpression =
			                 String.valueOf(parametersMap.get(ForwardingProcessorConstants.CRON_EXPRESSION));
		}

		// Default Value should be -1.
		if (cronExpression != null &&
		    parametersMap.get(ForwardingProcessorConstants.THROTTLE_INTERVAL) != null) {
			throttlingInterval =
			                     Long.parseLong((String) parametersMap.get(ForwardingProcessorConstants.THROTTLE_INTERVAL));
		}

		nonRetryStatusCodes =
		                      (String[]) parametersMap.get(ForwardingProcessorConstants.NON_RETRY_STATUS_CODES);

		// Default to FALSE.
		if (parametersMap.get(ForwardingProcessorConstants.MAX_DELIVERY_DROP) != null &&
		    parametersMap.get(ForwardingProcessorConstants.MAX_DELIVERY_DROP).toString()
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
    
    private Set<Integer> getNonRetryStatusCodes() {
        Set<Integer>nonRetryCodes = new HashSet<Integer>();
        if (nonRetryStatusCodes != null) {
            for (String code : nonRetryStatusCodes) {
                try {
                    int codeI = Integer.parseInt(code.trim());
                    nonRetryCodes.add(codeI);
                } catch (NumberFormatException e) {
                } // ignore the invalid status code
            }
        }
         return nonRetryCodes;
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
	 * Sends the mesage to a given endpoint.
	 * 
	 * @param messageContext
	 *            synapse {@link MessageContext} to be sent
     */
    public void dispatch(MessageContext messageContext) {
        if (log.isDebugEnabled()) {
			log.debug("Sending the message to client with message processor [" +
			          messageProcessor.getName() + "]");
		}

		// The below code is just for keeping the backward compatibility with
		// the old code.
		if (targetEndpoint == null) {
			targetEndpoint =
			                 (String) messageContext.getProperty(ForwardingProcessorConstants.TARGET_ENDPOINT);
		}

		MessageContext outCtx = null;
		SOAPEnvelope originalEnvelop = messageContext.getEnvelope();

		if (targetEndpoint != null) {
			Endpoint ep = messageContext.getEndpoint(targetEndpoint);
			AbstractEndpoint abstractEndpoint = (AbstractEndpoint) ep;
			EndpointDefinition endpointDefinition = abstractEndpoint.getDefinition();
			String endpointReferenceValue = null;
	        if (endpointDefinition.getAddress() != null) {
		        endpointReferenceValue = endpointDefinition.getAddress();
		        isResponseValidationNotRequired = !isResponseValidationRequiredEndpoint(endpointReferenceValue);
	        }
			try {
				// Send message to the client
				while (!isSuccessful && !isTerminated) {
					try {
						// For each retry we need to have a fresh copy of the
						// actual message. otherwise retry may not
						// work as expected.
						messageContext.setEnvelope(MessageHelper.cloneSOAPEnvelope(originalEnvelop));
                        setSoapHeaderBlock(messageContext);
						OMElement firstChild = null; //
						org.apache.axis2.context.MessageContext origAxis2Ctx =
						                                                       ((Axis2MessageContext) messageContext).getAxis2MessageContext();
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
						}// Had to do this because
						 // MessageHelper#cloneSOAPEnvelope does not clone
						 // OMSourcedElemImpl correctly.
						origAxis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
						                         getNonRetryStatusCodes());

						if (messageConsumer != null && messageConsumer.isAlive()) {
							outCtx = sender.send(ep, messageContext);
						}

                        if (isResponseValidationNotRequired) {
                            /*
                             * There is no status codes to deal with JMS eps. So
                             * merely set it as a success if there's no any
                             * exceptions.
                             */
                            isSuccessful = true;
                        } else {
                            String responseSc =
                                                ((String) ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                                                                                                .getProperty(SynapseConstants.HTTP_SC));
							// Some events where response code is null (i.e. sender socket timeout
							// when there is no response from endpoint)
							int sc;
							try {
								sc = Integer.parseInt(responseSc.trim());
								isSuccessful =
										getHTTPStatusCodeFamily(sc).equals(
												HTTPStatusCodeFamily.SUCCESSFUL) ||
												isNonRetryErrorCode(responseSc);
							} catch (NumberFormatException nfe) {
								isSuccessful = false;
							}
                        }
					} catch (Exception e) {
                        // this means send has failed due to some reason so we
                        // have to retry it
                        /*
                         * If an exception is thrown in a JMS scenario then we
                         * have to consider it as a failure.
                         */
                        if (isResponseValidationNotRequired) {
                            isSuccessful = false;
						} else if (outCtx != null && "true".equals(outCtx.getProperty(
								ForwardingProcessorConstants.BLOCKING_SENDER_ERROR))) {
							isSuccessful = false;
						} else {
                            if (e instanceof SynapseException) {
                                String responseSc =
                                                    ((String) ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                                                                                                    .getProperty(SynapseConstants.HTTP_SC));
								// We can come to this exception where sender has
								// not responded, in which case there is no SC - we can't guarantee
								// if send was successful or not in such cases other than those
								// handled above.
								int sc;
								try {
									sc = Integer.parseInt(responseSc.trim());
									isSuccessful =
											getHTTPStatusCodeFamily(sc).equals(
													HTTPStatusCodeFamily.SUCCESSFUL) ||
													isNonRetryErrorCode(responseSc);
								} catch (NumberFormatException nfe) {
									isSuccessful = false;
								}
                            }
                        }
						if (!isSuccessful) {
							log.error("BlockingMessageSender of message processor [" +
							          this.messageProcessor.getName() +
							          "] failed to send message to the endpoint");
							// Some Error has occurred while having out only
							// operation. Try to send the to fault sequence,
							// since
							// outCtx is null passing the messageContext
							sendThroughFaultSeq(messageContext);
						}
					}

                    if (outCtx != null) {
                        if (isSuccessful) {
                            sendThroughReplySeq(outCtx);
                            messageConsumer.ack();
                            attemptCount = 0;
                            isSuccessful = true;
                            if (log.isDebugEnabled()) {
                                log.debug("Successfully sent the message to endpoint [" +
                                          ep.getName() + "]" + " with message processor [" +
                                          messageProcessor.getName() + "]");
                            }
                        } else {
                            // This means some error has occurred so
                            // must try to send down the fault sequence.
                            log.error("BlockingMessageSender of message processor [" +
                                      this.messageProcessor.getName() +
                                      "] failed to send message to the endpoint");
                            sendThroughFaultSeq(outCtx);
                        }
                    } else {
                        if (isSuccessful) {
                            // This Means we have invoked an out only operation
                            // remove the message and reset the count
                            messageConsumer.ack();
                            attemptCount = 0;
                            isSuccessful = true;

                            if (log.isDebugEnabled()) {
                                log.debug("Successfully sent the message to endpoint [" +
                                          ep.getName() + "]" + " with message processor [" +
                                          messageProcessor.getName() + "]");
                            }

                        } else {
                            // This means some error has occurred.
                            log.error("BlockingMessageSender of message processor [" +
                                      this.messageProcessor.getName() +
                                      "] failed to send message to the endpoint");
                        }
                    }
					
					if (!isSuccessful) {
						// Then we have to retry sending the message to the
						// client.
						prepareToRetry(messageContext);
					} 
				}
			} catch (Exception e) {
				log.error("Message processor [" + messageProcessor.getName() +
				          "] failed to send the message to" + " client", e);
			}
		} else {
			/*
			 * No Target Endpoint defined for the Message So we do not have a
			 * place to deliver.
			 * Here we log a warning and remove the message todo: we can improve
			 * this by implementing a target inferring
			 * mechanism.
			 */

			log.warn("Property " + ForwardingProcessorConstants.TARGET_ENDPOINT +
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
	 * Sending the out message through the reply sequence.
	 * 
	 * @param outCtx
	 *            Synapse out {@link MessageContext} to be sent through the
	 *            reply sequence.
	 */
	public void sendThroughReplySeq(MessageContext outCtx) {
		if (replySeq == null) {
			deactivateMessageProcessor(outCtx);
			log.error("Failed to send the out message. Reply sequence does not Exist. Deactivated the message processor");
			return;
		}
		Mediator mediator = outCtx.getSequence(replySeq);

		if (mediator == null) {
			deactivateMessageProcessor(outCtx);
			log.error("Failed to send the out message. Reply sequence [" + replySeq +
			          "] does not exist. Deactivated the message processor");
			return;
		}

		mediator.mediate(outCtx);
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
				log.debug("Failed to send to client retrying after " + retryInterval +
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

    private boolean isNonRetryErrorCode(final String responseHttpSc) {
        boolean isNonRetryErrCode = false;
		if(nonRetryStatusCodes != null) {
			for (String nonretrySc : nonRetryStatusCodes) {
				if (nonretrySc.trim().contains(responseHttpSc.trim())) {
					isNonRetryErrCode = true;
					break;
				}
			}
		}
        return isNonRetryErrCode;
    }

	private boolean isRunningUnderCronExpression() {
		return (cronExpression != null) && (throttlingInterval > -1);
	}

	private void dropMessageAndContinueMessageProcessor() {
		messageConsumer.ack();
		attemptCount = 0;
		isSuccessful = true;
		log.info("Removed failed message and continue the message processor [" +
		         this.messageProcessor.getName() + "]");
	}
    
	private boolean setMessageConsumer() {
		final String messageStore = messageProcessor.getMessageStoreName();
		messageConsumer =
		                  synapseEnvironment.getSynapseConfiguration()
		                                    .getMessageStore(messageStore).getConsumer();
		
        /*
         * If Message Processor is deactivated via Advanced params, then we need
         * to cleanup the JMS consumers here. Ideally a deactivated MP should
         * not have any active JMS consumers.
         */
        if (!((ScheduledMessageProcessor) messageProcessor).getIsActivatedParamValue()) {
            messageConsumer.cleanup();
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
    
    private boolean isResponseValidationRequiredEndpoint(String epAddress) {
        Matcher match = httpPattern.matcher(epAddress);
        return match.find();
    }

    /**
     * + * Used to determine the family of HTTP status codes to which the given
     * code
     * + * belongs.
     * + *
     * + * @param statusCode - The HTTP status code
     * +
     */
    private HTTPStatusCodeFamily getHTTPStatusCodeFamily(int statusCode) {
        switch (statusCode / 100) {
            case 1:
                return HTTPStatusCodeFamily.INFORMATIONAL;
            case 2:
                return HTTPStatusCodeFamily.SUCCESSFUL;
            case 3:
                return HTTPStatusCodeFamily.REDIRECTION;
            case 4:
                return HTTPStatusCodeFamily.CLIENT_ERROR;
            case 5:
                return HTTPStatusCodeFamily.SERVER_ERROR;
            default:
                return HTTPStatusCodeFamily.OTHER;
        }
    }

    /**
     * The set of HTTP status code families.
     */
    private enum HTTPStatusCodeFamily {
        INFORMATIONAL, SUCCESSFUL, REDIRECTION, CLIENT_ERROR, SERVER_ERROR, OTHER
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
					if (element instanceof OMElement) {
						newHeaderNodes.add(ElementHelper.toSOAPHeaderBlock((OMElement) element, fac));
					}
					iHeader.remove();
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

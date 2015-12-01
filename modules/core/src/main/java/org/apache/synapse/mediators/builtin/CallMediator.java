/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.SynapseException;

import java.util.Set;

/**
 * Call Mediator sends a message using specified semantics. If it contains an endpoint it will
 * send the message to that endpoint. Once a message is sent to the endpoint further sending
 * behaviors are completely governed by that endpoint. If there is no endpoint available,
 * CallMediator will send the message to the implicitly stated destination.
 * <p/>
 * Even though Call mediator leverages the non-blocking transports which is same as Send mediator,
 * response will be mediated from the next mediator placed after the Call mediator. Behaviour is
 * very much same as the Callout Mediator.
 * So Call mediator can be considered as a non-blocking Callout mediator.
 * <p/>
 * To implement this behaviour, important states in the mediation flow is stored in the
 * message context.
 * An important state in the mediation flow is represented by the
 * {@link org.apache.synapse.ContinuationState} which are stored in the ContinuationStateStack
 * which resides in the MessageContext.
 * <p/>
 * These ContinuationStates are used to mediate the response message and continue the message flow.
 */
public class CallMediator extends AbstractMediator implements ManagedLifecycle {

    public final static String DEFAULT_CLIENT_REPO = "./repository/deployment/client";
    public final static String DEFAULT_AXIS2_XML = "./repository/conf/axis2/axis2_blocking_client.xml";

    private BlockingMsgSender blockingMsgSender = null;

    private ConfigurationContext configCtx = null;

    private Endpoint endpoint = null;

    private boolean blocking = false;

    private SynapseEnvironment synapseEnv;

    /**
     * This will call the send method on the messages with implicit message parameters
     * or else if there is an endpoint, with that endpoint parameters
     *
     * @param synInCtx the current message to be sent
     * @return false for in-out invocations as flow should put on to hold after the Call mediator,
     * true for out only invocations
     */
    public boolean mediate(MessageContext synInCtx) {

        if (synInCtx.getEnvironment().isDebugEnabled()) {
            if (super.divertMediationRoute(synInCtx)) {
                return true;
            }
        }

        if (blocking) {
            return handleBlockingCall(synInCtx);
        } else {
            return handleNonBlockingCall(synInCtx);
        }
    }

    /**
     * Send request in blocking manner
     *
     * @param synInCtx message context
     * @return  continue the mediation flow or not
     */
    private boolean handleBlockingCall(MessageContext synInCtx) {

        SynapseLog synLog = getLog(synInCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Call mediator - Blocking Call");
            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synInCtx.getEnvelope());
            }
        }

        MessageContext resultMsgCtx = null;
        try {
            if ("true".equals(synInCtx.getProperty(SynapseConstants.OUT_ONLY))) {
                blockingMsgSender.send(endpoint, synInCtx);
            } else {
                resultMsgCtx = blockingMsgSender.send(endpoint, synInCtx);
                if ("true".equals(resultMsgCtx.getProperty(SynapseConstants.BLOCKING_SENDER_ERROR))) {
                    handleFault(synInCtx, (Exception) resultMsgCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
                }
            }
        } catch (Exception ex) {
            handleFault(synInCtx, ex);
        }

        if (resultMsgCtx != null) {
            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Response payload received : " + resultMsgCtx.getEnvelope());
            }
            try {
                synInCtx.setEnvelope(resultMsgCtx.getEnvelope());

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("End : Call mediator - Blocking Call");
                }
                return true;
            } catch (Exception e) {
                handleFault(synInCtx, e);
            }
        } else {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Service returned a null response");
            }
        }
        return true;
    }

    /**
     * Send request in non-blocking manner
     *
     * @param synInCtx message context
     * @return  continue the mediation flow or not
     */
    private boolean handleNonBlockingCall(MessageContext synInCtx) {
        SynapseLog synLog = getLog(synInCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Call mediator - Non Blocking Call");
            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synInCtx.getEnvelope());
            }
        }

        // clear the message context properties related to endpoint in last service invocation
        Set keySet = synInCtx.getPropertyKeySet();
        if (keySet != null) {
            keySet.remove(SynapseConstants.RECEIVING_SEQUENCE);
            keySet.remove(EndpointDefinition.DYNAMIC_URL_VALUE);
        }

        boolean outOnlyMessage = "true".equals(synInCtx.getProperty(SynapseConstants.OUT_ONLY));

        // Prepare the outgoing message context
        MessageContext synOutCtx = null;
        if (outOnlyMessage) {
            try {
                synOutCtx = MessageHelper.cloneMessageContext(synInCtx);
            } catch (AxisFault axisFault) {
                handleException("Error occurred while cloning msg context", axisFault, synInCtx);
            }
        } else {
            synOutCtx = synInCtx;
        }

        synOutCtx.setProperty(SynapseConstants.CONTINUATION_CALL, true);
        ContinuationStackManager.updateSeqContinuationState(synOutCtx, getMediatorPosition());

        // If no endpoints are defined, send where implicitly stated
        if (endpoint == null) {

            if (synLog.isTraceOrDebugEnabled()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Calling ").append(synOutCtx.isResponse() ? "response" : "request")
                        .append(" message using implicit message properties..");
                sb.append("\nCalling To: ").append(synOutCtx.getTo() != null ?
                        synOutCtx.getTo().getAddress() : "null");
                sb.append("\nSOAPAction: ").append(synOutCtx.getWSAAction() != null ?
                        synOutCtx.getWSAAction() : "null");
                synLog.traceOrDebug(sb.toString());
            }

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Envelope : " + synOutCtx.getEnvelope());
            }
            synOutCtx.getEnvironment().send(null, synOutCtx);

        } else {
            endpoint.send(synOutCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Call mediator - Non Blocking Call");
        }

        if (outOnlyMessage) {
            // For out only invocations request flow should continue
            // otherwise flow should stop from here
            return true;
        }
        return false;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void init(SynapseEnvironment synapseEnvironment) {
        this.synapseEnv = synapseEnvironment;

        if (endpoint != null) {
            endpoint.init(synapseEnvironment);
        }

        if (blocking) {
            try {
                configCtx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                        DEFAULT_CLIENT_REPO, DEFAULT_AXIS2_XML);
                blockingMsgSender = new BlockingMsgSender();
                blockingMsgSender.setConfigurationContext(configCtx);
                blockingMsgSender.init();

            } catch (AxisFault axisFault) {
                String msg = "Error while initializing the Call mediator";
                log.error(msg, axisFault);
                throw new SynapseException(msg, axisFault);
            }
        } else {
            synapseEnvironment.updateCallMediatorCount(true);
        }
    }

    public void destroy() {
        if (endpoint != null) {
            endpoint.destroy();
        }
        synapseEnv.updateCallMediatorCount(false);
    }

    @Override
    public boolean isContentAware() {
        return false;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    private void handleFault(MessageContext synCtx, Exception ex) {
        synCtx.setProperty(SynapseConstants.SENDING_FAULT, Boolean.TRUE);

        if (ex != null && ex instanceof AxisFault) {
            AxisFault axisFault = (AxisFault) ex;

            if (axisFault.getFaultCodeElement() != null) {
                synCtx.setProperty(SynapseConstants.ERROR_CODE,
                        axisFault.getFaultCodeElement().getText());
            } else {
                synCtx.setProperty(SynapseConstants.ERROR_CODE,
                        SynapseConstants.CALLOUT_OPERATION_FAILED);
            }

            if (axisFault.getMessage() != null) {
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                        axisFault.getMessage());
            } else {
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, "Error while performing " +
                        "the call operation");
            }

            if (axisFault.getFaultDetailElement() != null) {
                if (axisFault.getFaultDetailElement().getFirstElement() != null) {
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                            axisFault.getFaultDetailElement().getFirstElement());
                } else {
                    synCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                            axisFault.getFaultDetailElement().getText());
                }
            }
        }

        synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, ex);
        throw new SynapseException("Error while performing the call operation", ex);
    }

}

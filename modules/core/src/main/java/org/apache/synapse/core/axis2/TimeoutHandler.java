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

package org.apache.synapse.core.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.synapse.CallbackAckConfigUtil;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.flow.statistics.collectors.CallbackStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.endpoints.dispatch.SALSessions;
import org.apache.synapse.commons.logger.ContextAwareLogger;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.ProtocolState;
import org.apache.synapse.transport.passthru.SourceContext;
import org.apache.synapse.transport.passthru.TargetContext;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.util.ConcurrencyThrottlingUtils;
import org.wso2.config.mapper.ConfigParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimerTask;

/**
 * An object of this class is registered to be invoked in some predefined time intervals. This
 * checks the timeouts of callbacks stored in the SynapseCallbackReceiver and removes all expired
 * callbacks. Timeouts of the callbacks are stored as the time, not the duration. So that the
 * time or the interval of invoking this class does not affect the correctness of the timeouts,
 * although longer intervals would introduce larger error between the actual timeout and the
 * specified timeout.
 *
 * For each invocation this gets a time value to be compared against the timeouts of the callback
 * objects. This time is the System.currentTimeMillis() for Java 1.4 and System.nanoTime() for
 * Java 1.5 and later.
 */
public class TimeoutHandler extends TimerTask {

    private static final Log log = LogFactory.getLog(TimeoutHandler.class);

    /** The callback map - already a Collections.synchronized() hash map */
    private final Map callbackStore;
    /** a lock to prevent concurrent execution while ensuring least overhead */
    private final Object lock = new Object();
    private boolean alreadyExecuting = false;
    /*This is the timeout for otherwise non-expiring callbacks to ensure system stability over time */
    private long globalTimeout = SynapseConstants.DEFAULT_GLOBAL_TIMEOUT;
    private static final String SEND_TIMEOUT_MESSAGE = "Send timeout";
    private ServerContextInformation contextInfo = null;
    private PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    public TimeoutHandler(Map callbacks, ServerContextInformation contextInfo) {
        this.callbackStore = callbacks;
        this.contextInfo = contextInfo;
        this.globalTimeout = SynapseConfigUtils.getGlobalTimeoutInterval();
        log.info("This engine will expire all callbacks after " +
                SynapseConstants.ENDPOINT_TIMEOUT_TYPE.GLOBAL_TIMEOUT.toString() + ": " + (globalTimeout / 1000) +
                " seconds, irrespective of the timeout action," +
                " after the specified or optional timeout");
    }

    /**
     * Checks if the timeout has expired for each callback in the callback store. If expired, removes
     * the callback. If specified sends a fault message to the client about the timeout.
     */
    public void run() {
        if (alreadyExecuting) return;

        synchronized (lock) {
            alreadyExecuting = true;
            try {
                processCallbacks();
            } catch (Exception ex) {
                log.warn("Exception occurred while processing callbacks", ex);
            } catch (Error ex) {
                log.warn("Error occurred while processing callbacks", ex);
            } finally {
                alreadyExecuting = false;
            }
        }
    }

    private void processCallbacks() {

        //clear all the expired sessions
        SALSessions.getInstance().clearSessions();

        // checks if callback store contains at least one entry before proceeding. otherwise getting
        // the time for doing nothing would be a inefficient task.

        // we have to synchronize this on the callbackStore as iterators of thread safe collections
        // are not thread safe. callbackStore can be modified
        // concurrently by the SynapseCallbackReceiver.
        synchronized(callbackStore) {

            if (callbackStore.size() > 0) {

                long currentTime = currentTime();
                boolean closeSocketOnEndpointTimeout = false;
                List toRemove = new ArrayList();

                for (Object key : callbackStore.keySet()) {

                    AsyncCallback callback = (AsyncCallback) callbackStore.get(key);
                    if (callback == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("There is no callback for key :" + key);
                        }
                        continue;
                    }

                    if (callback.getTimeOutOn() <= currentTime) {

                        synchronized (callback) {
                            if (callback.isMarkedForRemoval()) {
                                continue;
                            }
                            callback.setMarkedForRemoval();
                            toRemove.add(key);
                        }
                        if (!"true".equals(callback.getSynapseOutMsgCtx().getProperty(SynapseConstants.OUT_ONLY))) {
                            org.apache.axis2.context.MessageContext axis2MessageContext = callback.getAxis2OutMsgCtx();
                            String timeoutWarnLog = "Expiring message ID : " + key + "; dropping message after "
                                    + callback.getTimeoutType().toString() + " of : "
                                    + (callback.getTimeoutDuration() / 1000) + " seconds for "
                                    + getEndpointLogMessage(callback.getSynapseOutMsgCtx(),
                                    callback.getAxis2OutMsgCtx()) + ", "
                                    + getServiceLogMessage(callback.getSynapseOutMsgCtx())
                                    + "Correlation ID : " + callback.getAxis2OutMsgCtx().getProperty(
                                    CorrelationConstants.CORRELATION_ID);
                            if (conf.isCloseSocketOnEndpointTimeout()) {
                                ContextAwareLogger.getLogger(axis2MessageContext, log, true)
                                        .warn(timeoutWarnLog + ", Closing the Target Connection");
                            } else {
                                ContextAwareLogger.getLogger(axis2MessageContext, log, true)
                                        .warn(timeoutWarnLog);
                            }
                        }

                        if (callback.getTimeOutAction() != SynapseConstants.NONE) {

                            // activate the fault sequence of the current sequence mediator
                            MessageContext msgContext = callback.getSynapseOutMsgCtx();
                            org.apache.axis2.context.MessageContext axis2MessageContext = callback.getAxis2OutMsgCtx();

                            // Callback-controlled ack: for RabbitMQ timeouts we:
                            // 1. Set CLIENT_API_NON_BLOCKING so Timer Thread Does not get blocked in any scenario.
                            // 2. Log and explicitly invoke onAppError to close channel / release resources early
                            //    preventing connection / buffer leaks since the message has already timed out.
                            if (CallbackAckConfigUtil.isCallbackControlledAckEnabled()) {
                                if (CallbackAckConfigUtil.isRabbitMQTransport(axis2MessageContext)) {
                                    TransportOutDescription transportOut =
                                            callback.getAxis2OutMsgCtx().getTransportOut();

                                    ((Axis2MessageContext) msgContext).getAxis2MessageContext().setProperty
                                            (SynapseConstants.CLIENT_API_NON_BLOCKING, Boolean.TRUE);
                                    log.warn("Callback Controlled ack is enabled. " +
                                            "Closing the RabbitMQ Channel for the timed out message : " + key);
                                    transportOut.getSender().onAppError(callback.getAxis2OutMsgCtx());
                                }
                            }

                            /* Clear the pipe to prevent release of the associated writer buffer
                               to the buffer factory.
                               This is to prevent same buffer is getting released to both source
                               and target buffer factories. Otherwise when a late response arrives,
                               buffer is released to both factories and makes system unstable
                            */
                            ((Axis2MessageContext) msgContext).getAxis2MessageContext().
                                    removeProperty(PassThroughConstants.PASS_THROUGH_PIPE);

                            // add an error code to the message context, so that error sequences
                            // can identify the cause of error
                            msgContext.setProperty(SynapseConstants.ERROR_CODE,
                                                   SynapseConstants.HANDLER_TIME_OUT);
                            msgContext.setProperty(SynapseConstants.ERROR_MESSAGE,
                                                   SEND_TIMEOUT_MESSAGE);
                            NHttpServerConnection sourceConn = (NHttpServerConnection) axis2MessageContext.
                                    getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                            if (sourceConn != null && SourceContext.get(sourceConn).getState()
                                    != ProtocolState.REQUEST_DONE) {
                                //Suspend input to avoid invoking input ready method and set this property here
                                //to avoid invoking the input ready method, while fault response is mediating through
                                //the mediation since we have set REQUEST_DONE state in SourceHandler responseReady
                                // method
                                sourceConn.suspendInput();
                                SourceContext sourceContext = (SourceContext) sourceConn.getContext().getAttribute
                                        (TargetContext.CONNECTION_INFORMATION);
                                if (sourceContext != null) {
                                    sourceContext.setIsSourceRequestMarkedToBeDiscarded(true);
                                }
                                // This is to gurantee that the connection is closed after the response is sent and
                                // not to release the buffer to the factory
                                SourceContext.get(sourceConn).setShutDown(true);

                                // We have to close the target connection since the request is invalid
                                closeSocketOnEndpointTimeout = true;
                            }

                            /* Clear the NO_KEEPALIVE property to prevent closing response connection when going through
                               the fault sequence due to end point time out. Since the axis2 message context used here
                               is the same axis2 message context used in request path and the intention of this property
                               is to Disable HTTP keep alive for outgoing requests. If it is required this can be put
                               in fault sequence.
                            */
                            ((Axis2MessageContext) msgContext).getAxis2MessageContext().
                                    removeProperty(PassThroughConstants.NO_KEEPALIVE);

                            if (!CallbackAckConfigUtil.isPreservePayloadOnTimeout()) {
                                SOAPEnvelope soapEnvelope;
                                if (msgContext.isSOAP11()) {
                                    soapEnvelope = OMAbstractFactory.
                                            getSOAP11Factory().createSOAPEnvelope();
                                    soapEnvelope.addChild(
                                            OMAbstractFactory.getSOAP11Factory().createSOAPBody());
                                } else {
                                    soapEnvelope = OMAbstractFactory.
                                            getSOAP12Factory().createSOAPEnvelope();
                                    soapEnvelope.addChild(
                                            OMAbstractFactory.getSOAP12Factory().createSOAPBody());
                                }
                                try {
                                    msgContext.setEnvelope(soapEnvelope);
                                } catch (Throwable ex) {
                                    ContextAwareLogger.getLogger(axis2MessageContext, log, true)
                                            .error("Exception or Error occurred resetting SOAP Envelope", ex);
                                    continue;
                                }
                            }

                            Stack<FaultHandler> faultStack = msgContext.getFaultStack();
                            if (!faultStack.isEmpty()) {
                                FaultHandler faultHandler = faultStack.pop();
                                if (faultHandler != null) {
                                    try {
                                        faultHandler.handleFault(msgContext);
                                    } catch (Throwable ex) {
                                        ContextAwareLogger.getLogger(axis2MessageContext, log, true)
                                                .warn("Exception or Error occurred while "
                                                        + "executing the fault handler", ex);
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }

                for(Object key : toRemove) {

                    AsyncCallback callback = (AsyncCallback) callbackStore.get(key);
                    if (callback == null) {
                        // we will get here if we get a response from the Backend while clearing callbacks
                        continue;
                    }

                    org.apache.synapse.MessageContext synapseOutMsgCtx = callback.getSynapseOutMsgCtx();
                    ConcurrencyThrottlingUtils.decrementConcurrencyThrottleAccessController(synapseOutMsgCtx);
                    callbackStore.remove(key);
                    if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                        CallbackStatisticCollector.callbackCompletionEvent(callback.getSynapseOutMsgCtx(), (String) key);
                    }

                    if (closeSocketOnEndpointTimeout || conf.isCloseSocketOnEndpointTimeout() ) {                        TransportOutDescription transportOut = callback.getAxis2OutMsgCtx().getTransportOut();
                        if (transportOut != null && transportOut.getSender() != null) {
                            // Call the TransportSender's onAppError method to release any resources
                            transportOut.getSender().onAppError(callback.getAxis2OutMsgCtx());
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the current time.
     *
     * @return  System.currentTimeMillis() on Java 1.4
     *          System.nanoTime() on Java 1.5 (todo: implement)
     */
    private long currentTime() {
        return System.currentTimeMillis();
    }


    private String getEndpointLogMessage(MessageContext synCtx,
                                                org.apache.axis2.context.MessageContext axisCtx) {
        return synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) + ", URI : " + axisCtx.getTo().getAddress();
    }

    private String getServiceLogMessage(MessageContext synCtx) {
        Object proxyName = synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
        if (proxyName != null) {
            return "Received through Proxy service : " + proxyName;
        }

        Object apiName = synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
        if (apiName != null) {
            return "Received through API : " + apiName;
        }

        Object inboundEndpointName = synCtx.getProperty(SynapseConstants.INBOUND_ENDPOINT_NAME);
        if (inboundEndpointName != null) {
            return "Received through Inbound Endpoint : " + inboundEndpointName;
        }
        return "Received through an entry point other than a proxy, an api or an inbound endpoint ";
    }
}

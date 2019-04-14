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
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessController;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessReplicator;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.inbound.InboundEndpointConstants;
import org.apache.synapse.inbound.InboundResponseSender;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.MediatorPropertyUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.POXUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * This class helps the Axis2SynapseEnvironment implement the send method
 */
public class Axis2Sender {

    private static final Log log = LogFactory.getLog(Axis2Sender.class);
    /**
     * Content type header name.
     */
    private static final String CONTENT_TYPE_STRING = "Content-Type";

    /**
     * Send a message out from the Synapse engine to an external service
     *
     * @param endpoint                the endpoint definition where the message should be sent
     * @param synapseInMessageContext the Synapse message context
     */
    public static void sendOn(EndpointDefinition endpoint,
                              org.apache.synapse.MessageContext synapseInMessageContext) {

        try {

            // Invoke Synapse Handlers
            Iterator<SynapseHandler> iterator =
                    synapseInMessageContext.getEnvironment().getSynapseHandlers().iterator();
            while (iterator.hasNext()) {
                SynapseHandler handler = iterator.next();
                if (!handler.handleRequestOutFlow(synapseInMessageContext)) {
                    return;
                }
            }

            Axis2FlexibleMEPClient.send(
                    // The endpoint where we are sending to
                    endpoint,
                    // The Axis2 Message context of the Synapse MC
                    synapseInMessageContext);
        } catch (Exception e) {
            handleException("Unexpected error during sending message out", e);
        }
    }

    /**
     * Send a response back to a client of Synapse
     *
     * @param smc the Synapse message context sent as the response
     */
    public static void sendBack(org.apache.synapse.MessageContext smc) {
        if (preventMultipleResponses(smc)) {
            return;
        }

        MessageContext messageContext = ((Axis2MessageContext) smc).getAxis2MessageContext();

        // if this is a dummy 202 Accepted message meant only for the http/s transports
        // prevent it from going into any other transport sender
        if (messageContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED) &&
                messageContext.getTransportOut() != null &&
                !messageContext.getTransportOut().getName().startsWith(Constants.TRANSPORT_HTTP)) {
            return;
        }

        // fault processing code
        if (messageContext.isDoingREST() && messageContext.isFault() &&
            isMessagePayloadHasASOAPFault(messageContext)) {
            POXUtils.convertSOAPFaultToPOX(messageContext);
        }

        try {
            messageContext.setProperty(SynapseConstants.ISRESPONSE_PROPERTY, Boolean.TRUE);
            // check if addressing is already engaged for this message.
            // if engaged we should use the addressing enabled Configuration context.            

            if (AddressingHelper.isReplyRedirected(messageContext) &&
                    !messageContext.getReplyTo().hasNoneAddress()) {

                messageContext.setTo(messageContext.getReplyTo());
                messageContext.setReplyTo(null);
                messageContext.setWSAAction("");
                messageContext.setSoapAction("");
                messageContext.setProperty(
                        NhttpConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
                messageContext.setProperty(
                        AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
            }

            if (messageContext.getEnvelope().hasFault() &&
                    AddressingHelper.isFaultRedirected(messageContext) &&
                    (messageContext.getFaultTo() == null || !messageContext.getFaultTo()
                            .hasNoneAddress())) {

                messageContext.setTo(messageContext.getFaultTo());
                messageContext.setFaultTo(null);
                messageContext.setWSAAction("");
                messageContext.setSoapAction("");
                messageContext.setProperty(
                        NhttpConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
                messageContext.setProperty(
                        AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.FALSE);
            }

            String preserveAddressingProperty = (String) smc.getProperty(
                    SynapseConstants.PRESERVE_WS_ADDRESSING);
            if (preserveAddressingProperty != null &&
                    Boolean.parseBoolean(preserveAddressingProperty)) {
                /*Avoiding duplicate addressing headers*/
                messageContext.setProperty(AddressingConstants.REPLACE_ADDRESSING_HEADERS, "true");
                messageContext.setMessageID(smc.getMessageID());
            } else {
                MessageHelper.removeAddressingHeaders(messageContext);
                messageContext.setMessageID(UIDGenerator.generateURNString());
            }

            // determine weather we need to preserve the processed headers
            String preserveHeaderProperty = (String) smc.getProperty(
                    SynapseConstants.PRESERVE_PROCESSED_HEADERS);
            if (preserveHeaderProperty == null || !Boolean.parseBoolean(preserveHeaderProperty)) {
                // remove the processed headers
                MessageHelper.removeProcessedHeaders(messageContext,
                        (preserveAddressingProperty != null &&
                                Boolean.parseBoolean(preserveAddressingProperty))
                );
            }

            // temporary workaround for https://issues.apache.org/jira/browse/WSCOMMONS-197
            if (messageContext.isEngaged(SynapseConstants.SECURITY_MODULE_NAME) &&
                    messageContext.getEnvelope().getHeader() == null) {
                SOAPFactory fac = messageContext.isSOAP11() ?
                        OMAbstractFactory.getSOAP11Factory() : OMAbstractFactory.getSOAP12Factory();
                fac.createSOAPHeader(messageContext.getEnvelope());
            }

            Axis2FlexibleMEPClient.clearSecurtityProperties(messageContext.getOptions());

            // Invoke Synapse Handlers
            Iterator<SynapseHandler> iterator = smc.getEnvironment().getSynapseHandlers().iterator();
            while (iterator.hasNext()) {
                SynapseHandler handler = iterator.next();
                if (!handler.handleResponseOutFlow(smc)) {
                    return;
                }
            }

            doSOAPFormatConversion(smc);

            //isServerSide should always be true because we are sending the response back to the client
            messageContext.setServerSide(true);

            // handles concurrent throttling based on the messagecontext.
            handleConcurrentThrottleCount(smc);

            // If the request arrives through an inbound endpoint
            if (smc.getProperty(SynapseConstants.IS_INBOUND) != null
                && (Boolean) smc.getProperty(SynapseConstants.IS_INBOUND)) {

                if (smc.getProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER) != null) {
                    InboundResponseSender inboundResponseSender =
                            (InboundResponseSender) smc.getProperty(
                                    InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER);
                    inboundResponseSender.sendBack(smc);
                } else {
                    String msg = "Inbound Response Sender not found -" +
                                 " Inbound Endpoint may not support sending a response back";
                    log.error(msg);
                    throw new SynapseException(msg);
                }
            } else { // If the request arrives through a conventional transport listener
                AxisEngine.send(messageContext);
            }
        } catch (AxisFault e) {
            handleException(getResponseMessage(messageContext), e);
        }
    }

    /**
     * Check whether message body has a SOAPFault
     *
     * @param axis2Ctx axis2 message context
     * @return whether message is a SOAPFault
     */
    private static boolean isMessagePayloadHasASOAPFault(MessageContext axis2Ctx) {
        SOAPBody body = axis2Ctx.getEnvelope().getBody();
        if (body != null) {
            OMElement element = body.getFirstElement();
            if (element != null && "Fault".equalsIgnoreCase(element.getLocalName())) {
                OMNamespace ns = element.getNamespace();
                if (ns != null) {
                    String nsURI = ns.getNamespaceURI();
                    if ("http://schemas.xmlsoap.org/soap/envelope".equals(nsURI) ||
                        "http://schemas.xmlsoap.org/soap/envelope/".equals(nsURI) ||
                        "http://www.w3.org/2003/05/soap-envelope".equals(nsURI) ||
                        "http://www.w3.org/2003/05/soap-envelope/".equals(nsURI)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * This will ensure only one response is sent for a single request.
     * In HTTP request-response paradigm only a one response is allowed for a single request.
     * Due to synapse configuration issues, there is a chance of multiple response getting sent for a single request
     * Calling this method from all the places where we sent out a response message from engine
     * will prevent that from happening
     *
     * @param messageContext Synapse message context
     * @return whether a response is already sent
     */
    public static boolean preventMultipleResponses(org.apache.synapse.MessageContext messageContext) {
        Object responseStateObj = messageContext.getProperty(SynapseConstants.RESPONSE_STATE);
        // Prevent two responses for a single request message
        if (responseStateObj != null) {
            if (responseStateObj instanceof ResponseState) {
                ResponseState responseState = (ResponseState) responseStateObj;
                synchronized (responseState) {
                    if (responseState.isRespondDone()) {
                        log.warn("Trying to send a response to an already responded client request - " + getInputInfo(
                                messageContext));
                        return true;
                    } else {
                        responseState.setRespondDone();
                    }
                }
            } else {
                // This can happen only if the user has used the SynapseConstants.RESPONSE_STATE as a user property
                handleException("Response State must be of type : " + ResponseState.class + ". "
                        + SynapseConstants.RESPONSE_STATE + " must not be used as an user property name", null);
            }
        }
        return false;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static String getResponseMessage(MessageContext msgContext) {
        StringBuilder sb = new StringBuilder();
        try {

            String strEndpoint = (String) msgContext.getProperty(NhttpConstants.ENDPOINT_PREFIX);
            if (strEndpoint != null) {
                sb.append(strEndpoint + ", ");
            }
            Map<String, Object> mHeader = (Map<String, Object>) msgContext.getProperty
                    (org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (mHeader != null) {
                for (String strKey : mHeader.keySet()) {
                    sb.append(strKey + ":" + mHeader.get(strKey).toString() + ",");
                }
            }
            sb.append(" Unexpected error sending message back");
        } catch (Exception e) {
            sb.append(" Unexpected error sending message back");
        }
        return sb.toString();
    }

    /**
     * Get the information of the input message context.
     *
     * @param smc Synapse message context
     * */
    private static String getInputInfo(org.apache.synapse.MessageContext smc) {
        String inputInfo = null;
        if (smc.getProperty("proxy.name") != null) {
            inputInfo = "Proxy Name : " + smc.getProperty("proxy.name");
        } else if (smc.getProperty("REST_API_CONTEXT") != null) {
            inputInfo = "Rest API Context : " + smc.getProperty("REST_API_CONTEXT");
        } else if (smc.getProperty(SynapseConstants.INBOUND_ENDPOINT_NAME) != null) {
            inputInfo = "Inbound endpoint : " + smc.getProperty(SynapseConstants.INBOUND_ENDPOINT_NAME);
        }
        return inputInfo;
    }

    /**
     * Convert the response SOAP format to the client format if it is required.
     * @param synCtx response synapse message context
     * @throws AxisFault
     */
    private static void doSOAPFormatConversion(org.apache.synapse.MessageContext synCtx) throws AxisFault {

        Object isClientDoingRest = synCtx.getProperty(SynapseConstants.IS_CLIENT_DOING_REST);
        if (isClientDoingRest == null) {
            return; // Request hasn't come through a Proxy or API or main sequence. So skip
        }

        if ((boolean) isClientDoingRest) {
            return; // Skip the conversion for REST
        }

        if (Constants.VALUE_TRUE.equals(((Axis2MessageContext) synCtx).getAxis2MessageContext().
                getProperty(Constants.Configuration.ENABLE_MTOM))) {
            return; // Skip the conversion if MTOM is enabled
        }

        Object isClientDoingSOAP11Obj = synCtx.getProperty(SynapseConstants.IS_CLIENT_DOING_SOAP11);

        // Determine whether generated response is in SOAP11 format.
        boolean isResponseSOAP11 = false;
        boolean isResponseSOAP12 = false;
        MessageContext responseCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Object headers = responseCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            Object responseContentType = headersMap.get(CONTENT_TYPE_STRING);
            if (responseContentType != null && responseContentType instanceof String) {
                if (((String) responseContentType).trim().startsWith(SynapseConstants.SOAP11_CONTENT_TYPE) &&
                        !responseCtx.isDoingREST()) {   // REST can also have text/xml
                    isResponseSOAP11 = true;
                } else if (((String) responseContentType).trim().startsWith(SynapseConstants.SOAP12_CONTENT_TYPE)) {
                    isResponseSOAP12 = true;
                }
            }
        }

        if ((Boolean) isClientDoingSOAP11Obj) {     // If request is in SOAP11 format

            // Response is not in SOAP12 format (i.e. can be SOAP11 or any other (non REST) format)
            // Hence, no conversion required is required
            if (!isResponseSOAP12 && !responseCtx.isDoingREST()) {
                return;
            }

            try {                   // Message need to be built prior to the conversion
                RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
            } catch (Exception e) {
                handleException("Error while building message", e);
            }

            try {
                // Message need to be serialized prior to the conversion
                MediatorPropertyUtils.serializeOMElement(synCtx);
            } catch (Exception e) {
                handleException("Error while serializing the  message", e);
            }

            if (!responseCtx.isSOAP11()) {
                SOAPUtils.convertSOAP12toSOAP11(responseCtx);
            }

            responseCtx.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    SynapseConstants.SOAP11_CONTENT_TYPE);
            responseCtx.setProperty(Constants.Configuration.CONTENT_TYPE,
                    SynapseConstants.SOAP11_CONTENT_TYPE);
        } else {        // If request is in SOAP12 format

            if (!isResponseSOAP11 && !responseCtx.isDoingREST()) {  // Response is not SOAP11 not REST - it is in SOAP12
                return;
            }

            try {                   // Message need to be built prior to the conversion
                RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
            } catch (Exception e) {
                handleException("Error while building message", e);
            }

            if (responseCtx.isSOAP11()) {         // If response is in SOAP11
                SOAPUtils.convertSOAP11toSOAP12(responseCtx);
            }

            responseCtx.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    SynapseConstants.SOAP12_CONTENT_TYPE);
            responseCtx.setProperty(Constants.Configuration.CONTENT_TYPE,
                    SynapseConstants.SOAP12_CONTENT_TYPE);
        }
        responseCtx.setDoingREST(false);

    }

    /**
     * handles the concurrent access limits
     *
     * @param smc SynapseMessageContext
     */
    private static void handleConcurrentThrottleCount(org.apache.synapse.MessageContext smc) {
        Boolean isConcurrencyThrottleEnabled = (Boolean) smc.getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE);
        Boolean isAccessAllowed = (Boolean) smc.getProperty(SynapseConstants.SYNAPSE_IS_CONCURRENT_ACCESS_ALLOWED);

        if (isAccessAllowed != null && isAccessAllowed && isConcurrencyThrottleEnabled != null
                && isConcurrencyThrottleEnabled) {

            ConcurrentAccessController concurrentAccessController = (ConcurrentAccessController)
                    smc.getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_CONTROLLER);

            int available = concurrentAccessController.incrementAndGet();
            int concurrentLimit = concurrentAccessController.getLimit();

            if (log.isDebugEnabled()) {
                log.debug("Concurrency Throttle : Connection returned" + " :: " +
                        available + " of available of " + concurrentLimit + " connections");
            }

            ConcurrentAccessReplicator concurrentAccessReplicator = (ConcurrentAccessReplicator)
                    smc.getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_REPLICATOR);

            String throttleKey = (String) smc.getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE_KEY);

            if (concurrentAccessReplicator != null) {
                concurrentAccessReplicator.replicate(throttleKey, true);
            }
        }
    }
}

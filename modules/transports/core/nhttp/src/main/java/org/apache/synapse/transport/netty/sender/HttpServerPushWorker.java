/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.netty.sender;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.wso2.transport.http.netty.message.Http2PushPromise;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code HttpServerPushWorker} is the Thread which does the server push processing.
 */
public class HttpServerPushWorker implements Runnable {

    private static final Log LOG = LogFactory.getLog(HttpServerPushWorker.class);
    private final Http2PushPromise pushPromise;
    private HttpCarbonMessage httpResponse;
    private final MessageContext requestMsgCtx;

    public HttpServerPushWorker(MessageContext requestMsgCtx, Http2PushPromise pushPromise,
                                HttpCarbonMessage httpResponse) {

        this.pushPromise = pushPromise;
        this.httpResponse = httpResponse;
        this.requestMsgCtx = requestMsgCtx;
    }

    public HttpServerPushWorker(MessageContext requestMsgCtx, Http2PushPromise pushPromise) {

        this.requestMsgCtx = requestMsgCtx;
        this.pushPromise = pushPromise;
    }

    @Override
    public void run() {

        MessageContext responseMsgCtx;
        try {
            responseMsgCtx = createResponseMessageContext();
        } catch (AxisFault e) {
            LOG.error("Error while creating response message context", e);
            return;
        }
        try {
            populateProperties(responseMsgCtx);
        } catch (AxisFault e) {
            LOG.error("Error occurred while setting the SOAP envelope to the response message context", e);
            cleanup();
            return;
        }
        try {
            AxisEngine.receive(responseMsgCtx);
        } catch (AxisFault e) {
            LOG.error("Error while mediating HTTP/2 server push through sequence", e);
        }
    }

    /**
     * Create new axis2 message context for response.
     *
     * @return axis2 message context
     * @throws AxisFault
     */
    private MessageContext createResponseMessageContext() throws AxisFault {

        MessageContext responseMsgCtx = requestMsgCtx.extractCopyMessageContext();
        if (Objects.isNull(responseMsgCtx)) {
            if (requestMsgCtx.getOperationContext().isComplete()) {
                String msg = "Error getting IN message context from the operation context. " + "Possibly an RM " +
                        "terminate sequence message";
                if (LOG.isDebugEnabled()) {
                    LOG.debug(msg);
                }
                throw new AxisFault(msg);
            }
            responseMsgCtx = new MessageContext();
        } else {
            responseMsgCtx.setSoapAction("");
            responseMsgCtx.resetExecutedPhases();
        }
        return responseMsgCtx;
    }

    /**
     * Populates required properties in the message context.
     *
     * @throws AxisFault if an error occurs while setting the SOAP envelope
     */
    private void populateProperties(MessageContext responseMsgCtx) throws AxisFault {

        responseMsgCtx.setOperationContext(requestMsgCtx.getOperationContext().getServiceContext().createOperationContext(requestMsgCtx.getAxisOperation()));
        responseMsgCtx.setServiceGroupContext(requestMsgCtx.getServiceGroupContext());
        responseMsgCtx.setAxisOperation(requestMsgCtx.getAxisOperation());
        responseMsgCtx.setAxisMessage(requestMsgCtx.getOperationContext().getAxisOperation().getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        responseMsgCtx.setConfigurationContext(requestMsgCtx.getConfigurationContext());
        responseMsgCtx.setServerSide(true);
        responseMsgCtx.setDoingREST(requestMsgCtx.isDoingREST());
        responseMsgCtx.setTransportIn(requestMsgCtx.getTransportIn());
        responseMsgCtx.setTransportOut(requestMsgCtx.getTransportOut());
        responseMsgCtx.setTo(null);

        if (httpResponse != null) {
            responseMsgCtx.removeProperty(BridgeConstants.NO_ENTITY_BODY);
            // Set status code and reason phrase
            int statusCode = httpResponse.getHttpStatusCode();
            responseMsgCtx.setProperty(BridgeConstants.HTTP_SC, statusCode);
            responseMsgCtx.setProperty(BridgeConstants.HTTP_SC_DESC, httpResponse.getReasonPhrase());
            responseMsgCtx.setProperty(BridgeConstants.HTTP_STATUS_CODE_SENT_FROM_BACKEND, statusCode);
            responseMsgCtx.setProperty(BridgeConstants.HTTP_REASON_PHRASE_SENT_FROM_BACKEND,
                    httpResponse.getReasonPhrase());
            //set http response
            responseMsgCtx.setProperty(BridgeConstants.HTTP_CARBON_MESSAGE, httpResponse);
            // Set any transport headers received
            Map<String, String> headers = new HashMap<>();
            httpResponse.getHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
            responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
            setContentTypeAndCharSetEncoding(responseMsgCtx);
        } else {
            responseMsgCtx.setProperty(BridgeConstants.IS_PUSH_PROMISE, Boolean.TRUE);
        }

        // Set rest of the properties
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN, requestMsgCtx.getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setProperty(BridgeConstants.INVOKED_REST, requestMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES,
                requestMsgCtx.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES));
        responseMsgCtx.setProperty(BridgeConstants.NON_BLOCKING_TRANSPORT, true);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE,
                requestMsgCtx.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE));
        responseMsgCtx.setProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION,
                requestMsgCtx.getProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION));
        responseMsgCtx.setProperty(BridgeConstants.SERVER_PUSH, Boolean.TRUE);
        responseMsgCtx.setProperty(BridgeConstants.SERVER_PUSH_SEQUENCE,
                requestMsgCtx.getProperty(BridgeConstants.SERVER_PUSH_SEQUENCE));
        responseMsgCtx.setProperty(BridgeConstants.SERVER_PUSH_RESOURCE_PATH, pushPromise.getPath());
        responseMsgCtx.setProperty(BridgeConstants.PUSH_PROMISE, pushPromise);
        responseMsgCtx.setEnvelope(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope());
    }

    private void setContentTypeAndCharSetEncoding(MessageContext responseMsgCtx) {

        String contentType = httpResponse.getHeader(BridgeConstants.CONTENT_TYPE_HEADER);
        if (contentType == null) {
            // Server hasn't sent the header - Try to infer the content type
            contentType = inferContentType(responseMsgCtx);
        }

        if (contentType != null) {
            responseMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
            String charSetEncoding = BuilderUtil.getCharSetEncoding(contentType);
            responseMsgCtx.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                    contentType.indexOf("charset") > 0 ? charSetEncoding : MessageContext.DEFAULT_CHAR_SET_ENCODING);
        }
    }

    private String inferContentType(MessageContext responseMsgCtx) {

        // When the response from backend does not have a body(Content-Length is 0 )
        // and Content-Type is not set; ESB should not do any modification to the response and pass-through as it is.
        HttpHeaders headers = httpResponse.getHeaders();
        if (!checkIfResponseHaveBodyBasedOnContentLenAndTransferEncodingHeaders(headers, responseMsgCtx)) {
            return null;
        }

        // Try to get the content type from the axis configuration
        Parameter cTypeParam =
                requestMsgCtx.getConfigurationContext().getAxisConfiguration().getParameter(PassThroughConstants.CONTENT_TYPE);
        if (cTypeParam != null) {
            return cTypeParam.getValue().toString();
        }

        // If unable to determine the content type - Return application/octet-stream as the default value
        return PassThroughConstants.DEFAULT_CONTENT_TYPE;
    }

    private boolean checkIfResponseHaveBodyBasedOnContentLenAndTransferEncodingHeaders(HttpHeaders headers,
                                                                                       MessageContext responseMsgCtx) {

        String contentLengthHeader = headers.get(HTTP.CONTENT_LEN);
        boolean contentLengthHeaderPresent = contentLengthHeader != null;
        boolean transferEncodingHeaderPresent = headers.get(HTTP.TRANSFER_ENCODING) != null;

        if ((!contentLengthHeaderPresent && !transferEncodingHeaderPresent) || "0".equals(contentLengthHeader)) {
            responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
            return false;
        }
        return true;
    }

    /**
     * Perform cleanup of ClientWorker.
     */
    private void cleanup() {
        //clean threadLocal variables
        MessageContext.destroyCurrentMessageContext();
    }
}

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
 *
 */
package org.apache.synapse.transport.netty.sender;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.TargetConfiguration;
import org.apache.synapse.transport.netty.util.HttpUtils;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code HttpTargetResponseWorker} is the Thread which does the response processing.
 */
public class HttpTargetResponseWorker implements Runnable {

    private static final Log LOG = LogFactory.getLog(HttpTargetResponseWorker.class);

    private final HttpCarbonMessage httpResponse;
    private final MessageContext requestMsgCtx;
    private final TargetConfiguration targetConfiguration;

    HttpTargetResponseWorker(MessageContext requestMsgCtx, HttpCarbonMessage httpResponse,
                             TargetConfiguration targetConfiguration) {

        this.httpResponse = httpResponse;
        this.requestMsgCtx = requestMsgCtx;
        this.targetConfiguration = targetConfiguration;
    }

    @Override
    public void run() {

        if (handleResponseFlow(httpResponse.getHttpStatusCode())) {
            return;
        }

        MessageContext responseMsgCtx;
        try {
            responseMsgCtx = createResponseMessageContext();
        } catch (AxisFault ex) {
            return;
        }

        handleLocationHeader(responseMsgCtx);

        try {
            populateProperties(responseMsgCtx);
        } catch (AxisFault e) {
            LOG.error("Error occurred while setting the SOAP envelope to the response message context", e);
            cleanup();
            return;
        }

        try {
            // Handover message to the axis engine for processing
            AxisEngine.receive(responseMsgCtx);
        } catch (AxisFault ex) {
            LOG.error("Error occurred while processing response message through Axis2", ex);
            String errorMessage = "Fault processing response message through Axis2: " + ex.getMessage();
            responseMsgCtx.setProperty(
                    NhttpConstants.SENDING_FAULT, Boolean.TRUE);
            responseMsgCtx.setProperty(
                    NhttpConstants.ERROR_CODE, NhttpConstants.RESPONSE_PROCESSING_FAILURE);
            responseMsgCtx.setProperty(
                    NhttpConstants.ERROR_MESSAGE, errorMessage.split("\n")[0]);
            responseMsgCtx.setProperty(
                    NhttpConstants.ERROR_DETAIL, JavaUtils.stackToString(ex));
            responseMsgCtx.setProperty(
                    NhttpConstants.ERROR_EXCEPTION, ex);
            try {
                responseMsgCtx.getAxisOperation().getMessageReceiver().receive(responseMsgCtx);
            } catch (AxisFault axisFault) {
                LOG.error("Error occurred while processing fault response message through Axis2", ex);
            }
        } finally {
            cleanup();
        }
    }

    private MessageContext createResponseMessageContext() throws AxisFault {

        MessageContext responseMsgCtx;
        try {
            responseMsgCtx = requestMsgCtx.getOperationContext().getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        } catch (AxisFault ex) {
            LOG.error("Error getting the response message context from the operation context", ex);
            throw ex;
        }

        if (Objects.isNull(responseMsgCtx)) {
            if (requestMsgCtx.getOperationContext().isComplete()) {
                String msg = "Error getting IN message context from the operation context. "
                        + "Possibly an RM terminate sequence message";
                if (LOG.isDebugEnabled()) {
                    LOG.debug(msg);
                }
                throw new AxisFault(msg);
            }
            responseMsgCtx = new MessageContext();
            responseMsgCtx.setOperationContext(requestMsgCtx.getOperationContext());
        } else {
            // fix for RM to work because of a soapAction and wsaAction conflict
            responseMsgCtx.setSoapAction("");
        }
        return responseMsgCtx;
    }

    private boolean handleResponseFlow(int statusCode) {

        if (isStatusCode1xx(statusCode)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received a " + statusCode + " informational response.");
            }
            return true;
        }

        try {
            if (isStatusCode202(statusCode) && handle202(requestMsgCtx)) {
                return true;
            }
        } catch (AxisFault ex) {
            LOG.error("Error while handling the 202 response. ", ex);
            cleanup();
            return true;
        }
        return false;
    }

    /**
     * Populates required properties in the message context.
     *
     * @throws AxisFault if an error occurs while setting the SOAP envelope
     */
    private void populateProperties(MessageContext responseMsgCtx) throws AxisFault {

        responseMsgCtx.setServerSide(true);
        responseMsgCtx.setDoingREST(requestMsgCtx.isDoingREST());
        responseMsgCtx.setTransportIn(requestMsgCtx.getTransportIn());
        responseMsgCtx.setTransportOut(requestMsgCtx.getTransportOut());
        responseMsgCtx.setAxisMessage(requestMsgCtx.getOperationContext().getAxisOperation().
                getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        responseMsgCtx.setOperationContext(requestMsgCtx.getOperationContext());
        responseMsgCtx.setConfigurationContext(requestMsgCtx.getConfigurationContext());
        responseMsgCtx.setTo(null);

        // Set status code and reason phrase
        int statusCode = httpResponse.getHttpStatusCode();
        responseMsgCtx.setProperty(BridgeConstants.HTTP_SC, statusCode);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_SC_DESC, httpResponse.getReasonPhrase());
        responseMsgCtx.setProperty(BridgeConstants.HTTP_STATUS_CODE_SENT_FROM_BACKEND, statusCode);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_REASON_PHRASE_SENT_FROM_BACKEND,
                httpResponse.getReasonPhrase());

        // Set rest of the properties
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN,
                requestMsgCtx.getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setProperty(BridgeConstants.INVOKED_REST, requestMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES,
                requestMsgCtx.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES));
        responseMsgCtx.setProperty(BridgeConstants.NON_BLOCKING_TRANSPORT, true);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_CARBON_MESSAGE, httpResponse);
        responseMsgCtx.setProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE,
                requestMsgCtx.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE));
        responseMsgCtx.setProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION,
                requestMsgCtx.getProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION));

        // Set any transport headers received
        Map<String, String> headers = new HashMap<>();
        httpResponse.getHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        if (isStatusCode202(statusCode)) {
            responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES, Boolean.TRUE);
            responseMsgCtx.setProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED, Boolean.FALSE);
            responseMsgCtx.setProperty(BridgeConstants.SC_ACCEPTED, Boolean.TRUE);
        } else if (isErrorResponse(statusCode)) {
            responseMsgCtx.setProperty(BridgeConstants.FAULT_MESSAGE, BridgeConstants.TRUE);
        }

        if (isResponseExpectingBody(requestMsgCtx, statusCode)) {
            setContentTypeAndCharSetEncoding(responseMsgCtx);
            responseMsgCtx.setEnvelope(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope());
        } else {
            // there is no response entity-body
            responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
            responseMsgCtx.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
        }
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
                    contentType.indexOf("charset") > 0
                            ? charSetEncoding : MessageContext.DEFAULT_CHAR_SET_ENCODING);
        }
    }

    private boolean isResponseExpectingBody(MessageContext requestMsgCtx, int statusCode) {

        // According to RFC 7230 - HTTP/1.1 Message Syntax and Routing - Message Body Length, the following logic
        // was implemented.
        if (HttpUtils.isHEADRequest(requestMsgCtx)) {
            // Any response to a HEAD request
            return false;
        } else if (HttpUtils.isCONNECTRequest(requestMsgCtx)) {
            // Any 2xx (Successful) response to a CONNECT request
            return (statusCode / 100 != 2);
        }

        // Any response with a 1xx (Informational), 204 (No Content), or 304 (Not Modified) status code
        return statusCode >= HttpStatus.SC_OK
                && statusCode != HttpStatus.SC_NO_CONTENT
                && statusCode != HttpStatus.SC_NOT_MODIFIED
                && statusCode != HttpStatus.SC_RESET_CONTENT;
    }

    private String inferContentType(MessageContext responseMsgCtx) {

        // When the response from backend does not have a body(Content-Length is 0 )
        // and Content-Type is not set; ESB should not do any modification to the response and pass-through as it is.
        HttpHeaders headers = httpResponse.getHeaders();
        if (!checkIfResponseHaveBodyBasedOnContentLenAndTransferEncodingHeaders(headers, responseMsgCtx)) {
            return null;
        }

        // Try to get the content type from the axis configuration
        Parameter cTypeParam = requestMsgCtx.getConfigurationContext().getAxisConfiguration().getParameter(
                PassThroughConstants.CONTENT_TYPE);
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

        if ((!contentLengthHeaderPresent && !transferEncodingHeaderPresent)
                || "0".equals(contentLengthHeader)) {
            responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
            return false;
        }
        return true;
    }

    private boolean handle202(MessageContext requestMsgContext) throws AxisFault {

        if (requestMsgContext.isPropertyTrue(BridgeConstants.IGNORE_SC_ACCEPTED)) {
            // We should not further process this 202 response - Ignore it
            return true;
        }

        MessageReceiver mr = requestMsgContext.getAxisOperation().getMessageReceiver();
        MessageContext responseMsgContext = requestMsgCtx.getOperationContext()
                .getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        if (responseMsgContext == null || requestMsgContext.getOptions().isUseSeparateListener()) {
            // Most probably a response from a dual channel invocation
            // Inject directly into the SynapseCallbackReceiver
            requestMsgContext.setProperty(BridgeConstants.HTTP_202_RECEIVED, "true");
            mr.receive(requestMsgContext);
            return true;
        }
        return false;
    }

    private void handleLocationHeader(MessageContext responseMsgCtx) {
        // Special casing 301, 302, 303 and 307 scenario in following section. Not sure whether it's the correct fix,
        // but this fix makes it possible to do http --> https redirection.
        int statusCode = httpResponse.getHttpStatusCode();
        String originalURL = httpResponse.getHeader(BridgeConstants.LOCATION);

        if (originalURL != null && shouldRewriteLocationHeader(statusCode)) {
            URL url;
            String urlContext;
            try {
                url = new URL(originalURL);
                urlContext = url.getFile();
            } catch (MalformedURLException e) {
                //Fix ESBJAVA-3461 - In the case when relative path is sent should be handled
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Relative URL received for Location : " + originalURL, e);
                }
                urlContext = originalURL;
            }

            httpResponse.removeHeader(BridgeConstants.LOCATION);
            String servicePrefix = (String) requestMsgCtx.getProperty(BridgeConstants.SERVICE_PREFIX);
            if (servicePrefix != null) {
                if (urlContext == null) {
                    urlContext = "";
                } else if (urlContext.startsWith("/")) {
                    //Remove the preceding '/' character
                    urlContext = urlContext.substring(1);
                }
                httpResponse.setHeader(BridgeConstants.LOCATION, servicePrefix + urlContext);
            }
        }
        responseMsgCtx.setProperty(BridgeConstants.PRE_LOCATION_HEADER, originalURL);
    }

    private boolean shouldRewriteLocationHeader(int statusCode) {

        return !targetConfiguration.isPreserveHttpHeader(BridgeConstants.LOCATION)
                && !isStatusCode3xxRedirection(statusCode)
                && !isStatusCode201(statusCode);
    }

    private boolean isStatusCode1xx(int statusCode) {

        return statusCode / 100 == 1;
    }

    private boolean isStatusCode202(int statusCode) {

        return statusCode == HttpStatus.SC_ACCEPTED;
    }

    private boolean isStatusCode3xxRedirection(int statusCode) {

        return statusCode == HttpStatus.SC_MOVED_TEMPORARILY || statusCode == HttpStatus.SC_MOVED_PERMANENTLY
                || statusCode == HttpStatus.SC_SEE_OTHER || statusCode == HttpStatus.SC_TEMPORARY_REDIRECT;
    }

    private boolean isStatusCode201(int statusCode) {

        return statusCode == HttpStatus.SC_CREATED;
    }

    private boolean isErrorResponse(int statusCode) {

        return statusCode >= 400;
    }

    /**
     * Perform cleanup of ClientWorker.
     */
    private void cleanup() {
        //clean threadLocal variables
        MessageContext.destroyCurrentMessageContext();
    }
}

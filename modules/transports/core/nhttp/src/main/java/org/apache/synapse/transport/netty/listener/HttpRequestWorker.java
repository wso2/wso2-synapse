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
package org.apache.synapse.transport.netty.listener;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.commons.handlers.ConnectionId;
import org.apache.synapse.commons.handlers.HandlerResponse;
import org.apache.synapse.commons.handlers.MessageInfo;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.commons.handlers.MessagingHandlerConstants;
import org.apache.synapse.commons.handlers.Protocol;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.SourceConfiguration;
import org.apache.synapse.transport.netty.util.HttpUtils;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.wso2.transport.http.netty.contract.exceptions.ServerConnectorException;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpCarbonRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import static org.apache.synapse.transport.netty.BridgeConstants.CONTENT_TYPE_HEADER;
import static org.apache.synapse.transport.netty.BridgeConstants.SOAP_ACTION_HEADER;

/**
 * {@code HttpRequestWorker} is the Thread that does the request processing.
 */
public class HttpRequestWorker implements Runnable {

    private static final Log LOG = LogFactory.getLog(HttpRequestWorker.class);
    private final HttpCarbonMessage incomingCarbonMsg;
    private final MessageContext msgContext;
    private final ConfigurationContext configurationContext;
    private final SourceConfiguration sourceConfiguration;
    private boolean requestHasEntityBody;

    public HttpRequestWorker(HttpCarbonMessage incomingCarbonMsg, SourceConfiguration sourceConfiguration) {

        this.sourceConfiguration = sourceConfiguration;
        this.incomingCarbonMsg = incomingCarbonMsg;
        this.configurationContext = sourceConfiguration.getConfigurationContext();
        this.msgContext = RequestResponseUtils.convertCarbonMsgToAxis2MsgCtx(incomingCarbonMsg, sourceConfiguration);
    }

    @Override
    public void run() {

        // first, get the URI of the underlying HttpCarbonMessage and generate the service prefix
        // and add to the message context.
        processHttpRequestUri();

        // check if the request is to fetch wsdl. If so, return the message flow without going through the normal flow.
        if (isRequestToFetchWSDL()) {
            return;
        }

        try {
            populateProperties();

            // invoke MessagingHandlers for further processing of the message.
            invokeHandlers();

            AxisEngine.receive(msgContext);
        } catch (AxisFault ex) {
            handleException("Error processing " + incomingCarbonMsg.getHttpMethod()
                    + " request for : " + incomingCarbonMsg.getProperty(BridgeConstants.TO), ex);
        }
        sendAck();
        cleanup();
    }

    /**
     * Check if the request is a WSDL query by invoking the registered {@code HttpGetRequestProcessor} for this
     * transport.
     *
     * @return true if the request is a WSDL query, otherwise false
     */
    private boolean isRequestToFetchWSDL() {

        String method = incomingCarbonMsg.getHttpMethod();

        // WSDL queries are normally GET or HEAD requests. Therefore, we need to invoke the http GET request processor
        // for such requests to handle WSDL requests.
        if (PassThroughConstants.HTTP_GET.equals(method)
                || PassThroughConstants.HTTP_HEAD.equals(method)
                || PassThroughConstants.HTTP_OPTIONS.equals(method)) {

            sourceConfiguration.getHttpGetRequestProcessor().process(incomingCarbonMsg, msgContext, true);
        }

        // if this request is to fetch WSDL, then the WSDL_REQUEST_HANDLED property should be set to true
        // in the message context by the HttpGetRequestProcessor.
        return Boolean.TRUE.equals((msgContext.getProperty(BridgeConstants.WSDL_REQUEST_HANDLED)));
    }

    /**
     * Get the URI of underlying HttpCarbonMessage and generate the service prefix and add to the message context.
     */
    private void processHttpRequestUri() {

        String servicePrefixIndex = "://";
        msgContext.setProperty(Constants.Configuration.HTTP_METHOD, incomingCarbonMsg.getHttpMethod().toUpperCase());
        String oriUri = (String) incomingCarbonMsg.getProperty(BridgeConstants.TO);
        String restUrlPostfix = RequestResponseUtils.getRestUrlPostfix(oriUri, configurationContext.getServicePath());

        String servicePrefix = oriUri.substring(0, oriUri.indexOf(restUrlPostfix));
        if (!servicePrefix.contains(servicePrefixIndex)) {
            InetSocketAddress localAddress = (InetSocketAddress) incomingCarbonMsg
                    .getProperty(org.wso2.transport.http.netty.contract.Constants.LOCAL_ADDRESS);
            if (localAddress != null) {
                servicePrefix = incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.PROTOCOL)
                        + servicePrefixIndex + localAddress.getHostName() + ":"
                        + incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.LISTENER_PORT)
                        + servicePrefix;
            }
        }
        msgContext.setProperty(BridgeConstants.SERVICE_PREFIX, servicePrefix);
        msgContext.setTo(new EndpointReference(restUrlPostfix));
        msgContext.setProperty(BridgeConstants.REST_URL_POSTFIX, restUrlPostfix);
    }

    /**
     * Populates required properties in the message context.
     *
     * @throws AxisFault if an error occurs while setting the SOAP envelope
     */
    private void populateProperties() throws AxisFault {

        this.requestHasEntityBody = HttpUtils.requestHasEntityBody(incomingCarbonMsg);
        if (!requestHasEntityBody) {
            msgContext.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
        }

        // set ContentType, MessageType, and CHARACTER_SET_ENCODING properties
        setContentTypeMessageTypeAndCharacterEncoding();

        msgContext.setProperty(HTTPConstants.HTTP_METHOD, incomingCarbonMsg.getHttpMethod().toUpperCase());
        msgContext.setTo(new EndpointReference((String) incomingCarbonMsg.getProperty(BridgeConstants.TO)));

        String contentType = msgContext.getProperty(Constants.Configuration.CONTENT_TYPE).toString();
        String soapAction = incomingCarbonMsg.getHeaders().get(SOAP_ACTION_HEADER);
        int soapVersion = RequestResponseUtils.populateSOAPVersion(msgContext, contentType);

        if (RequestResponseUtils.isRESTRequest(msgContext, contentType, soapVersion, soapAction)) {
            msgContext.setProperty(PassThroughConstants.REST_REQUEST_CONTENT_TYPE, contentType);
            msgContext.setDoingREST(true);
        }

        setSOAPAction(soapAction);
        setSOAPEnvelope(soapVersion);
    }

    private void setSOAPAction(String soapAction) {

        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
            msgContext.setSoapAction(soapAction);
        }
    }

    private void setSOAPEnvelope(int soapVersion) throws AxisFault {

        SOAPEnvelope envelope;
        SOAPFactory fac;
        if (soapVersion == 1) {
            fac = OMAbstractFactory.getSOAP11Factory();
        } else {
            fac = OMAbstractFactory.getSOAP12Factory();
        }
        envelope = fac.getDefaultEnvelope();
        try {
            msgContext.setEnvelope(envelope);
        } catch (AxisFault axisFault) {
            LOG.error("Error occurred while setting the SOAP envelope to the request message context");
            throw axisFault;
        }
    }

    public void setContentTypeMessageTypeAndCharacterEncoding() {

        String contentTypeHeader = incomingCarbonMsg.getHeaders().get(CONTENT_TYPE_HEADER);
        String charSetEncoding;
        String contentType;
        String messageType;

        if (contentTypeHeader != null) {
            contentType = contentTypeHeader;
            if (HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals(contentTypeHeader)) {
                // if the Content-Type headers is application/x-www-form-urlencoded, then setting the message type as
                // application/xml.
                messageType = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
            } else {
                messageType = TransportUtils.getContentType(contentTypeHeader, msgContext);
            }
        } else {
            if (requestHasEntityBody) {
                Parameter param = sourceConfiguration.getConfigurationContext().getAxisConfiguration().
                        getParameter(BridgeConstants.DEFAULT_REQUEST_CONTENT_TYPE);
                if (param != null) {
                    contentType = param.getValue().toString();
                    messageType = contentType;
                } else {
                    // According to the RFC 7231 section 3.1.5.5, if the request containing a payload body does not
                    // have a Content-Type header field, then the recipient may assume a media type
                    // of "application/octet-stream"
                    contentType = BridgeConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM;
                    messageType = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
                }
            } else {
                String httpMethod = (String) this.msgContext.getProperty(BridgeConstants.HTTP_METHOD);
                if (HTTPConstants.HEADER_GET.equals(httpMethod) || HTTPConstants.HEADER_DELETE.equals(httpMethod)) {
                    contentType = HTTPConstants.MEDIA_TYPE_X_WWW_FORM;
                } else {
                    contentType = BridgeConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM;
                }
                messageType = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
            }
        }
        msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
        msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE, messageType);
        charSetEncoding = BuilderUtil.getCharSetEncoding(contentType);
        msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding);
    }

    private boolean isResponseWrittenOrSkipped() {

        String respWritten = (String) msgContext.getOperationContext().getProperty(
                Constants.RESPONSE_WRITTEN);
        return BridgeConstants.VALUE_TRUE.equals(respWritten) || "SKIP".equals(respWritten);
    }

    private boolean isSoapFault() {

        return msgContext.getProperty(BridgeConstants.FORCE_SOAP_FAULT) != null;
    }

    private boolean forceSCAccepted() {

        return msgContext.isPropertyTrue(BridgeConstants.FORCE_SC_ACCEPTED);
    }

    private boolean requestResponseTransportStatusEqualsToAcked() {

        RequestResponseTransport.RequestResponseTransportStatus transportStatus =
                ((RequestResponseTransport) msgContext.getProperty(RequestResponseTransport.TRANSPORT_CONTROL))
                        .getStatus();
        return RequestResponseTransport.RequestResponseTransportStatus.ACKED.equals(transportStatus);
    }

    private boolean nioAckRequested() {
        // TODO: check this
        return msgContext.isPropertyTrue(BridgeConstants.NIO_ACK_REQUESTED);
    }

    public boolean ackShouldSend() {

        return forceSCAccepted()
                || requestResponseTransportStatusEqualsToAcked()
                || nioAckRequested() ||
                !(isResponseWrittenOrSkipped() || isSoapFault());
    }

    /**
     * Sends a HTTP response to the client immediately after the current execution thread finishes, if the
     * 1. FORCE_SC_ACCEPTED property is true or
     * 2. A response is not written and no FORCE_SOAP_FAULT property is set or
     * 3. NIO-ACK-Requested property is set to true or
     * 4. RequestResponseTransportStatus is set to ACKED.
     */
    private void sendAck() {

        if (ackShouldSend()) {
            int statusCode;
            HttpResponseStatus responseStatus;
            if (!nioAckRequested()) {
                statusCode = HttpStatus.SC_ACCEPTED;
                responseStatus = HttpResponseStatus.ACCEPTED;
            } else {
                statusCode = Integer.parseInt(msgContext.getProperty(NhttpConstants.HTTP_SC).toString());
                responseStatus = HttpResponseStatus.valueOf(statusCode);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending ACK response with status " + statusCode + ", for MessageID : "
                        + msgContext.getMessageID());
            }
            sendResponse(statusCode, responseStatus, false, false, null, null);
        }
    }

    private void sendResponse(int statusCode, HttpResponseStatus responseStatus, boolean disableKeepAlive,
                              boolean contentAvailable, String content, String contentType) {

        HttpCarbonMessage clientRequest =
                (HttpCarbonRequest) this.msgContext.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE);

        HttpCarbonMessage outboundResponse;
        try {
            HttpVersion version = HttpVersion.HTTP_1_1;
            if (BridgeConstants.HTTP_2_0_VERSION.equals(sourceConfiguration.getProtocol())) {
                version = new HttpVersion(BridgeConstants.HTTP_2_0, true);
                disableKeepAlive = false;
            }
            outboundResponse = new HttpCarbonMessage(new DefaultHttpResponse(version, responseStatus));
            outboundResponse.setHttpStatusCode(statusCode);
            if (disableKeepAlive) {
                outboundResponse.setKeepAlive(false);
            }
            clientRequest.respond(outboundResponse);

        } catch (ServerConnectorException e) {
            LOG.error("Error occurred while submitting the Ack to the client", e);
            return;
        }

        if (!contentAvailable) {
            try {
                OutputStream messageOutputStream = HttpUtils.getHttpMessageDataStreamer(outboundResponse)
                        .getOutputStream();
                HttpUtils.writeEmptyBody(messageOutputStream);
            } catch (AxisFault e) {
                LOG.error("Error occurred while writing the Ack to the client", e);
            }
            return;
        }

        outboundResponse.setHeader(HTTP.CONTENT_TYPE, contentType);
        try (OutputStream outputStream =
                     HttpUtils.getHttpMessageDataStreamer(outboundResponse).getOutputStream()) {
            outputStream.write(content.getBytes());
        } catch (IOException ioException) {
            LOG.error("Error occurred while writing the response body to the client", ioException);
        }
    }

    private void handleException(String msg, Exception e) {

        if (Objects.isNull(e)) {
            LOG.error(msg);
            e = new Exception(msg);
        } else {
            LOG.error(msg, e);
        }

        try {
            MessageContext faultContext = MessageContextBuilder.createFaultMessageContext(msgContext, e);
            msgContext.setProperty(PassThroughConstants.FORCE_SOAP_FAULT, Boolean.TRUE);
            AxisEngine.sendFault(faultContext);

        } catch (Exception ex) {
            String body = "<html><body><h1>Failed to process the request</h1>"
                    + "<p>" + msg + "</p></body></html>";

            sendResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    false, true, body, "text/html");
        }
    }

    /**
     * {@code MessagingHandler} is an extension point to intercept the inbound HTTP request for further processing.
     * This invokes the {@code handleSourceRequest} method of all the registered MessagingHandler instances to handle
     * the inbound request before going to the mediation flow.
     *
     * @return whether flow should continue further
     */
    private boolean invokeHandlers() {

        List<MessagingHandler> messagingHandlers = sourceConfiguration.getMessagingHandlers();
        if (Objects.isNull(messagingHandlers) || messagingHandlers.isEmpty()) {
            return true;
        }

        Protocol protocol;
        if (sourceConfiguration.getScheme().isSSL()) {
            protocol = Protocol.HTTPS;
        } else {
            protocol = Protocol.HTTP;
        }

        MessageInfo message = new MessageInfo(incomingCarbonMsg, protocol,
                new ConnectionId(incomingCarbonMsg.getSourceContext().channel().id().asShortText()));
        msgContext.setProperty(MessagingHandlerConstants.HANDLER_MESSAGE_CONTEXT, message);

        for (MessagingHandler handler: messagingHandlers) {
            HandlerResponse response = handler.handleRequest(msgContext);
            if (Objects.nonNull(response) && response.isError()) {
                LOG.error("Source request validation failed. " + response.getErrorResponseString());
                sendResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR, true,
                        false, null, null);
                return false;
            }
        }
        return true;
    }

    /**
     * Perform cleanup of HttpRequestWorker.
     */
    private void cleanup() {
        //clean threadLocal variables
        MessageContext.destroyCurrentMessageContext();
    }
}

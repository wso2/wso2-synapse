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

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.NettyConfiguration;
import org.apache.synapse.transport.netty.config.TargetConfiguration;
import org.apache.synapse.transport.netty.util.HttpUtils;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.transport.passthru.util.TargetRequestFactory;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.config.ChunkConfig;
import org.wso2.transport.http.netty.contract.config.KeepAliveConfig;
import org.wso2.transport.http.netty.contract.config.SenderConfiguration;
import org.wso2.transport.http.netty.contractimpl.sender.channel.BootstrapConfiguration;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.ConnectionManager;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

/**
 * {@code TargetRequestHandler} have utilities for creating and preparing an outbound request to be sent
 * to the backend service.
 */
public class TargetRequestHandler {

    private static final Log LOG = LogFactory.getLog(TargetRequestHandler.class);

    /**
     * Creates outbound request to be sent to the Backend service.
     *
     * @param url                 URL of the backend service
     * @param msgContext          axis2 message context
     * @param targetConfiguration configurations of the Transport Sender
     * @return the outbound request HttpCarbonMessage
     */
    public static HttpCarbonMessage createOutboundRequestMsg(URL url, MessageContext msgContext,
                                                             TargetConfiguration targetConfiguration)
            throws AxisFault {

        HttpCarbonMessage outboundRequest = new HttpCarbonMessage(
                new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, ""));
        prepareOutboundRequest(url, outboundRequest, msgContext, targetConfiguration);
        return outboundRequest;
    }

    private static void prepareOutboundRequest(URL url, HttpCarbonMessage outboundRequest, MessageContext msgContext,
                                               TargetConfiguration targetConfiguration) throws AxisFault {

        try {
            int port = getOutboundReqPort(url);
            String host = url.getHost();

            setOutboundReqProperties(outboundRequest, url, port, host, msgContext);
            setOutboundReqHeaders(outboundRequest, port, host, msgContext, targetConfiguration);

        } catch (MalformedURLException e) {
            RequestResponseUtils.handleException("Malformed URL in the target EPR.", e);
        } catch (IOException e) {
            RequestResponseUtils.handleException("Failed to prepare the outbound request.", e);
        }
    }

    private static void setOutboundReqProperties(HttpCarbonMessage outboundRequest, URL url, int port, String host,
                                                 MessageContext msgContext) throws IOException {

        setHTTPMethod(msgContext, outboundRequest);
        outboundRequest.setProperty(BridgeConstants.HTTP_HOST, host);
        outboundRequest.setProperty(BridgeConstants.HTTP_PORT, port);
        outboundRequest.setProperty(BridgeConstants.TO, getOutboundReqPath(url, msgContext));
        outboundRequest.setProperty(BridgeConstants.PROTOCOL, url.getProtocol());
        outboundRequest.setProperty(BridgeConstants.NO_ENTITY_BODY, HttpUtils.isNoEntityBodyRequest(msgContext));
    }

    private static void setOutboundReqHeaders(HttpCarbonMessage outboundRequest, int port, String host,
                                              MessageContext msgContext, TargetConfiguration targetConfiguration)
            throws AxisFault {

        HttpHeaders headers = outboundRequest.getHeaders();
        HttpUtils.removeUnwantedHeadersFromInternalTransportHeadersMap(msgContext, targetConfiguration);
        HttpUtils.addTransportHeadersToTransportMessage(headers, msgContext);
        setContentTypeHeaderIfApplicable(msgContext, outboundRequest, targetConfiguration);
        setWSAActionIfApplicable(msgContext, headers);
        HttpUtils.setHostHeader(host, port, headers, msgContext,
                targetConfiguration.isPreserveHttpHeader(HTTPConstants.HEADER_HOST));
        setOutboundUserAgent(headers);
    }

    private static void setHTTPMethod(MessageContext msgCtx, HttpCarbonMessage outboundRequest) {

        String httpMethod = (String) msgCtx.getProperty(BridgeConstants.HTTP_METHOD);
        if (Objects.isNull(httpMethod)) {
            httpMethod = HTTPConstants.HTTP_METHOD_POST;
        }
        outboundRequest.setHttpMethod(httpMethod);
    }

    private static int getOutboundReqPort(URL url) {

        int port = 80;
        if (url.getPort() != -1) {
            port = url.getPort();
        } else if (url.getProtocol().equalsIgnoreCase(Constants.HTTPS_SCHEME)) {
            port = 443;
        }
        return port;
    }

    private static String getOutboundReqPath(URL url, MessageContext msgCtx) throws IOException {

        if (HttpUtils.isGETRequest(msgCtx) || (RelayUtils.isDeleteRequestWithoutPayload(msgCtx))) {
            MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgCtx);
            OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgCtx);

            if (formatter != null) {
                URL targetURL = formatter.getTargetAddress(msgCtx, format, url);
                if (targetURL != null && !targetURL.toString().isEmpty()) {
                    if (msgCtx.isPropertyTrue(BridgeConstants.POST_TO_URI)) {
                        return targetURL.toString();
                    } else {
                        return targetURL.getPath()
                                + ((targetURL.getQuery() != null && !targetURL.getQuery().isEmpty())
                                ? ("?" + targetURL.getQuery())
                                : "");
                    }
                }
            }
        }

        if (msgCtx.isPropertyTrue(BridgeConstants.POST_TO_URI)) {
            return url.toString();
        }

        // TODO: need to check "(route.getProxyHost() != null && !route.isTunnelled())" as well
        return msgCtx.isPropertyTrue(BridgeConstants.FULL_URI)
                ? url.toString() : url.getPath() + (url.getQuery() != null ? "?" + url.getQuery() : "");
    }

    private static void setOutboundUserAgent(HttpHeaders headers) {

        if (!headers.contains(HttpHeaderNames.USER_AGENT)) {
            headers.set(HttpHeaderNames.USER_AGENT, BridgeConstants.DEFAULT_OUTBOUND_USER_AGENT);
        }
    }

    private static void setContentTypeHeaderIfApplicable(MessageContext msgCtx, HttpCarbonMessage outboundRequest,
                                                         TargetConfiguration targetConfiguration)
            throws AxisFault {

        Map transportHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (transportHeaders != null) {
            String trpContentType = (String) transportHeaders.get(HTTP.CONTENT_TYPE);
            if (trpContentType != null && !trpContentType.equals("")) {
                if (!TargetRequestFactory.isMultipartContent(trpContentType) && !msgCtx.isDoingSwA()) {
                    outboundRequest.setHeader(HTTP.CONTENT_TYPE, trpContentType);
                    return;
                }
            }
        }

        String cType = getContentType(msgCtx,
                targetConfiguration.isPreserveHttpHeader(HTTP.CONTENT_TYPE), transportHeaders);
        if (cType != null
                && !HTTPConstants.HTTP_METHOD_GET.equals((String) msgCtx.getProperty(BridgeConstants.HTTP_METHOD))
                && shouldOverwriteContentType(msgCtx, outboundRequest)) {
            String messageType = (String) msgCtx.getProperty(NhttpConstants.MESSAGE_TYPE);
            if (messageType != null) {
                // if multipart related message type and unless if message
                // not get build we should
                // skip of setting formatter specific content Type
                if (!messageType.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED)
                        && !messageType.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA)) {
                    outboundRequest.setHeader(HTTP.CONTENT_TYPE, cType);
                } else {
                    // if messageType is related to multipart and if message
                    // already built we need to set new
                    // boundary related content type at Content-Type header
                    boolean builderInvoked = Boolean.TRUE.equals(msgCtx
                            .getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));
                    if (builderInvoked) {
                        outboundRequest.setHeader(HTTP.CONTENT_TYPE, cType);
                    }
                }
            } else {
                outboundRequest.setHeader(HTTP.CONTENT_TYPE, cType);
            }
        }

        if ((PassThroughConstants.HTTP_GET.equals(msgCtx.getProperty(BridgeConstants.HTTP_METHOD))) ||
                (RelayUtils.isDeleteRequestWithoutPayload(msgCtx))) {
            MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgCtx);
            if (formatter != null) {
                outboundRequest.removeHeader(HTTP.CONTENT_TYPE);
            }
        }
    }

    public static String getContentType(MessageContext msgCtx, boolean isContentTypePreservedHeader,
                                        Map trpHeaders) throws AxisFault {

        String setEncoding = (String) msgCtx.getProperty(PassThroughConstants.SET_CHARACTER_ENCODING);

        // If incoming transport isn't HTTP, transport headers can be null. Therefore null check is required
        // and if headers not null check whether request comes with Content-Type header before preserving Content-Type
        // Need to avoid this for multipart headers, need to add MIME Boundary property
        if (trpHeaders != null
                && (trpHeaders).get(HTTPConstants.HEADER_CONTENT_TYPE) != null
                && (isContentTypePreservedHeader || PassThroughConstants.VALUE_FALSE.equals(setEncoding))
                && !RequestResponseUtils.isMultipartContent((trpHeaders).get(HTTPConstants.HEADER_CONTENT_TYPE)
                .toString())) {
            if (msgCtx.getProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE) != null) {
                return (String) msgCtx.getProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE);
            } else if (msgCtx.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE) != null) {
                return (String) msgCtx.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE);
            }
        }

        MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgCtx);
        OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgCtx);

        if (formatter != null) {
            return formatter.getContentType(msgCtx, format, msgCtx.getSoapAction());

        } else {
            String contentType = (String) msgCtx.getProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE);
            if (contentType != null) {
                return contentType;
            } else {
                return new SOAPMessageFormatter().getContentType(
                        msgCtx, format, msgCtx.getSoapAction());
            }
        }
    }

    /**
     * Check whether the we should overwrite the content type for the outgoing request.
     *
     * @param msgContext MessageContext
     * @return whether to overwrite the content type for the outgoing request
     */
    public static boolean shouldOverwriteContentType(MessageContext msgContext, HttpCarbonMessage outboundRequest) {

        boolean builderInvoked = Boolean.TRUE.equals(msgContext
                .getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));
        boolean noEntityBodySet =
                Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY));

        // if contentTypeInRequest is true, that means it is set from the transport headers. that means the header
        // came in the source request.
        boolean contentTypeInRequest = outboundRequest.getHeader("Content-Type") != null
                || outboundRequest.getHeader("content-type") != null;
        boolean isDefaultContentTypeEnabled = false;
        ConfigurationContext configurationContext = msgContext.getConfigurationContext();
        if (configurationContext != null && configurationContext.getAxisConfiguration()
                .getParameter(NhttpConstants.REQUEST_CONTENT_TYPE) != null) {
            isDefaultContentTypeEnabled = true;
        }
        // If builder is not invoked, which means the passthrough scenario, we should overwrite the content-type
        // depending on the presence of the incoming content-type.
        // If builder is invoked and no entity body property is not set (which means there is a payload in the request)
        // we should consider overwriting the content-type.
        return (builderInvoked && !noEntityBodySet) || contentTypeInRequest || isDefaultContentTypeEnabled;
    }

    private static void setWSAActionIfApplicable(MessageContext msgCtx, HttpHeaders headers) {

        String soapAction = msgCtx.getSoapAction();
        if (soapAction == null) {
            soapAction = msgCtx.getWSAAction();
        }

        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgCtx);
        if (msgCtx.isSOAP11() && Objects.nonNull(soapAction) && !soapAction.isEmpty()
                && Objects.nonNull(messageFormatter)) {
            headers.add(HTTPConstants.HEADER_SOAP_ACTION, messageFormatter.formatSOAPAction(msgCtx, null, soapAction));
        }
    }

    public static HttpClientConnector createHttpClient(URL url, MessageContext msgContext,
                                                       HttpWsConnectorFactory httpWsConnectorFactory,
                                                       ConnectionManager connectionManager,
                                                       BootstrapConfiguration bootstrapConfiguration,
                                                       TargetConfiguration targetConfiguration) throws AxisFault {

        try {
            SenderConfiguration senderConfiguration = new SenderConfiguration();
            populateSenderConfigurations(msgContext, senderConfiguration, targetConfiguration, url);

            return httpWsConnectorFactory.createHttpClientConnector(bootstrapConfiguration, senderConfiguration,
                    connectionManager);

        } catch (Exception ex) {
            throw new AxisFault("Error while creating the HTTP Client Connector. ", ex);
        }
    }

    public static void populateSenderConfigurations(MessageContext msgContext,
                                                    SenderConfiguration senderConfiguration,
                                                    TargetConfiguration targetConfiguration,
                                                    URL url) throws AxisFault {

        String scheme = url.getProtocol() != null ? url.getProtocol() : BridgeConstants.PROTOCOL_HTTP;
        senderConfiguration.setScheme(scheme);

        String httpVersion = BridgeConstants.HTTP_1_1_VERSION;
        String forceHttp10 = (String) msgContext.getProperty(PassThroughConstants.FORCE_HTTP_1_0);
        if (BridgeConstants.VALUE_TRUE.equalsIgnoreCase(forceHttp10)) {
            httpVersion = BridgeConstants.HTTP_1_0_VERSION;
        }
        senderConfiguration.setHttpVersion(httpVersion);

        if (isClientEndpointChunkingEnabled(msgContext)) {
            senderConfiguration.setChunkingConfig(ChunkConfig.ALWAYS);
        } else {
            senderConfiguration.setChunkingConfig(ChunkConfig.NEVER);
        }

        if (isClientEndpointKeepAliveDisabled(msgContext)) {
            senderConfiguration.setKeepAliveConfig(KeepAliveConfig.NEVER);
        } else {
            senderConfiguration.setKeepAliveConfig(KeepAliveConfig.ALWAYS);
        }

        senderConfiguration.setHttpTraceLogEnabled(targetConfiguration.isHttpTraceLogEnabled());

        // Set Request validation limits.
        boolean isRequestLimitsValidationEnabled = targetConfiguration.isRequestLimitsValidationEnabled();
        if (isRequestLimitsValidationEnabled) {
            RequestResponseUtils.setInboundMgsSizeValidationConfig(
                    targetConfiguration.getClientRequestMaxStatusLineLength(),
                    targetConfiguration.getClientRequestMaxHeaderSize(),
                    targetConfiguration.getClientRequestMaxEntityBodySize(),
                    senderConfiguration.getMsgSizeValidationConfig());
        }

        senderConfiguration.setSocketIdleTimeout(targetConfiguration.getSocketTimeout() * 1000);

        if (BridgeConstants.PROTOCOL_HTTPS.equals(scheme)) {
            targetConfiguration.getClientSSLConfigurationBuilder().setClientSSLConfig(senderConfiguration);
        }
    }

    private static boolean isClientEndpointChunkingEnabled(MessageContext msgContext) {

        if (msgContext.isPropertyTrue(NhttpConstants.FORCE_HTTP_CONTENT_LENGTH)) {
            return false;
        } else {
            String disableChunking = (String) msgContext.getProperty(PassThroughConstants.DISABLE_CHUNKING);
            return !BridgeConstants.VALUE_TRUE.equals(disableChunking)
                    && !BridgeConstants.VALUE_TRUE.equals(msgContext.getProperty(PassThroughConstants.FORCE_HTTP_1_0));
        }
    }

    private static boolean isClientEndpointKeepAliveDisabled(MessageContext msgContext) {

        String noKeepAliveProperty = (String) msgContext.getProperty(BridgeConstants.NO_KEEPALIVE);
        if (Objects.nonNull(noKeepAliveProperty)) {
            return msgContext.isPropertyTrue(noKeepAliveProperty);
        }

        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null) {
            Object connectionHeader = transportHeaders.get(HTTP.CONN_DIRECTIVE);
            if (Objects.nonNull(connectionHeader) && "close".equalsIgnoreCase(connectionHeader.toString())) {
                return true;
            }
        }

        return NettyConfiguration.getInstance().isKeepAliveDisabled();
    }

    public static void sendRequest(HttpClientConnector clientConnector, HttpCarbonMessage outboundRequestMsg,
                                   MessageContext msgContext, TargetConfiguration targetConfiguration)
            throws AxisFault {

        sendOutboundRequest(clientConnector, outboundRequestMsg, msgContext, targetConfiguration);
        serializeData(msgContext, outboundRequestMsg);
    }

    private static void sendOutboundRequest(HttpClientConnector clientConnector,
                                            HttpCarbonMessage outboundRequestMsg,
                                            MessageContext msgContext,
                                            TargetConfiguration targetConfiguration) {

        HttpResponseFuture future = clientConnector.send(outboundRequestMsg);
        future.setHttpConnectorListener(new Axis2HttpTargetRespListener(targetConfiguration.getWorkerPool(),
                msgContext, targetConfiguration));

    }

    private static void serializeData(MessageContext msgCtx, HttpCarbonMessage responseMsg)
            throws AxisFault {

        if (ignoreMessageBody(msgCtx)) {
            OutputStream messageOutputStream = HttpUtils.getHttpMessageDataStreamer(responseMsg).getOutputStream();
            HttpUtils.writeEmptyBody(messageOutputStream);
        } else {
            if (RequestResponseUtils.shouldInvokeFormatterToWriteBody(msgCtx)) {
                HttpMessageDataStreamer outboundMsgDataStreamer = HttpUtils.getHttpMessageDataStreamer(responseMsg);
                OutputStream messageOutputStream = outboundMsgDataStreamer.getOutputStream();
                MessageFormatter messageFormatter =
                        MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgCtx);
                if (Objects.nonNull(messageFormatter)) {
                    HttpUtils.serializeDataUsingMessageFormatter(msgCtx, messageFormatter, messageOutputStream);
                } else {
                    LOG.warn("Could not serialize the message. No available formatter to write the "
                            + "message to the backend.");
                }
            } else {
                HttpCarbonMessage inboundCarbonMessage =
                        (HttpCarbonMessage) msgCtx.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE);
                HttpUtils.copyContentFromInboundHttpCarbonMessage(inboundCarbonMessage, responseMsg);
            }
        }
    }

    /**
     * Checks if we can ignore the message body.
     *
     * @param msgContext axis2 message context
     * @return whether we can ignore the message body
     */
    private static boolean ignoreMessageBody(MessageContext msgContext) {

        return HttpUtils.isGETRequest(msgContext) || RelayUtils.isDeleteRequestWithoutPayload(msgContext);
    }
}

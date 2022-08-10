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
package org.apache.synapse.transport.netty.util;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.BaseConfiguration;
import org.apache.synapse.transport.netty.config.NettyConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contract.exceptions.ServerConnectorException;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.ConnectionManager;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.PoolConfiguration;
import org.wso2.transport.http.netty.message.Http2PushPromise;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;
import org.wso2.transport.http.netty.message.PooledDataStreamerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class providing HTTP utility methods.
 */
public class HttpUtils {

    private static final Log LOG = LogFactory.getLog(HttpUtils.class);

    public static ConnectionManager getConnectionManager() {

        PoolConfiguration poolConfiguration = new PoolConfiguration();
        if (NettyConfiguration.getInstance().isCustomConnectionPoolConfigsEnabled()) {
            populatePoolingConfig(poolConfiguration);
        }
        return new ConnectionManager(poolConfiguration);
    }

    public static void populatePoolingConfig(PoolConfiguration poolConfiguration) {

        NettyConfiguration globalConf = NettyConfiguration.getInstance();
        poolConfiguration.setMaxActivePerPool(globalConf.getConnectionPoolingMaxActiveConnections());
        poolConfiguration.setMaxIdlePerPool(globalConf.getConnectionPoolingMaxIdleConnections());
        poolConfiguration.setMaxWaitTime((long) globalConf.getConnectionPoolingWaitTime() * 1000);
    }

    /**
     * RRemove unwanted headers from the http response of outgoing request. These are headers which
     * should be dictated by the transport and not by the user. We remove these as these may get
     * copied from the request messages.
     *
     * @param msgContext        axis2 message context
     * @param baseConfiguration configuration that has all the preserved header details
     */
    public static void removeUnwantedHeadersFromInternalTransportHeadersMap(MessageContext msgContext,
                                                                            BaseConfiguration baseConfiguration) {

        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (Objects.nonNull(transportHeaders) && !transportHeaders.isEmpty()) {
            Iterator iter = transportHeaders.keySet().iterator();
            while (iter.hasNext()) {
                String headerName = (String) iter.next();
                if (HTTP.TRANSFER_ENCODING.equalsIgnoreCase(headerName)) {
                    iter.remove();
                }

                if (HTTP.CONN_DIRECTIVE.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.CONN_DIRECTIVE)) {
                    iter.remove();
                }

                if (HTTP.CONN_KEEP_ALIVE.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.CONN_KEEP_ALIVE)) {
                    iter.remove();
                }

                if (HTTP.CONTENT_LEN.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.CONTENT_LEN)) {
                    iter.remove();
                }

                if (HTTP.DATE_HEADER.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.DATE_HEADER)) {
                    iter.remove();
                }

                if (HTTP.SERVER_HEADER.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.SERVER_HEADER)) {
                    iter.remove();
                }

                if (HTTP.USER_AGENT.equalsIgnoreCase(headerName)
                        && !baseConfiguration.isPreserveHttpHeader(HTTP.USER_AGENT)) {
                    iter.remove();
                }

                if (HTTP.TARGET_HOST.equalsIgnoreCase(headerName)) {
                    iter.remove();
                }
            }
        }
    }

    public static boolean isFaultMessage(MessageContext msgContext) {

        return msgContext.getEnvelope() != null
                && (msgContext.getEnvelope().getBody().hasFault() || msgContext.isProcessingFault());
    }

    public static boolean sendFaultAsHTTP200(MessageContext msgContext) {

        Object faultsAsHttp200Property = msgContext.getProperty(BridgeConstants.FAULTS_AS_HTTP_200);
        return Objects.nonNull(faultsAsHttp200Property) && "true".equalsIgnoreCase(faultsAsHttp200Property.toString());
    }

    /**
     * Checks if the given HttpCarbonMessage has an entity body.
     *
     * @param httpCarbonMessage HttpCarbonMessage in which we need to check if an entity body is present
     * @return true if the HttpCarbonMessage has an entity body enclosed
     */
    public static boolean requestHasEntityBody(HttpCarbonMessage httpCarbonMessage) {

        // TODO: check for an alternative
        long contentLength = BridgeConstants.NO_CONTENT_LENGTH_FOUND;
        String lengthStr = httpCarbonMessage.getHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
        try {
            contentLength = lengthStr != null ? Long.parseLong(lengthStr) : contentLength;
            if (contentLength == BridgeConstants.NO_CONTENT_LENGTH_FOUND) {
                //Read one byte to make sure the incoming stream has data
                contentLength = httpCarbonMessage.countMessageLengthTill(BridgeConstants.ONE_BYTE);
            }
        } catch (NumberFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid content length found while checking the content length of the request entity body");
            }
        }
        return contentLength > 0;
    }

    /**
     * Invokes {@code HttpResponseFuture} respond method to send the response back to the client.
     *
     * @param requestMsg  Represent the request message
     * @param responseMsg Represent the corresponding response
     * @return HttpResponseFuture that represent the future results
     */
    public static HttpResponseFuture sendOutboundResponse(HttpCarbonMessage requestMsg,
                                                          HttpCarbonMessage responseMsg) throws AxisFault {

        HttpResponseFuture responseFuture;
        try {
            responseFuture = requestMsg.respond(responseMsg);
        } catch (ServerConnectorException e) {
            throw new AxisFault("Error occurred while submitting the response to the client", e);
        }
        return responseFuture;
    }

    /**
     * Send the server push promises to the client.
     *
     * @param pushPromise Http2PushPromise
     * @param requestMsg  HttpCarbonMessage
     * @return HttpResponseFuture
     * @throws AxisFault if error occurred while sending server pushes
     */
    public static HttpResponseFuture pushPromise(Http2PushPromise pushPromise, HttpCarbonMessage requestMsg) throws AxisFault {

        HttpResponseFuture responseFuture;
        try {
            responseFuture = requestMsg.pushPromise(pushPromise);
        } catch (ServerConnectorException e) {
            throw new AxisFault("Error occurred while sending push promise", e);
        }
        return responseFuture;
    }

    /**
     * Send the promised server push responses to the client.
     *
     * @param http2PushPromise Http2PushPromise
     * @param outboundPushMsg  HttpCarbonMessage
     * @param requestMsg       HttpCarbonMessage
     * @return HttpResponseFuture
     * @throws AxisFault if error occurred while sending the server push responses
     */
    public static HttpResponseFuture pushResponse(Http2PushPromise http2PushPromise,
                                                  HttpCarbonMessage outboundPushMsg, HttpCarbonMessage requestMsg) throws AxisFault {

        HttpResponseFuture responseFuture;
        try {
            responseFuture = requestMsg.pushResponse(outboundPushMsg, http2PushPromise);
        } catch (ServerConnectorException e) {
            throw new AxisFault("Error occurred while sending server push", e);
        }
        return responseFuture;
    }

    /**
     * Get the response data streamer that should be used for serializing data.
     *
     * @param outboundResponse Represents native response
     * @return HttpMessageDataStreamer that should be used for serializing
     */
    public static HttpMessageDataStreamer getHttpMessageDataStreamer(HttpCarbonMessage outboundResponse) {

        final HttpMessageDataStreamer outboundMsgDataStreamer;
        final PooledDataStreamerFactory pooledDataStreamerFactory = (PooledDataStreamerFactory)
                outboundResponse.getProperty(BridgeConstants.POOLED_BYTE_BUFFER_FACTORY);
        if (pooledDataStreamerFactory != null) {
            outboundMsgDataStreamer = pooledDataStreamerFactory.createHttpDataStreamer(outboundResponse);
        } else {
            outboundMsgDataStreamer = new HttpMessageDataStreamer(outboundResponse);
        }
        return outboundMsgDataStreamer;
    }

    public static void serializeDataUsingMessageFormatter(MessageContext msgContext, MessageFormatter messageFormatter,
                                                          OutputStream outputStream) throws AxisFault {

        OMOutputFormat format = MessageUtils.getOMOutputFormat(msgContext);
        try {
            messageFormatter.writeTo(msgContext, format, outputStream, false);
        } catch (AxisFault e) {
            RequestResponseUtils.handleException("Error occurred while serializing the message body.", e);
        } finally {
            HttpUtils.closeMessageOutputStreamQuietly(outputStream);
        }
    }

    public static void serializeBytes(OutputStream outputStream, byte[] bytes) throws AxisFault {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            RequestResponseUtils.handleException("Error occurred while serializing the message body.", e);
        } finally {
            HttpUtils.closeMessageOutputStreamQuietly(outputStream);
        }
    }

    public static void writeEmptyBody(OutputStream outputStream) throws AxisFault {
        serializeBytes(outputStream, new byte[0]);
    }

    public static void copyContentFromInboundHttpCarbonMessage(HttpCarbonMessage inboundMsg,
                                                               HttpCarbonMessage outboundResponseMsg) {

        do {
            HttpContent httpContent = inboundMsg.getHttpContent();
            outboundResponseMsg.addHttpContent(httpContent);
            if (httpContent instanceof LastHttpContent) {
                break;
            }
        } while (true);
    }

    public static void closeMessageOutputStreamQuietly(OutputStream messageOutputStream) {

        try {
            if (messageOutputStream != null) {
                messageOutputStream.close();
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Couldn't close the message output stream: " + e.getMessage());
            }
        }
    }

    public static boolean isGETRequest(MessageContext msgCtx) {

        Object httpMethod = msgCtx.getProperty(BridgeConstants.HTTP_METHOD);
        if (Objects.nonNull(httpMethod)) {
            return BridgeConstants.HTTP_GET.equalsIgnoreCase(httpMethod.toString());
        }
        return false;
    }

    public static boolean isHEADRequest(MessageContext msgCtx) {

        Object httpMethod = msgCtx.getProperty(BridgeConstants.HTTP_METHOD);
        if (Objects.nonNull(httpMethod)) {
            return BridgeConstants.HTTP_HEAD.equalsIgnoreCase(httpMethod.toString());
        }
        return false;
    }

    public static boolean isCONNECTRequest(MessageContext msgCtx) {

        Object httpMethod = msgCtx.getProperty(BridgeConstants.HTTP_METHOD);
        if (Objects.nonNull(httpMethod)) {
            return BridgeConstants.HTTP_CONNECT.equalsIgnoreCase(httpMethod.toString());
        }
        return false;
    }

    public static boolean isNoEntityBodyRequest(MessageContext msgCtx) {

        if (HttpUtils.isGETRequest(msgCtx) || (RelayUtils.isDeleteRequestWithoutPayload(msgCtx))) {
            return true;
        }
        return !hasEntityBody(msgCtx);
    }

    private static boolean hasEntityBody(MessageContext msgCtx) {

        if (msgCtx.getEnvelope().getBody().getFirstElement() != null) {
            return true;
        }
        return !msgCtx.isPropertyTrue(BridgeConstants.NO_ENTITY_BODY);
    }

    public static void addTransportHeadersToTransportMessage(HttpHeaders headers, MessageContext msgCtx) {

        Map transportHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null) {
            for (Object entryObj : transportHeaders.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObj;
                if (entry.getValue() != null && entry.getKey() instanceof String &&
                        entry.getValue() instanceof String) {
                    headers.add((String) entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static void setHostHeader(String host, int port, HttpHeaders headers, MessageContext msgCtx,
                                     boolean isPreservedHeader) {

        if (headers.contains(HttpHeaderNames.HOST) && isPreservedHeader) {
            return;
        }
        // If REQUEST_HOST_HEADER property is defined, the value of this property will be set as the
        // HTTP host header of outgoing request.
        if (msgCtx.getProperty(BridgeConstants.REQUEST_HOST_HEADER) != null) {
            headers.set(HttpHeaderNames.HOST, msgCtx.getProperty(NhttpConstants.REQUEST_HOST_HEADER));
            return;
        }

        if (port == 80 || port == 443) {
            headers.set(HttpHeaderNames.HOST, host);
        } else {
            headers.set(HttpHeaderNames.HOST, host + ":" + port);
        }
    }
}

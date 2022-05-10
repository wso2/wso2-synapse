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

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.SourceConfiguration;
import org.apache.synapse.transport.netty.util.CacheUtils;
import org.apache.synapse.transport.netty.util.HttpUtils;
import org.apache.synapse.transport.netty.util.MessageUtils;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.wso2.caching.CachingConstants;
import org.wso2.transport.http.netty.contract.config.ChunkConfig;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * {@code SourceResponseHandler} have utilities for creating and preparing an outbound response to be sent
 * to the HTTP client.
 */
public class SourceResponseHandler {

    private static final Log LOG = LogFactory.getLog(SourceResponseHandler.class);

    /**
     * Creates outbound response to be sent back to the HTTP client using the axis2 message context and
     * the original client request.
     *
     * @param msgCtx        axis2 message context
     * @param clientRequest original HTTP client request
     * @return the outbound response HttpCarbonMessage
     * @throws AxisFault if something goes wrong when creating the outbound response
     */
    public static HttpCarbonMessage createOutboundResponseMsg(MessageContext msgCtx, HttpCarbonMessage clientRequest)
            throws AxisFault {

        HttpVersion version = new HttpVersion(BridgeConstants.HTTP_2_0, true);
        if (BridgeConstants.HTTP_1_1_VERSION.equals(((SourceConfiguration) msgCtx.getProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION)).getProtocol())) {
            version = HttpVersion.HTTP_1_1;
        }
        HttpCarbonMessage outboundResponseMsg = new HttpCarbonMessage(new DefaultHttpResponse(version,
                HttpResponseStatus.OK));
        try {
            handleMTOM(msgCtx);
            handleETAGCaching(clientRequest, outboundResponseMsg, msgCtx);
            if (isValidCacheResponse(msgCtx)) {
                return outboundResponseMsg;
            }
            prepareOutboundResponse(clientRequest, outboundResponseMsg, msgCtx);
        } catch (AxisFault e) {
            RequestResponseUtils.handleException("Error while creating the outbound response!", e);
        }
        return outboundResponseMsg;
    }

    private static void handleMTOM(MessageContext msgCtx) throws AxisFault {

        if (isMTOMEnabled(msgCtx)) {
            try {
                MessageUtils.buildMessage(msgCtx);
            } catch (IOException e) {
                RequestResponseUtils.handleException("IO Error occurred while building the message", e);
            }
        }
    }

    private static void prepareOutboundResponse(HttpCarbonMessage inboundRequestMsg,
                                                HttpCarbonMessage outboundResponseMsg, MessageContext msgContext) {

        setOutboundResponseProperties(outboundResponseMsg, msgContext);
        setOutboundResponseHeaders(inboundRequestMsg, outboundResponseMsg, msgContext);
    }

    private static void setOutboundResponseProperties(HttpCarbonMessage outboundResponseMsg,
                                                      MessageContext msgContext) {

        setHttpVersion(outboundResponseMsg, msgContext);

        int statusCode = determineHttpStatusCode(msgContext);
        outboundResponseMsg.setHttpStatusCode(statusCode);

        setReasonPhrase(outboundResponseMsg, msgContext, statusCode);
    }

    private static void setOutboundResponseHeaders(HttpCarbonMessage inboundRequestMsg,
                                                   HttpCarbonMessage outboundResponseMsg, MessageContext msgContext) {

        HttpUtils.removeUnwantedHeadersFromInternalTransportHeadersMap(msgContext,
                (SourceConfiguration) msgContext.getProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION));
        HttpUtils.addTransportHeadersToTransportMessage(outboundResponseMsg.getHeaders(), msgContext);
        setChunkingHeader(inboundRequestMsg, outboundResponseMsg, msgContext);
        setContentTypeHeader(outboundResponseMsg, msgContext);
    }

    private static void setContentTypeHeader(HttpCarbonMessage outboundResponseMsg, MessageContext msgContext) {

        if (!canOutboundResponseHaveContentTypeHeader(msgContext)) {
            outboundResponseMsg.removeHeader(BridgeConstants.CONTENT_TYPE_HEADER);
            return;
        }

        if (shouldOverwriteOutboundResponseContentTypeHeader(msgContext)) {
            try {
                String contentType = getContentTypeForOutboundResponse(msgContext);
                outboundResponseMsg.setHeader(BridgeConstants.CONTENT_TYPE_HEADER, contentType);
            } catch (AxisFault axisFault) {
                LOG.error("Error occurred while setting the Content-Type header. Hence, not overwriting the "
                        + "outbound response Content-Type Header");
            }
        }
    }

    private static boolean shouldOverwriteOutboundResponseContentTypeHeader(MessageContext msgContext) {

        // the below null check is to decide whether the last call is an http request or not.
        // If it is an http and if the request has been built, we need to overwrite the Content-Type header.
        // Or else, if the http request was a pass through one, we do not need to modify the
        // Content-Type header.
        if (RequestResponseUtils.isHttpCarbonMessagePresent(msgContext)) {
            return !isPassThroughMessage(msgContext);
        }
        return true;
    }

    public static boolean canOutboundResponseHaveContentTypeHeader(MessageContext msgContext) {
        //TODO: verify this -> if we do not have a content to be sent in the source response, can we just
        // skip setting the content-type header?
        return true;
    }

    public static String getContentTypeForOutboundResponse(MessageContext msgContext) throws AxisFault {
        //This is to support MTOM in response path for requests sent without a SOAPAction. The reason is
        //axis2 selects application/xml formatter as the formatter for formatting the ESB to client response
        //when there is no SOAPAction.
        if (msgContext.isPropertyTrue(Constants.Configuration.ENABLE_MTOM)
                || msgContext.isPropertyTrue(Constants.Configuration.ENABLE_SWA)) {
            Object contentType = msgContext.getProperty(Constants.Configuration.CONTENT_TYPE);
            // The following condition will allow us to set the content-type as multipart/related if and only if
            // the content type is null or not starts with multipart/related. We cannot blindly set the content-type
            // as multipart/related as it would replace the multipart content-type with MIME boundary and the cause the
            // issue of response dropping the MIME boundary.
            if (Objects.isNull(contentType)
                    || !((String) contentType).trim().startsWith(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED)) {
                msgContext.setProperty(Constants.Configuration.CONTENT_TYPE,
                        HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED);
            }
            msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED);
        }

        Object contentTypeInMsgCtx = msgContext.getProperty(Constants.Configuration.CONTENT_TYPE);
        OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgContext);

        // If ContentType header is set in the axis2 message context, use it.
        if (contentTypeInMsgCtx != null) {
            String contentTypeValueInMsgCtx = contentTypeInMsgCtx.toString();
            // Skip multipart/related as it should be taken from formatter.
            if (!(contentTypeValueInMsgCtx.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED)
                    || contentTypeValueInMsgCtx.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA))) {

                // adding charset only if charset is not available,
                if (!contentTypeValueInMsgCtx.contains(HTTPConstants.CHAR_SET_ENCODING)
                        && msgContext.isPropertyTrue(BridgeConstants.SET_CHARACTER_ENCODING, true)) {
                    String encoding = format.getCharSetEncoding();
                    if (encoding != null) {
                        contentTypeValueInMsgCtx += "; charset=" + encoding;
                    }
                }
                return contentTypeValueInMsgCtx;
            }
        }

        // If ContentType is not set from msg context, get the formatter ContentType
        MessageFormatter formatter = null;
        try {
            formatter = MessageProcessorSelector.getMessageFormatter(msgContext);
        } catch (AxisFault e) {
            RequestResponseUtils.handleException("Cannot find a suitable MessageFormatter.", e);
        }
        return formatter.getContentType(msgContext, format, msgContext.getSoapAction());
    }

    /**
     * The pass through (when message body is not built) status of the message.
     *
     * @return true if it is a pass through.
     */
    public static boolean isPassThroughMessage(MessageContext msgContext) {

        boolean builderInvoked = Boolean.TRUE.equals(
                msgContext.getProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED));
        return !builderInvoked;
    }

    private static void setHttpVersion(HttpCarbonMessage outboundResponseMsg, MessageContext msgContext) {

        String version = determineHttpVersion(msgContext);
        outboundResponseMsg.setHttpVersion(version);
    }

    private static void setReasonPhrase(HttpCarbonMessage outboundResponseMsg, MessageContext msgContext,
                                        int statusCode) {

        String reasonPhrase = determineResponseReasonPhrase(msgContext, statusCode);
        // Whenever the reason phrase is null, the transport-http will infer the correct reason phrase based on the
        // provided status code.
        outboundResponseMsg.setProperty(org.wso2.transport.http.netty.contract.Constants.HTTP_REASON_PHRASE,
                reasonPhrase);
    }

    private static void setChunkingHeader(HttpCarbonMessage inboundRequestMsg, HttpCarbonMessage outboundResponseMsg,
                                          MessageContext msgContext) {

        boolean canHaveContentLengthOrTransferEncodingHeader =
                checkContentLengthAndTransferEncodingHeaderAllowance(inboundRequestMsg.getHttpMethod(),
                        outboundResponseMsg.getHttpStatusCode());
        if (canHaveContentLengthOrTransferEncodingHeader) {
            if (disableChunking(msgContext)) {
                outboundResponseMsg.setProperty(BridgeConstants.CHUNKING_CONFIG, ChunkConfig.NEVER);
            } else {
                outboundResponseMsg.setProperty(BridgeConstants.CHUNKING_CONFIG, ChunkConfig.ALWAYS);
            }
        }
    }

    private static boolean checkContentLengthAndTransferEncodingHeaderAllowance(String httpMethod, int statusCode) {
        // According to RFC 7230 - HTTP/1.1 Message Syntax and Routing - Message Body Length, the following logic
        // was implemented.
        if (BridgeConstants.HTTP_HEAD.equalsIgnoreCase(httpMethod)) {
            // Any response to a HEAD request
            return false;
        } else if (BridgeConstants.HTTP_CONNECT.equals(httpMethod)) {
            // Any 2xx (Successful) response to a CONNECT request
            return (statusCode / 100 != 2);
        }

        // Any response with a 1xx (Informational), 204 (No Content), or 304 (Not Modified) status code
        return statusCode >= HttpStatus.SC_OK
                && statusCode != HttpStatus.SC_NO_CONTENT
                && statusCode != HttpStatus.SC_NOT_MODIFIED
                && statusCode != HttpStatus.SC_RESET_CONTENT;
    }

    private static boolean disableChunking(MessageContext msgContext) {

        if (msgContext.isPropertyTrue(BridgeConstants.FORCE_HTTP_CONTENT_LENGTH)) {
            return true;
        } else {
            String disableChunking = (String) msgContext.getProperty(BridgeConstants.DISABLE_CHUNKING);
            return Constants.VALUE_TRUE.equals(disableChunking)
                    || Constants.VALUE_TRUE.equals(msgContext.getProperty(BridgeConstants.FORCE_HTTP_1_0));
        }
    }

    private static String determineHttpVersion(MessageContext msgContext) {

        if (msgContext.isPropertyTrue(BridgeConstants.FORCE_HTTP_1_0)) {
            return "1.0";
        } else if (((HttpCarbonMessage) msgContext.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE)).getHttpVersion() == "2.0") {
            return "2.0";
        }
        return "1.1";
    }

    /**
     * Determine the Http Status Code depending on the message type processed <br>
     * (normal response versus fault response) as well as Axis2 message context properties set
     * via Synapse configuration or MessageBuilders.
     *
     * @param msgContext the Axis2 message context
     * @return the HTTP status code to set in the HTTP response object
     * @see BridgeConstants#SC_ACCEPTED
     * @see BridgeConstants#ERROR_CODE
     */
    private static int determineHttpStatusCode(MessageContext msgContext) {

        int httpStatus = HttpStatus.SC_OK;

        Integer errorCode = (Integer) msgContext.getProperty(BridgeConstants.ERROR_CODE);
        if (errorCode != null) {
            return HttpStatus.SC_BAD_GATEWAY;
        }

        // if this is a dummy message to handle http 202 case with non-blocking IO
        // set the status code to 202
        if (msgContext.isPropertyTrue(BridgeConstants.SC_ACCEPTED)) {
            return HttpStatus.SC_ACCEPTED;
        } else {
            Object statusCode = msgContext.getProperty(BridgeConstants.HTTP_SC);
            if (statusCode != null) {
                try {
                    httpStatus = Integer.parseInt(statusCode.toString());
                    return httpStatus;
                } catch (NumberFormatException e) {
                    LOG.warn("Unable to set the HTTP status code from the property "
                            + BridgeConstants.HTTP_SC + " with value: " + statusCode);
                }
            }

            // Is this a fault message?
            boolean handleFault = HttpUtils.isFaultMessage(msgContext);
            boolean faultsAsHttp200 = HttpUtils.sendFaultAsHTTP200(msgContext);

            // Set HTTP status code to 500 if this is a fault case and we shall not use HTTP 200
            if (handleFault && !faultsAsHttp200) {
                httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            }
        }
        return httpStatus;
    }

    /**
     * Determine the Http Status Message depending on the message type processed <br>
     * (normal response versus fault response) as well as Axis2 message context properties set
     * via Synapse configuration or MessageBuilders.
     *
     * @param msgContext the Axis2 message context
     * @return the HTTP status message string or null
     * @see BridgeConstants#HTTP_SC_DESC
     * @see BridgeConstants#HTTP_STATUS_CODE_SENT_FROM_BACKEND
     * @see BridgeConstants#HTTP_REASON_PHRASE_SENT_FROM_BACKEND
     */
    private static String determineResponseReasonPhrase(MessageContext msgContext, int statusCode) {

        String statusLine = null;
        Object statusLineProperty = msgContext.getProperty(BridgeConstants.HTTP_SC_DESC);
        if (statusLineProperty != null) {
            statusLine = (String) statusLineProperty;
        }

        Object httpReasonPhraseFromBackend =
                msgContext.getProperty(BridgeConstants.HTTP_REASON_PHRASE_SENT_FROM_BACKEND);
        Object httpStatusCodeFromBackend = msgContext.getProperty(BridgeConstants.HTTP_STATUS_CODE_SENT_FROM_BACKEND);

        if (Objects.isNull(httpStatusCodeFromBackend) || Objects.isNull(httpReasonPhraseFromBackend)) {
            return statusLine;
        }

        if (statusCode != (Integer) httpStatusCodeFromBackend && httpReasonPhraseFromBackend.equals(statusLine)) {
            // make the statusLine null so that the proper status code will be by the Netty server.
            statusLine = null;
        }
        return statusLine;
    }

    private static void handleETAGCaching(HttpCarbonMessage inboundRequestMsg, HttpCarbonMessage outboundResponseMsg,
                                          MessageContext msgCtx) throws AxisFault {

        if (isEtagEnabled(msgCtx)) {
            try {
                MessageUtils.buildMessage(msgCtx);
            } catch (IOException e) {
                RequestResponseUtils.handleException("IO Error occurred while building the message", e);
            }
            String hash = CachingConstants.DEFAULT_XML_IDENTIFIER.getDigest(msgCtx);
            outboundResponseMsg.setHeader(BridgeConstants.ETAG_HEADER, "\"" + hash + "\"");
        }

        if (CacheUtils.isValidCachedResponse(outboundResponseMsg, inboundRequestMsg)) {
            // A 304 Not Modified message is an HTTP response status code indicating that the requested resource
            // has not been modified since the previous transmission, so there is no need to retransmit the
            // requested resource to the client. In effect, a 304 Not Modified response code acts as an
            // implicit redirection to a cached version of the requested resource.
            outboundResponseMsg.setHttpStatusCode(HttpResponseStatus.NOT_MODIFIED.code());
            outboundResponseMsg.setProperty(org.wso2.transport.http.netty.contract.Constants.HTTP_REASON_PHRASE,
                    HttpResponseStatus.NOT_MODIFIED.reasonPhrase());
            setHttpVersion(outboundResponseMsg, msgCtx);
            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_LENGTH.toString());
            outboundResponseMsg.removeHeader(HttpHeaderNames.CONTENT_TYPE.toString());
            msgCtx.setProperty(BridgeConstants.VALID_CACHED_RESPONSE, true);
        }
    }

    /**
     * Writes the response headers and the response body to the client.
     *
     * @param msgCtx              axis2 message context
     * @param inboundRequestMsg   inbound request carbon message
     * @param outboundResponseMsg outbound response carbon message
     * @throws AxisFault if something goes wrong when sending out the response
     */
    public static void sendResponse(MessageContext msgCtx, HttpCarbonMessage inboundRequestMsg,
                                    HttpCarbonMessage outboundResponseMsg) throws AxisFault {

        HttpUtils.sendOutboundResponse(inboundRequestMsg, outboundResponseMsg);
        serializeData(msgCtx, outboundResponseMsg);
    }

    private static void serializeData(MessageContext msgCtx, HttpCarbonMessage responseMsg)
            throws AxisFault {

        if (hasNoResponseBodyToSend(msgCtx)) {
            OutputStream messageOutputStream = HttpUtils.getHttpMessageDataStreamer(responseMsg).getOutputStream();
            HttpUtils.writeEmptyBody(messageOutputStream);
        } else {
            if (RequestResponseUtils.shouldInvokeFormatterToWriteBody(msgCtx)) {
                OutputStream messageOutputStream = HttpUtils.getHttpMessageDataStreamer(responseMsg).getOutputStream();
                MessageFormatter messageFormatter = MessageUtils.getMessageFormatter(msgCtx);
                HttpUtils.serializeDataUsingMessageFormatter(msgCtx, messageFormatter, messageOutputStream);
            } else {
                HttpCarbonMessage inboundCarbonMessage =
                        (HttpCarbonMessage) msgCtx.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE);
                HttpUtils.copyContentFromInboundHttpCarbonMessage(inboundCarbonMessage, responseMsg);
            }
        }
    }

    private static boolean hasNoResponseBodyToSend(MessageContext msgCtx) {

        return msgCtx.isPropertyTrue(BridgeConstants.NO_ENTITY_BODY)
                || msgCtx.isPropertyTrue(BridgeConstants.VALID_CACHED_RESPONSE);
    }

    private static boolean isEtagEnabled(MessageContext msgCtx) {

        return msgCtx.isPropertyTrue(BridgeConstants.HTTP_ETAG_ENABLED);
    }

    private static boolean isMTOMEnabled(MessageContext msgCtx) {

        return Objects.nonNull(msgCtx.getProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM));
    }

    private static boolean isValidCacheResponse(MessageContext msgCtx) {

        return msgCtx.isPropertyTrue(BridgeConstants.VALID_CACHED_RESPONSE);
    }
}

/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.vt;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.nhttp.util.RESTUtil;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-request server-side worker that processes <b>one HTTP request</b>
 * entirely within a single Virtual Thread.
 * <p>
 * HttpCore 5's {@link org.apache.hc.core5.http.impl.io.HttpService} handles
 * all HTTP protocol concerns (parsing, keep-alive, chunked decoding, response
 * serialization).  This worker is called by an
 * {@link org.apache.hc.core5.http.io.HttpRequestHandler} for each request
 * and is responsible only for:
 * <ol>
 *   <li>Building an Axis2 {@link MessageContext} from the HttpCore 5
 *       {@link ClassicHttpRequest}.</li>
 *   <li>Dispatching to the Axis2/Synapse mediation engine via
 *       {@link AxisEngine#receive(MessageContext)}.</li>
 *   <li>Populating the HttpCore 5 {@link ClassicHttpResponse} when the
 *       sender calls {@link #submitResponse(MessageContext)}.</li>
 *   <li>Cleaning up Axis2 context objects between requests.</li>
 * </ol>
 * </p>
 * <p>This class implements {@link OutTransportInfo} so the Axis2 engine can
 * treat it as the out-transport-info for response writing.</p>
 */
public class VTBlockingServerWorker implements OutTransportInfo {

    private static final Log log = LogFactory.getLog(VTBlockingServerWorker.class);
    private static final String SOAP_ACTION_HEADER = "SOAPAction";

    // ---- Per-request fields (set by constructor) ----
    private final ClassicHttpRequest httpRequest;
    private final ClassicHttpResponse httpResponse;
    private final HttpContext httpContext;
    private final SourceConfiguration sourceConfiguration;
    private final ConfigurationContext configurationContext;
    private final Map<String, String> serviceNameToEPRMap;
    private final Map<String, String> eprToServiceNameMap;

    // ---- Per-request state ----
    private MessageContext msgContext;
    private volatile boolean responseSent = false;

    /** Parsed request headers (case-insensitive) — populated from ClassicHttpRequest */
    private Map<String, String> requestHeaders;

    /** HTTP method */
    private String method;

    /** Request URI */
    private String uri;

    public VTBlockingServerWorker(ClassicHttpRequest httpRequest,
                                  ClassicHttpResponse httpResponse,
                                  HttpContext httpContext,
                                  SourceConfiguration sourceConfiguration,
                                  ConfigurationContext configurationContext,
                                  Map<String, String> serviceNameToEPRMap,
                                  Map<String, String> eprToServiceNameMap) {
        this.httpRequest = httpRequest;
        this.httpResponse = httpResponse;
        this.httpContext = httpContext;
        this.sourceConfiguration = sourceConfiguration;
        this.configurationContext = configurationContext;
        this.serviceNameToEPRMap = serviceNameToEPRMap;
        this.eprToServiceNameMap = eprToServiceNameMap;
    }

    /**
     * Process the HTTP request: build MessageContext, dispatch to Axis2,
     * and ensure the ClassicHttpResponse is populated before returning.
     * Called by HttpCore 5's HttpRequestHandler.
     */
    public void process() throws HttpException, IOException {
        try {
            // ---- Extract request info from HttpCore 5 objects ----
            method = httpRequest.getMethod().toUpperCase();
            uri = httpRequest.getRequestUri();

            // Parse headers into a case-insensitive map
            requestHeaders = new TreeMap<>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            Map<String, String> excessHeaders = new java.util.LinkedHashMap<>();
            for (Header header : httpRequest.getHeaders()) {
                if (requestHeaders.containsKey(header.getName())) {
                    excessHeaders.put(header.getName(), requestHeaders.get(header.getName()));
                }
                requestHeaders.put(header.getName(), header.getValue());
            }

            if (log.isDebugEnabled()) {
                log.debug("VT received: " + method + " " + uri);
            }


            // ---- Create Axis2 MessageContext ----
            msgContext = createMessageContext();
            msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
            msgContext.setProperty(VTConstants.VT_SOURCE_CONFIGURATION, sourceConfiguration);
            msgContext.setProperty(Constants.Configuration.HTTP_METHOD, method);

            if (PassThroughConstants.HTTP_GET.equals(method)
                    || PassThroughConstants.HTTP_DELETE.equals(method)) {
                msgContext.setProperty(
                        PassThroughConstants.REST_GET_DELETE_INVOKE, Boolean.TRUE);
            }

            processHttpRequestUri(method);

            // ---- Dispatch to Axis2/Synapse engine ----
            boolean isRest = isRESTRequest(method);

            if (!isRest) {
                boolean entityEnclosing = hasEntity();
                if (entityEnclosing) {
                    processEntityEnclosingRequest(true);
                } else {
                    processNonEntityEnclosingRESTHandler(null, true);
                }
            } else {
                String contentTypeHeader = requestHeaders.get(
                        org.apache.http.protocol.HTTP.CONTENT_TYPE);
                SOAPEnvelope soapEnvelope = handleRESTUrlPost(contentTypeHeader);
                processNonEntityEnclosingRESTHandler(soapEnvelope, true);
            }

            // At this point AxisEngine.receive() has returned.
            // In the VT blocking model the entire mediation flow
            // (including the backend call and response writing via
            // VTBlockingClientWorker) is synchronous, so the response
            // should already be set via submitResponse().

        } catch (Exception e) {
            log.error("Error processing request in VT server worker", e);
            handleException("Error processing request", e);
        } finally {
            // Safety net: if no response was populated yet, set a default
            if (!responseSent && msgContext != null) {
                try {
                    msgContext.removeProperty(MessageContext.TRANSPORT_HEADERS);
                    submitResponse(msgContext);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Last-resort response write failed", e);
                    }
                    if (!responseSent) {
                        httpResponse.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                        httpResponse.setEntity(new StringEntity("Internal Server Error",
                                ContentType.TEXT_PLAIN));
                        responseSent = true;
                    }
                }
            }

            // ---- Thorough per-request cleanup ----
            if (msgContext != null) {
                try {
                    // 1. Clean up the IN (request) OperationContext
                    if (msgContext.getOperationContext() != null) {
                        msgContext.getOperationContext().setComplete(true);
                        msgContext.getOperationContext().cleanup();
                    }

                    // 2. Clean up the OUT (response) OperationContext
                    if (msgContext.getOperationContext() != null) {
                        MessageContext outMsgCtx = msgContext
                                .getOperationContext()
                                .getMessageContext(
                                        org.apache.axis2.wsdl.WSDLConstants
                                                .MESSAGE_LABEL_OUT_VALUE);
                        if (outMsgCtx != null
                                && outMsgCtx.getOperationContext() != null) {
                            outMsgCtx.getOperationContext().setComplete(true);
                            outMsgCtx.getOperationContext().cleanup();
                        }
                    }

                    // 3. Remove ServiceGroupContext from cache
                    String sgcId = msgContext.getServiceGroupContextId();
                    if (sgcId != null) {
                        configurationContext.removeServiceGroupContext(sgcId);
                    }
                } catch (Exception ignore) { }

                // 4. Close backend response if not already
                try {
                    VTTargetResponse tr = (VTTargetResponse)
                            msgContext.getProperty(VTConstants.VT_TARGET_RESPONSE);
                    if (tr != null) tr.close();
                } catch (Exception ignore) { }

                // 5. Remove heavyweight properties
                try {
                    msgContext.removeProperty(VTConstants.VT_SOURCE_CONFIGURATION);
                    msgContext.removeProperty(VTConstants.VT_TARGET_RESPONSE);
                    msgContext.removeProperty(Constants.OUT_TRANSPORT_INFO);
                    msgContext.removeProperty(PassThroughConstants.PASS_THROUGH_PIPE);
                    msgContext.removeProperty(MessageContext.TRANSPORT_HEADERS);
                } catch (Exception ignore) { }
            }
            // 6. Clear ThreadLocal
            MessageContext.destroyCurrentMessageContext();
        }
    }

    // ================================================================
    //  Response handling
    // ================================================================

    /**
     * Called by VTPassThroughHttpSender to populate the HttpCore 5
     * {@link ClassicHttpResponse} with status, headers, and body.
     * HttpCore 5 serializes this response to the socket after the
     * handler returns.
     */
    public void submitResponse(MessageContext responseMsgCtx) throws AxisFault {
        if (responseSent) return;

        try {
            // ---- Status code ----
            Object httpSC = responseMsgCtx.getProperty(PassThroughConstants.HTTP_SC);
            if (httpSC instanceof Integer) {
                httpResponse.setCode((Integer) httpSC);
            } else if (httpSC instanceof String) {
                httpResponse.setCode(Integer.parseInt((String) httpSC));
            } else {
                httpResponse.setCode(HttpStatus.SC_OK);
            }

            // ---- Reason phrase ----
            Object httpSCDesc = responseMsgCtx.getProperty(PassThroughConstants.HTTP_SC_DESC);
            if (httpSCDesc instanceof String) {
                httpResponse.setReasonPhrase((String) httpSCDesc);
            }

            // ---- Transport headers (skip hop-by-hop) ----
            @SuppressWarnings("unchecked")
            Map<String, Object> transportHeaders =
                    (Map<String, Object>) responseMsgCtx.getProperty(
                            MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                for (Map.Entry<String, Object> entry : transportHeaders.entrySet()) {
                    if (entry.getValue() != null
                            && !VTConstants.HOP_BY_HOP_HEADERS.contains(
                            entry.getKey().toLowerCase())) {
                        httpResponse.addHeader(entry.getKey(),
                                entry.getValue().toString());
                    }
                }
            }

            // ---- Body ----
            Boolean noEntityBody = (Boolean) responseMsgCtx
                    .getProperty(PassThroughConstants.NO_ENTITY_BODY);
            VTInputStreamPipe vtInputStreamPipe =
                    (VTInputStreamPipe) responseMsgCtx.getProperty(VTConstants.VT_INPUT_STREAM_PIPE);
            boolean messageBuilderInvoked =
                    Boolean.TRUE.equals(responseMsgCtx.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));

            if (noEntityBody != null && noEntityBody) {
                // No body — HttpCore 5 writes just the status + headers
            } else if (vtInputStreamPipe != null && !messageBuilderInvoked) {
                InputStream bodyStream = vtInputStreamPipe.getInputStream();
                if (bodyStream != null) {
                    byte[] bodyBytes = bodyStream.readAllBytes();
                    if (bodyBytes.length > 0) {
                        String contentType = getResponseContentType(responseMsgCtx);
                        ContentType parsedContentType = parseContentTypeOrDefault(contentType);
                        httpResponse.setEntity(new ByteArrayEntity(bodyBytes, parsedContentType));
                    } else {
                        responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                    }
                } else {
                    responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                }
            } else {
                MessageFormatter formatter =
                        MessageFormatterDecoratorFactory
                                .createMessageFormatterDecorator(responseMsgCtx);
                OMOutputFormat format = PassThroughTransportUtils
                        .getOMOutputFormat(responseMsgCtx);

                String contentType = formatter.getContentType(
                        responseMsgCtx, format, responseMsgCtx.getSoapAction());

                ByteArrayOutputStream bodyBuf = new ByteArrayOutputStream(8192);
                formatter.writeTo(responseMsgCtx, format, bodyBuf, false);
                byte[] bodyBytes = bodyBuf.toByteArray();
                if (bodyBytes.length > 0) {
                    ContentType parsedContentType = parseContentTypeOrDefault(contentType);
                    httpResponse.setEntity(new ByteArrayEntity(bodyBytes, parsedContentType));
                } else {
                    responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                }
            }

            responseSent = true;

        } catch (Exception e) {
            throw new AxisFault("Error building response", e);
        }
    }

    private String getResponseContentType(MessageContext responseMsgCtx) {
        @SuppressWarnings("unchecked")
        Map<String, Object> transportHeaders =
                (Map<String, Object>) responseMsgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null) {
            Object headerContentType = transportHeaders.get(HTTP.CONTENT_TYPE);
            if (headerContentType == null) {
                for (Map.Entry<String, Object> entry : transportHeaders.entrySet()) {
                    if (HTTP.CONTENT_TYPE.equalsIgnoreCase(entry.getKey())) {
                        headerContentType = entry.getValue();
                        break;
                    }
                }
            }
            if (headerContentType != null) {
                return headerContentType.toString();
            }
        }

        Object axisContentType = responseMsgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
        return axisContentType != null ? axisContentType.toString() : null;
    }

    private ContentType parseContentTypeOrDefault(String contentType) {
        if (contentType != null) {
            try {
                return ContentType.parse(contentType);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid content type while building response: " + contentType, e);
                }
            }
        }
        return ContentType.APPLICATION_OCTET_STREAM;
    }

    // ================================================================
    //  MessageContext creation
    // ================================================================

    private MessageContext createMessageContext() {
        ConfigurationContext cfgCtx = sourceConfiguration.getConfigurationContext();

        MessageContext msgCtx = new MessageContext();
        msgCtx.setMessageID(UIDGenerator.generateURNString());
        msgCtx.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, Boolean.FALSE);
        msgCtx.setConfigurationContext(cfgCtx);

        // Transport IN/OUT setup — use standard names for IN so Axis2 dispatch
        // works, and VT transport name for OUT so response goes through our sender.
        String vtOutName = sourceConfiguration.getInDescription().getName();
        org.apache.axis2.description.TransportOutDescription vtOut =
            cfgCtx.getAxisConfiguration().getTransportOut(vtOutName);
        if (vtOut == null) {
            vtOut = cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP);
        }
        msgCtx.setTransportOut(vtOut);

        org.apache.axis2.description.TransportInDescription transportIn =
            cfgCtx.getAxisConfiguration().getTransportIn(Constants.TRANSPORT_HTTP);
        if (transportIn == null) {
            transportIn = sourceConfiguration.getInDescription();
        }
        msgCtx.setTransportIn(transportIn);
        msgCtx.setIncomingTransportName(Constants.TRANSPORT_HTTP);

        msgCtx.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        msgCtx.setServerSide(true);
        msgCtx.setProperty(Constants.Configuration.TRANSPORT_IN_URL, uri);

        // Copy headers
        Map<String, String> headers = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        headers.putAll(requestHeaders);
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
        msgCtx.setProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS,
                new java.util.LinkedHashMap<>());

        // Remote address from context (set by listener's accept loop)
        SocketAddress remoteAddr = (SocketAddress)
                httpContext.getAttribute(VTConstants.CTX_REMOTE_ADDRESS);
        if (remoteAddr instanceof InetSocketAddress) {
            InetAddress remoteInet = ((InetSocketAddress) remoteAddr).getAddress();
            if (remoteInet != null) {
                msgCtx.setProperty(MessageContext.REMOTE_ADDR,
                        remoteInet.getHostAddress());
                msgCtx.setProperty(NhttpConstants.REMOTE_HOST,
                        remoteInet.getHostName());
            }
        }

        msgCtx.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
                new VTRequestResponseTransport(msgCtx));

        return msgCtx;
    }

    // ================================================================
    //  Request URI / REST / SOAP processing  (kept from original)
    // ================================================================

    private void processHttpRequestUri(String method) {
        String servicePrefixIndex = "://";
        String restUrlPostfix = NhttpUtil.getRestUrlPostfix(uri,
                configurationContext.getServicePath());

        String servicePrefix = uri.substring(0, uri.indexOf(restUrlPostfix));
        if (servicePrefix.indexOf(servicePrefixIndex) == -1) {
            // Construct a full prefix from context attributes
            SocketAddress localAddr = (SocketAddress)
                    httpContext.getAttribute(VTConstants.CTX_LOCAL_ADDRESS);
            Integer localPort = (Integer)
                    httpContext.getAttribute(VTConstants.CTX_LOCAL_PORT);
            if (localAddr instanceof InetSocketAddress) {
                InetAddress addr = ((InetSocketAddress) localAddr).getAddress();
                if (addr != null) {
                    servicePrefix = sourceConfiguration.getScheme().getName()
                            + servicePrefixIndex + addr.getHostAddress()
                            + ":" + localPort + servicePrefix;
                }
            }
        }

        msgContext.setProperty(PassThroughConstants.SERVICE_PREFIX, servicePrefix);
        msgContext.setTo(new EndpointReference(restUrlPostfix));
        msgContext.setProperty(PassThroughConstants.REST_URL_POSTFIX, restUrlPostfix);
    }

    private boolean isRESTRequest(String method) {
        if (msgContext.getProperty(PassThroughConstants.REST_GET_DELETE_INVOKE) != null
                && (Boolean) msgContext.getProperty(
                PassThroughConstants.REST_GET_DELETE_INVOKE)) {
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            msgContext.setServerSide(true);
            msgContext.setDoingREST(true);
            return true;
        }
        return false;
    }

    /** Whether the request has an entity body (from HttpCore 5). */
    private boolean hasEntity() {
        HttpEntity entity = httpRequest.getEntity();
        return entity != null;
    }

    /** Get body InputStream from HttpCore 5 entity. */
    private InputStream getBodyInputStream() throws IOException {
        HttpEntity entity = httpRequest.getEntity();
        return entity != null ? entity.getContent() : null;
    }

    public void processEntityEnclosingRequest(boolean injectToAxis2Engine) {
        try {
            String contentTypeHeader = requestHeaders.get(
                    org.apache.http.protocol.HTTP.CONTENT_TYPE);
            if (contentTypeHeader == null) {
                contentTypeHeader = inferContentType();
            }

            String charSetEncoding = null;
            String contentType = null;
            if (contentTypeHeader != null) {
                charSetEncoding = BuilderUtil.getCharSetEncoding(contentTypeHeader);
                contentType = TransportUtils.getContentType(contentTypeHeader, msgContext);
            }
            if (charSetEncoding == null) {
                charSetEncoding = MessageContext.DEFAULT_CHAR_SET_ENCODING;
            }

            msgContext.setTo(new EndpointReference(uri));
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                    charSetEncoding);
            msgContext.setServerSide(true);
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentTypeHeader);
            msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE, contentType);

            if (contentTypeHeader == null
                    || HTTPTransportUtils.isRESTRequest(contentTypeHeader)
                    || isRest(contentTypeHeader)) {
                msgContext.setProperty(
                        PassThroughConstants.REST_REQUEST_CONTENT_TYPE, contentType);
                msgContext.setDoingREST(true);
                SOAPEnvelope soapEnvelope = handleRESTUrlPost(contentTypeHeader);
                // Set entity body stream via VTInputStreamPipe
                InputStream bodyStream = getBodyInputStream();
                if (bodyStream != null) {
                    msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE,
                            new VTInputStreamPipe(bodyStream));
                }
                processNonEntityEnclosingRESTHandler(soapEnvelope, injectToAxis2Engine);
                return;
            } else {
                String soapAction = requestHeaders.get(SOAP_ACTION_HEADER);
                int soapVersion = HTTPTransportUtils.initializeMessageContext(
                        msgContext, soapAction, uri, contentTypeHeader);

                SOAPEnvelope envelope;
                if (soapVersion == 1) {
                    SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
                    envelope = fac.getDefaultEnvelope();
                } else {
                    SOAPFactory fac = OMAbstractFactory.getSOAP12Factory();
                    envelope = fac.getDefaultEnvelope();
                }

                if (soapAction != null && soapAction.startsWith("\"")
                        && soapAction.endsWith("\"")) {
                    soapAction = soapAction.substring(1, soapAction.length() - 1);
                    msgContext.setSoapAction(soapAction);
                }
                msgContext.setEnvelope(envelope);
            }

            // Set the entity body stream
            InputStream bodyStream = getBodyInputStream();
            if (bodyStream != null) {
                msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE,
                        new VTInputStreamPipe(bodyStream));
            }

            if (injectToAxis2Engine) {
                AxisEngine.receive(msgContext);
            }
        } catch (AxisFault axisFault) {
            handleException("Error processing " + method
                    + " request for: " + uri, axisFault);
        } catch (Exception e) {
            handleException("Error processing " + method
                    + " request for: " + uri, e);
        }
    }

    public void processNonEntityEnclosingRESTHandler(SOAPEnvelope soapEnvelope,
                                                     boolean injectToAxis2Engine) {
        String soapAction = requestHeaders.get(SOAP_ACTION_HEADER);
        if (soapAction != null && soapAction.startsWith("\"")
                && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }

        msgContext.setSoapAction(soapAction);
        msgContext.setTo(new EndpointReference(uri));
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        if (!hasEntity()) {
            msgContext.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
        }

        try {
            if (soapEnvelope == null) {
                msgContext.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
            } else {
                msgContext.setEnvelope(soapEnvelope);
            }

            if (injectToAxis2Engine) {
                AxisEngine.receive(msgContext);
            }
        } catch (AxisFault axisFault) {
            handleException("Error processing " + method
                    + " request for: " + uri, axisFault);
        } catch (Exception e) {
            handleException("Error processing " + method
                    + " request for: " + uri, e);
        }
    }

    public SOAPEnvelope handleRESTUrlPost(String contentTypeHdr) {
        SOAPEnvelope soapEnvelope = null;
        String contentType = contentTypeHdr != null
                ? TransportUtils.getContentType(contentTypeHdr, msgContext) : null;

        if (contentType == null || contentType.isEmpty()) {
            contentType = PassThroughConstants.APPLICATION_OCTET_STREAM;
            if (HTTPConstants.HTTP_METHOD_GET.equals(method)
                    || "DELETE".equals(method)) {
                contentType = HTTPConstants.MEDIA_TYPE_X_WWW_FORM;
            }
        }

        if (HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals(contentType)
                || (PassThroughConstants.APPLICATION_OCTET_STREAM.equals(contentType)
                && contentTypeHdr == null)) {

            msgContext.setTo(new EndpointReference(uri));
            String charSetEncoding;
            if (contentTypeHdr != null) {
                msgContext.setProperty(Constants.Configuration.CONTENT_TYPE,
                        contentTypeHdr);
                charSetEncoding = BuilderUtil.getCharSetEncoding(contentTypeHdr);
            } else {
                msgContext.setProperty(Constants.Configuration.CONTENT_TYPE,
                        contentType);
                charSetEncoding = BuilderUtil.getCharSetEncoding(contentType);
            }
            msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING,
                    charSetEncoding);

            try {
                RESTUtil.dispatchAndVerify(msgContext);
            } catch (AxisFault e1) {
                log.error("Error while building message for REST_URL request", e1);
            }

            try {
                boolean reverseProxyMode = PassThroughConfiguration.getInstance()
                        .isReverseProxyMode();
                AxisService axisService = null;
                if (!reverseProxyMode) {
                    RequestURIBasedDispatcher requestDispatcher =
                            new RequestURIBasedDispatcher();
                    axisService = requestDispatcher.findService(msgContext);
                }

                boolean isCustomRESTDispatcher = false;
                if (uri.matches(PassThroughConfiguration.getInstance()
                        .getRestUriApiRegex())
                        || uri.matches(PassThroughConfiguration.getInstance()
                        .getRestUriProxyRegex())) {
                    isCustomRESTDispatcher = true;
                }

                if (!isCustomRESTDispatcher) {
                    if (axisService == null) {
                        String defaultSvcName = PassThroughConfiguration.getInstance()
                                .getPassThroughDefaultServiceName();
                        axisService = msgContext.getConfigurationContext()
                                .getAxisConfiguration().getService(defaultSvcName);
                        msgContext.setAxisService(axisService);
                    }
                } else {
                    String multiTenantDispatchService =
                            PassThroughConfiguration.getInstance()
                                    .getRESTDispatchService();
                    axisService = msgContext.getConfigurationContext()
                            .getAxisConfiguration()
                            .getService(multiTenantDispatchService);
                    msgContext.setAxisService(axisService);
                }
            } catch (AxisFault e) {
                handleException("Error processing " + method
                        + " request for: " + uri, e);
            }

            try {
                soapEnvelope = TransportUtils.createSOAPMessage(
                        msgContext, null, contentType);
            } catch (Exception e) {
                log.error("Error while building message for REST_URL request", e);
            }
            msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    HTTPConstants.MEDIA_TYPE_APPLICATION_XML);
        }
        return soapEnvelope;
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private boolean isRest(String contentType) {
        return contentType != null
                && contentType.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) == -1
                && contentType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) == -1;
    }

    private String inferContentType() {
        for (String header : requestHeaders.keySet()) {
            if (org.apache.http.protocol.HTTP.CONTENT_TYPE.equalsIgnoreCase(header)) {
                return requestHeaders.get(header);
            }
        }
        Parameter param = sourceConfiguration.getConfigurationContext()
                .getAxisConfiguration()
                .getParameter(PassThroughConstants.REQUEST_CONTENT_TYPE);
        if (param != null) {
            return param.getValue().toString();
        }
        return null;
    }

    private void handleException(String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }
        if (responseSent) return;

        // Set error response on the HttpCore 5 response object
        httpResponse.setCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        String body = "<html><body><h1>Failed to process the request</h1><p>"
                + msg + "</p></body></html>";
        httpResponse.setEntity(new StringEntity(body,
                ContentType.create("text/html", "UTF-8")));
        responseSent = true;
    }

    // ---- OutTransportInfo ----

    @Override
    public void setContentType(String contentType) {
        // Handled in submitResponse
    }

    // ---- Accessors ----

    public MessageContext getRequestContext() { return msgContext; }
    public SourceConfiguration getSourceConfiguration() { return sourceConfiguration; }
    public boolean isResponseSent() { return responseSent; }
}

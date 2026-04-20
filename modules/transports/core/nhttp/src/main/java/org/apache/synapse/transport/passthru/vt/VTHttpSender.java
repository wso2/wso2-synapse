package org.apache.synapse.transport.passthru.vt;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPSender;
import org.apache.axis2.transport.http.AbstractHTTPSender;
import org.apache.axis2.transport.http.ServletBasedOutTransportInfo;
import org.apache.axis2.transport.http.server.AxisHttpResponseImpl;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --- HttpClient 4.5.x imports (replacing Commons HttpClient 3.x) ---
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.apache.synapse.transport.passthru.PassThroughConstants;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP Transport Sender implementation using Apache HttpClient 4.5.x.
 * <p>
 * Migrated from the legacy Commons HttpClient 3.x API to HttpClient 4.5.13.
 * Key changes:
 * <ul>
 *   <li>{@code HttpClient} → {@link CloseableHttpClient}</li>
 *   <li>{@code MultiThreadedHttpConnectionManager} → {@link PoolingHttpClientConnectionManager}</li>
 *   <li>{@code HttpMethod} → {@code HttpRequestBase} (for cleanup)</li>
 *   <li>Connection/socket timeouts via {@link RequestConfig} instead of {@code HttpConnectionManagerParams}</li>
 * </ul>
 */
public class VTHttpSender extends AbstractHandler implements
        TransportSender {

    private static final Log log = LogFactory.getLog(VTHttpSender.class);

    /**
     * The {@link TransportOutDescription} object received by the call to
     * {@link #init(ConfigurationContext, TransportOutDescription)}.
     */
    private TransportOutDescription transportOut;

    /**
     * Default HTTP version as configured in <tt>axis2.xml</tt>. This may be overridden on a per
     * message basis using the {@link HTTPConstants#HTTP_PROTOCOL_VERSION} property.
     */
    private String defaultHttpVersion = HTTPConstants.HEADER_PROTOCOL_11;

    /**
     * Specifies whether chunked encoding is enabled by default. This is configured in
     * <tt>axis2.xml</tt> and may be overridden on a per message basis using the
     * {@link HTTPConstants#CHUNKED} property.
     */
    private boolean defaultChunked = false;

    private int soTimeout = HTTPConstants.DEFAULT_SO_TIMEOUT;

    private int connectionTimeout = HTTPConstants.DEFAULT_CONNECTION_TIMEOUT;

    /**
     * Connection manager for the cached HttpClient (HttpClient 4.5.x).
     * Kept as a field so it can be shut down in {@link #stop()}.
     */
    private PoolingHttpClientConnectionManager connectionManager;

    // ------------------------------------------------------------------ lifecycle

    public void cleanup(MessageContext msgContext) throws AxisFault {
        // In HttpClient 4.5.x the connection is released when the response entity is consumed
        // or when the CloseableHttpResponse is closed. HttpMethod no longer exists.
        // For backward compatibility we still check the property and attempt cleanup.
        Object requestBase = msgContext.getProperty(HTTPConstants.HTTP_METHOD);

        if (requestBase != null) {
            log.trace("cleanup() releasing connection for " + requestBase);

            if (requestBase instanceof org.apache.http.client.methods.HttpRequestBase) {
                // Abort the request if it is still in flight; this releases the
                // underlying connection back to the pool.
                ((org.apache.http.client.methods.HttpRequestBase) requestBase).abort();
            }
            msgContext.removeProperty(HTTPConstants.HTTP_METHOD); // guard against multiple calls
        }
    }

    public void init(ConfigurationContext confContext,
                     TransportOutDescription transportOut) throws AxisFault {
        this.transportOut = transportOut;

        // ---- HTTP version (1.0 / 1.1) ----
        Parameter version = transportOut.getParameter(HTTPConstants.PROTOCOL_VERSION);
        if (version != null) {
            if (HTTPConstants.HEADER_PROTOCOL_11.equals(version.getValue())) {
                defaultHttpVersion = HTTPConstants.HEADER_PROTOCOL_11;

                Parameter transferEncoding = transportOut
                        .getParameter(HTTPConstants.HEADER_TRANSFER_ENCODING);

                if ((transferEncoding != null)
                        && HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED
                        .equals(transferEncoding.getValue())) {
                    defaultChunked = true;
                }
            } else if (HTTPConstants.HEADER_PROTOCOL_10.equals(version.getValue())) {
                defaultHttpVersion = HTTPConstants.HEADER_PROTOCOL_10;
            } else {
                throw new AxisFault("Parameter "
                        + HTTPConstants.PROTOCOL_VERSION
                        + " Can have values only HTTP/1.0 or HTTP/1.1");
            }
        }

        // ---- Timeout values ----
        try {
            Parameter tempSoTimeoutParam = transportOut
                    .getParameter(HTTPConstants.SO_TIMEOUT);
            Parameter tempConnTimeoutParam = transportOut
                    .getParameter(HTTPConstants.CONNECTION_TIMEOUT);

            if (tempSoTimeoutParam != null) {
                soTimeout = Integer.parseInt((String) tempSoTimeoutParam.getValue());
            }

            if (tempConnTimeoutParam != null) {
                connectionTimeout = Integer.parseInt((String) tempConnTimeoutParam.getValue());
            }
        } catch (NumberFormatException nfe) {
            log.error("Invalid timeout value format: not a number", nfe);
        }

        // ---- Cached / pooled HttpClient (HttpClient 4.5.x) ----
        Parameter cacheHttpClientParam = transportOut.getParameter(HTTPConstants.CACHE_HTTP_CLIENT);
        if (cacheHttpClientParam != null && "true".equals(cacheHttpClientParam.getValue())) {

            Parameter defaultMaxConnectionsPerHostParam = transportOut.getParameter(
                    HTTPConstants.DEFAULT_MAX_CONNECTIONS_PER_HOST);
            Parameter totalConnectionsParam = transportOut.getParameter(
                    HTTPConstants.MAX_TOTAL_CONNECTIONS);

            // Replace MultiThreadedHttpConnectionManager with PoolingHttpClientConnectionManager
            connectionManager = new PoolingHttpClientConnectionManager();

            // Default max connections per route (was "per host" in 3.x)
            if (defaultMaxConnectionsPerHostParam != null &&
                    defaultMaxConnectionsPerHostParam.getValue() != null) {
                try {
                    int defaultMaxPerRoute = Integer.parseInt(
                            (String) defaultMaxConnectionsPerHostParam.getValue());
                    connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
                } catch (NumberFormatException nfe) {
                    log.error("Invalid defaultMaxConnectionsPerHost " +
                            "value format: not a number", nfe);
                    connectionManager.setDefaultMaxPerRoute(100);
                }
            } else {
                connectionManager.setDefaultMaxPerRoute(100);
            }

            // Max total connections
            if (totalConnectionsParam != null && totalConnectionsParam.getValue() != null) {
                try {
                    int totalConnections = Integer.parseInt(
                            (String) totalConnectionsParam.getValue());
                    connectionManager.setMaxTotal(totalConnections);
                } catch (NumberFormatException nfe) {
                    log.error("Invalid totalConnections value format: not a number", nfe);
                    connectionManager.setMaxTotal(1000);
                }
            } else {
                connectionManager.setMaxTotal(1000);
            }

            // In HttpClient 4.5.x, timeouts are set via RequestConfig (not on the
            // connection manager). Build a default RequestConfig and apply it to the client.
            RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setSocketTimeout(soTimeout)
                    .setConnectTimeout(connectionTimeout)
                    .setConnectionRequestTimeout(connectionTimeout) // timeout to obtain a connection from the pool
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .build();

            confContext.setProperty(HTTPConstants.REUSE_HTTP_CLIENT, "true");
            confContext.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpClient);
        }
    }

    public void stop() {
        // Shut down the connection manager to release all pooled connections.
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    // ------------------------------------------------------------------ invoke

    public InvocationResponse invoke(MessageContext msgContext)
            throws AxisFault {
        try {
            OMOutputFormat format = new OMOutputFormat();
            msgContext.setDoingMTOM(TransportUtils.doWriteMTOM(msgContext));
            msgContext.setDoingSwA(TransportUtils.doWriteSwA(msgContext));
            msgContext.setDoingREST(TransportUtils.isDoingREST(msgContext));
            format.setSOAP11(msgContext.isSOAP11());
            format.setDoOptimize(msgContext.isDoingMTOM());
            format.setDoingSWA(msgContext.isDoingSwA());
            format.setCharSetEncoding(TransportUtils.getCharSetEncoding(msgContext));

            Object mimeBoundaryProperty = msgContext
                    .getProperty(Constants.Configuration.MIME_BOUNDARY);
            if (mimeBoundaryProperty != null) {
                format.setMimeBoundary((String) mimeBoundaryProperty);
            }

            // Set the timeout properties
            Parameter soTimeoutParam = transportOut.getParameter(HTTPConstants.SO_TIMEOUT);
            Parameter connTimeoutParam = transportOut.getParameter(HTTPConstants.CONNECTION_TIMEOUT);
            Parameter disableKeepalive = transportOut.getParameter(HTTPConstants.NO_KEEPALIVE);

            // Set the property values only if they are not set by the user explicitly
            if ((soTimeoutParam != null) &&
                    (msgContext.getProperty(HTTPConstants.SO_TIMEOUT) == null)) {
                msgContext.setProperty(HTTPConstants.SO_TIMEOUT,
                        Integer.valueOf((String) soTimeoutParam.getValue()));
            }

            if ((connTimeoutParam != null) &&
                    (msgContext.getProperty(HTTPConstants.CONNECTION_TIMEOUT) == null)) {
                msgContext.setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                        Integer.valueOf((String) connTimeoutParam.getValue()));
            }

            if ((disableKeepalive != null) &&
                    (msgContext.getProperty(HTTPConstants.NO_KEEPALIVE) == null)) {
                msgContext.setProperty(HTTPConstants.NO_KEEPALIVE, disableKeepalive.getValue());
            }

            // If a parameter has been set, we will omit the SOAP action for SOAP 1.2
            if (!msgContext.isSOAP11()) {
                Parameter param = transportOut.getParameter(HTTPConstants.OMIT_SOAP_12_ACTION);
                Object parameterValue = null;
                if (param != null) {
                    parameterValue = param.getValue();
                }

                if (parameterValue != null && JavaUtils.isTrueExplicitly(parameterValue)) {
                    Object propertyValue = msgContext.getProperty(
                            Constants.Configuration.DISABLE_SOAP_ACTION);

                    if (propertyValue == null || !JavaUtils.isFalseExplicitly(propertyValue)) {
                        msgContext.setProperty(Constants.Configuration.DISABLE_SOAP_ACTION,
                                Boolean.TRUE);
                    }
                }
            }

            // Transport URL can be different from the WSA-To
            EndpointReference epr = null;
            String transportURL = (String) msgContext
                    .getProperty(Constants.Configuration.TRANSPORT_URL);

            if (transportURL != null) {
                epr = new EndpointReference(transportURL);
            } else if (msgContext.getTo() != null
                    && !msgContext.getTo().hasAnonymousAddress()) {
                epr = msgContext.getTo();
            }

            if (epr != null) {
                if (!epr.hasNoneAddress()) {
                    writeMessageWithCommons(msgContext, epr, format);
                } else {
                    if (msgContext.isFault()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Fault sent to WS-A None URI: "
                                    + msgContext.getEnvelope().getBody().getFault());
                        }
                    }
                }
            } else {
                if (msgContext.getProperty(MessageContext.TRANSPORT_OUT) != null) {
                    sendUsingOutputStream(msgContext, format);
                    TransportUtils.setResponseWritten(msgContext, true);
                } else {
                    throw new AxisFault("Both the TO and MessageContext.TRANSPORT_OUT property " +
                            "are null, so nowhere to send");
                }
            }
        } catch (FactoryConfigurationError e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        }
        return InvocationResponse.CONTINUE;
    }

    // ------------------------------------------------------------------ output stream path

    /**
     * Send a message (which must be a response) via the OutputStream sitting in the
     * MessageContext TRANSPORT_OUT property.
     *
     * @param msgContext the active MessageContext
     * @param format     output formatter for our message
     * @throws AxisFault if a general problem arises
     */
    private void sendUsingOutputStream(MessageContext msgContext,
                                       OMOutputFormat format) throws AxisFault {
        OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);

        OutTransportInfo transportInfo = (OutTransportInfo) msgContext
                .getProperty(Constants.OUT_TRANSPORT_INFO);

        if (transportInfo == null) throw new AxisFault("No transport info in MessageContext");

        ServletBasedOutTransportInfo servletBasedOutTransportInfo = null;
        if (transportInfo instanceof ServletBasedOutTransportInfo) {
            servletBasedOutTransportInfo =
                    (ServletBasedOutTransportInfo) transportInfo;

            // If sending a fault, set HTTP status code to 500
            if (msgContext.isFault()) {
                servletBasedOutTransportInfo.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            addCustomHeaders(msgContext, servletBasedOutTransportInfo);

        } else if (transportInfo instanceof AxisHttpResponseImpl) {
            addCustomHeadersToAxisResponse(msgContext, (AxisHttpResponseImpl) transportInfo);
        }

        format.setAutoCloseWriter(true);

        MessageFormatter messageFormatter = MessageProcessorSelector.getMessageFormatter(msgContext);
        if (messageFormatter == null) throw new AxisFault("No MessageFormatter in MessageContext");

        try {
            transportInfo.setContentType(
                    messageFormatter.getContentType(msgContext, format, findSOAPAction(msgContext)));

            Object gzip = msgContext.getOptions().getProperty(HTTPConstants.MC_GZIP_RESPONSE);
            if (gzip != null && JavaUtils.isTrueExplicitly(gzip)) {
                if (servletBasedOutTransportInfo != null) {
                    servletBasedOutTransportInfo.addHeader(
                            HTTPConstants.HEADER_CONTENT_ENCODING,
                            HTTPConstants.COMPRESSION_GZIP);
                }
                try {
                    out = new GZIPOutputStream(out);
                    out.write(messageFormatter.getBytes(msgContext, format));
                    ((GZIPOutputStream) out).finish();
                    out.flush();
                } catch (IOException e) {
                    throw new AxisFault("Could not compress response");
                }
            } else {
                messageFormatter.writeTo(msgContext, format, out, false);
            }
        } catch (AxisFault axisFault) {
            log.error(axisFault.getMessage(), axisFault);
            throw axisFault;
        }
    }

    /**
     * Helper: add custom HTTP headers from MessageContext to a ServletBasedOutTransportInfo.
     */
    @SuppressWarnings("unchecked")
    private void addCustomHeaders(MessageContext msgContext,
                                  ServletBasedOutTransportInfo transportInfo) {
        Object customHeaders = msgContext.getProperty(HTTPConstants.HTTP_HEADERS);
        if (customHeaders == null) return;

        if (customHeaders instanceof List) {
            for (Object obj : (List<?>) customHeaders) {
                // Note: this is the commons-httpclient Header in Axis2's classpath.
                // If Axis2 has been updated to use HttpClient 4.x Header, adjust accordingly.
                org.apache.commons.httpclient.Header header =
                        (org.apache.commons.httpclient.Header) obj;
                if (header != null) {
                    transportInfo.addHeader(header.getName(), header.getValue());
                }
            }
        } else if (customHeaders instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) customHeaders).entrySet()) {
                if (entry != null) {
                    transportInfo.addHeader((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
    }

    /**
     * Helper: add custom HTTP headers from MessageContext to an AxisHttpResponseImpl.
     */
    @SuppressWarnings("unchecked")
    private void addCustomHeadersToAxisResponse(MessageContext msgContext,
                                                AxisHttpResponseImpl transportInfo) {
        Object customHeaders = msgContext.getProperty(HTTPConstants.HTTP_HEADERS);
        if (customHeaders == null) return;

        if (customHeaders instanceof List) {
            for (Object obj : (List<?>) customHeaders) {
                org.apache.commons.httpclient.Header header =
                        (org.apache.commons.httpclient.Header) obj;
                if (header != null) {
                    transportInfo.addHeader(header.getName(), header.getValue());
                }
            }
        } else if (customHeaders instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) customHeaders).entrySet()) {
                if (entry != null) {
                    transportInfo.addHeader((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
    }

    // ------------------------------------------------------------------ commons sender path

    private void writeMessageWithCommons(MessageContext messageContext,
                                         EndpointReference toEPR, OMOutputFormat format)
            throws AxisFault {

        // Streaming fast path: if a VTInputStreamPipe is available on the context
        // and no mediator has forced buildMessage, send the body directly from the
        // pipe via HttpClient 4.x InputStreamEntity. Mirrors the NIO non-blocking
        // behaviour where raw bytes flow from Pipe → backend without hitting the
        // OM tree.
        Object pipeObj = messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        boolean builderInvoked = Boolean.TRUE.equals(
                messageContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));
        if (pipeObj instanceof VTInputStreamPipe && !builderInvoked) {
            sendStreamedRequest(messageContext, toEPR, (VTInputStreamPipe) pipeObj);
            return;
        }

        try {
            URL url = new URL(toEPR.getAddress());

            AbstractHTTPSender sender = new HTTPSender();

            boolean chunked;
            if (messageContext.getProperty(HTTPConstants.CHUNKED) != null) {
                chunked = JavaUtils.isTrueExplicitly(
                        messageContext.getProperty(HTTPConstants.CHUNKED));
            } else {
                chunked = defaultChunked;
            }

            String httpVersion;
            if (messageContext.getProperty(HTTPConstants.HTTP_PROTOCOL_VERSION) != null) {
                httpVersion = (String) messageContext
                        .getProperty(HTTPConstants.HTTP_PROTOCOL_VERSION);
            } else {
                httpVersion = defaultHttpVersion;
            }

            // Order matters: HTTP/1.0 does not support chunk encoding
            sender.setChunked(chunked);
            sender.setHttpVersion(httpVersion);
            sender.setFormat(format);

            sender.send(messageContext, url, findSOAPAction(messageContext));
        } catch (MalformedURLException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        }
    }

    // ------------------------------------------------------------------ streaming sender path

    /**
     * Send the outbound request by streaming the body directly from the
     * {@link VTInputStreamPipe} via HttpClient 4.x {@link InputStreamEntity},
     * bypassing OM-tree serialisation. The cached {@link CloseableHttpClient}
     * is retrieved from the ConfigurationContext — same pooled client set up
     * in {@link #init(ConfigurationContext, TransportOutDescription)}.
     * <p>
     * After the HTTP call, response status / headers / body stream are placed
     * on the message context so Axis2's {@code OperationClient} can build the
     * response envelope via its standard builder pipeline.
     */
    private void sendStreamedRequest(MessageContext msgContext, EndpointReference toEPR,
                                     VTInputStreamPipe vtPipe) throws AxisFault {
        CloseableHttpResponse response = null;
        try {
            String url = toEPR.getAddress();
            String httpMethod = (String) msgContext.getProperty(
                    Constants.Configuration.HTTP_METHOD);
            if (httpMethod == null) {
                httpMethod = "POST";
            }
            httpMethod = httpMethod.toUpperCase();

            RequestBuilder reqBuilder = RequestBuilder.create(httpMethod).setUri(url);

            // Copy transport headers (skip hop-by-hop / HttpClient-managed)
            @SuppressWarnings("unchecked")
            Map<String, Object> transportHeaders = (Map<String, Object>) msgContext
                    .getProperty(MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                for (Map.Entry<String, Object> e : transportHeaders.entrySet()) {
                    if (e.getValue() != null && !isRestrictedHeader(e.getKey())) {
                        reqBuilder.addHeader(e.getKey(), e.getValue().toString());
                    }
                }
            }

            boolean hasBody = !HTTPConstants.HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)
                    && !"DELETE".equalsIgnoreCase(httpMethod);
            if (hasBody) {
                String ct = (String) msgContext.getProperty(
                        Constants.Configuration.CONTENT_TYPE);
                ContentType parsedCT;
                try {
                    parsedCT = ct != null
                            ? ContentType.parse(ct)
                            : ContentType.APPLICATION_OCTET_STREAM;
                } catch (Exception parseEx) {
                    parsedCT = ContentType.APPLICATION_OCTET_STREAM;
                }
                InputStream bodyStream = vtPipe.getInputStream();
                if (bodyStream != null) {
                    reqBuilder.setEntity(new InputStreamEntity(bodyStream, parsedCT));
                }
            }

            CloseableHttpClient httpClient = (CloseableHttpClient) msgContext
                    .getConfigurationContext().getProperty(HTTPConstants.CACHED_HTTP_CLIENT);
            if (httpClient == null) {
                throw new AxisFault("VT cached HttpClient not available — ensure VTHttpSender "
                        + "is registered in axis2_blocking_client.xml with cacheHttpClient=true");
            }

            if (log.isDebugEnabled()) {
                log.debug("VTHttpSender streaming " + httpMethod + " to " + url
                        + " (body from VTInputStreamPipe, no OM build)");
            }

            response = httpClient.execute(reqBuilder.build());

            populateResponseOnMessageContext(msgContext, response);

        } catch (IOException e) {
            // Make sure we don't leak the response if execute succeeded but
            // response population failed.
            if (response != null) {
                try { response.close(); } catch (IOException ignore) { }
            }
            log.debug(e);
            throw AxisFault.makeFault(e);
        }
    }

    /**
     * Populate the message context with the HTTP response status, headers, and
     * body input stream, in the same shape Axis2's {@code AbstractHTTPSender}
     * leaves things — so {@code OperationClient.execute()} → builder pipeline
     * can read the response via {@code TRANSPORT_IN}.
     */
    private void populateResponseOnMessageContext(MessageContext msgContext,
                                                  CloseableHttpResponse response)
            throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        msgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, statusCode);
        // SynapseConstants.HTTP_SENDER_STATUSCODE — string literal to avoid a
        // core-module dependency from the transports/nhttp module.
        msgContext.setProperty("transport.http.statusCode", String.valueOf(statusCode));

        Map<String, String> respHeaders = new HashMap<>();
        for (Header h : response.getAllHeaders()) {
            respHeaders.put(h.getName(), h.getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, respHeaders);

        Header ctHeader = response.getFirstHeader(HTTPConstants.HEADER_CONTENT_TYPE);
        if (ctHeader != null) {
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, ctHeader.getValue());
        }

        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            msgContext.setProperty(MessageContext.TRANSPORT_IN, response.getEntity().getContent());
        }

        // Stash the response so cleanup() / caller can close it (releases the
        // pooled connection). HTTPConstants.HTTP_METHOD is the canonical key
        // Axis2 uses for this, and cleanup() already handles HttpRequestBase;
        // we use a parallel key so we don't collide.
        msgContext.setProperty("VT_HTTP_RESPONSE", response);
    }

    /**
     * HttpClient manages Host / Content-Length / Transfer-Encoding / Connection
     * headers internally — passing them through causes duplicates or conflicts.
     */
    private static boolean isRestrictedHeader(String name) {
        String lc = name.toLowerCase();
        return "content-length".equals(lc)
                || "host".equals(lc)
                || "connection".equals(lc)
                || "transfer-encoding".equals(lc);
    }

    // ------------------------------------------------------------------ SOAP action helpers

    /**
     * @param actionString the action string to check
     * @return true if the specified String represents a generated (anonymous) name
     */
    public static boolean isGeneratedName(String actionString) {
        if (actionString == null) {
            return false;
        }

        if (actionString.indexOf("anon") >= 0) {
            if (actionString.equals("anonOutInOp") ||
                    actionString.endsWith(":anonOutInOp") ||
                    actionString.endsWith("/anonOutInOp") ||
                    actionString.endsWith("}anonOutInOp") ||

                    actionString.equals("anonOutonlyOp") ||
                    actionString.endsWith(":anonOutonlyOp") ||
                    actionString.endsWith("/anonOutonlyOp") ||
                    actionString.endsWith("}anonOutonlyOp") ||

                    actionString.equals("anonRobustOp") ||
                    actionString.endsWith(":anonRobustOp") ||
                    actionString.endsWith("/anonRobustOp") ||
                    actionString.endsWith("}anonRobustOp")) {
                return true;
            }
        }
        return false;
    }

    private static String findSOAPAction(MessageContext messageContext) {
        String soapActionString = null;

        Parameter parameter =
                messageContext.getTransportOut().getParameter(HTTPConstants.OMIT_SOAP_12_ACTION);
        if (parameter != null && JavaUtils.isTrueExplicitly(parameter.getValue()) &&
                !messageContext.isSOAP11()) {
            return "\"\"";
        }

        Object disableSoapAction = messageContext.getOptions().getProperty(
                Constants.Configuration.DISABLE_SOAP_ACTION);

        if (!JavaUtils.isTrueExplicitly(disableSoapAction)) {
            soapActionString = messageContext.getSoapAction();
            if (log.isDebugEnabled()) {
                log.debug("SOAP Action from messageContext : (" + soapActionString + ")");
            }
            if (isGeneratedName(soapActionString)) {
                if (log.isDebugEnabled()) {
                    log.debug("Will not use SOAP Action because (" + soapActionString
                            + ") was auto-generated");
                }
                soapActionString = null;
            }
            if ((soapActionString == null) || (soapActionString.length() == 0)) {
                soapActionString = messageContext.getWSAAction();
                if (log.isDebugEnabled()) {
                    log.debug("SOAP Action from getWSAAction was : (" + soapActionString + ")");
                }
                if (messageContext.getAxisOperation() != null
                        && ((soapActionString == null) || (soapActionString.length() == 0))) {
                    String axisOpSOAPAction = messageContext.getAxisOperation().getSoapAction();
                    if (log.isDebugEnabled()) {
                        log.debug("SOAP Action from AxisOperation was : ("
                                + axisOpSOAPAction + ")");
                    }
                    if (isGeneratedName(axisOpSOAPAction)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Will not override SOAP Action because ("
                                    + axisOpSOAPAction + ") in AxisOperation was auto-generated");
                        }
                    } else {
                        soapActionString = axisOpSOAPAction;
                    }
                }
            }
        }

        // Since action is optional for SOAP 1.2 we can return null here.
        if (soapActionString == null && messageContext.isSOAP11()) {
            soapActionString = "\"\"";
        }

        return soapActionString;
    }
}
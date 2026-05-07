package org.apache.synapse.transport.passthru.vt;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
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
    private static final String VT_CACHED_HTTP_CLIENT =
            "org.apache.synapse.transport.passthru.vt.CACHED_HTTP_CLIENT";

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

    /** Protocol scheme (passthrough support, merged from VTPassThroughHttpSender). */
    private Scheme scheme;

    /** Target configuration (passthrough support, merged from VTPassThroughHttpSender). */
    private TargetConfiguration targetConfiguration;

    /** Sender state. */
    private volatile int state = BaseConstants.STOPPED;

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

            confContext.setProperty(VT_CACHED_HTTP_CLIENT, httpClient);
        }

        // --- Passthrough support (merged from VTPassThroughHttpSender) ---
        scheme = new Scheme("http", 80, false);

        WorkerPool workerPool = null;
        Object wpObj = confContext.getProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        if (wpObj != null) {
            workerPool = (WorkerPool) wpObj;
        }

        PassThroughTransportMetricsCollector metrics =
                new PassThroughTransportMetricsCollector(false, scheme.getName());

        targetConfiguration = new TargetConfiguration(confContext, transportOut, workerPool, metrics, null);
        targetConfiguration.build();

        confContext.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                targetConfiguration.getWorkerPool());

        state = BaseConstants.STARTED;
    }

    public void stop() {
        state = BaseConstants.STOPPED;
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        state = BaseConstants.PAUSED;
        log.info("VTHttpSender Paused");
    }

    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        state = BaseConstants.STARTED;
        log.info("VTHttpSender Resumed");
    }

    // ------------------------------------------------------------------ invoke

    public InvocationResponse invoke(MessageContext msgContext)
            throws AxisFault {
        try {
            // --- Passthrough header cleanup (from VTPassThroughHttpSender) ---
            if (targetConfiguration != null) {
                PassThroughTransportUtils.removeUnwantedHeaders(msgContext, targetConfiguration);
            }

            if (AddressingHelper.isReplyRedirected(msgContext)
                    && !msgContext.getReplyTo().hasNoneAddress()) {
                msgContext.setProperty(PassThroughConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
            }

            // --- Format setup (existing VTHttpSender logic) ---
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

            boolean vtBackChannelResponse =
                    msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof VTBlockingServerWorker
                            && !AddressingHelper.isReplyRedirected(msgContext)
                            && Boolean.TRUE.equals(msgContext.getProperty("synapse.isresponse"));

            if (vtBackChannelResponse) {
                submitResponse(msgContext);
                if (msgContext.getOperationContext() != null) {
                    msgContext.getOperationContext().setProperty(
                            Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
                }
                return InvocationResponse.CONTINUE;
            }

            // --- EPR resolution ---
            EndpointReference epr = null;
            String transportURL = (String) msgContext
                    .getProperty(Constants.Configuration.TRANSPORT_URL);

            if (transportURL != null) {
                epr = new EndpointReference(transportURL);
            } else if (msgContext.getTo() != null
                    && !msgContext.getTo().hasAnonymousAddress()) {
                epr = msgContext.getTo();
            }

            // --- Dispatch ---
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
                // No EPR — this is a response back to the client
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof VTBlockingServerWorker) {
                    submitResponse(msgContext);
                } else if (msgContext.getProperty(MessageContext.TRANSPORT_OUT) != null) {
                    sendUsingOutputStream(msgContext, format);
                    TransportUtils.setResponseWritten(msgContext, true);
                } else {
                    throw new AxisFault("Both the TO and MessageContext.TRANSPORT_OUT property " +
                            "are null, so nowhere to send");
                }
            }

            // Mark response written (from VTPassThroughHttpSender)
            if (msgContext.getOperationContext() != null) {
                msgContext.getOperationContext().setProperty(
                        Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
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

        // VT backend path: use the HC4 client for every VT blocking backend call
        // so the backend response can always be exposed as VT_STREAM_PIPE. If a
        // raw request pipe is still available, the request body is streamed too;
        // if a mediator already built the request, the request is serialized
        // normally while the response still remains streamable.
        Object pipeObj = messageContext.getProperty(VTConstants.VT_STREAM_PIPE);
        boolean vtBackendCall = Boolean.TRUE.equals(messageContext.getProperty(VTConstants.VT_BACKEND_CALL));
        boolean builderInvoked = Boolean.TRUE.equals(
                messageContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));
        if (vtBackendCall) {
            VTInputStreamPipe vtPipe = pipeObj instanceof VTInputStreamPipe && !builderInvoked
                    ? (VTInputStreamPipe) pipeObj : null;
            sendStreamedRequest(messageContext, toEPR, vtPipe, format, builderInvoked);
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
     */
    private void sendStreamedRequest(MessageContext msgContext, EndpointReference toEPR,
                                     VTInputStreamPipe vtPipe, OMOutputFormat format,
                                     boolean builderInvoked) throws AxisFault {
        CloseableHttpResponse response = null;
        boolean ownershipTransferred = false;
        String url = toEPR.getAddress();
        String httpMethod = (String) msgContext.getProperty(
                Constants.Configuration.HTTP_METHOD);
        try {
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

            boolean methodAllowsBody = !HTTPConstants.HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)
                    && !"DELETE".equalsIgnoreCase(httpMethod);
            if (methodAllowsBody) {
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
                if (vtPipe != null && vtPipe.getInputStream() != null) {
                    InputStream bodyStream = vtPipe.getInputStream();
                    long contentLength = getContentLength(transportHeaders);
                    InputStreamEntity entity = new InputStreamEntity(bodyStream, contentLength, parsedCT);
                    entity.setChunked(contentLength < 0 && defaultChunked);
                    reqBuilder.setEntity(entity);
                } else if (builderInvoked && !Boolean.TRUE.equals(
                        msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY))) {
                    MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgContext);
                    if (formatter == null) {
                        throw new AxisFault("No MessageFormatter in MessageContext");
                    }
                    ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream(8192);
                    formatter.writeTo(msgContext, format, bodyBuffer, false);
                    byte[] bodyBytes = bodyBuffer.toByteArray();
                    if (bodyBytes.length > 0) {
                        ContentType formatterContentType = parseContentType(formatter.getContentType(
                                msgContext, format, findSOAPAction(msgContext)), parsedCT);
                        reqBuilder.setEntity(new ByteArrayEntity(bodyBytes, formatterContentType));
                    }
                }
            }

            CloseableHttpClient httpClient = getOrCreateHttpClient(msgContext.getConfigurationContext());

            if (log.isDebugEnabled()) {
                log.debug("VTHttpSender streaming " + httpMethod + " to " + url
                        + " (body from VTInputStreamPipe, no OM build)");
            }
            response = httpClient.execute(reqBuilder.build());

            setupStreamingResponse(msgContext, response);
            ownershipTransferred = true;

        } catch (IOException e) {
            log.warn("VTHttpSender streaming request failed: " + httpMethod + " " + url, e);
            throw AxisFault.makeFault(e);
        } finally {
            if (response != null && !ownershipTransferred) {
                try { response.close(); } catch (IOException ignore) { }
            }
        }
    }

    /**
     * Wire the backend response into the Axis2 IN MessageContext for streaming
     */
    private void setupStreamingResponse(MessageContext msgContext,
                                        final CloseableHttpResponse response)
            throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        msgContext.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, statusCode);
        msgContext.setProperty(PassThroughConstants.HTTP_SC, statusCode);
        msgContext.setProperty(PassThroughConstants.HTTP_SC_DESC,
                response.getStatusLine().getReasonPhrase());
        msgContext.setProperty("transport.http.statusCode", String.valueOf(statusCode));

        Map<String, String> respHeaders = new HashMap<>();
        for (Header h : response.getAllHeaders()) {
            respHeaders.put(h.getName(), h.getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, respHeaders);

        Header ctHeader = response.getFirstHeader(HTTPConstants.HEADER_CONTENT_TYPE);
        if (ctHeader != null) {
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, ctHeader.getValue());
            msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    TransportUtils.getContentType(ctHeader.getValue(), msgContext));
        }

        // Wrap the backend response stream so close() also releases the
        // pooled HC4 connection (via CloseableHttpResponse.close()).
        InputStream rawContent = (response.getEntity() != null)
                ? response.getEntity().getContent() : null;
        InputStream wrappedContent = (rawContent == null) ? null
                : new FilterInputStream(rawContent) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            response.close();
                        }
                    }
                };

        OperationContext opCtx = msgContext.getOperationContext();
        MessageContext inMsgCtx = (opCtx == null) ? null
                : opCtx.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

        if (wrappedContent != null) {
            msgContext.setProperty(VTConstants.VT_STREAM_PIPE,
                    new VTInputStreamPipe(wrappedContent));
            msgContext.removeProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED);
            msgContext.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
        } else {
            msgContext.removeProperty(VTConstants.VT_STREAM_PIPE);
            msgContext.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
        }

        if (inMsgCtx != null) {
            // Pre-set a default envelope so handleResponse() skips createSOAPMessage().
            SOAPFactory soapFactory = msgContext.isSOAP11()
                    ? OMAbstractFactory.getSOAP11Factory()
                    : OMAbstractFactory.getSOAP12Factory();
            inMsgCtx.setEnvelope(soapFactory.getDefaultEnvelope());
            inMsgCtx.setProperty(HTTPConstants.MC_HTTP_STATUS_CODE, statusCode);
            inMsgCtx.setProperty(PassThroughConstants.HTTP_SC, statusCode);
            inMsgCtx.setProperty(PassThroughConstants.HTTP_SC_DESC,
                    response.getStatusLine().getReasonPhrase());
            inMsgCtx.setProperty("transport.http.statusCode", String.valueOf(statusCode));
            inMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, respHeaders);
            if (ctHeader != null) {
                inMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, ctHeader.getValue());
                inMsgCtx.setProperty(Constants.Configuration.MESSAGE_TYPE,
                        TransportUtils.getContentType(ctHeader.getValue(), inMsgCtx));
            }

            if (wrappedContent != null) {
                inMsgCtx.setProperty(VTConstants.VT_STREAM_PIPE,
                        msgContext.getProperty(VTConstants.VT_STREAM_PIPE));
                inMsgCtx.removeProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED);
                inMsgCtx.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
            } else {
                inMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
            }
        }
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

    private static long getContentLength(Map<String, Object> transportHeaders) {
        String contentLength = getHeaderValue(transportHeaders, HTTPConstants.HEADER_CONTENT_LENGTH);
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength.trim());
            } catch (NumberFormatException ignore) {
                return -1;
            }
        }
        return -1;
    }

    private static ContentType parseContentType(String contentType, ContentType defaultContentType) {

        if (contentType != null) {
            try {
                return ContentType.parse(contentType);
            } catch (Exception ignore) {
                return defaultContentType;
            }
        }
        return defaultContentType;
    }

    private static String getHeaderValue(Map<String, Object> transportHeaders, String name) {
        if (transportHeaders == null || name == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : transportHeaders.entrySet()) {
            if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null) {
                return entry.getValue().toString();
            }
        }
        return null;
    }

    private CloseableHttpClient getOrCreateHttpClient(ConfigurationContext configurationContext)
            throws AxisFault {
        Object cached = configurationContext.getProperty(VT_CACHED_HTTP_CLIENT);
        if (cached instanceof CloseableHttpClient) {
            return (CloseableHttpClient) cached;
        }
        if (cached != null) {
            throw new AxisFault("Cached VT HTTP client is not an HttpClient 4.x CloseableHttpClient: "
                    + cached.getClass().getName());
        }

        synchronized (this) {
            cached = configurationContext.getProperty(VT_CACHED_HTTP_CLIENT);
            if (cached instanceof CloseableHttpClient) {
                return (CloseableHttpClient) cached;
            }
            if (cached != null) {
                throw new AxisFault("Cached VT HTTP client is not an HttpClient 4.x CloseableHttpClient: "
                        + cached.getClass().getName());
            }

            if (connectionManager == null) {
                connectionManager = new PoolingHttpClientConnectionManager();
                connectionManager.setDefaultMaxPerRoute(100);
                connectionManager.setMaxTotal(1000);
            }

            RequestConfig defaultRequestConfig = RequestConfig.custom()
                    .setSocketTimeout(soTimeout)
                    .setConnectTimeout(connectionTimeout)
                    .setConnectionRequestTimeout(connectionTimeout)
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(defaultRequestConfig)
                    .build();
            configurationContext.setProperty(VT_CACHED_HTTP_CLIENT, httpClient);
            return httpClient;
        }
    }

    // ------------------------------------------------------------------ passthrough response path

    /**
     * Write the response back to the client via the VTBlockingServerWorker.
     * This path is taken when there is no EPR (i.e. this is a response, not a backend call)
     * and the OUT_TRANSPORT_INFO is a VTBlockingServerWorker.
     */
    private void submitResponse(MessageContext msgContext) throws AxisFault {
        VTBlockingServerWorker serverWorker =
                (VTBlockingServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        if (serverWorker == null) {
            throw new AxisFault("No VTBlockingServerWorker found to submit response");
        }
        serverWorker.submitResponse(msgContext);
    }

    // ------------------------------------------------------------------ accessor

    public TargetConfiguration getTargetConfiguration() {
        return targetConfiguration;
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

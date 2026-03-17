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

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A fully blocking HTTP transport sender that uses Java 21+ Virtual Threads
 * and Apache HttpClient 4.x for outbound calls.
 * <p>
 * All outbound HTTP calls are made using blocking {@code CloseableHttpClient.execute()}
 * within Virtual Threads. The shared {@link CloseableHttpClient} is configured with
 * a {@link PoolingHttpClientConnectionManager} for connection reuse.
 * Each {@code invoke()} call builds an HTTP request via {@link RequestBuilder},
 * executes it, reads the response, and feeds it back through the Axis2 engine —
 * all in the same Virtual Thread that originated from the listener.
 * </p>
 *
 * <p>Configure in axis2.xml as a {@code transportSender}:</p>
 * <pre>
 * &lt;transportSender name="vt-http" class="org.apache.synapse.transport.passthru.vt.VTPassThroughHttpSender"/&gt;
 * </pre>
 */
public class VTPassThroughHttpSender extends AbstractHandler implements TransportSender {

    protected Log log;

    /** Headers managed by Apache HttpClient — must not be set by application code. */
    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "host", "content-length", "transfer-encoding", "connection"
    );

    /** Protocol scheme */
    private Scheme scheme;

    /** Target configuration */
    private TargetConfiguration targetConfiguration;

    /** Name prefix for logging */
    private String namePrefix;

    /** State */
    private volatile int state = BaseConstants.STOPPED;

    /** Connection / read timeout values */
    private int connectTimeout = VTConstants.DEFAULT_CONNECT_TIMEOUT;
    private int soTimeout = VTConstants.DEFAULT_SO_TIMEOUT;

    /** The shared Apache HttpClient instance (thread-safe, connection-pooled) */
    private CloseableHttpClient httpClient;

    public VTPassThroughHttpSender() {
        log = LogFactory.getLog(this.getClass().getName());
    }

    protected Scheme getScheme() {
        return new Scheme("http", 80, false);
    }

    /**
     * Build the {@link CloseableHttpClient} used for all outbound calls.
     * Subclasses (SSL) may override to supply a custom {@link javax.net.ssl.SSLContext}.
     */
    protected CloseableHttpClient buildHttpClient() {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(200);
        connManager.setMaxTotal(1000);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(soTimeout)
                .setRedirectsEnabled(false)
                .build();

        return HttpClientBuilder.create()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Override
    public void init(ConfigurationContext cfgCtx, TransportOutDescription transportOut) throws AxisFault {
        log.info("Initializing Virtual Thread Pass-through HTTP/S Sender...");

        namePrefix = transportOut.getName().toUpperCase(Locale.US);
        scheme = getScheme();

        WorkerPool workerPool = null;
        Object obj = cfgCtx.getProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        if (obj != null) {
            workerPool = (WorkerPool) obj;
        }

        PassThroughTransportMetricsCollector metrics =
                new PassThroughTransportMetricsCollector(false, scheme.getName());

        targetConfiguration = new TargetConfiguration(cfgCtx, transportOut, workerPool, metrics, null);
        targetConfiguration.build();

        // Read optional parameters
        Parameter ctParam = transportOut.getParameter(VTConstants.PARAM_CONNECT_TIMEOUT);
        if (ctParam != null) {
            connectTimeout = Integer.parseInt(ctParam.getValue().toString());
        }
        Parameter stParam = transportOut.getParameter(VTConstants.PARAM_SO_TIMEOUT);
        if (stParam != null) {
            soTimeout = Integer.parseInt(stParam.getValue().toString());
        }

        cfgCtx.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                targetConfiguration.getWorkerPool());

        // Build the shared Apache HttpClient with connection pool
        httpClient = buildHttpClient();

        state = BaseConstants.STARTED;
        log.info("Virtual Thread Pass-through " + namePrefix + " Sender started.");
    }

    @Override
    public void cleanup(MessageContext msgContext) throws AxisFault {
        VTTargetResponse targetResponse =
                (VTTargetResponse) msgContext.getProperty(VTConstants.VT_TARGET_RESPONSE);
        if (targetResponse != null) {
            targetResponse.close();
        }
    }

    @Override
    public void stop() {
        state = BaseConstants.STOPPED;
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.warn("Error closing Apache HttpClient", e);
            }
        }
        log.info("Virtual Thread Pass-through " + namePrefix + " Sender stopped.");
    }

    @Override
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {

        PassThroughTransportUtils.removeUnwantedHeaders(msgContext, targetConfiguration);

        if (AddressingHelper.isReplyRedirected(msgContext)
                && !msgContext.getReplyTo().hasNoneAddress()) {
            msgContext.setProperty(PassThroughConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
        }

        EndpointReference epr = PassThroughTransportUtils.getDestinationEPR(msgContext);
        if (epr != null) {
            if (!epr.hasNoneAddress()) {
                sendToBackend(msgContext, epr);
            } else {
                handleException("Cannot send message to " + AddressingConstants.Final.WSA_NONE_URI);
            }
        } else {
            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof VTBlockingServerWorker) {
                    try {
                        submitResponse(msgContext);
                    } catch (Exception e) {
                        handleException("Failed to submit the response", e);
                    }
                } else {
                    sendUsingOutputStream(msgContext);
                }
            } else {
                handleException("No valid destination EPR to send message");
            }
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext().setProperty(
                    Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * Send the outbound request to a backend using the shared Apache {@link CloseableHttpClient}.
     * The call to {@code execute()} is blocking, which is cheap inside a Virtual Thread.
     */
    private void sendToBackend(MessageContext msgContext, EndpointReference epr) throws AxisFault {
        try {
            String url = epr.getAddress();
            msgContext.setProperty(VTConstants.VT_TARGET_CONFIGURATION, targetConfiguration);

            String httpMethod = (String) msgContext.getProperty(Constants.Configuration.HTTP_METHOD);
            if (httpMethod == null) {
                httpMethod = "POST";
            }
            httpMethod = httpMethod.toUpperCase();

            RequestBuilder reqBuilder = RequestBuilder.create(httpMethod).setUri(url);

            // Copy transport headers, skipping those managed by HttpClient
            @SuppressWarnings("unchecked")
            Map<String, Object> transportHeaders =
                    (Map<String, Object>) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                for (Map.Entry<String, Object> entry : transportHeaders.entrySet()) {
                    String name = entry.getKey();
                    if (entry.getValue() != null
                            && !RESTRICTED_HEADERS.contains(name.toLowerCase())) {
                        reqBuilder.addHeader(name, entry.getValue().toString());
                    }
                }
            }

            boolean hasBody = !HTTPConstants.HTTP_METHOD_GET.equals(httpMethod)
                    && !("DELETE".equalsIgnoreCase(httpMethod)
                    && msgContext.getEnvelope().getBody().getFirstElement() == null);

            if (hasBody) {
                MessageFormatter formatter =
                        MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
                OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);

                String contentType = formatter.getContentType(
                        msgContext, format, msgContext.getSoapAction());

                ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream(8192);
                formatter.writeTo(msgContext, format, bodyBuffer, false);

                reqBuilder.setEntity(new ByteArrayEntity(
                        bodyBuffer.toByteArray(), ContentType.parse(contentType)));
            }

            // Blocking execute — cheap in a Virtual Thread
            CloseableHttpResponse httpResponse = httpClient.execute(reqBuilder.build());

            VTTargetResponse targetResponse = new VTTargetResponse(httpResponse);
            msgContext.setProperty(VTConstants.VT_TARGET_RESPONSE, targetResponse);

            VTBlockingClientWorker clientWorker = new VTBlockingClientWorker(
                    targetConfiguration, msgContext, targetResponse);
            clientWorker.run(); // run in same VT — fully blocking

            // Explicitly close the backend response so the pooled
            // HTTP connection is released cleanly.  If the response
            // body was not fully consumed by the message builder,
            // HttpClient will discard (not reuse) the connection —
            // which is safe.  Without this close, the connection
            // stays "in use" and the next request on the same
            // keep-alive connection may read stale data from the
            // backend pool, causing 405 / corrupt responses.
            targetResponse.close();

        } catch (IOException e) {
            handleException("Error sending request to backend: " + epr.getAddress(), e);
        }
    }

    private void submitResponse(MessageContext msgContext) throws AxisFault {
        VTBlockingServerWorker serverWorker =
                (VTBlockingServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
        if (serverWorker == null) {
            throw new AxisFault("No VTBlockingServerWorker found to submit response");
        }
        serverWorker.submitResponse(msgContext);
    }

    private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {
        OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
        MessageFormatter formatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);

        if (msgContext.isServerSide()) {
            OutTransportInfo transportInfo =
                    (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
            if (transportInfo != null) {
                transportInfo.setContentType(
                        formatter.getContentType(msgContext, format, msgContext.getSoapAction()));
            } else {
                throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
            }
        }

        try {
            formatter.writeTo(msgContext, format, out, false);
            out.close();
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }
    }

    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        state = BaseConstants.PAUSED;
        log.info(namePrefix + " Sender Paused");
    }

    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        state = BaseConstants.STARTED;
        log.info(namePrefix + " Sender Resumed");
    }

    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }

    // ---- Accessors ----

    public TargetConfiguration getTargetConfiguration() {
        return targetConfiguration;
    }

    public Scheme getSenderScheme() {
        return scheme;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }
}

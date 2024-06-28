/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.base.MetricsCollector;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.commons.logger.ContextAwareLogger;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.http.conn.LoggingNHttpClientConnection;
import org.apache.synapse.transport.http.conn.ProxyTunnelHandler;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.PassThroughCorrelationConfigDataHolder;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.HostConnections;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class is handling events from the transport -- > client.
 */
public class TargetHandler implements NHttpClientEventHandler {
    private static Log log = LogFactory.getLog(TargetHandler.class);

    /** log for correlation.log */
    private static final Log correlationLog = LogFactory.getLog(PassThroughConstants.CORRELATION_LOGGER);
    private static final Log transportLatencyLog = LogFactory.getLog(PassThroughConstants.TRANSPORT_LATENCY_LOGGER);

    /** Delivery agent */
    private final DeliveryAgent deliveryAgent;

    /** Connection factory */
    private ClientConnFactory connFactory;
    
    /** Configuration used by the sender */
    private final TargetConfiguration targetConfiguration;

    /** Error handler for injecting faults */
    private final TargetErrorHandler targetErrorHandler;

    private PassThroughTransportMetricsCollector metrics = null;

    private static boolean isMessageSizeValidationEnabled = false;

    private static int validMaxMessageSize = Integer.MAX_VALUE;

    public static final String PROPERTY_FILE = "passthru-http.properties";
    public static final String MESSAGE_SIZE_VALIDATION = "message.size.validation.enabled";
    public static final String VALID_MAX_MESSAGE_SIZE = "valid.max.message.size.in.bytes";
    public static final String CONNECTION_POOL = "CONNECTION_POOL";

    private List<StreamInterceptor> streamInterceptors;
    private boolean interceptStream;
    private int noOfInterceptors;
    private static List<String> allowedResponseProperties = new ArrayList<>();

    private PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    public TargetHandler(DeliveryAgent deliveryAgent, ClientConnFactory connFactory,
                         TargetConfiguration configuration) {
        this(deliveryAgent, connFactory, configuration, new ArrayList<StreamInterceptor>());
    }

    public TargetHandler(DeliveryAgent deliveryAgent,
                         ClientConnFactory connFactory,
                         TargetConfiguration configuration,
                         List<StreamInterceptor> interceptors) {
        this.deliveryAgent = deliveryAgent;
        this.connFactory = connFactory;
        this.targetConfiguration = configuration;
        this.targetErrorHandler = new TargetErrorHandler(targetConfiguration);
        this.metrics = targetConfiguration.getMetrics();
        this.streamInterceptors = interceptors;
        this.interceptStream = !streamInterceptors.isEmpty();
        this.noOfInterceptors = streamInterceptors.size();

        Properties props = MiscellaneousUtil.loadProperties(PROPERTY_FILE);
        String validationProperty = MiscellaneousUtil.getProperty(props, MESSAGE_SIZE_VALIDATION, "false");
        String validMaxMessageSizeStr = MiscellaneousUtil
                .getProperty(props, VALID_MAX_MESSAGE_SIZE, String.valueOf(Integer.MAX_VALUE));
        isMessageSizeValidationEnabled = Boolean.valueOf(validationProperty);
        try {
            validMaxMessageSize = Integer.valueOf(validMaxMessageSizeStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid max message size configured for property \"valid.max.message.size.in.bytes\", "
                    + "setting the Integer MAX_VALUE as the valid maximum message size", e);
            validMaxMessageSize = Integer.MAX_VALUE;
        }

        PassThroughConfiguration conf = PassThroughConfiguration.getInstance();
        String properties = conf.getAllowedResponseProperties();
        if (properties != null) {
            String[] propertyList = properties.trim().split(",");
            for (String property : propertyList) {
                allowedResponseProperties.add(property.trim());
            }
        }
    }

    public void connected(NHttpClientConnection conn, Object o) {
        long connectionCreationTimestamp = 0;
        if (transportLatencyLog.isDebugEnabled()) {
            connectionCreationTimestamp = System.currentTimeMillis();
        }
        assert o instanceof HostConnections : "Attachment should be a HostConnections";
        HostConnections pool = (HostConnections) o;
        conn.getContext().setAttribute(PassThroughConstants.CONNECTION_POOL, pool);
        HttpRoute route = pool.getRoute();
          
        // create the connection information and set it to request ready
        TargetContext.create(conn, ProtocolState.REQUEST_READY, targetConfiguration);

        // notify the pool about the new connection
        targetConfiguration.getConnections().addConnection(conn);

        // notify about the new connection
        deliveryAgent.connected(pool.getRoute(), conn);
        
        HttpContext context = conn.getContext();
        context.setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME, System.currentTimeMillis());
        metrics.connected();
        
        if (route.isTunnelled()) {
            // Requires a proxy tunnel
            ProxyTunnelHandler tunnelHandler = new ProxyTunnelHandler(route, connFactory);
            context.setAttribute(PassThroughConstants.TUNNEL_HANDLER, tunnelHandler);
        }
        if (transportLatencyLog.isDebugEnabled()) {
            transportLatencyLog.debug(context.getAttribute(CorrelationConstants.CORRELATION_ID) + "|" +
                    "Connection established at time stamp: " + connectionCreationTimestamp + " for route: " + route);
        }
    }

    public void requestReady(NHttpClientConnection conn) {
        HttpContext context = conn.getContext();
        ProtocolState connState = null;
        try {
            
            connState = TargetContext.getState(conn);

            if (connState == ProtocolState.REQUEST_DONE || connState == ProtocolState.RESPONSE_BODY) {
                return;
            }

            if (connState != ProtocolState.REQUEST_READY) {
                handleInvalidState(conn, "Request not started");
                return;
            }

            ProxyTunnelHandler tunnelHandler = (ProxyTunnelHandler) context.getAttribute(PassThroughConstants.TUNNEL_HANDLER);
            if (tunnelHandler != null && !tunnelHandler.isCompleted()) {
                Object targetHost = TargetContext.get(conn).getRequestMsgCtx().getProperty(PassThroughConstants.PROXY_PROFILE_TARGET_HOST);
                context.setAttribute(PassThroughConstants.PROXY_PROFILE_TARGET_HOST, targetHost);
                if (!tunnelHandler.isRequested()) {
                    HttpRequest request = tunnelHandler.generateRequest(context);
                    if (targetConfiguration.getProxyAuthenticator() != null) {
                        targetConfiguration.getProxyAuthenticator().authenticatePreemptively(request, context);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(conn + ": Sending CONNECT request to " + tunnelHandler.getProxy());
                    }
                    conn.submitRequest(request);
                    tunnelHandler.setRequested();
                }
                return;
            }
            
            TargetRequest request = TargetContext.getRequest(conn);
            if (request != null) {
                TargetContext targetContext = TargetContext.get(conn);
                targetContext.updateLastStateUpdatedTime();
                request.start(conn);
                targetConfiguration.getMetrics().incrementMessagesSent();
            }

            if (transportLatencyLog.isDebugEnabled()) {
                String route = request == null ? "null" : request.getRoute().toString();
                transportLatencyLog.debug(context.getAttribute(CorrelationConstants.CORRELATION_ID) + "|" +
                        "Request writing started at time stamp: " + System.currentTimeMillis() + " and route: " +
                        route);
            }
            context.setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME, System.currentTimeMillis());
            context.setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME, System.currentTimeMillis());
        } catch (IOException e) {
            logIOException(conn, e);
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);

            MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
            if (requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.SND_IO_ERROR,
                        "Error in Sender",
                        null,
                        connState);
            }
        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);

            MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
            if (requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.SND_HTTP_ERROR,
                        "Error in Sender",
                        null,
                        connState);
            }
        }
    }

    public void outputReady(NHttpClientConnection conn, ContentEncoder encoder) {
        ProtocolState connState = null;
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        if (transportLatencyLog.isTraceEnabled()) {
            HttpContext context = conn.getContext();
            HostConnections pool = (HostConnections) context.getAttribute(CONNECTION_POOL);
            String route = pool == null ? "null" : pool.getRoute().toString();
            transportLatencyLog.trace(context.getAttribute(CorrelationConstants.CORRELATION_ID) + "|" +
                    "Writing request chunk to Backend at time stamp: " + System.currentTimeMillis() +
                    " and route: " + route);
        }
        try {
            connState = TargetContext.getState(conn);
            if (connState != ProtocolState.REQUEST_HEAD &&
                    connState != ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing message body");
                return;
            }

            TargetRequest request = TargetContext.getRequest(conn);
            if (request.hasEntityBody()) {
                int bytesWritten = -1;
                boolean interceptionEnabled = false;
                Boolean[] interceptorResults = new Boolean[noOfInterceptors];
                if (interceptStream) {
                    int index = 0;
                    for (StreamInterceptor interceptor : streamInterceptors) {
                        interceptorResults[index] = interceptor.interceptTargetRequest(
                                (MessageContext) conn.getContext()
                                        .getAttribute(PassThroughConstants.REQUEST_MESSAGE_CONTEXT));
                        if (!interceptionEnabled && interceptorResults[index]) {
                            interceptionEnabled = true;
                        }
                        index++;
                    }
                    if (interceptionEnabled) {
                        ByteBuffer bytesSent = request.copyAndWrite(conn, encoder);
                        if (bytesSent != null) {
                            bytesWritten = bytesSent.remaining();
                            index = 0;
                            for (StreamInterceptor interceptor : streamInterceptors) {
                                if (interceptorResults[index]) {
                                    interceptor.targetRequest(bytesSent.duplicate().asReadOnlyBuffer(),
                                                              (MessageContext) conn.getContext().getAttribute(
                                                                      PassThroughConstants.REQUEST_MESSAGE_CONTEXT));
                                }
                                index++;
                            }
                        }
                    } else {
                        bytesWritten = request.write(conn, encoder);
                    }
                } else {
                    bytesWritten = request.write(conn, encoder);
                }
                if (bytesWritten > 0) {
                    if (metrics.getLevel() == MetricsCollector.LEVEL_FULL) {
                        metrics.incrementBytesSent(requestMsgCtx, bytesWritten);
                    } else {
                        metrics.incrementBytesSent(bytesWritten);
                    }
                }

                if (encoder.isCompleted()) {
                    if (metrics.getLevel() == MetricsCollector.LEVEL_FULL) {
                        metrics.incrementMessagesSent(requestMsgCtx);
                    } else {
                        metrics.incrementMessagesSent();
                    }
                }
            }
        } catch (IOException ex) {
            logIOException(conn, ex);
            TargetContext.updateState(conn, ProtocolState.CLOSING);
            targetConfiguration.getConnections().shutdownConnection(conn, true);

            informWriterError(conn);

            if (requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.SND_HTTP_ERROR,
                        "Error in Sender",
                        null,
                        connState);
            }
        } catch (Exception e) {
            log.error("Error occurred while writing data to the target", e);
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);

            informWriterError(conn);

            if (requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.SND_HTTP_ERROR,
                        "Error in Sender",
                        null,
                        connState);
            }
        }
    }

    public void responseReceived(NHttpClientConnection conn) {
        HttpContext context = conn.getContext();
        if (transportLatencyLog.isDebugEnabled()) {
            HostConnections pool = (HostConnections) context.getAttribute(CONNECTION_POOL);
            String route = pool == null ? "null" : pool.getRoute().toString();
            transportLatencyLog.debug(context.getAttribute(CorrelationConstants.CORRELATION_ID) + "|" +
                    "Received Response headers from Backend at time stamp: " + System.currentTimeMillis() +
                    " and route: " + route);
        }
        if (isMessageSizeValidationEnabled) {
            context.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, 0);
        }
        HttpResponse response = conn.getHttpResponse();
        ProtocolState connState;
        try {
            boolean isError = false;
            String method = null;
            ProxyTunnelHandler tunnelHandler = (ProxyTunnelHandler) context.getAttribute(PassThroughConstants.TUNNEL_HANDLER);
            if (tunnelHandler != null && !tunnelHandler.isCompleted()) {
                method = "CONNECT";
                context.removeAttribute(PassThroughConstants.TUNNEL_HANDLER);
                tunnelHandler.handleResponse(response, conn);
                if (tunnelHandler.isSuccessful()) {
                    log.debug(conn + ": Tunnel established");
                    conn.resetInput();
                    conn.requestOutput();
                    return;
                } else {
                    // TLS tunnel has not been established, so mark connection as failed to prevent that it is
                    // returned back into pool
                    isError = true;
                    log.warn("Tunnel response failed");
                    // the reason for getting the targetRequest and calling the consumeError() on pipe. Instead of
                    // calling the informWriterError(NHTTPClientConnection) is, at this point the
                    // "writeCondition.await()" is already called but the corresponding pipe is not yet set as
                    // a writer in TargetContext
                    TargetRequest targetRequest = TargetContext.getRequest(conn);
                    if (targetRequest != null) {
                        targetRequest.getPipe().consumerError();
                    } else {
                        log.warn("Failed target response, but the target request is null");
                    }
                    TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
                }
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < HttpStatus.SC_OK) {
                if (log.isDebugEnabled()) {
                    log.debug(conn + ": Received a 100 Continue response");
                }
                // Ignore 1xx response
                return;
            }
            context.setAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME, System.currentTimeMillis());
            connState = TargetContext.getState(conn);
            //check correlation logs enabled
            if (PassThroughCorrelationConfigDataHolder.isEnable()
                    && TargetContext.isCorrelationIdAvailable(conn)) {
                long startTime = (long) context.getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
                ContextAwareLogger.getLogger(context, correlationLog, false)
                        .info((System.currentTimeMillis() - startTime) + "|HTTP|"
                        + TargetContext.getRequest(conn).getUrl().toString() + "|BACKEND LATENCY");
            }
            if (connState != ProtocolState.REQUEST_DONE) {
                isError = true;
                MessageContext requestMsgContext = TargetContext.get(conn).getRequestMsgCtx();
                if (conf.isConsumeAndDiscard()) {
                    log.warn("Response received before the request is sent to the backend completely , CORRELATION_ID = "
                            + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID) + ". Consuming the " +
                            "request message and discarding the data completely");
                } else {
                    log.warn("Response received before the request is sent to the backend completely , CORRELATION_ID = "
                            + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID));
                }
                // State is not REQUEST_DONE. i.e the request is not completely written. But the response is started
                // receiving, therefore informing a write error has occurred. So the thread which is
                // waiting on writing the request out, will get notified. And we will proceed with the response
                // regardless of the http status code. But mark target and source connections to be closed.
                informWriterError(conn);
                requestMsgContext.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                StatusLine errorStatus = response.getStatusLine();
                if (errorStatus != null) {
                    TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
                    conn.resetOutput();
                    if (log.isDebugEnabled()) {
                        log.debug(conn + ": Received response with status code : " + response.getStatusLine()
                                .getStatusCode() + " in invalid state : " + connState.name());
                    }
                    if (errorStatus.getStatusCode() < HttpStatus.SC_BAD_REQUEST) {
                        log.warn(conn + ": Received a response with status code : "
                                + response.getStatusLine().getStatusCode() + " in state : " + connState.name()
                                + " but request is not completely written to the backend, CORRELATION_ID = "
                                + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID));
                    }
                    if (requestMsgContext != null) {
                        NHttpServerConnection sourceConn = (NHttpServerConnection) requestMsgContext.getProperty(
                                PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                        if (sourceConn != null) {
                            if (!conf.isConsumeAndDiscard()) {
                                //Suspend input to avoid invoking input ready method and set this property here
                                //to avoid invoking the input ready method, while response is mediating through the
                                // mediation since we have set REQUEST_DONE state in SourceHandler responseReady method
                                sourceConn.suspendInput();
                                SourceContext sourceContext = (SourceContext)sourceConn.getContext().getAttribute(TargetContext.CONNECTION_INFORMATION);
                                if (sourceContext != null) {
                                    sourceContext.setIsSourceRequestMarkedToBeDiscarded(true);
                                }
                            }
                            SourceContext.get(sourceConn).setShutDown(true);
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(conn + ": has not started any request");
                        }
                        if (statusCode == HttpStatus.SC_REQUEST_TIMEOUT) {
                            return; // ignoring the stale connection close
                        }
                    }
                } else {
                    handleInvalidState(conn, "Receiving response");
                    return;
                }
            }
            context.setAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME,System.currentTimeMillis());
            TargetRequest targetRequest = TargetContext.getRequest(conn);

            if (targetRequest != null) {
                method = targetRequest.getMethod();
            }
            boolean canResponseHaveBody =
                    isResponseHaveBodyExpected(method, response);
            if (!canResponseHaveBody) {
                if (log.isDebugEnabled()) {
                    log.debug(conn + ": Received no-content response " +
                            response.getStatusLine().getStatusCode());
                }
                conn.resetInput();
            }
            TargetResponse targetResponse = new TargetResponse(
                    targetConfiguration, response, conn, canResponseHaveBody, isError);
            TargetContext.setResponse(conn, targetResponse);
            targetResponse.start(conn);

            MessageContext requestMsgContext = TargetContext.get(conn).getRequestMsgCtx();

            if (statusCode == HttpStatus.SC_ACCEPTED && handle202(requestMsgContext)) {
                return;
            }
            if (targetResponse.isForceShutdownConnectionOnComplete() && conf.isConsumeAndDiscardBySecondaryWorkerPool()
                    && conf.isConsumeAndDiscard()) {
                NHttpServerConnection sourceConn = (NHttpServerConnection) requestMsgContext.getProperty(
                        PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                MessageDiscardWorker messageDiscardWorker = new MessageDiscardWorker(requestMsgContext,
                        targetResponse, targetConfiguration, conn);
                conn.getContext().setAttribute(PassThroughConstants.MESSAGE_DISCARD_WORKER_REFERENCE
                        , messageDiscardWorker);
                if (sourceConn != null) {
                    sourceConn.getContext().setAttribute(PassThroughConstants.MESSAGE_DISCARD_WORKER_REFERENCE
                            , messageDiscardWorker);
                }
                targetConfiguration.getSecondaryWorkerPool().execute(messageDiscardWorker);
                return;
            }

            WorkerPool workerPool = targetConfiguration.getWorkerPool();
            ClientWorker clientWorker = new ClientWorker(targetConfiguration, requestMsgContext, targetResponse);
            conn.getContext().setAttribute(PassThroughConstants.CLIENT_WORKER_REFERENCE, clientWorker);
            workerPool.execute(clientWorker);

            targetConfiguration.getMetrics().incrementMessagesReceived();

            NHttpServerConnection sourceConn = (NHttpServerConnection) requestMsgContext.getProperty(
                    PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
            if (sourceConn != null) {
                PassThroughTransportUtils.setSourceConnectionContextAttributes(sourceConn, conn);
            }

        } catch (Exception ex) {
            log.error("Exception occurred while processing response", ex);

            informReaderError(conn);

            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);
        }
    }

    private boolean handle202(MessageContext requestMsgContext) throws AxisFault {
        if (requestMsgContext.isPropertyTrue(NhttpConstants.IGNORE_SC_ACCEPTED)) {
            // We should not further process this 202 response - Ignore it
            return true;
        }

        MessageReceiver mr = requestMsgContext.getAxisOperation().getMessageReceiver();
        MessageContext responseMsgCtx = requestMsgContext.getOperationContext().
                        getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
        if (responseMsgCtx == null || requestMsgContext.getOptions().isUseSeparateListener()) {
            // Most probably a response from a dual channel invocation
            // Inject directly into the SynapseCallbackReceiver
            requestMsgContext.setProperty(NhttpConstants.HTTP_202_RECEIVED, "true");
            mr.receive(requestMsgContext);
            return true;
        }

        return false;
    }

    /**
     * Closes the target side HTTP connection.
     *
     * @param conn HTTP client connection reference
     */
    private void dropTargetConnection(NHttpClientConnection conn) {
        try {
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            conn.close();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);
        }
    }

    public void inputReady(NHttpClientConnection conn, ContentDecoder decoder) {
        ProtocolState connState;
        MessageContext msgCtx = TargetContext.get(conn).getRequestMsgCtx();
        if (transportLatencyLog.isTraceEnabled()) {
            HttpContext context = conn.getContext();
            HostConnections pool = (HostConnections) context.getAttribute(CONNECTION_POOL);
            String route = pool == null ? "null" : pool.getRoute().toString();
            transportLatencyLog.trace(context.getAttribute(CorrelationConstants.CORRELATION_ID) + "|" +
                    "Response chunk received from Backend at time stamp: " + System.currentTimeMillis() +
                    " and route: " + route);
        }
        try {
            connState = TargetContext.getState(conn);
            if (connState.compareTo(ProtocolState.RESPONSE_HEAD) < 0) {
                return;
            }
            if (connState != ProtocolState.RESPONSE_HEAD &&
                    connState != ProtocolState.RESPONSE_BODY) {
                handleInvalidState(conn, "Response received");
                return;
            }

            TargetContext.updateState(conn, ProtocolState.RESPONSE_BODY);

            TargetResponse response = TargetContext.getResponse(conn);
            int statusCode = -1;

            if (response != null) {
                statusCode = conn.getHttpResponse().getStatusLine().getStatusCode();
                int responseRead = -1;
                boolean interceptionEnabled = false;
                Boolean[] interceptorResults = new Boolean[noOfInterceptors];
                if (conn.getContext().getAttribute(PassThroughConstants.RESPONSE_MESSAGE_CONTEXT) != null
                        && interceptStream) {
                    int index = 0;
                    for (StreamInterceptor interceptor : streamInterceptors) {
                        interceptorResults[index] = interceptor.interceptTargetResponse(
                                (MessageContext) conn.getContext()
                                        .getAttribute(PassThroughConstants.RESPONSE_MESSAGE_CONTEXT));
                        if (!interceptionEnabled && interceptorResults[index]) {
                            interceptionEnabled = true;
                        }
                        index++;
                    }
                    if (interceptionEnabled) {
                        ByteBuffer bytesRead = response.copyAndRead(conn, decoder);
                        if (bytesRead != null) {
                            responseRead = bytesRead.remaining();
                            boolean proceed;
                            index = 0;
                            for (StreamInterceptor interceptor : streamInterceptors) {
                                if (interceptorResults[index]) {
                                    proceed = interceptor.targetResponse(bytesRead.duplicate().asReadOnlyBuffer(),
                                                                         (MessageContext) conn.getContext()
                                                                                 .getAttribute(
                                                                                         PassThroughConstants.RESPONSE_MESSAGE_CONTEXT));
                                    if (!proceed) {
                                        log.info("Dropping target connection since request is blocked by : "
                                                         + interceptor.getClass().getName());
                                        dropTargetConnection(conn);
                                        response.getPipe().forceProducerComplete(decoder);
                                        break;
                                    }
                                }
                                index++;
                            }
                        }
                    } else {
                        responseRead = response.read(conn, decoder);
                    }
                } else {
                    responseRead = response.read(conn, decoder);
                }
          if (isMessageSizeValidationEnabled) {
              HttpContext httpContext = conn.getContext();
              //this is introduced as some transports which extends passthrough target handler which have overloaded
              //method responseReceived()
              if (httpContext.getAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM) == null) {
                  httpContext.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, 0);
              }
              int messageSizeSum = (int) httpContext.getAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM);

              messageSizeSum += responseRead;

              if (messageSizeSum > validMaxMessageSize) {
                  log.warn("Payload exceeds valid payload size range, hence discontinuing chunk stream at "
                          + messageSizeSum + " bytes to prevent OOM.");
                  dropTargetConnection(conn);
                  response.getPipe().forceProducerComplete(decoder);
              }
              httpContext.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, messageSizeSum);
          }

          if (metrics.getLevel() == MetricsCollector.LEVEL_FULL) {
                    metrics.incrementBytesReceived(msgCtx, responseRead);
                } else {
                    metrics.incrementBytesReceived(responseRead);
                }
            }
            if (decoder.isCompleted()) {
                if (metrics.getLevel() == MetricsCollector.LEVEL_FULL) {
                    metrics.incrementMessagesReceived(msgCtx);
                    metrics.notifyReceivedMessageSize(
                            msgCtx, conn.getMetrics().getReceivedBytesCount());
                    metrics.notifySentMessageSize(msgCtx, conn.getMetrics().getSentBytesCount());
                    if(statusCode != -1) {
                        metrics.reportResponseCode(msgCtx, statusCode);
                    }
                } else {
                    metrics.incrementMessagesReceived();
                    metrics.notifyReceivedMessageSize(
                            conn.getMetrics().getReceivedBytesCount());
                    metrics.notifySentMessageSize(conn.getMetrics().getSentBytesCount());
                }
                MessageContext requestMsgContext = TargetContext.get(conn).getRequestMsgCtx();
                NHttpServerConnection sourceConn =
                        (NHttpServerConnection) requestMsgContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                if (sourceConn != null) {
                    if (conn.getContext().getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_END_TIME) != null) {
                        sourceConn.getContext().setAttribute(
                                PassThroughConstants.RES_FROM_BACKEND_READ_END_TIME,
                                conn.getContext().getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_END_TIME)
                        );
                        conn.getContext().removeAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_END_TIME);

                    }
                    sourceConn.getContext().setAttribute(
                            PassThroughConstants.RES_ARRIVAL_TIME,
                            conn.getContext().getAttribute(PassThroughConstants.RES_ARRIVAL_TIME)
                    );
                    conn.getContext().removeAttribute(PassThroughConstants.RES_ARRIVAL_TIME);
                }
            }
        } catch (IOException e) {
            logException(conn, e);
            logIOException(conn, e);
            informReaderError(conn);

            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);
        } catch (Exception ex) {
            log.error("Exception occurred while reading request body", ex);

            informReaderError(conn);

            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);
        }
    }

    private void logException(NHttpClientConnection conn, Exception e) {
        ProtocolState state = TargetContext.getState(conn);
        if (state != null) {
            MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
            Map<String, String> logDetails = getLoggingInfo(conn, state, requestMsgCtx, e);
            log.warn("ERROR_CODE = " + logDetails.get("error_code") + ", STATE_DESCRIPTION = Exception occurred "
                    + logDetails.get("state_description") + ", INTERNAL_STATE = " + state + ", DIRECTION = "
                    + logDetails.get("direction") + ", " + "CAUSE_OF_ERROR = " + logDetails.get("cause_of_error")
                    + ", TARGET_HOST = " + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port")
                    + ", TARGET_CONTEXT = " + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                    + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                    .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
        }
    }

    public void closed(NHttpClientConnection conn) {
        ProtocolState state = TargetContext.getState(conn);
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        Map<String, String> logDetails = getLoggingInfo(conn, state, requestMsgCtx);

        boolean isFault = false;
        if (log.isDebugEnabled()) {
            log.debug("Connection closed by target host while in state " + state.name() + ". Response code : " + conn.getStatus());
        }
        if (state == ProtocolState.RESPONSE_DONE || state == ProtocolState.REQUEST_READY) {
            if (log.isDebugEnabled()) {
                log.debug("Keep-Alive Connection closed " + getConnectionLoggingInfo(conn));
            }
        } else if (state == ProtocolState.REQUEST_HEAD || state == ProtocolState.REQUEST_BODY) {
            informWriterError(conn);
            log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_CLOSED + ", STATE_DESCRIPTION = Connection closed by "
                    + "target host " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                    + ", DIRECTION = " + logDetails.get("direction") + ", "
                    + "CAUSE_OF_ERROR = Connection between the Server and the BackEnd has been closed, TARGET_HOST = "
                    + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                    + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method") + ", TRIGGER_TYPE = "
                    + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails.get("trigger_name")
                    + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
            isFault = true;
        } else if (state == ProtocolState.RESPONSE_HEAD || state == ProtocolState.RESPONSE_BODY) {
            informReaderError(conn);
            log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_CLOSED + ", STATE_DESCRIPTION = Connection closed by "
                    + "target host " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                    + ", DIRECTION = " + logDetails.get("direction") + ", "
                    + "CAUSE_OF_ERROR = Connection between the Server and the BackEnd has been closed, TARGET_HOST = "
                    + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                    + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method") + ", TRIGGER_TYPE = "
                    + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails.get("trigger_name")
                    + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
            isFault = true;
        } else if (state == ProtocolState.REQUEST_DONE) {
            log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_CLOSED + ", STATE_DESCRIPTION = Connection closed by "
                    + "target host " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                    + ", DIRECTION = " + logDetails.get("direction") + ", "
                    + "CAUSE_OF_ERROR = Connection between the Server and the BackEnd has been closed, TARGET_HOST = "
                    + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                    + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method") + ", TRIGGER_TYPE = "
                    + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails.get("trigger_name")
                    + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
            isFault = true;
        }

        if (isFault) {
            if (requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.CONNECTION_CLOSED,
                        "Error in Sender",
                        null,
                        state);
            }
        }

        metrics.disconnected();

        TargetContext.updateState(conn, ProtocolState.CLOSED);
        
        if (conf.isTLSGracefulConnectionTerminationEnabled()) {
            targetConfiguration.getConnections().closeConnection(conn, isFault);
        } else {
            targetConfiguration.getConnections().shutdownConnection(conn, isFault);
        }

    }

    private void logIOException(NHttpClientConnection conn, IOException e) {
        String message = getErrorMessage("I/O error : " + e.getMessage(), conn);

        if (e.getMessage() != null && (e instanceof ConnectionClosedException
                || e.getMessage().toLowerCase().contains("connection reset")
                || e.getMessage().toLowerCase().contains("forcibly closed"))) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": I/O error (Probably the keep-alive connection "
                        + "was closed):" + e.getMessage()
                        + "CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID));
            }
        } else if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("broken") != -1) {
                log.warn("I/O error (Probably the connection "
                        + "was closed by the remote party):" + e.getMessage() + "CORRELATION_ID = "
                        + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID));
            } else {
                log.error("I/O error: " + e.getMessage()
                        + "CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID), e);
            }
        } else {
            log.error(message, e);
        }
    }

    public void timeout(NHttpClientConnection conn) {
        ProtocolState state = TargetContext.getState(conn);
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        Map<String, String> logDetails = getLoggingInfo(conn, state, requestMsgCtx);
        Object clientWorker = conn.getContext().getAttribute(
                PassThroughConstants.CLIENT_WORKER_REFERENCE);
        Object messageDiscardWorker = conn.getContext().getAttribute(
                PassThroughConstants.MESSAGE_DISCARD_WORKER_REFERENCE);

        if (log.isDebugEnabled()) {
            log.debug(getErrorMessage("Connection timeout", conn) + " "+ getConnectionLoggingInfo(conn));
        }
        if (state != null &&
                (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE)) {
            if (log.isDebugEnabled()) {
                log.debug(getErrorMessage("Keep-alive connection timed out", conn) + " " +
                          getConnectionLoggingInfo(conn));
            }
        } else if (state != null ) {
            if (state == ProtocolState.REQUEST_BODY) {
                metrics.incrementTimeoutsSending();
                informWriterError(conn);
                log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_TIMEOUT + ", STATE_DESCRIPTION = Socket Timeout "
                        + "occurred " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                        + ", DIRECTION = " + logDetails.get("direction") + ", "
                        + "CAUSE_OF_ERROR = Connection between the WSO2 Server and the BackEnd timeouts, TARGET_HOST = "
                        + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                        + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                        + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                        .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn) + ", "
                        + "CONNECTION = " + conn + ", SOCKET_TIMEOUT = " + conn.getSocketTimeout() + ", CORRELATION_ID"
                        + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                        + workerPoolExhaustedErrorMessage(clientWorker, messageDiscardWorker));
            }

            if (state == ProtocolState.RESPONSE_BODY || state == ProtocolState.REQUEST_HEAD) {
                metrics.incrementTimeoutsReceiving();
                informReaderError(conn);
                log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_TIMEOUT + ", STATE_DESCRIPTION = Socket Timeout "
                        + "occurred " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                        + ", DIRECTION = " + logDetails.get("direction") + ", "
                        + "CAUSE_OF_ERROR = Connection between the WSO2 Server and the BackEnd timeouts, TARGET_HOST = "
                        + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                        + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                        + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                        .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn) + ", "
                        + "CONNECTION = " + conn + ", SOCKET_TIMEOUT = " + conn.getSocketTimeout() + ", "
                        + "CORRELATION_ID = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                        + workerPoolExhaustedErrorMessage(clientWorker, messageDiscardWorker));
            }

            if (state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
                log.warn("ERROR_CODE = " + ErrorCodes.CONNECTION_TIMEOUT + ", STATE_DESCRIPTION = Socket Timeout "
                        + "occurred " + logDetails.get("state_description") + ", INTERNAL_STATE = " + state
                        + ", DIRECTION = " + logDetails.get("direction") + ", "
                        + "CAUSE_OF_ERROR = Connection between the WSO2 Server and the BackEnd timeouts, TARGET_HOST = "
                        + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port") + ", TARGET_CONTEXT = "
                        + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                        + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                        .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn) + ", "
                        + "CONNECTION = " + conn + ", SOCKET_TIMEOUT = " + conn.getSocketTimeout() + ", CORRELATION_ID"
                        + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                        + workerPoolExhaustedErrorMessage(clientWorker, messageDiscardWorker));

                if (PassThroughCorrelationConfigDataHolder.isEnable()) {
                    logHttpRequestErrorInCorrelationLog(conn, "Timeout in " + state);
                }
                if (requestMsgCtx != null) {
                    requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                            PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                    targetErrorHandler.handleError(requestMsgCtx,
                            ErrorCodes.CONNECTION_TIMEOUT,
                            "Error in Sender",
                            null,
                            state);
                }
            }
        }

        TargetContext.updateState(conn, ProtocolState.CLOSED);
        
        if (conf.isTLSGracefulConnectionTerminationEnabled()) {
            targetConfiguration.getConnections().closeConnection(conn, true);
        } else {
            targetConfiguration.getConnections().shutdownConnection(conn, true);
        }
    }

    private String workerPoolExhaustedErrorMessage(Object clientWorker
            , Object messageDiscardWorker) {
        String workerPoolExhaustedMessage = "";
        if (messageDiscardWorker != null
                && WorkerState.CREATED == ((MessageDiscardWorker) messageDiscardWorker).getWorkerState()) {
            workerPoolExhaustedMessage = ", Could not get a secondary worker thread to discard the request content. "
                    + "The secondary worker pool is exhausted.";
            return workerPoolExhaustedMessage;
        } else if (messageDiscardWorker != null
                && WorkerState.RUNNING == ((MessageDiscardWorker) messageDiscardWorker).getWorkerState()) {
            workerPoolExhaustedMessage = ", The secondary worker thread which was discarding the request content"
                    + " has been released.";
            return workerPoolExhaustedMessage;
        } else if (messageDiscardWorker != null
                && WorkerState.FINISHED == ((MessageDiscardWorker) messageDiscardWorker).getWorkerState()) {
            if (clientWorker != null
                    && WorkerState.CREATED == ((ClientWorker)clientWorker).getWorkerState()) {
                workerPoolExhaustedMessage = ", Could not get a  PassThroughMessageProcessor thread to process the "
                        + "response message. The primary worker pool is exhausted.";
                return workerPoolExhaustedMessage;
            }
        } else if (clientWorker != null && WorkerState.CREATED == ((ClientWorker)clientWorker).getWorkerState()) {
            workerPoolExhaustedMessage = ", Could not get a  PassThroughMessageProcessor thread to process the "
                    + "response message. The primary worker pool is exhausted.";
            return workerPoolExhaustedMessage;
        }
        return workerPoolExhaustedMessage;
    }

    private boolean isResponseHaveBodyExpected(
            final String method, final HttpResponse response) {

        if ("HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        int status = response.getStatusLine().getStatusCode();
        return status >= HttpStatus.SC_OK
            && status != HttpStatus.SC_NO_CONTENT
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT;
    }

    /**
     * Include remote host and port information to an error message
     *
     * @param message the initial message
     * @param conn    the connection encountering the error
     * @return the updated error message
     */
    private String getErrorMessage(String message, NHttpClientConnection conn) {
        if (conn != null && conn instanceof DefaultNHttpClientConnection) {
            DefaultNHttpClientConnection c = ((DefaultNHttpClientConnection) conn);

            if (c.getRemoteAddress() != null) {
                return message + " For : " + c.getRemoteAddress().getHostAddress() + ":" +
                        c.getRemotePort();
            }
        }
        return message;
    }

    private void handleInvalidState(NHttpClientConnection conn, String action) {
        ProtocolState state = TargetContext.getState(conn);

        if (log.isWarnEnabled()) {
            log.warn(conn + ": " + action + " while the handler is in an inconsistent state " +
                TargetContext.getState(conn));
        }
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        TargetContext.updateState(conn, ProtocolState.CLOSED);
        targetConfiguration.getConnections().shutdownConnection(conn, true);
        if (requestMsgCtx != null) {
            requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                    PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
            targetErrorHandler.handleError(requestMsgCtx,
                    ErrorCodes.SND_INVALID_STATE,
                    "Error in Sender",
                    null,
                    state);
        }
    }

    private void informReaderError(NHttpClientConnection conn) {
        Pipe reader = TargetContext.get(conn).getReader();

        metrics.incrementFaultsReceiving();

        if (reader != null) {
            reader.producerError();
        }
    }

    private void informWriterError(NHttpClientConnection conn) {
        Pipe writer = TargetContext.get(conn).getWriter();

        metrics.incrementFaultsReceiving();

        if (writer != null) {
            writer.consumerError();
        }
    }

    public void endOfInput(NHttpClientConnection conn) throws IOException {
        conn.close();
    }

    public void exception(NHttpClientConnection conn, Exception ex) {
        ProtocolState state = TargetContext.getState(conn);
        MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
        Map<String, String> logDetails = getLoggingInfo(conn, state, requestMsgCtx, ex);

        if (state == ProtocolState.REQUEST_HEAD || state == ProtocolState.REQUEST_BODY) {
            informWriterError(conn);
            log.warn("ERROR_CODE = " + logDetails.get("error_code") + ", STATE_DESCRIPTION = Exception occurred "
                    + logDetails.get("state_description") + ", INTERNAL_STATE = " + state + ", DIRECTION = "
                    + logDetails.get("direction") + ", " + "CAUSE_OF_ERROR = " + logDetails.get("cause_of_error")
                    + ", TARGET_HOST = " + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port")
                    + ", TARGET_CONTEXT = " + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                    + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                    .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID" + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
        } else if (state == ProtocolState.RESPONSE_HEAD || state == ProtocolState.RESPONSE_BODY) {
            informReaderError(conn);
            log.warn("ERROR_CODE = " + logDetails.get("error_code") + ", STATE_DESCRIPTION = Exception occurred "
                    + logDetails.get("state_description") + ", INTERNAL_STATE = " + state + ", DIRECTION = "
                    + logDetails.get("direction") + ", " + "CAUSE_OF_ERROR = " + logDetails.get("cause_of_error")
                    + ", TARGET_HOST = " + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port")
                    + ", TARGET_CONTEXT = " + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                    + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                    .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID" + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
        } else if (state == ProtocolState.REQUEST_DONE) {
            log.warn("ERROR_CODE = " + logDetails.get("error_code") + ", STATE_DESCRIPTION = Exception occurred "
                    + logDetails.get("state_description") + ", INTERNAL_STATE = " + state + ", DIRECTION = "
                    + logDetails.get("direction") + ", " + "CAUSE_OF_ERROR = " + logDetails.get("cause_of_error")
                    + ", TARGET_HOST = " + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port")
                    + ", TARGET_CONTEXT = " + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                    + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                    .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID" + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
        } else if (state == ProtocolState.REQUEST_READY) {
            log.warn("ERROR_CODE = " + logDetails.get("error_code") + ", STATE_DESCRIPTION = Exception occurred "
                    + logDetails.get("state_description") + ", INTERNAL_STATE = " + state + ", DIRECTION = "
                    + logDetails.get("direction") + ", " + "CAUSE_OF_ERROR = " + logDetails.get("cause_of_error")
                    + ", TARGET_HOST = " + logDetails.get("host") + ", TARGET_PORT = " + logDetails.get("port")
                    + ", TARGET_CONTEXT = " + logDetails.get("url") + ", " + "HTTP_METHOD = " + logDetails.get("method")
                    + ", TRIGGER_TYPE = " + logDetails.get("trigger_type") + ", TRIGGER_NAME = " + logDetails
                    .get("trigger_name") + ", REMOTE_ADDRESS = " + getBackEndConnectionInfo(conn)
                    + ", CORRELATION_ID" + " = " + conn.getContext().getAttribute(CorrelationConstants.CORRELATION_ID)
                    + ", CONNECTION = " + conn);
        } else if (state == ProtocolState.RESPONSE_DONE) {
            return;
        }
        
        if (ex instanceof IOException) {

            logIOException(conn, (IOException) ex);
            if (PassThroughCorrelationConfigDataHolder.isEnable()){
                logHttpRequestErrorInCorrelationLog(conn, "IO Exception in " + state.name());
            }
            if (state != ProtocolState.RESPONSE_DONE && requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.SND_IO_ERROR,
                        "Error in Sender",
                        ex,
                        state);
            }

            TargetContext.updateState(conn, ProtocolState.CLOSING);
        } else if (ex instanceof HttpException) {
            String message = getErrorMessage("HTTP protocol violation : " + ex.getMessage(), conn);
            log.error(message, ex);
            if (PassThroughCorrelationConfigDataHolder.isEnable()){
                logHttpRequestErrorInCorrelationLog(conn, "HTTP Exception in " + state.name());
            }
            if (state != ProtocolState.RESPONSE_DONE && requestMsgCtx != null) {
                requestMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.PROTOCOL_VIOLATION,
                        "Error in Sender",
                        null,
                        state);
            }

            TargetContext.updateState(conn, ProtocolState.CLOSED);
        } else {
            if(null != ex && null != ex.getMessage()) {
                log.error("Unexpected error: " + ex.getMessage(), ex);
            } else {
                log.error("Unexpected error.");
            }
            if (PassThroughCorrelationConfigDataHolder.isEnable()) {
                logHttpRequestErrorInCorrelationLog(conn, "Unexpected error");
            }
            TargetContext.updateState(conn, ProtocolState.CLOSED);
        }
        targetConfiguration.getConnections().shutdownConnection(conn, true);
    }

    public void setConnFactory(ClientConnFactory connFactory) {
        this.connFactory = connFactory;
    }

    public TargetConfiguration getTargetConfiguration() {
        return targetConfiguration;
    }


    private String getConnectionLoggingInfo(NHttpClientConnection conn) {
        if (conn instanceof LoggingNHttpClientConnection) {
            IOSession ioSession = ((LoggingNHttpClientConnection) conn).getIOSession();
            if (ioSession != null) {
                return " Remote Address : " + ioSession.getRemoteAddress();
            }
        }
        return "";
    }

    private String getBackEndConnectionInfo(NHttpClientConnection conn) {
        if (conn instanceof LoggingNHttpClientConnection) {
            IOSession ioSession = ((LoggingNHttpClientConnection) conn).getIOSession();
            if (ioSession != null) {
                SocketAddress socketAddress = ioSession.getRemoteAddress();
                if (socketAddress != null) {
                    return socketAddress.toString();
                }
            }
        }
        return "";
    }

    private Map<String, String> getLoggingInfo(NHttpClientConnection conn, ProtocolState state,
                                               MessageContext requestMsgCtx) {
        return getLoggingInfo(conn, state, requestMsgCtx, null);
    }

    private Map<String, String> getLoggingInfo(NHttpClientConnection conn, ProtocolState state,
                                               MessageContext requestMsgCtx, Exception ex) {
        HashMap<String, String> logDetails = new HashMap<>();
        TargetContext targetContext = TargetContext.get(conn);
        if (targetContext != null) {
            String url = "", method = "";
            if (targetContext.getRequest() != null) {
                url = targetContext.getRequest().getUrl().toString();
                method = targetContext.getRequest().getMethod();
            } else {
                HttpRequest httpRequest = conn.getHttpRequest();
                if (httpRequest != null) {
                    url = httpRequest.getRequestLine().getUri();
                    method = httpRequest.getRequestLine().getMethod();
                }
            }
            logDetails.put("url", url);
            logDetails.put("method", method);
        }

        if (conn != null && conn instanceof DefaultNHttpClientConnection) {
            DefaultNHttpClientConnection connection = ((DefaultNHttpClientConnection) conn);
            if (connection.getRemoteAddress() != null) {
                logDetails.put("host", connection.getRemoteAddress().getHostAddress());
                logDetails.put("port", Integer.toString(connection.getRemotePort()));
            }
        }

        if (state != null) {
            if (state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
                logDetails.put("direction", "REQUEST");
            } else {
                logDetails.put("direction", "RESPONSE");
            }
            if (state == ProtocolState.REQUEST_BODY || state == ProtocolState.REQUEST_HEAD) {
                logDetails.put("state_description", "after Server written the request headers but prior to write the "
                        + "request body to the backend");
            } else if (state == ProtocolState.RESPONSE_BODY || state == ProtocolState.RESPONSE_HEAD) {
                logDetails.put("state_description",
                        "after Server read the response headers but prior to reading the response body from the "
                                + "backend");
            } else if (state == ProtocolState.REQUEST_DONE) {
                logDetails.put("state_description",
                        "after Server written the request headers and the request body to the backend");
            } else if (state == ProtocolState.REQUEST_READY) {
                logDetails.put("state_description",
                        "when Server establishing a connection to the backend");
            }
        }

        if (requestMsgCtx != null) {
            Object triggerName = requestMsgCtx.getProperty(PassThroughConstants.INTERNAL_TRIGGER_NAME);
            Object triggerType = requestMsgCtx.getProperty(PassThroughConstants.INTERNAL_TRIGGER_TYPE);
            if (triggerType != null) {
                logDetails.put("trigger_type", triggerType.toString());
            }
            if (triggerName != null) {
                logDetails.put("trigger_name", triggerName.toString());
            }
        }

        if (ex != null) {
            if (ex instanceof IOException) {
                logDetails.put("error_code", Integer.toString(ErrorCodes.SND_IO_ERROR));
                logDetails.put("cause_of_error", "I/O exception : " + ex.getMessage());
            } else if (ex instanceof HttpException) {
                logDetails.put("error_code", Integer.toString(ErrorCodes.PROTOCOL_VIOLATION));
                logDetails.put("cause_of_error", "HTTP Protocol violation : " + ex.getMessage());
            } else {
                logDetails.put("cause_of_error", "Unexpected error : " + ex.getMessage());
            }

        }
        return logDetails;
    }

    private void logHttpRequestErrorInCorrelationLog(NHttpClientConnection conn, String state) {

        TargetContext targetContext = TargetContext.get(conn);
        if (targetContext != null && TargetContext.isCorrelationIdAvailable(conn)) {
            String url = "", method = "";
            if (targetContext.getRequest() != null) {
                url = targetContext.getRequest().getUrl().toString();
                method = targetContext.getRequest().getMethod();
            } else {
                HttpRequest httpRequest = conn.getHttpRequest();
                if (httpRequest != null) {
                    url = httpRequest.getRequestLine().getUri();
                    method = httpRequest.getRequestLine().getMethod();
                }
            }
            if ((method.length() != 0) && (url.length() != 0)) {
                Object requestStartTime =
                        conn.getContext().getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
                long startTime = 0;
                if (requestStartTime != null) {
                    startTime = (long) requestStartTime;
                }
                ContextAwareLogger.getLogger(conn.getContext(), correlationLog, false)
                        .info((System.currentTimeMillis() - startTime) + "|HTTP|"
                                + conn.getContext().getAttribute("http.connection") + "|" + method + "|" + url
                                + "|" + state);
            }
        }
    }
}

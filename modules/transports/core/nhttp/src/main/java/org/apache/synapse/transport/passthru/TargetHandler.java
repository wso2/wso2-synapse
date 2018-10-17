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
import org.apache.log4j.MDC;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.http.conn.LoggingNHttpClientConnection;
import org.apache.synapse.transport.http.conn.ProxyTunnelHandler;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.HostConnections;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;

import java.io.IOException;
import java.util.Properties;


/**
 * This class is handling events from the transport -- > client.
 */
public class TargetHandler implements NHttpClientEventHandler {
    private static Log log = LogFactory.getLog(TargetHandler.class);

    /** log for correlation.log */
    private static final Log correlationLog = LogFactory.getLog(PassThroughConstants.CORRELATION_LOGGER);

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

    public TargetHandler(DeliveryAgent deliveryAgent,
                         ClientConnFactory connFactory,
                         TargetConfiguration configuration) {
        this.deliveryAgent = deliveryAgent;
        this.connFactory = connFactory;
        this.targetConfiguration = configuration;
        this.targetErrorHandler = new TargetErrorHandler(targetConfiguration);
        this.metrics = targetConfiguration.getMetrics();

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

    }

    public void connected(NHttpClientConnection conn, Object o) {
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
                request.start(conn);
                targetConfiguration.getMetrics().incrementMessagesSent();
            }
            context.setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME, System.currentTimeMillis());
            context.setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME, System.currentTimeMillis());
        } catch (IOException e) {
            logIOException(conn, e);
            TargetContext.updateState(conn, ProtocolState.CLOSED);
            targetConfiguration.getConnections().shutdownConnection(conn, true);

            MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
            if (requestMsgCtx != null) {
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
        try {
            connState = TargetContext.getState(conn);
            if (connState != ProtocolState.REQUEST_HEAD &&
                    connState != ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing message body");
                return;
            }

            TargetRequest request = TargetContext.getRequest(conn);
            if (request.hasEntityBody()) {
                int bytesWritten = request.write(conn, encoder);
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
        if (isMessageSizeValidationEnabled) {
            context.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, 0);
        }
        HttpResponse response = conn.getHttpResponse();
        ProtocolState connState;
        try {
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
            boolean isError = false;
        	context.setAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME, System.currentTimeMillis());
            connState = TargetContext.getState(conn);
            MessageContext requestMsgContext = TargetContext.get(conn).getRequestMsgCtx();
            NHttpServerConnection sourceConn =
                    (NHttpServerConnection) requestMsgContext.getProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);

            //check correlation logs enabled
            if (targetConfiguration.isCorrelationLoggingEnabled()) {
                long startTime = (long) context.getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
                MDC.put(PassThroughConstants.CORRELATION_MDC_PROPERTY,
                        context.getAttribute(PassThroughConstants.CORRELATION_ID).toString());
                correlationLog.info((System.currentTimeMillis() - startTime) + "|HTTP|" +
                        TargetContext.getRequest(conn).getUrl().toString()+ "|BACKEND LATENCY");
                MDC.remove(PassThroughConstants.CORRELATION_MDC_PROPERTY);
            }

            if (connState != ProtocolState.REQUEST_DONE) {
                isError = true;
                // State is not REQUEST_DONE. i.e the request is not completely written. But the response is started
                // receiving, therefore informing a write error has occurred. So the thread which is
                // waiting on writing the request out, will get notified.
                informWriterError(conn);
                StatusLine errorStatus = response.getStatusLine();
                /* We might receive a 404 or a similar type, even before we write the request body. */
                if (errorStatus != null) {
                    if (errorStatus.getStatusCode() >= HttpStatus.SC_BAD_REQUEST) {
                        TargetContext.updateState(conn, ProtocolState.REQUEST_DONE);
                        conn.resetOutput();
                        if (sourceConn != null) {
                            SourceContext.updateState(sourceConn, ProtocolState.REQUEST_DONE);
                            SourceContext.get(sourceConn).setShutDown(true);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(conn + ": Received response with status code : " +
                                    response.getStatusLine().getStatusCode() + " in invalid state : " + connState.name());
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
            if (method == null) {
                method = "POST";
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

            if (statusCode == HttpStatus.SC_ACCEPTED && handle202(requestMsgContext)) {
                return;
            }

            targetConfiguration.getWorkerPool().execute(
                    new ClientWorker(targetConfiguration, requestMsgContext, targetResponse));

            targetConfiguration.getMetrics().incrementMessagesReceived();

                sourceConn = (NHttpServerConnection) requestMsgContext.getProperty
                           (PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
            if (sourceConn != null) {
                sourceConn.getContext().setAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME,
                        conn.getContext()
                                .getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME)
                );
                conn.getContext().removeAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME);

                sourceConn.getContext().setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME,
                        conn.getContext()
                                .getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME)
                );
                conn.getContext().removeAttribute(PassThroughConstants.REQ_DEPARTURE_TIME);
                sourceConn.getContext().setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME,
                        conn.getContext()
                                .getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME)
                );

                conn.getContext().removeAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
                sourceConn.getContext().setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME,
                        conn.getContext()
                                .getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME)
                );
                conn.getContext().removeAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME);
                sourceConn.getContext().setAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME,
                        conn.getContext()
                                .getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME)
                );
                conn.getContext().removeAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME);

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
				int responseRead = response.read(conn, decoder);
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

    public void closed(NHttpClientConnection conn) {
        ProtocolState state = TargetContext.getState(conn);

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
            log.warn("Connection closed by target host while sending the request " + getConnectionLoggingInfo(conn));
            isFault = true;
        } else if (state == ProtocolState.RESPONSE_HEAD || state == ProtocolState.RESPONSE_BODY) {
            informReaderError(conn);
            log.warn("Connection closed by target host while receiving the response " + getConnectionLoggingInfo(conn));
            isFault = true;
        } else if (state == ProtocolState.REQUEST_DONE) {
            log.warn("Connection closed by target host before receiving the response " + getConnectionLoggingInfo(conn));
            isFault = true;
        }

        if (isFault) {
            MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();
            if (requestMsgCtx != null) {
                targetErrorHandler.handleError(requestMsgCtx,
                        ErrorCodes.CONNECTION_CLOSED,
                        "Error in Sender",
                        null,
                        state);
            }
        }

        metrics.disconnected();

        TargetContext.updateState(conn, ProtocolState.CLOSED);
        targetConfiguration.getConnections().shutdownConnection(conn, isFault);

    }

    private void logIOException(NHttpClientConnection conn, IOException e) {
        String message = getErrorMessage("I/O error : " + e.getMessage(), conn);

        if (e instanceof ConnectionClosedException || (e.getMessage() != null &&
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed"))) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": I/O error (Probably the keep-alive connection " +
                        "was closed):" + e.getMessage());
            }
        } else if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            if (msg.indexOf("broken") != -1) {
                log.warn("I/O error (Probably the connection " +
                        "was closed by the remote party):" + e.getMessage());
            } else {
                log.error("I/O error: " + e.getMessage(), e);
            }
        } else {
            log.error(message, e);
        }
    }

    public void timeout(NHttpClientConnection conn) {
        ProtocolState state = TargetContext.getState(conn);

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
            }

            if (state == ProtocolState.RESPONSE_BODY || state == ProtocolState.REQUEST_HEAD) {
                metrics.incrementTimeoutsReceiving();
                informReaderError(conn);
            }

            if (state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
                MessageContext requestMsgCtx = TargetContext.get(conn).getRequestMsgCtx();

                log.warn("Connection time out after while in state : " + state +
                         " Socket Timeout : " + conn.getSocketTimeout() +
                         getConnectionLoggingInfo(conn));
                if (targetConfiguration.isCorrelationLoggingEnabled()) {
                    logHttpRequestErrorInCorrelationLog(conn, "Timeout in " + state);
                }
                if (requestMsgCtx != null) {
                    targetErrorHandler.handleError(requestMsgCtx,
                            ErrorCodes.CONNECTION_TIMEOUT,
                            "Error in Sender",
                            null,
                            state);
                }
            }
        }

        TargetContext.updateState(conn, ProtocolState.CLOSED);
        targetConfiguration.getConnections().shutdownConnection(conn, true);
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

        if (state == ProtocolState.REQUEST_HEAD || state == ProtocolState.REQUEST_BODY) {
            informWriterError(conn);
            if (targetConfiguration.isCorrelationLoggingEnabled()){
                logHttpRequestErrorInCorrelationLog(conn, "Exception in "+state.name());
            }
            log.warn("Exception occurred while sending the request " + getConnectionLoggingInfo(conn));
        } else if (state == ProtocolState.RESPONSE_HEAD || state == ProtocolState.RESPONSE_BODY) {
            informReaderError(conn);
            if (targetConfiguration.isCorrelationLoggingEnabled()){
                logHttpRequestErrorInCorrelationLog(conn, "Exception in "+state.name());
            }
            log.warn("Exception occurred while reading the response " + getConnectionLoggingInfo(conn));
        } else if (state == ProtocolState.REQUEST_DONE) {
            if (targetConfiguration.isCorrelationLoggingEnabled()){
                logHttpRequestErrorInCorrelationLog(conn, "Exception in "+state.name());
            }
            log.warn("Exception occurred before reading the response " + getConnectionLoggingInfo(conn));
        }
        
        if (ex instanceof IOException) {

            logIOException(conn, (IOException) ex);

            if (requestMsgCtx != null) {
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

            if (requestMsgCtx != null) {
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

    private void logHttpRequestErrorInCorrelationLog(NHttpClientConnection conn, String state) {

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
            if ((method.length() != 0) && (url.length() != 0)) {
                MDC.put(PassThroughConstants.CORRELATION_MDC_PROPERTY,
                        conn.getContext().getAttribute(PassThroughConstants.CORRELATION_ID).toString());
                Object requestStartTime =
                        conn.getContext().getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
                long startTime = 0;
                if (requestStartTime != null) {
                    startTime = (long) requestStartTime;
                }
                correlationLog.info((System.currentTimeMillis() - startTime) + "|HTTP|"
                        + conn.getContext().getAttribute("http.connection") + "|" + method + "|" + url
                        + "|" + state);
                MDC.remove(PassThroughConstants.CORRELATION_MDC_PROPERTY);
            }
        }
    }
}

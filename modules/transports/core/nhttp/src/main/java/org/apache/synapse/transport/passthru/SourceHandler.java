/**
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SimpleOutputBuffer;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.MDC;
import org.apache.synapse.commons.jmx.ThreadingView;
import org.apache.synapse.commons.transaction.TranscationManger;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.transport.http.conn.LoggingNHttpServerConnection;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.LatencyCollector;
import org.apache.synapse.transport.passthru.jmx.LatencyView;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * This is the class where transport interacts with the client. This class
 * receives events for a particular connection. These events give information
 * about the message and its various states.
 */
public class SourceHandler implements NHttpServerEventHandler {
    private static Log log = LogFactory.getLog(SourceHandler.class);
    /** logger for correlation.log */
    private static final Log correlationLog = LogFactory.getLog(PassThroughConstants.CORRELATION_LOGGER);

    private final SourceConfiguration sourceConfiguration;

    private PassThroughTransportMetricsCollector metrics = null;
    
    private LatencyView latencyView = null;
    
    private LatencyView s2sLatencyView = null;
    private  ThreadingView threadingView;

    private static boolean isMessageSizeValidationEnabled = false;

    private static int validMaxMessageSize = Integer.MAX_VALUE;

    public static final String PROPERTY_FILE = "passthru-http.properties";
    public static final String MESSAGE_SIZE_VALIDATION = "message.size.validation.enabled";
    public static final String VALID_MAX_MESSAGE_SIZE = "valid.max.message.size.in.bytes";

    public SourceHandler(SourceConfiguration sourceConfiguration) {
        this.sourceConfiguration = sourceConfiguration;
        this.metrics = sourceConfiguration.getMetrics();

        String strNamePostfix = "";
        if (sourceConfiguration.getInDescription() != null &&
            sourceConfiguration.getInDescription().getName() != null) {
            strNamePostfix = "-" + sourceConfiguration.getInDescription().getName();
            Scheme scheme = sourceConfiguration.getScheme();
            boolean enableAdvancedForLatencyView = sourceConfiguration.getBooleanValue(
                    PassThroughConstants.SYNAPSE_PASSTHROUGH_LATENCY_ADVANCE_VIEW, false);
            boolean enableAdvancedForS2SView = sourceConfiguration.getBooleanValue(
                    PassThroughConstants.SYNAPSE_PASSTHROUGH_S2SLATENCY_ADVANCE_VIEW, false);
            this.latencyView = new LatencyView(PassThroughConstants.PASSTHROUGH_LATENCY_VIEW,
                                               scheme.isSSL(), strNamePostfix, enableAdvancedForLatencyView);
            this.s2sLatencyView = new LatencyView(PassThroughConstants.PASSTHROUGH_S2SLATENCY_VIEW, scheme.isSSL(),
                                                  strNamePostfix, enableAdvancedForS2SView);
            this.threadingView = new ThreadingView(PassThroughConstants.PASSTHOUGH_HTTP_SERVER_WORKER, true, 50);
        }

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

    public void connected(NHttpServerConnection conn) {
        // we have to have these two operations in order
        sourceConfiguration.getSourceConnections().addConnection(conn);
        SourceContext.create(conn, ProtocolState.REQUEST_READY, sourceConfiguration);
        metrics.connected();
    }

    public void requestReceived(NHttpServerConnection conn) {
        try {
            HttpContext httpContext = conn.getContext();
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                setCorrelationId(conn);
            }
            httpContext.setAttribute(PassThroughConstants.REQ_ARRIVAL_TIME, System.currentTimeMillis());
            httpContext.setAttribute(PassThroughConstants.REQ_FROM_CLIENT_READ_START_TIME, System.currentTimeMillis());
            if (isMessageSizeValidationEnabled) {
                httpContext.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, 0);
            }
            SourceRequest request = getSourceRequest(conn);
            if (request == null) {
                return;

            }

            String method = request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase() : "";

            if (!request.isEntityEnclosing()) {
                conn.getContext().setAttribute(PassThroughConstants.REQ_FROM_CLIENT_READ_END_TIME, System.currentTimeMillis());
            }
            OutputStream os = getOutputStream(method, request);
            sourceConfiguration.getWorkerPool().execute(new ServerWorker(request, sourceConfiguration, os));
        } catch (HttpException e) {
            log.error("HttpException occurred when request is processing probably when creating SourceRequest", e);

            informReaderError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } catch (IOException e) {
            logIOException(conn, e);

            informReaderError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    private void setCorrelationId(NHttpServerConnection conn) {
        HttpContext httpContext = conn.getContext();
        String correlationHeaderName = PassThroughConfiguration.getInstance().getCorrelationHeaderName();
        Header[] correlationHeader = conn.getHttpRequest().getHeaders(correlationHeaderName);
        String corId;
        if (correlationHeader.length != 0) {
            corId = correlationHeader[0].getValue();
        } else {
            corId = UUID.randomUUID().toString();
        }
        httpContext.setAttribute(PassThroughConstants.CORRELATION_ID, corId);
    }

    public void inputReady(NHttpServerConnection conn,
                           ContentDecoder decoder) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);

            if (protocolState != ProtocolState.REQUEST_HEAD
                    && protocolState != ProtocolState.REQUEST_BODY) {
                handleInvalidState(conn, "Request message body data received");
                return;
            }

            SourceContext.updateState(conn, ProtocolState.REQUEST_BODY);

            SourceRequest request = SourceContext.getRequest(conn);

            int readBytes = request.read(conn, decoder);

            if (isMessageSizeValidationEnabled) {
                HttpContext httpContext = conn.getContext();
                //this is introduced as some transports which extends passthrough source handler which have overloaded
                //method requestReceived() Eg:- inbound http/https
                if (httpContext.getAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM) == null) {
                    httpContext.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, 0);
                }
                int messageSizeSum = (int) httpContext.getAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM);

                messageSizeSum += readBytes;

                if (messageSizeSum > validMaxMessageSize) {
                    log.warn("Payload exceeds valid payload size range, hence discontinuing chunk stream at "
                            + messageSizeSum + " bytes to prevent OOM.");
                    dropSourceConnection(conn);
                    conn.getContext().setAttribute(PassThroughConstants.SOURCE_CONNECTION_DROPPED, true);
                    //stopped http chunk stream from here and mark producer complete
                    request.getPipe().forceProducerComplete(decoder);
                }
                httpContext.setAttribute(PassThroughConstants.MESSAGE_SIZE_VALIDATION_SUM, messageSizeSum);
            }

            if (readBytes > 0) {
                metrics.incrementBytesReceived(readBytes);
            }

        } catch (IOException e) {
            logIOException(conn, e);

            informReaderError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    /**
     * Closes the source side HTTP connection.
     *
     * @param conn HTTP server connection reference
     */
    private void dropSourceConnection(NHttpServerConnection conn) {
        try {
            HttpContext httpContext = conn.getContext();

            HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_REQUEST_TOO_LONG,
                    "Payload Too Large");
            response.setParams(new DefaultedHttpParams(sourceConfiguration.getHttpParams(), response.getParams()));
            response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

            httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, null);
            httpContext.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

            sourceConfiguration.getHttpProcessor().process(response, httpContext);

            conn.submitResponse(response);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            conn.close();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void responseReady(NHttpServerConnection conn) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);
            if (protocolState.compareTo(ProtocolState.REQUEST_DONE) < 0) {                
                return;
            }

            if (protocolState.compareTo(ProtocolState.CLOSING) >= 0) {
                informWriterError(conn);
                return;
            }

            if (protocolState != ProtocolState.REQUEST_DONE) {
                handleInvalidState(conn, "Writing a response");
                return;
            }

            // because the duplex nature of http core we can reach hear without a actual response
            SourceResponse response = SourceContext.getResponse(conn);
            SourceRequest request = SourceContext.getRequest(conn);
            if (response != null) {

                // Handle Http ETag
                String ifNoneMatchHeader =
                        SourceContext.getRequest(conn).getHeaders().get(HttpHeaders.IF_NONE_MATCH);
                if (ifNoneMatchHeader != null) {
                    String eTagHeader = response.getHeader(HttpHeaders.ETAG);
                    if (eTagHeader != null) {
                        for (String hashValue : ifNoneMatchHeader.split(",")) {
                            if (hashValue.trim().equals(eTagHeader)) {
                                response.setStatus(HttpStatus.SC_NOT_MODIFIED);
                                break;
                            }
                        }
                    }
                }
            
                response.start(conn);
                conn.getContext().setAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_START_TIME,
                        System.currentTimeMillis());
                metrics.incrementMessagesSent();
                if (!response.hasEntity()) {
                   // Update stats as outputReady will not be triggered for no entity responses
                    HttpContext context = conn.getContext();
                    if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                        logCorrelationRoundTrip(context,request);
                    }
                    updateLatencyView(context);
                }
            }
        } catch (IOException e) {
            logIOException(conn, e);

            informWriterError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } catch (HttpException e) {
            log.error(e.getMessage(), e);

            informWriterError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    public void outputReady(NHttpServerConnection conn,
                            ContentEncoder encoder) {
        try {
            ProtocolState protocolState = SourceContext.getState(conn);
            
            //special case to handle WSDLs
            if(protocolState == ProtocolState.WSDL_RESPONSE_DONE){
            	// we need to shut down if the shutdown flag is set
            	 HttpContext context = conn.getContext();
            	 ContentOutputBuffer outBuf = (ContentOutputBuffer) context.getAttribute(
                         "synapse.response-source-buffer");
            	  int bytesWritten = outBuf.produceContent(encoder);
                  if (metrics != null && bytesWritten > 0) {
                      metrics.incrementBytesSent(bytesWritten);
                  }
                
                  conn.requestInput();
                  if(outBuf instanceof SimpleOutputBuffer && !((SimpleOutputBuffer)outBuf).hasData()){
                	  sourceConfiguration.getSourceConnections().releaseConnection(conn);
                  }
                  endTransaction(conn);
            	return;
            }
            
                        
            if (protocolState != ProtocolState.RESPONSE_HEAD
                    && protocolState != ProtocolState.RESPONSE_BODY) {
                log.warn("Illegal incoming connection state: "
                        + protocolState + " . Possibly two send backs " +
                        "are happening for the same request");

                handleInvalidState(conn, "Trying to write response body");
                endTransaction(conn);
                return;
            }
            SourceRequest request = SourceContext.getRequest(conn);
            SourceContext.updateState(conn, ProtocolState.RESPONSE_BODY);

            SourceResponse response = SourceContext.getResponse(conn);

            int bytesSent = response.write(conn, encoder);
            
			if (encoder.isCompleted()) {
                HttpContext context = conn.getContext();
                long departure = System.currentTimeMillis();
                context.setAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_END_TIME,departure);
                context.setAttribute(PassThroughConstants.RES_DEPARTURE_TIME,departure);

                if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                    logCorrelationRoundTrip(context, request);
                }
                updateLatencyView(context);
			}
			endTransaction(conn);
            metrics.incrementBytesSent(bytesSent);
        } catch (IOException e) {
            logIOException(conn, e);

            informWriterError(conn);

            SourceContext.updateState(conn, ProtocolState.CLOSING);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        }
    }

    private void logCorrelationRoundTrip(HttpContext context, SourceRequest request) {
        MDC.put(PassThroughConstants.CORRELATION_MDC_PROPERTY,
                context.getAttribute(PassThroughConstants.CORRELATION_ID).toString());
        long startTime = (long) context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME);
        correlationLog.info((System.currentTimeMillis() - startTime) + " | HTTP | "
                + context.getAttribute("http.connection") + " | " + request.getMethod() + " | " + request.getUri()
                + " | ROUND-TRIP LATENCY ");
        MDC.remove(PassThroughConstants.CORRELATION_MDC_PROPERTY);
    }

    public void logIOException(NHttpServerConnection conn, IOException e) {
        // this check feels like crazy! But weird things happened, when load testing.
        if (e == null) {
            return;
        }
        if (e instanceof ConnectionClosedException || (e.getMessage() != null && (
                e.getMessage().toLowerCase().contains("connection reset by peer") ||
                e.getMessage().toLowerCase().contains("forcibly closed")))) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": I/O error (Probably the keepalive connection " +
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

            metrics.incrementFaultsReceiving();
        } else {
            log.error("Unexpected I/O error: " + e.getClass().getName(), e);

            metrics.incrementFaultsReceiving();
        }
    }

    public void timeout(NHttpServerConnection conn) {
    	boolean isTimeoutOccurred = false;
        ProtocolState state = SourceContext.getState(conn);

        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": Keep-Alive connection was time out: ");
            }
        } else if (state == ProtocolState.REQUEST_BODY ||
                state == ProtocolState.REQUEST_HEAD) {

            metrics.incrementTimeoutsReceiving();

            informReaderError(conn);
            isTimeoutOccurred = true;

            log.warn("Connection time out while reading the request: " + conn +
                     " Socket Timeout : " + conn.getSocketTimeout() +
                     getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "TIMEOUT in " + state.name());
            }
        } else if (state == ProtocolState.RESPONSE_BODY ||
                state == ProtocolState.RESPONSE_HEAD) {
            informWriterError(conn);
            isTimeoutOccurred = true;
            log.warn("Connection time out while writing the response: " + conn +
                     " Socket Timeout : " + conn.getSocketTimeout() +
                     getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "TIMEOUT in " + state.name());
            }
        } else if (state == ProtocolState.REQUEST_DONE) {
            informWriterError(conn);
        	isTimeoutOccurred = true;
            log.warn("Connection time out after request is read: " + conn +
                     " Socket Timeout : " + conn.getSocketTimeout() +
                     getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "TIMEOUT in " + state.name());
            }
        }

        SourceContext.updateState(conn, ProtocolState.CLOSED);
   
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
		if (isTimeoutOccurred) {
			rollbackTransaction(conn);
		}        
    }

    public void closed(NHttpServerConnection conn) {
        ProtocolState state = SourceContext.getState(conn);
        boolean isFault = false;
        if (state == ProtocolState.REQUEST_READY || state == ProtocolState.RESPONSE_DONE) {
            if (log.isDebugEnabled()) {
                log.debug(conn + ": Keep-Alive connection was closed: " +
                          getConnectionLoggingInfo(conn));
            }
        } else if (state == ProtocolState.REQUEST_BODY ||
                state == ProtocolState.REQUEST_HEAD) {
        	isFault = true;
            informReaderError(conn);
            log.warn("Connection closed while reading the request: " + conn + getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "Connection Closed in " + state.name());
            }
        } else if (state == ProtocolState.RESPONSE_BODY ||
                state == ProtocolState.RESPONSE_HEAD) {
        	isFault = true;
            informWriterError(conn);
            log.warn("Connection closed while writing the response: " + conn + getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "Connection Closed in " + state.name());
            }
        } else if (state == ProtocolState.REQUEST_DONE) {
        	isFault = true;
            informWriterError(conn);
            log.warn("Connection closed by the client after request is read: " + conn + getConnectionLoggingInfo(conn));
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "Connection Closed in " + state.name());
            }
        }

        metrics.disconnected();

        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, isFault);
		if (isFault) {
			rollbackTransaction(conn);
		}        
    }

    public void endOfInput(NHttpServerConnection conn) throws IOException {
        conn.close();
    }

    public void exception(NHttpServerConnection conn, Exception ex) {
    	boolean isFault = false;
        if (ex instanceof IOException) {
            logIOException(conn, (IOException) ex);

            metrics.incrementFaultsReceiving();

            ProtocolState state = SourceContext.getState(conn);
            if (state == ProtocolState.REQUEST_BODY ||
                    state == ProtocolState.REQUEST_HEAD) {
                informReaderError(conn);
            } else if (state == ProtocolState.RESPONSE_BODY ||
                    state == ProtocolState.RESPONSE_HEAD) {
                informWriterError(conn);
            } else if (state == ProtocolState.REQUEST_DONE) {
                informWriterError(conn);
            } else if (state == ProtocolState.RESPONSE_DONE) {
                informWriterError(conn);
            }
            isFault = true;
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
        } else if (ex instanceof HttpException) {
            log.error("HttpException occurred ", ex);
            if (sourceConfiguration.isCorrelationLoggingEnabled()) {
                logHttpRequestErrorInCorrelationLog(conn, "HTTP Exception");
            }
            try {
                if (conn.isResponseSubmitted()) {
                    sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
                    return;
                }
                HttpContext httpContext = conn.getContext();

                HttpResponse response = new BasicHttpResponse(
                        HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad request");
                response.setParams(
                        new DefaultedHttpParams(sourceConfiguration.getHttpParams(),
                                response.getParams()));
                response.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

                // Pre-process HTTP request
                httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
                httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, null);
                httpContext.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

                sourceConfiguration.getHttpProcessor().process(response, httpContext);

                conn.submitResponse(response);            
                SourceContext.updateState(conn, ProtocolState.CLOSED);
                informWriterError(conn);
                conn.close();
            } catch (Exception ex1) {
                log.error(ex1.getMessage(), ex1);
                SourceContext.updateState(conn, ProtocolState.CLOSED);
                sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
                isFault = true;
            }
        } else {
            log.error("Unexpected error: " + ex.getMessage(), ex);
            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
            isFault = true;
        }
        
		if (isFault) {
			rollbackTransaction(conn);
		}     
    }

    private void handleInvalidState(NHttpServerConnection conn, String action) {
        log.warn(action + " while the handler is in an inconsistent state " +
                SourceContext.getState(conn));
        SourceContext.updateState(conn, ProtocolState.CLOSED);
        sourceConfiguration.getSourceConnections().shutDownConnection(conn, true);
    }

    public void informReaderError(NHttpServerConnection conn) {
        Pipe reader = SourceContext.get(conn).getReader();

        metrics.incrementFaultsReceiving();

        if (reader != null) {
            reader.producerError();
        } else {
            log.info("Reader null when calling informReaderError");
        }
    }

    public void informWriterError(NHttpServerConnection conn) {
        Pipe writer = SourceContext.get(conn).getWriter();

        metrics.incrementFaultsSending();

        if (writer != null) {
            writer.consumerError();
        } else {
            log.info("Writer null when calling informWriterError");
        }
    }
    
    /**
     * Commit the response to the connection. Processes the response through the configured
     * HttpProcessor and submits it to be sent out. This method hides any exceptions and is targetted
     * for non critical (i.e. browser requests etc) requests, which are not core messages
     * @param conn the connection being processed
     * @param response the response to commit over the connection
     */
    public void commitResponseHideExceptions(
            final NHttpServerConnection conn, final HttpResponse response) {
        try {
            conn.suspendInput();
            sourceConfiguration.getHttpProcessor().process(response, conn.getContext());
            conn.submitResponse(response);
        } catch (HttpException e) {
            handleException("Unexpected HTTP protocol error : " + e.getMessage(), e, conn);
        } catch (IOException e) {
            handleException("IO error submiting response : " + e.getMessage(), e, conn);
        }
    }
    /**
     * Shutting down the thread pools.
     */
    public void stop() {
        latencyView.destroy();
        s2sLatencyView.destroy();
        threadingView.destroy();
        try {
            if (sourceConfiguration.getWorkerPool() != null) {
                sourceConfiguration.getWorkerPool().shutdown(1000);
            }
        } catch (InterruptedException e) {
            log.warn("Error while shutting down worker thread pool. " + e.getMessage());
        }
    }
    
    // ----------- utility methods -----------

    private void handleException(String msg, Exception e, NHttpServerConnection conn) {
        log.error(msg, e);
        if (conn != null) {
            //shutdownConnection(conn);
        }
    }


    private void updateLatencyView(HttpContext context) {
        if (context == null) {
            return;
        }
        latencyView.notifyTimes(new LatencyCollector(context, false));
        s2sLatencyView.notifyTimes(new LatencyCollector(context, true));
        LatencyCollector.clearTimestamps(context);
    }
    

    /**
     * Create synapse.response-source-buffer for GET and HEAD Http methods
     * @param method  Http Method
     * @param request Source Request
     * @return OutputStream
     */
    public OutputStream getOutputStream(String method,SourceRequest request){
        OutputStream os=null;
        if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)) {
            HttpContext context = request.getConnection().getContext();
            ContentOutputBuffer outputBuffer = new SimpleOutputBuffer(
                    sourceConfiguration.getIOBufferSize(), new HeapByteBufferAllocator());
            context.setAttribute("synapse.response-source-buffer",outputBuffer);
            os = new ContentOutputStream(outputBuffer);
        }
        return os;
    }

    /**
     * Create SourceRequest from NHttpServerConnection conn
     * @param conn the connection being processed
     * @return SourceRequest
     * @throws IOException
     * @throws HttpException
     */
    public SourceRequest getSourceRequest(NHttpServerConnection conn) throws IOException, HttpException {
        HttpContext context = conn.getContext();
        context.setAttribute(PassThroughConstants.REQ_ARRIVAL_TIME, System.currentTimeMillis());

        if (!SourceContext.assertState(conn, ProtocolState.REQUEST_READY) && !SourceContext.assertState(conn, ProtocolState.WSDL_RESPONSE_DONE)) {
            handleInvalidState(conn, "Request received");
            return null;
        }
        // we have received a message over this connection. So we must inform the pool
        sourceConfiguration.getSourceConnections().useConnection(conn);

        // at this point we have read the HTTP Headers
        SourceContext.updateState(conn, ProtocolState.REQUEST_HEAD);

        SourceRequest request = new SourceRequest(
                sourceConfiguration, conn.getHttpRequest(), conn);
        SourceContext.setRequest(conn, request);
        request.start(conn);
        metrics.incrementMessagesReceived();
        return request;
    }
    
	private void rollbackTransaction(NHttpServerConnection conn) {
		try {
			Long serverWorkerThreadId = (Long) conn.getContext().getAttribute(
					PassThroughConstants.SERVER_WORKER_THREAD_ID);
			if (serverWorkerThreadId != null) {
				TranscationManger.rollbackTransaction(false,
						serverWorkerThreadId);
			}
		} catch (Exception ex) {
			log.warn("Transaction rollback error after Connection closed "
					+ ex.getMessage() + conn);
		}
	}

	private void endTransaction(NHttpServerConnection conn) {
		try {
			Long serverWorkerThreadId = (Long) conn.getContext().getAttribute(
					PassThroughConstants.SERVER_WORKER_THREAD_ID);
			if (serverWorkerThreadId != null) {
				TranscationManger.endTransaction(false, serverWorkerThreadId);
			}
		} catch (Exception ex) {
			log.warn("Transaction rollback error after Connection closed "
					+ ex.getMessage() + conn);
		}
	}

	private String getConnectionLoggingInfo(NHttpServerConnection conn) {
        if (conn instanceof LoggingNHttpServerConnection) {
            IOSession session = ((LoggingNHttpServerConnection) conn).getIOSession();
            if (session != null) {
                return " Remote Address : " + session.getRemoteAddress();
            }
        }
	    return "";
    }

    private void logHttpRequestErrorInCorrelationLog(NHttpServerConnection conn, String state) {

        SourceContext sourceContext = SourceContext.get(conn);
        if (sourceContext != null) {
            String url = "", method = "";
            if (sourceContext.getRequest() != null) {
                url = sourceContext.getRequest().getUri();
                method = sourceContext.getRequest().getMethod();
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
                long startTime = (long) conn.getContext().getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME);
                correlationLog.info((System.currentTimeMillis() - startTime) + "|HTTP|"
                        + conn.getContext().getAttribute("http.connection") + "|" + method + "|" + url
                        + "|" + state);
                MDC.remove(PassThroughConstants.CORRELATION_MDC_PROPERTY);
            }
        }
    }
}

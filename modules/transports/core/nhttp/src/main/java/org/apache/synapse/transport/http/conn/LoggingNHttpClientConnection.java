/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.http.conn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpMessageParser;
import org.apache.http.nio.NHttpMessageWriter;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionInputBuffer;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.http.access.AccessHandler;
import org.apache.synapse.transport.passthru.TargetHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;

public class LoggingNHttpClientConnection extends DefaultNHttpClientConnection
        implements UpgradableNHttpConnection {

    private static final AtomicLong COUNT = new AtomicLong();

    private final Log log;
    private final Log iolog;
    private final Log headerlog;
    private final Log wirelog;
    private final Log accesslog;
    private final String id;

    private IOSession original;
    private boolean releaseConn = false;

    public LoggingNHttpClientConnection(
            final IOSession session,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(session, responseFactory, allocator, params);
        this.log = LogFactory.getLog(getClass());
        this.iolog = LogFactory.getLog(session.getClass());
        this.headerlog = LogFactory.getLog(LoggingUtils.HEADER_LOG_ID);
        this.wirelog = LogFactory.getLog(LoggingUtils.WIRE_LOG_ID);
        this.accesslog = LogFactory.getLog(LoggingUtils.ACCESS_LOG_ID);
        this.id = "http-outgoing-" + COUNT.incrementAndGet();
        this.original = session;
        if (this.iolog.isDebugEnabled() || this.wirelog.isDebugEnabled() || SynapseDebugInfoHolder.getInstance().isDebuggerEnabled()) {
            super.bind(new LoggingIOSession(session, this.id, this.iolog, this.wirelog));
        }
    }

    @Override
    public void close() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Close connection");
        }
        super.close();
    }

    @Override
    public void shutdown() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Shutdown connection");
        }
        super.shutdown();
    }

    @Override
    public void submitRequest(final HttpRequest request) throws IOException, HttpException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": "  + request.getRequestLine().toString());
        }
        super.submitRequest(request);
    }

    @Override
    public void consumeInput(final NHttpClientEventHandler handler) {
//        this.log.info("***************** LoggingNHttpClientConnection consumeInput begin - " + Thread.currentThread().getId());
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Consume input");
        }
//        super.consumeInput(handler);
        if (getContext() == null) {
            return;
        }
        SynapseWireLogHolder logHolder = null;
        if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
            logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
        } else {
            logHolder = new SynapseWireLogHolder();
        }
//        this.log.info("***************** LoggingNHttpClientConnection consumeInput locking - " + Thread.currentThread().getId());
        synchronized (logHolder) {
//            this.log.info("***************** LoggingNHttpClientConnection consumeInput locked - " + Thread.currentThread().getId());
            logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_RESPONSE_READY);
            getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
            if (this.status != ACTIVE) {
                this.session.clearEvent(EventMask.READ);
                return;
            }
            try {
                if (this.response == null) {
                    int bytesRead;
                    do {
                        bytesRead = this.responseParser.fillBuffer(this.session.channel());
                        if (bytesRead > 0) {
                            this.inTransportMetrics.incrementBytesTransferred(bytesRead);
                        }
                        this.response = this.responseParser.parse();
                    } while (bytesRead > 0 && this.response == null);
                    if (this.response != null) {
                        if (this.response.getStatusLine().getStatusCode() >= 200) {
                            final HttpEntity entity = prepareDecoder(this.response);
                            this.response.setEntity(entity);
                            this.connMetrics.incrementResponseCount();
                        }
                        onResponseReceived(this.response);
                        handler.responseReceived(this);
                        if (this.contentDecoder == null) {
                            resetInput();
                        }
                    }
                    if (bytesRead == -1) {
                        handler.endOfInput(this);
                    }
                }
                if (this.contentDecoder != null && (this.session.getEventMask() & SelectionKey.OP_READ) > 0) {
                    handler.inputReady(this, this.contentDecoder);
                    if (this.contentDecoder.isCompleted()) {
//                        this.log.info("***************** LoggingNHttpClientConnection consumeInput TARGET_RESPONSE_DONE - final 1111111 - " + Thread.currentThread().getId()); //todo this is the place where it ends
                        if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
                            logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                            logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_RESPONSE_DONE);
                            getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
                        }
                        // Response entity received
                        // Ready to receive a new response
                        resetInput();
                    }
                }
            } catch (final HttpException ex) {
                resetInput();
                handler.exception(this, ex);
            } catch (final Exception ex) {
                handler.exception(this, ex);
            } finally {
                // Finally set buffered input flag
                if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
                    logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                    logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_RESPONSE_DONE);
                    getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
                }
                this.hasBufferedInput = this.inbuf.hasData();
            }

            if (isReleaseConn()) {
                if (handler instanceof TargetHandler) {
//                    this.log.info("***************** LoggingNHttpClientConnection consumeInput TARGET_RESPONSE_DONE - final 2222222 - " + Thread.currentThread().getId()); //todo this is the place where it ends
                    ((TargetHandler) handler).getTargetConfiguration().getConnections().releaseConnection(this);
                    this.setReleaseConn(false);
                }
            }
//            this.log.info("***************** LoggingNHttpClientConnection consumeInput unlocking - " + Thread.currentThread().getId());
        }
//        this.log.info("***************** LoggingNHttpClientConnection consumeInput unlocked - " + Thread.currentThread().getId());
    }

    @Override
    public void produceOutput(final NHttpClientEventHandler handler) {
//        this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput begin - " + Thread.currentThread().getId());
        if (this.log.isDebugEnabled()) {
            this.log.debug(this.id + ": Produce output");
        }
//        super.produceOutput(handler);

        if (getContext() == null) {
            return;
        }
        SynapseWireLogHolder logHolder = null;
        if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
            logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
        } else {
            logHolder = new SynapseWireLogHolder();
        }

//        this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput locking - " + Thread.currentThread().getId());
        synchronized (logHolder) {
//            this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput locked - " + Thread.currentThread().getId());
            logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_REQUEST_READY);
            getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
            try {
                if (this.status == ACTIVE) {
                    if (this.contentEncoder == null) {
                        handler.requestReady(this);
                    }
                    if (this.contentEncoder != null) {
                        handler.outputReady(this, this.contentEncoder);
                        if (this.contentEncoder.isCompleted()) {
                            resetOutput();
                        }
                    }
                }
                if (this.outbuf.hasData()) {
                    final int bytesWritten = this.outbuf.flush(this.session.channel());
                    if (bytesWritten > 0) {
                        this.outTransportMetrics.incrementBytesTransferred(bytesWritten);
                    }
                }
                if (!this.outbuf.hasData()) {
                    if (this.status == CLOSING) {
                        this.session.close();
                        this.status = CLOSED;
                        resetOutput();
                    }
                    if (this.contentEncoder == null && this.status != CLOSED) {
//                        this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput TARGET_REQUEST_DONE - final - " + Thread.currentThread().getId()); //todo this is the place where it ends
                        if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
                            logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                            logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_REQUEST_DONE);
                            getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
                        }
                        this.session.clearEvent(EventMask.WRITE);
                    }
                }
            } catch (final Exception ex) {
                handler.exception(this, ex);
            } finally {
                // Finally set the buffered output flag
                if (getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) != null) {
                    logHolder = (SynapseWireLogHolder) getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                    logHolder.setPhase(SynapseWireLogHolder.PHASE.TARGET_REQUEST_DONE);
                    getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
                }
                this.hasBufferedOutput = this.outbuf.hasData();
            }

//            this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput unlocking - " + Thread.currentThread().getId());
        }
//        this.log.info("++++++++++++++++ LoggingNHttpClientConnection produceOutput unlocked - " + Thread.currentThread().getId());
    }

    @Override
    protected NHttpMessageWriter<HttpRequest> createRequestWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        return new LoggingNHttpMessageWriter(
                super.createRequestWriter(buffer, params));
    }

    @Override
    protected NHttpMessageParser<HttpResponse> createResponseParser(
            final SessionInputBuffer buffer,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        return new LoggingNHttpMessageParser(
                super.createResponseParser(buffer, responseFactory, params));
    }

    public IOSession getIOSession() {
        return this.original;
    }

    @Override
    public void bind(final IOSession session) {
        this.original = session;
        if (this.iolog.isDebugEnabled() || this.wirelog.isDebugEnabled() || SynapseDebugInfoHolder.getInstance().isDebuggerEnabled()) {
            super.bind(new LoggingIOSession(session, this.id, this.iolog, this.wirelog));
        } else {
            super.bind(session);
        }
    }

    @Override
    public String toString() {
        return this.id;
    }

    class LoggingNHttpMessageWriter implements NHttpMessageWriter<HttpRequest> {

        private final NHttpMessageWriter<HttpRequest> writer;

        public LoggingNHttpMessageWriter(final NHttpMessageWriter<HttpRequest> writer) {
            super();
            this.writer = writer;
        }

        public void reset() {
            this.writer.reset();
        }

        public void write(final HttpRequest message) throws IOException, HttpException {
            if (message != null && headerlog.isDebugEnabled()) {
                headerlog.debug(id + " >> " + message.getRequestLine().toString());
                Header[] headers = message.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    headerlog.debug(id + " >> " + headers[i].toString());
                }
            }
            if (message != null && accesslog.isInfoEnabled()) {
                HttpRequest request = (HttpRequest) message;
                HttpParams params = request.getParams();

                final SocketAddress remoteAddress = session.getRemoteAddress();
                if (remoteAddress instanceof InetSocketAddress) {
                    final InetSocketAddress remote = (( InetSocketAddress) remoteAddress);
                    params.setParameter("http.remote.addr",
                            remote.getAddress().getHostAddress());
                }

                AccessHandler.getAccess().addAccessToQueue(message);
            }
            this.writer.write(message);
        }

    }

    class LoggingNHttpMessageParser implements NHttpMessageParser<HttpResponse> {

        private final NHttpMessageParser<HttpResponse> parser;

        public LoggingNHttpMessageParser(final NHttpMessageParser<HttpResponse> parser) {
            super();
            this.parser = parser;
        }

        public void reset() {
            this.parser.reset();
        }

        public int fillBuffer(final ReadableByteChannel channel) throws IOException {
            return this.parser.fillBuffer(channel);
        }

        public HttpResponse parse() throws IOException, HttpException {
            HttpResponse message = this.parser.parse();
            if (message != null && headerlog.isDebugEnabled()) {
                headerlog.debug(id + " << " + message.getStatusLine().toString());
                Header[] headers = message.getAllHeaders();
                for (int i = 0; i < headers.length; i++) {
                    headerlog.debug(id + " << " + headers[i].toString());
                }
            }
            if (message != null && accesslog.isInfoEnabled()) {
                HttpResponse response = (HttpResponse) message;
                HttpParams params = response.getParams();

                final SocketAddress remoteAddress = session.getRemoteAddress();
                if (remoteAddress instanceof InetSocketAddress) {
                    final InetSocketAddress remote = (( InetSocketAddress) remoteAddress);
                    params.setParameter("http.remote.addr",
                            remote.getAddress().getHostAddress());
                }

                AccessHandler.getAccess().addAccessToQueue(message);
            }
            return message;
        }

    }

    public boolean isReleaseConn() {
        return releaseConn;
    }

    public void setReleaseConn(boolean releaseConn) {
        this.releaseConn = releaseConn;
    }

}

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

package org.apache.synapse.samples.framework;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple HTTP server implementation which is capable of echoing back the body
 * or the headers of an HTTP request.
 */
public class EchoHttpServerController extends AbstractBackEndServerController {

    private static final Log log = LogFactory.getLog(EchoHttpServerController.class);

    private final int port;
    private final boolean echoHeaders;
    private RequestListenerThread requestListener;

    public EchoHttpServerController(OMElement element) {
        super(element);
        port = Integer.parseInt(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_ECHO_HTTP_PORT, "9000"));
        echoHeaders = Boolean.parseBoolean(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_ECHO_HEADERS, "false"));
    }

    public boolean startProcess() {
        try {
            requestListener = new RequestListenerThread(port, echoHeaders);
            requestListener.start();
            return true;
        } catch (IOException e) {
            log.error("Error while initializing echo server", e);
            return false;
        }
    }

    public boolean stopProcess() {
        requestListener.halt();
        requestListener = null;
        return true;
    }

    static class EchoHttpHandler implements HttpRequestHandler {

        private final boolean echoHeaders;

        EchoHttpHandler(boolean echoHeaders) {
            this.echoHeaders = echoHeaders;
        }

        public void handle(HttpRequest request, HttpResponse response,
                           HttpContext context) throws HttpException, IOException {

            if (log.isDebugEnabled()) {
                log.debug(request.getRequestLine().toString());
            }

            if (echoHeaders) {
                StringBuilder body = new StringBuilder();
                for (Header header : request.getAllHeaders()) {
                    body.append(header.getName()).append(": ").append(header.getValue()).append("\n");
                }
                response.setStatusCode(HttpStatus.SC_OK);
                response.setEntity(new StringEntity(body.toString(), ContentType.TEXT_PLAIN));
            } else {
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    byte[] entityContent = EntityUtils.toByteArray(entity);
                    if (log.isDebugEnabled()) {
                        log.debug("Request entity read; size=" + entityContent.length);
                    }
                    response.setStatusCode(HttpStatus.SC_OK);
                    response.setEntity(new ByteArrayEntity(entityContent,
                            ContentType.create(entity.getContentType().getValue())));
                } else {
                    response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                }
            }
        }
    }

    static class RequestListenerThread extends Thread {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpService httpService;

        public RequestListenerThread(final int port, final boolean echoHeaders) throws IOException {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = new ServerSocket();
            this.serversocket.setReuseAddress(true);
            this.serversocket.bind(new InetSocketAddress(port));

            // Set up the HTTP protocol processor
            HttpProcessor httpProcessor = HttpProcessorBuilder.create()
                    .add(new ResponseDate())
                    .add(new ResponseServer("EchoServer"))
                    .add(new ResponseContent())
                    .add(new ResponseConnControl()).build();

            // Set up request handlers
            UriHttpRequestHandlerMapper registry = new UriHttpRequestHandlerMapper();
            registry.register("*", new EchoHttpHandler(echoHeaders));

            // Set up the HTTP service
            this.httpService = new HttpService(httpProcessor, registry);
            this.setName("echo-http-server");
        }

        @Override
        public void run() {
            log.info("Listening on port " + this.serversocket.getLocalPort());
            AtomicInteger counter = new AtomicInteger(0);
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn, counter.incrementAndGet());
                    t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    if (Thread.interrupted()) {
                        break;
                    }
                    log.error("I/O error initializing connection thread", e);
                    break;
                }
            }
        }

        public void halt() {
            log.info("Shutting down echo server");
            try {
                this.interrupt();
                this.serversocket.close();
                while (this.isAlive() || !this.serversocket.isClosed()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (IOException e) {
                log.warn("Error while shutting down echo server", e);
            }
        }
    }

    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn,
                final int counter) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
            this.setName("echo-http-worker-" + counter);
        }

        @Override
        public void run() {
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                log.debug("Client closed the connection", ex);
            } catch (IOException ex) {
                log.error("I/O error: " + ex.getMessage(), ex);
            } catch (HttpException ex) {
                log.error("Unrecoverable HTTP protocol violation: " + ex.getMessage(), ex);
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    }
}

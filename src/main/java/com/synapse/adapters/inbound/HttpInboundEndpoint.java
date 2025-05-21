/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.adapters.inbound;

import com.synapse.adapters.inbound.utils.HTTPInboundConfigurationException;
import com.synapse.adapters.inbound.utils.InboundException;
import com.synapse.core.domain.InboundConfig;
import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.Message;
import com.synapse.core.synctx.MsgContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpInboundEndpoint implements InboundEndpoint {

    private static final Logger logger = LogManager.getLogger(HttpInboundEndpoint.class);

    private final InboundConfig config;
    private InboundMessageMediator mediator;
    private HttpServer httpServer;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public HttpInboundEndpoint(InboundConfig config) {
        this.config = config;
    }

    @Override
    public void start(InboundMessageMediator mediator) throws Exception {
        this.mediator = mediator;
        validateConfig();

        int port = Integer.parseInt(config.getParameters().getOrDefault("inbound.http.port", "8080"));
        String contextPath = config.getParameters().getOrDefault("inbound.http.context", "/");

        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        httpServer.createContext(contextPath, new HttpRequestHandler());

        httpServer.setExecutor(virtualExecutor);
        httpServer.start();
        isRunning.set(true);

        logger.info("HTTP Inbound Endpoint started at http://localhost:{}{}", port, contextPath);
    }

    @Override
    public void stop(){

        isRunning.set(false);
        virtualExecutor.shutdown();
        if (httpServer != null) {
            httpServer.stop(0);
            logger.info("HTTP Inbound Endpoint stopped.");
        }
    }

    private void validateConfig() throws InboundException {
        if (!"http".equalsIgnoreCase(config.getProtocol())) {
            throw new HTTPInboundConfigurationException("Unsupported protocol, should be 'http'");
        }
        if (!config.getParameters().containsKey("inbound.http.port")) {
            throw new HTTPInboundConfigurationException("Missing 'port' parameter");
        }
    }

    private class HttpRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!isRunning.get()) return;

            try {
                byte[] requestBody = exchange.getRequestBody().readAllBytes();

                String contentType = exchange.getRequestHeaders()
                        .getFirst("Content-Type") != null
                        ? exchange.getRequestHeaders().getFirst("Content-Type")
                        : "text/plain";

                MsgContext context = new MsgContext();
                Message msg = new Message(requestBody, contentType);
                context.setMessage(msg);

                Map<String, String> properties = Map.of(
                        "isInbound", "true",
                        "ARTIFACT_NAME", "httpinbound",
                        "inboundEndpointName", "http",
                        "ClientApiNonBlocking", "true"
                );

                context.setHeaders(Map.of(
                        "HTTP_METHOD", exchange.getRequestMethod(),
                        "REQUEST_URI", exchange.getRequestURI().toString()
                ));

                context.setProperties(properties);

                mediator.mediateInboundMessage(config.getSequenceName(), context);

                // hardcoded response for now
                String response = "Message received";
                exchange.sendResponseHeaders(200, response.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                logger.info("HTTP Request handled and passed to mediator");

            } catch (Exception e) {
                logger.error("Error handling HTTP request: {}", e.getMessage());
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
            }
        }
    }
}

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

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.HttpHeaders;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.synapse.transport.passthru.RouteRequestMapping;

public class ProxyTunnelHandler {

    private final RouteRequestMapping routeRequestMapping;
    private final ClientConnFactory connFactory;
    private final HttpProcessor httpProcessor;
    
    private volatile boolean requested;
    private volatile boolean completed;
    private volatile boolean successful;
    
    public ProxyTunnelHandler(
            final RouteRequestMapping routeRequestMapping,
            final ClientConnFactory connFactory) {
        super();
        this.routeRequestMapping = routeRequestMapping;
        this.connFactory = connFactory;
        this.httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
            new RequestClientConnControl()
        } );
    }

    public HttpRequest generateRequest(final HttpContext context) throws IOException, HttpException {
        HttpHost target = this.routeRequestMapping.getRoute().getTargetHost();
        HttpRequest connect = new BasicHttpRequest("CONNECT", target.toHostString(), HttpVersion.HTTP_1_1);
        connect.setHeader(HttpHeaders.HOST, target.toHostString());
        this.httpProcessor.process(connect, context);
        context.setAttribute(SynapseHTTPRequestFactory.ENDPOINT_URL, target.toString());
        return connect;
    }
    
    public void setRequested() {
        this.requested = true;
    }
    
    public boolean isRequested() {
        return this.requested;
    }
    
    public void handleResponse(final HttpResponse response, final NHttpClientConnection conn) {
        this.completed = true;
        int code = response.getStatusLine().getStatusCode();
        if (code >= 200 && code < 300) {
            this.successful = true;
            HttpRoute route = routeRequestMapping.getRoute();
            if (route.isLayered() && conn instanceof UpgradableNHttpConnection) {
                this.connFactory.upgrade((UpgradableNHttpConnection) conn, routeRequestMapping);
            }
        } else {
            this.successful = false;
        }
    }
    
    public boolean isCompleted() {
        return this.completed;
    }
    
    public boolean isSuccessful() {
        return this.successful;
    }
    
    public HttpHost getProxy() {
        return this.routeRequestMapping.getRoute().getProxyHost();
    }
    
}

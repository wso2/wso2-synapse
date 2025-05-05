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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponseFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.nio.reactor.ssl.SSLMode;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.passthru.RouteRequestMapping;

import javax.net.ssl.SSLContext;

/**
 * This custom client connection factory can keep a map of SSLContexts and use the correct
 * SSLContext when connecting to different servers. If a SSLContext cannot be found for a
 * particular server from the specified map it uses the default SSLContext.
 */
public class ClientConnFactory {
    protected Log log = LogFactory.getLog(ClientConnFactory.class);;

    private final HttpResponseFactory responseFactory;
    private final ByteBufferAllocator allocator;
    private final SSLContextDetails ssl;
    private final ConcurrentMap<RequestDescriptor, SSLContext> sslByHostMap;
    private final HttpParams params;
    private static final String ALL_HOSTS = "*";
    public ClientConnFactory(
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final SSLContextDetails ssl,
            final Map<RequestDescriptor, SSLContext> sslByHostMap,
            final HttpParams params) {
        super();
        this.responseFactory = responseFactory != null ? responseFactory : new DefaultHttpResponseFactory();
        this.allocator = allocator != null ? allocator : new HeapByteBufferAllocator();
        this.ssl = ssl;
        this.sslByHostMap = sslByHostMap != null ? new ConcurrentHashMap<RequestDescriptor, SSLContext>(sslByHostMap) : null;
        this.params = params != null ? params : new BasicHttpParams();
    }

    public ClientConnFactory(
            final SSLContextDetails ssl,
            final Map<RequestDescriptor, SSLContext> sslByHostMap,
            final HttpParams params) {
        this(null, null, ssl, sslByHostMap, params);
    }

    public ClientConnFactory(
            final HttpParams params) {
        this(null, null, null, null, params);
    }
    
    private SSLContext getSSLContext(final IOSession iosession, String requestID) {
        InetSocketAddress address = (InetSocketAddress) iosession.getRemoteAddress();
        SSLContext customContext = null;
        if (sslByHostMap != null) {
            String host = address.getHostName() + ":" + address.getPort();
            // See if there's a custom SSL profile configured for this server
            customContext = sslByHostMap.get(new RequestDescriptor(host, requestID));
            if (customContext == null) {
                customContext = sslByHostMap.get(new RequestDescriptor(ALL_HOSTS, ""));
            }
        }
        if (customContext != null) {
            return customContext;
        } else {
            return ssl != null ? ssl.getContext() : null;
        }
    }

    private SSLContext getSSLContext(final org.apache.http.HttpHost httpHost, String requestID) {
        String host = httpHost.getHostName() + ":" + httpHost.getPort();
        return getSSLContextForHost(host, requestID);
    }

    private SSLContext getSSLContextForHost(String host, String requestID) {
        SSLContext customContext = null;
        if (sslByHostMap != null) {
            customContext = sslByHostMap.get(new RequestDescriptor(host, requestID));
        }
        if (customContext != null) {
            return customContext;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("The sslByHostMap is null or Custom SSL context is null for the host : " + host);
            }
            return ssl != null ? ssl.getContext() : null;
        }
    }



    public DefaultNHttpClientConnection createConnection(
               final IOSession iosession, final HttpRoute route, String requestID) {
        IOSession customSession;
        if (ssl != null && route.isSecure() && !route.isTunnelled()) {
            SSLContext customContext = getSSLContext(iosession, requestID);
            SSLIOSession ssliosession = createClientModeSSLsession(iosession, customContext);
            iosession.setAttribute(SSLIOSession.SESSION_KEY, ssliosession);
            customSession = ssliosession;
        } else {
            customSession = iosession;
        }
        DefaultNHttpClientConnection conn = LoggingUtils.createClientConnection(
                   customSession, responseFactory, allocator, params);
        int timeout = HttpConnectionParams.getSoTimeout(params);
        conn.setSocketTimeout(timeout);
        return conn;
    }

    public void upgrade(final UpgradableNHttpConnection conn) {
        if (ssl != null) {
            IOSession iosession = conn.getIOSession();
            if (!(iosession instanceof SSLIOSession)) {
                SSLContext customContext = getSSLContext(iosession, "");
                SSLIOSession ssliosession = createClientModeSSLsession(iosession, customContext);
                iosession.setAttribute(SSLIOSession.SESSION_KEY, ssliosession);
                conn.bind(ssliosession);
            }
        }
    }

    public void upgrade(final UpgradableNHttpConnection conn, RouteRequestMapping routeRequestMapping) {
        HttpRoute route = routeRequestMapping.getRoute();
        String requestID = routeRequestMapping.getIdentifier();
        org.apache.http.HttpHost targetHost = route.getTargetHost();
        if (ssl != null) {
            IOSession iosession = conn.getIOSession();
            if (!(iosession instanceof SSLIOSession)) {
                SSLContext customContext = getSSLContext(targetHost, requestID);
                SSLIOSession ssliosession = createClientModeSSLsession(iosession, customContext);
                iosession.setAttribute(SSLIOSession.SESSION_KEY, ssliosession);
                conn.bind(ssliosession);
            }
        }
    }

    /**
     * Returns Set of Host:Port String entries
     *
     * @return String Set
     */
    public Set<RequestDescriptor> getHostList() {
        return sslByHostMap.keySet();

    }

    /**
     * Creates and returns a ssliosession in client mode
     *
     * @param iosession IO session associated with the ssl connection
     * @param customContext SSL context associated with the ssl connection
     *
     * @return created new SSLIOsession
     */
    private SSLIOSession createClientModeSSLsession(IOSession iosession, SSLContext customContext) {
        final SocketAddress address = iosession.getRemoteAddress();
        SSLIOSession ssliosession;
        if (address instanceof InetSocketAddress) {
            final String endpoint = (String) iosession.getAttribute("endPointURI");
            String hostname;
            int port;
            if (endpoint != null && !endpoint.isEmpty()) {
                URI endpointURI;
                URL endpointURL;
                try {
                    endpointURI = new URI(endpoint);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Invalid endpointURI");
                }
                hostname = endpointURI.getHost();
                port = endpointURI.getPort();
                if (hostname == null) {
                    try {
                        endpointURL = new URL(endpoint);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Invalid endpointURL");
                    }
                    hostname = endpointURL.getHost();
                    port = endpointURL.getPort();
                }
            } else {
                hostname = ((InetSocketAddress) address).getHostName();
                port = ((InetSocketAddress) address).getPort();
            }
            ssliosession = new SSLIOSession(
                    iosession, SSLMode.CLIENT, new HttpHost(hostname, port), customContext, ssl.getHandler());
        } else {
            ssliosession = new SSLIOSession(
                    iosession, SSLMode.CLIENT, customContext, ssl.getHandler());
        }
        return ssliosession;
    }

}

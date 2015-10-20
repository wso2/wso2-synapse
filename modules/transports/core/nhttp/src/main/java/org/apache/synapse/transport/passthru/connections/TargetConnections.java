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

package org.apache.synapse.transport.passthru.connections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.synapse.transport.passthru.ConnectCallback;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.TargetContext;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Manages the connection from transport to the back end servers. It keeps track of the
 * connections for host:port pair. 
 */
public class TargetConnections {
    private static final Log log = LogFactory.getLog(TargetConnections.class);

    private HostConnectionPool connectionPool;

    private final String sslSchemaName = "https";

    /** max connections per host:port pair. At the moment all the host:ports can
     * have the same max */
    private int maxConnections;

    /** io-reactor to use for creating connections */
    private ConnectingIOReactor ioReactor;

    /** callback invoked when a connection is made */
    private ConnectCallback callback = null;

    /**
     * Create a TargetConnections with the given IO-Reactor
     *
     * @param ioReactor the IO-Reactor
     * @param targetConfiguration the configuration of the sender
     * @param callback the callback
     */
    public TargetConnections(ConnectingIOReactor ioReactor,
                             TargetConfiguration targetConfiguration,
                             ConnectCallback callback) {

        this.maxConnections = targetConfiguration.getMaxConnections();
        this.ioReactor = ioReactor;
        this.callback = callback;
        this.connectionPool = new HostConnectionPool(maxConnections);
    }

    /**
     * Return a connection to the host:port pair. If a connection is not available
     * return <code>null</code>. If the particular host:port allows to create more connections
     * this method will try to connect asynchronously. If the connection is successful it will
     * be notified in a separate thread.
     *
     * @param route route for the connection
     * @return Either returns a connection if already available or returns null and notifies
     *         the delivery agent when the connection is available
     */
    public HostConnection getConnection(HttpRoute route) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to get a connection " + route);
        }
        HostConnection hostConnection = getHostConnection(route);
        // trying to get an existing connection
        if (hostConnection == null) {
            hostConnection = new HostConnection(route, null);
            HttpHost host = route.getProxyHost() != null ? route.getProxyHost() : route.getTargetHost();
            ioReactor
                    .connect(new InetSocketAddress(host.getHostName(), host.getPort()), null, hostConnection, callback);

        }
        return hostConnection;
    }

    /**
     * get a existing connection for the route from free connection pool
     *
     * @param route route for the connection
     * @return returns a connection if already available or returns null
     */
    public NHttpClientConnection getExistingConnection(HttpRoute route) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to get a existing connection connection " + route);
        }
        HostConnection hostConnection = getHostConnection(route);
        if (hostConnection == null) {
            return null;
        }
        return hostConnection.getConnection();
    }

    /**
     * This connection is no longer valid. So we need to shutdownConnection connection.
     *
     * @param conn connection to shutdownConnection
     */
    public void shutdownConnection(NHttpClientConnection conn) {
        shutdownConnection(conn, false);
    }

    /**
     * This connection is no longer valid. So we need to shutdownConnection connection.
     *
     * @param conn    connection to shutdownConnection
     * @param isError whether an error is causing this shutdown of the connection.
     *                It is very important to set this flag correctly.
     *                When an error causing the shutdown of the connections we should not
     *                release associated writer buffer to the pool as it might lead into
     *                situations like same buffer is getting released to both source and target
     *                buffer factories
     */
    public void shutdownConnection(NHttpClientConnection conn, boolean isError) {
        HostConnection hostConnection =
                (HostConnection) conn.getContext().getAttribute(PassThroughConstants.HOST_CONNECTION);

        TargetContext.get(conn).reset(isError);

        if (hostConnection != null) {
            connectionPool.forget(hostConnection);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");
        }

        try {
            conn.shutdown();
        } catch (IOException ignored) {
        }
    }

    /**
     * Release an active connection to the pool
     *
     * @param conn connection to be released
     */
    public void releaseConnection(NHttpClientConnection conn) {
        HostConnection hostConnection =
                (HostConnection) conn.getContext().getAttribute(PassThroughConstants.HOST_CONNECTION);

        TargetContext.get(conn).reset();

        if (hostConnection != null) {
            connectionPool.release(hostConnection);
        } else {
            // we shouldn't get here
            log.fatal("Connection without a pool. Something wrong. Need to fix.");
        }
    }

    private HostConnection getHostConnection(HttpRoute route) {
        // see weather a pool already exists for this host:port
        return connectionPool.getConnection(route);
    }

    /**
     * Shutdown the connections of the given host:port list. This will allow to create new connection
     * at the next request happens.
     *
     * @param hostList Set of String which contains entries in hots:port format
     */
    public void resetConnectionPool(Set<String> hostList) {

        for (String host : hostList) {
            String[] params = host.split(":");

	        for (HttpRoute httpRoute : connectionPool.getKeySet()) {
		        if (params.length > 1 && params[0].equalsIgnoreCase(httpRoute.getTargetHost().getHostName())
                    && (Integer.valueOf(params[1]) == (httpRoute.getTargetHost().getPort())) &&
                    httpRoute.getTargetHost().getSchemeName().equalsIgnoreCase(sslSchemaName)) {

                    try {
	                    NHttpClientConnection connection = connectionPool.getConnection(httpRoute).getConnection();
	                    if (connection != null && connection.getContext() != null) {

                            shutdownConnection(connection);
                            log.info("Connection " + httpRoute.getTargetHost().getHostName() + ":"
                                     + httpRoute.getTargetHost().getPort() + " Successful");

                        } else {
                            log.debug("Error shutdown connection for " + httpRoute.getTargetHost().getHostName()
                                      + " " + httpRoute.getTargetHost().getPort() + " - Connection not available");
                        }
                    } catch (Exception e) {
                        log.warn("Error shutdown connection for " + httpRoute.getTargetHost().getHostName()
                                 + " " + httpRoute.getTargetHost().getPort() + " ", e);
                    }

                }

            }
        }
    }

}

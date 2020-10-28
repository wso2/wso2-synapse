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
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.http.conn.SynapseHTTPRequestFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.ConnectionTimeoutConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This stores connections for a particular host + port.
 */
public class HostConnections {
    private static final Log log = LogFactory.getLog(HostConnections.class);
    /**
     * route
     */
    private final HttpRoute route;
    /**
     * maximum number of connections allowed for this host + port
     */
    private final int maxSize;
    /**
     * number of awaiting connections
     */
    private int pendingConnections;
    /**
     * connection idle time for connection removal
     */
    private int connectionIdleTime;
    /**
     * maximum life span of a connection
     */
    private int maximumConnectionLifeSpan;

    /**
     * time allocated to avoid a connection being used at the moment it is being closed or timed out
     */
    private int connectionGraceTime;

    /**
     * list of free connections available
     */
    private List<NHttpClientConnection> freeConnections = new ArrayList<NHttpClientConnection>();
    /**
     * list of connections in use
     */
    private List<NHttpClientConnection> busyConnections = new ArrayList<NHttpClientConnection>();

    private Lock lock = new ReentrantLock();

    public HostConnections(HttpRoute route, int maxSize) {
        if (log.isDebugEnabled()) {
            log.debug("Creating new connection pool: " + route);
        }
        this.route = route;
        this.maxSize = maxSize;
    }

    public HostConnections(HttpRoute route, int maxSize, ConnectionTimeoutConfiguration
            connectionTimeoutConfiguration) {

        if (log.isDebugEnabled()) {
            log.debug("Creating new connection pool: " + route);
        }
        this.route = route;
        this.maxSize = maxSize;

        this.connectionIdleTime = connectionTimeoutConfiguration.getConnectionIdleTime();
        this.maximumConnectionLifeSpan = connectionTimeoutConfiguration.getMaximumConnectionLifeSpane();
        this.connectionGraceTime = connectionTimeoutConfiguration.getConnectionGraceTime();
    }

    /**
     * Get a connection for the host:port
     *
     * @return a connection
     */
    public NHttpClientConnection getConnection() {
        lock.lock();
        try {
            while (!freeConnections.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Returning an existing free connection " + route);
                }
                NHttpClientConnection conn = freeConnections.get(0);
                long currentTime = System.currentTimeMillis();
                long connectionInitTime = (Long) conn.getContext().getAttribute(PassThroughConstants.
                                                                                        CONNECTION_INIT_TIME);
                long expiryTime = (Long) conn.getContext().getAttribute(PassThroughConstants.
                        CONNECTION_EXPIRY_TIME);
                if (isMaximumLifeSpanExceeded(currentTime, connectionInitTime) ||  currentTime >= expiryTime ) {
                    freeConnections.remove(conn);
                    try {
                        conn.shutdown();
                    } catch (IOException io) {
                        log.error("Error occurred while shutting down connection." + io.getMessage(), io);
                    }
                } else {
                    freeConnections.remove(conn);
                    busyConnections.add(conn);
                    return conn;
                }
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    private boolean isMaximumLifeSpanExceeded(long currentTime, long connectionInitTime) {
        if (maximumConnectionLifeSpan > 0 && currentTime > maximumConnectionLifeSpan + connectionInitTime) {
            if (log.isDebugEnabled()) {
                log.debug("Connection has been persisted for " + (currentTime - connectionInitTime)
                                  + " milliseconds where the maximum connection life span is " +
                                  maximumConnectionLifeSpan + " milliseconds.");
            }
            return true;
        }
        return false;
    }

    public void release(NHttpClientConnection conn) {
        conn.getMetrics().reset();
        HttpContext ctx = conn.getContext();
        ctx.removeAttribute(ExecutionContext.HTTP_REQUEST);
        ctx.removeAttribute(ExecutionContext.HTTP_RESPONSE);
        ctx.setAttribute(PassThroughConstants.CONNECTION_EXPIRY_TIME, getExpiryTime(conn));
        ctx.removeAttribute(SynapseHTTPRequestFactory.ENDPOINT_URL);
        lock.lock();
        try {
            if (busyConnections.remove(conn)) {
                freeConnections.add(conn);
            } else {
                log.error("Attempted to releaseConnection connection not in the busy list");
            }
        } finally {
            lock.unlock();
        }
    }

    private long getExpiryTime(NHttpClientConnection connection) {

        long expiryTime = System.currentTimeMillis();

        Object keepAlive = connection.getContext().
                getAttribute(PassThroughConstants.CONNECTION_KEEP_ALIVE_TIME_OUT);
        if (keepAlive != null) {
            int keepAliveTimeout = (int) keepAlive;
            expiryTime = expiryTime + keepAliveTimeout - this.connectionGraceTime;
        } else {
            expiryTime = expiryTime + this.connectionIdleTime;
        }
        if (log.isDebugEnabled()) {
            log.debug("Expiry time set for connection: " + expiryTime + " milliseconds");
        }
        return expiryTime;
    }

    public void forget(NHttpClientConnection conn) {
        lock.lock();
        try {
            if (!freeConnections.remove(conn)) {
                busyConnections.remove(conn);
            }
        } finally {
            lock.unlock();
        }
    }

    public void addConnection(NHttpClientConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("New connection " + route + " is added to the free list");
        }
        lock.lock();
        try {
            conn.getContext().setAttribute(PassThroughConstants.CONNECTION_INIT_TIME, System.currentTimeMillis());
            busyConnections.add(conn);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indicates that a connection has been successfully established with a remote server
     * as notified by the session request call back.
     */
    public synchronized void pendingConnectionSucceeded() {
        lock.lock();
        try {
            pendingConnections--;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Keep track of the number of times connections to this host:port has failed
     * consecutively
     */
    public void pendingConnectionFailed() {
        lock.lock();
        try {
            pendingConnections--;
        } finally {
            lock.unlock();
        }
    }

    public HttpRoute getRoute() {
        return route;
    }

    public boolean canHaveMoreConnections() {
        return busyConnections.size() + pendingConnections < maxSize;
    }
}

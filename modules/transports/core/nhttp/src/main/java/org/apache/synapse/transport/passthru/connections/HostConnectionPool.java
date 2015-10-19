/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.synapse.transport.passthru.connections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the Target Connection Pool for the transport.
 */
public class HostConnectionPool {

	private static final Log log = LogFactory.getLog(HostConnection.class);

	private final Map<HttpRoute, HostConnections> connMap;
	private Lock lock = new ReentrantLock();

	public HostConnectionPool(int maxConnectionPerRoute) {
		this.connMap = Collections.synchronizedMap(new HashMap<HttpRoute, HostConnections>());
	}

	/**
	 * Get a connection for the host:port
	 *
	 * @return a connection
	 */
	public HostConnection getConnection(HttpRoute route) {
		lock.lock();
		try {
			HostConnections connections = connMap.get(route);
			if (connections == null || connections.getHostConnections() == null ||
			    connections.getHostConnections().isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("No connections available for reuse");
				}
				return null;
			} else {
				HostConnection conn;
				synchronized (connections) {
					while (!connections.getHostConnections().isEmpty()) {
						conn = connections.getHostConnections().remove(0);
						if (conn.getConnection().isOpen() && !conn.getConnection().isStale()) {
							if (log.isDebugEnabled()) {
								log.debug("A connection  : " + route +
								          " is available in the pool, and will be reused");
							}
							conn.getConnection().requestInput();
							return conn;
						} else {
							if (log.isDebugEnabled()) {
								log.debug("closing stale connection : " + route);
							}
							try {
								conn.getConnection().close();
							} catch (IOException ignore) {
							}
						}
					}
				}
				return null;
			}
		} finally {
			lock.unlock();
		}
	}

	public void release(HostConnection conn) {
		conn.getConnection().getMetrics().reset();
		HttpContext ctx = conn.getConnection().getContext();
		ctx.removeAttribute(ExecutionContext.HTTP_REQUEST);
		ctx.removeAttribute(ExecutionContext.HTTP_RESPONSE);
		try {
			lock.lock();
			HostConnections connections;
			synchronized (connMap) {
				// use double locking to make sure
				connections = connMap.get(conn.getRoute());
				if (connections == null) {
					connections = new HostConnections(Collections.synchronizedList(new LinkedList<HostConnection>()));
					connMap.put(conn.getRoute(), connections);
				}
			}
			connections.getHostConnections().add(conn);
			if (log.isDebugEnabled()) {
				log.debug("Released a connection : " + conn.getRoute() +
				          " to the connection pool of current size : " + connections.getHostConnections().size());
			}
		} finally {
			lock.unlock();
		}
	}

	public void forget(HostConnection conn) {
		lock.lock();
		try {
			HostConnections connections = connMap.get(conn.getRoute());
			if (connections != null) {
				synchronized (connections) {
				    /*
                     * This removal is heavily unlikely. It is kept just for the
                     * sake of safety.
                     */
					connections.getHostConnections().remove(conn);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public Set<HttpRoute> getKeySet() {
		return connMap.keySet();
	}
}

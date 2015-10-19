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

import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.NHttpClientConnection;

/**
 * This stores connections for a particular host + port.
 */
public class HostConnection {

	private HttpRoute route;
	private NHttpClientConnection connection;

	public HostConnection(HttpRoute route, NHttpClientConnection connection) {
		this.route = route;
		this.connection = connection;
	}

	public HttpRoute getRoute() {
		return route;
	}

	public void setRoute(HttpRoute route) {
		this.route = route;
	}

	public NHttpClientConnection getConnection() {
		return connection;
	}

	public void setConnection(NHttpClientConnection connection) {
		this.connection = connection;
	}
}

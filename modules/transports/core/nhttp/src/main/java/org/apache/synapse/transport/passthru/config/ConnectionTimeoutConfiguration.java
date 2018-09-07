/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.transport.passthru.config;

/**
 * This class holds the parameters related to control the connection removal.
 *
 * Scenario:
 * This issue happens when calling salesforce endpoint with keepalive connections. First ESB create the connection with
 * salesforce and send the request and get the response. However if the connection is idle for 1min and ESB used the
 * 1min idle connection to send the next message, salesforce will reset the connection. As a solution to this we decided
 * to introduce a connection eviction mechanism as an improvement. So if a connection is idle for a "connectionIdletime"
 * of time or a connection persisted for more than it's "maximumConnectionLifeSpan" then the connection is removed from
 * the connection pool.
 */
public class ConnectionTimeoutConfiguration {

    private int connectionIdleTime;
    private int maximumConnectionLifeSpan;

    public ConnectionTimeoutConfiguration(int connectionIdleTime, int maximumConnectionLifeSpan) {
        this.connectionIdleTime = connectionIdleTime;
        this.maximumConnectionLifeSpan = maximumConnectionLifeSpan;
    }

    public int getConnectionIdleTime() {
        return connectionIdleTime;
    }

    public int getMaximumConnectionLifeSpane() {
        return maximumConnectionLifeSpan;
    }
}

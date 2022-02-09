/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty.config;

/**
 * This class contains the names of Netty transport related configuration properties.
 */
public class NettyConfigPropertyNames {

    /**
     * Defines the core size (number of threads) of the worker thread pool.
     */
    public static final String WORKER_POOL_SIZE_CORE = "worker_pool_size_core";

    /**
     * Defines the maximum size (number of threads) of the worker thread pool.
     */
    public static final String WORKER_POOL_SIZE_MAX = "worker_pool_size_max";

    /**
     * Defines the keep-alive time for extra threads in the worker pool.
     */
    public static final String WORKER_THREAD_KEEP_ALIVE_SEC = "worker_thread_keepalive_sec";

    /**
     * Defines the length of the queue that is used to hold Runnable tasks to be executed by the
     * worker pool.
     */
    public static final String WORKER_POOL_QUEUE_LENGTH = "worker_pool_queue_length";

    /**
     * Defines whether ESB needs to preserve the original User-Agent header.
     */
    public static final String USER_AGENT_HEADER_PRESERVE = "http.user.agent.preserve";

    /**
     * Defines whether ESB needs to preserve the original Server header.
     */
    public static final String SERVER_HEADER_PRESERVE = "http.server.preserve";

    /**
     * Defines whether ESB needs to preserve the original Http header.
     */
    public static final String HTTP_RESPONSE_HEADERS_PRESERVE = "http.response.headers.preserve";

    /**
     * Defines whether ESB needs to preserve the original Http header.
     */
    public static final String HTTP_HEADERS_PRESERVE = "http.headers.preserve";

    /**
     * Defines whether HTTP keep-alive is disabled.
     */
    public static final String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";

    public static final String TRANSPORT_LISTENER_SHUTDOWN_WAIT_TIME_SEC = "transport.listener.shutdown.wait.sec";

    /**
     * Validate the bad formed xml message by building the whole xml document.
     */
    public static final String FORCE_XML_MESSAGE_VALIDATION = "force.xml.message.validation";

    /**
     * Check for invalid json message by parsing the input message.
     */
    public static final String FORCE_JSON_MESSAGE_VALIDATION = "force.json.message.validation";

    public static final String HTTP_GET_REQUEST_PROCESSOR = "http.get.request.processor";

    public static final String HTTP_SERVER_HOSTNAME = "http.server.hostname";

    public static final String HTTP_TRANSPORT_MEDIATION_INTERCEPTOR = "http.transport.mediation.interceptor";

    public static final String HTTP_BLOCK_SERVICE_LIST = "http.block_service_list";

    public static final String FORCE_MESSAGE_BUILDER = "force.message.builder";

    // properties which are allowed to be directly pass through from request context to response context explicitly
    public static final String ALLOWED_RESPONSE_PROPERTIES = "allowed_response_properties";
    public static final String REQUEST_LIMIT_VALIDATION = "http.requestLimits.validation.enabled";
    public static final String MAX_STATUS_LINE_LENGTH = "http.requestLimits.maxStatusLineLength";
    public static final String MAX_HEADER_SIZE = "http.requestLimits.maxHeaderSize";
    public static final String MAX_ENTITY_BODY_SIZE = "http.requestLimits.maxEntityBodySize";
    public static final String CLIENT_REQUEST_LIMIT_VALIDATION = "http.client.requestLimits.validation.enabled";
    public static final String MAX_CLIENT_REQUEST_STATUS_LINE_LENGTH = "http.client.requestLimits.maxStatusLineLength";
    public static final String MAX_CLIENT_REQUEST_HEADER_SIZE = "http.client.requestLimits.maxHeaderSize";
    public static final String MAX_CLIENT_REQUEST_ENTITY_BODY_SIZE = "http.client.requestLimits.maxEntityBodySize";

    public static final String HTTP_SOCKET_TIMEOUT = "http.socket.timeout";

    //Client connection pooling configs
    public static final String ENABLE_CUSTOM_CONNECTION_POOL_CONFIG = "custom.client.connection.pool.config.enabled";
    /**
     * Max active connections per route(host:port). Default value is -1 which indicates unlimited.
     */
    public static final String CONNECTION_POOLING_MAX_ACTIVE_CONNECTIONS =
            "client.connection.pool.maxActiveConnections";
    /**
     * Maximum number of idle connections allowed per pool.
     */
    public static final String CONNECTION_POOLING_MAX_IDLE_CONNECTIONS = "client.connection.pool.maxIdleConnections";

    /**
     * Maximum amount of time, the client should wait for an idle connection before it sends an error
     * when the pool is exhausted.
     */
    public static final String CONNECTION_POOLING_WAIT_TIME = "client.connection.pool.waitTimeInMillis";

    public static final String CLIENT_ENDPOINT_SOCKET_TIMEOUT = "http.client.endpoint.socket.timeout";

}

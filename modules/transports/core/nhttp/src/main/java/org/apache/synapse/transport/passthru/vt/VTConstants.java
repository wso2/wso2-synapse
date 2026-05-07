/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru.vt;

/**
 * Constants for the Virtual Thread based blocking pass-through transport.
 * This transport uses Java 21+ Virtual Threads (Project Loom) and blocking I/O
 * instead of NIO/reactor patterns.
 */
public final class VTConstants {

    private VTConstants() {
        // prevent instantiation
    }

    // ---- Message Context property keys ----
    public static final String VT_SOURCE_REQUEST = "VT_SOURCE_REQUEST";
    public static final String VT_SOURCE_CONFIGURATION = "VT_SOURCE_CONFIGURATION";
    public static final String VT_SOURCE_CONNECTION = "VT_SOURCE_CONNECTION";
    public static final String VT_TARGET_CONFIGURATION = "VT_TARGET_CONFIGURATION";
    public static final String VT_BACKEND_CALL = "VT_BACKEND_CALL";
    /**
     * Body stream of the current Axis2 message context.
     * <p>
     * Request contexts carry the client request body under this key; response
     * contexts carry the backend response body under the same key. This mirrors
     * the pass-through transport's context-local pipe model without using
     * PASS_THROUGH_PIPE, which existing code casts to the NIO Pipe type.
     */
    public static final String VT_STREAM_PIPE = "VT_STREAM_PIPE";

    // ---- Transport names (used in axis2.xml) ----
    public static final String TRANSPORT_NAME_HTTP = "vt-http";

    // ---- Configuration parameter keys ----
    public static final String PARAM_PORT = "port";
    public static final String PARAM_BIND_ADDRESS = "bind-address";
    public static final String PARAM_HOSTNAME = "hostname";

    /** Socket backlog size for ServerSocket */
    public static final String PARAM_BACKLOG = "backlog";
    public static final int DEFAULT_BACKLOG = 1024;

    /** Max active accepted connections handled by virtual threads */
    public static final String VT_MAX_ACCEPT_CONNECTIONS =
            "synapse.vt.accept.max.connections";
    public static final int DEFAULT_VT_MAX_ACCEPT_CONNECTIONS = 1000;

    /** Socket read timeout in millis for both source and target sockets */
    public static final String PARAM_SO_TIMEOUT = "so_timeout";
    public static final int DEFAULT_SO_TIMEOUT = 60_000;

    /** Keep-alive idle timeout in millis — how long to wait for the next request
     *  on a keep-alive connection before closing it.  If not configured,
     *  defaults to {@link #DEFAULT_KEEP_ALIVE_TIMEOUT}. */
    public static final String PARAM_KEEP_ALIVE_TIMEOUT = "keep_alive_timeout";
    public static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 115_000;

    /** HTTP hop-by-hop headers that MUST NOT be forwarded through a proxy. */
    public static final java.util.Set<String> HOP_BY_HOP_HEADERS = java.util.Set.of(
            "transfer-encoding", "content-length", "connection",
            "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "upgrade"
    );

    /** Connection timeout for outbound connections */
    public static final String PARAM_CONNECT_TIMEOUT = "connect_timeout";
    public static final int DEFAULT_CONNECT_TIMEOUT = 10_000;

    /** Whether to enable TCP_NODELAY (Nagle disabled) */
    public static final String PARAM_TCP_NODELAY = "tcp_nodelay";
    public static final boolean DEFAULT_TCP_NODELAY = true;

    /** Socket receive buffer size */
    public static final String PARAM_SO_RCVBUF = "so_rcvbuf";
    public static final int DEFAULT_SO_RCVBUF = 8192;

    /** Socket send buffer size */
    public static final String PARAM_SO_SNDBUF = "so_sndbuf";
    public static final int DEFAULT_SO_SNDBUF = 8192;

    /** Max request body size in bytes (-1 = unlimited) */
    public static final String PARAM_MAX_REQUEST_SIZE = "max_request_size";
    public static final long DEFAULT_MAX_REQUEST_SIZE = -1;

    /** Internal read buffer size for socket streams */
    public static final int STREAM_BUFFER_SIZE = 8192;

    /** Default content type when none given */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** Name prefix for virtual threads */
    public static final String VT_THREAD_PREFIX = "vt-passthru-";

    /**
     * MessageContext property set by {@link VTBlockingMsgSender} before calling
     * {@code VTHttpSender.invoke()}.
     * When present, the sender populates the response directly on the
     * original context and returns — skipping {@code AxisEngine.receive()}.
     */
    public static final String VT_BLOCKING_CALL = "VT_BLOCKING_CALL";

    /**
     * ConfigurationContext property key under which {@link VTHttpSender}
     * registers itself during {@code init()}.
     * {@code CallMediator.init()} looks up this key to obtain the VT transport sender
     * and wraps it in a {@code VTBlockingMsgSender}. The string value must match
     * the literal used in {@code CallMediator} (which cannot import this class).
     */
    public static final String VT_TRANSPORT_SENDER = "VT_TRANSPORT_SENDER";

    // ---- HttpContext attribute keys (set by VTPassThroughHttpListener) ----
    /** Remote socket address stored in HttpContext for the handler */
    public static final String CTX_REMOTE_ADDRESS = "vt.remote.address";
    /** Local socket address stored in HttpContext */
    public static final String CTX_LOCAL_ADDRESS = "vt.local.address";
    /** Local port stored in HttpContext */
    public static final String CTX_LOCAL_PORT = "vt.local.port";

    public static int getSystemInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ignore) {
            return defaultValue;
        }
    }
}

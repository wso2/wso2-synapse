/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.http.conn;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.TargetContext;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ReplayIOSession implements IOSession {

    private static AtomicLong COUNT = new AtomicLong(0);
    private final Log log;
    private final IOSession session;
    private final ByteChannel channel;
    private final String id;

    public ReplayIOSession(
            final IOSession session, final String id) {
        super();
        if (session == null) {
            throw new IllegalArgumentException("I/O session may not be null");
        }
        this.session = session;
        this.channel = new ReplayIOSession.ReplayByteChannel();
        this.id = id + "-" + COUNT.incrementAndGet();
        this.log = LogFactory.getLog(session.getClass());
    }

    public int getStatus() {
        return this.session.getStatus();
    }

    public ByteChannel channel() {
        return this.channel;
    }

    public SocketAddress getLocalAddress() {
        return this.session.getLocalAddress();
    }

    public SocketAddress getRemoteAddress() {
        return this.session.getRemoteAddress();
    }

    public int getEventMask() {
        return this.session.getEventMask();
    }

    private static String formatOps(int ops) {
        StringBuffer buffer = new StringBuffer(6);
        buffer.append('[');
        if ((ops & SelectionKey.OP_READ) > 0) {
            buffer.append('r');
        }
        if ((ops & SelectionKey.OP_WRITE) > 0) {
            buffer.append('w');
        }
        if ((ops & SelectionKey.OP_ACCEPT) > 0) {
            buffer.append('a');
        }
        if ((ops & SelectionKey.OP_CONNECT) > 0) {
            buffer.append('c');
        }
        buffer.append(']');
        return buffer.toString();
    }

    public void setEventMask(int ops) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set event mask "
                    + formatOps(ops));
        }
        this.session.setEventMask(ops);
    }

    public void setEvent(int op) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set event "
                    + formatOps(op));
        }
        this.session.setEvent(op);
    }

    public void clearEvent(int op) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Clear event "
                    + formatOps(op));
        }
        this.session.clearEvent(op);
    }

    public void close() {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Close");
        }
        this.session.close();
    }

    public boolean isClosed() {
        return this.session.isClosed();
    }

    public void shutdown() {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Shutdown");
        }
        this.session.shutdown();
    }

    public int getSocketTimeout() {
        return this.session.getSocketTimeout();
    }

    public void setSocketTimeout(int timeout) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set timeout "
                    + timeout);
        }
        this.session.setSocketTimeout(timeout);
    }

    public void setBufferStatus(final SessionBufferStatus status) {
        this.session.setBufferStatus(status);
    }

    public boolean hasBufferedInput() {
        return this.session.hasBufferedInput();
    }

    public boolean hasBufferedOutput() {
        return this.session.hasBufferedOutput();
    }

    public Object getAttribute(final String name) {
        return this.session.getAttribute(name);
    }

    public void setAttribute(final String name, final Object obj) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Set attribute "
                    + name);
        }
        this.session.setAttribute(name, obj);
    }

    public Object removeAttribute(final String name) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("I/O session " + this.id + " " + this.session + ": Remove attribute "
                    + name);
        }
        return this.session.removeAttribute(name);
    }

    class ReplayByteChannel implements ByteChannel {

        public int read(final ByteBuffer dst) throws IOException {

            int bytesRead = session.channel().read(dst);
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": " + bytesRead + " bytes read");
            }
            return bytesRead;
        }

        public int write(final ByteBuffer src) throws IOException {
            int byteWritten = session.channel().write(src);
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": " + byteWritten + " bytes written");
            }
            // Retrieve request context associated with this session
            Object request = session.getAttribute("CONNECTION_INFORMATION");
            if (!(request instanceof TargetContext)) {
                log.warn("Unexpected connection type in session attribute: " + (request == null ?
                        "null" :
                        request.getClass()));
                return byteWritten;
            }
            // Extract the MessageContext from the target connection
            MessageContext mc = ((TargetContext) request).getRequestMsgCtx();
            // Skip recording for methods not in allowed write list
            String httpMethod = (String) mc.getProperty("HTTP_METHOD");
            if (!isAllowedWriteMethod(httpMethod)) {
                return byteWritten;
            }
            // Generate or fallback to UNKNOWN message ID for traceability
            String messageID = (mc.getMessageID() != null) ? mc.getMessageID() : "UNKNOWN";

            // Read boolean flag to check if replay is enabled per API or resource
            Boolean enabled = (Boolean) mc.getProperty("ENABLE_REPLAY_TRANSACTION");
            boolean isReplayEnabled = enabled != null && enabled;
            if (!isReplayEnabled) {
                if (log.isDebugEnabled()) {
                    log.debug("Replay transaction disabled for message ID: " + messageID);
                }
                return byteWritten;
            }
            // Duplicate and segment ByteBuffer to isolate written data
            ByteBuffer copyBuffer = src.duplicate();
            int currentPos = copyBuffer.position();
            copyBuffer.limit(currentPos);
            copyBuffer.position(currentPos - byteWritten);

            // Handle chunk order tracking
            Integer chunkOrder = (Integer) mc.getProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY);
            if (chunkOrder == null) {
                // First chunk, initialize to 1
                chunkOrder = 1;
            } else {
                // Increment for subsequent chunks
                chunkOrder++;
            }
            // Set or update chunk order in the message context
            mc.setProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY, chunkOrder);

            // Efficiently gather all MessageContext properties into a local metadata map.
            // getProperties() does not return all properties due to AbstractContext implementation,
            // hence properties are explicitly retrieved by iterating over property names.
            Map<String, Object> metadata = new HashMap<>();
            for (Iterator<String> it = mc.getPropertyNames(); it.hasNext(); ) {
                String key = it.next();
                Object value = mc.getProperty(key);
                if (value != null) {
                    metadata.put(key, value);
                }
            }
            // Dispatch replay record for asynchronous persistence or processing
            ReplayDispatcher.getInstance().addReplayRecord(copyBuffer, byteWritten, messageID, metadata);
            return byteWritten;
        }

        /**
         * Checks whether the given HTTP method is permitted for write operations during replay capture.
         *
         * @param method HTTP method name (case-insensitive)
         * @return true if the method is allowed for replay write operations; false otherwise
         */
        private boolean isAllowedWriteMethod(String method) {
            if (method == null) return false;
            return PassThroughConstants.ALLOWED_WRITE_METHODS.contains(method.toUpperCase());
        }

        public void close() throws IOException {
            if (log.isDebugEnabled()) {
                log.debug("I/O session " + id + " " + session + ": Channel close");
            }
            session.channel().close();
        }

        public boolean isOpen() {
            return session.channel().isOpen();
        }

    }

}

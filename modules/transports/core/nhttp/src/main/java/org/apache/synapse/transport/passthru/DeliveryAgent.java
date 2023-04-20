/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.transport.http.conn.LoggingNHttpServerConnection;
import org.apache.synapse.transport.http.conn.ProxyConfig;
import org.apache.synapse.transport.http.conn.SynapseDebugInfoHolder;
import org.apache.synapse.transport.passthru.config.PassThroughConfigPNames;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.util.TargetRequestFactory;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class acts as a gateway for differed delivery of the messages. When a message is to be
 * delivered it is submitted to this class. If a connection is available to the target this
 * class will try to deliver the message immediately over that connection. If a connection is
 * not available it will queue the message and request a connection from the pool. When a new
 * connection is available a queued message will be sent through it. 
 */
public class DeliveryAgent {

    private static final Log log = LogFactory.getLog(DeliveryAgent.class);
    private static final String SYNAPSE_ARTIFACT_TYPE = "SYNAPSE_ARTIFACT_TYPE";
    private static final String SSE = "SSE";
    private static final String SSE_TARGET_CONNECTION = "SSE_TARGET_CONNECTION";
    private static final String SSE_TARGET_CONNECTIONS = "SSE_TARGET_CONNECTIONS";

    /**
     * This Map holds the messages that need to be delivered. But at the moment maximum
     * number of connections to the host:pair is being used. So these messages has to wait
     * until a new connection is available.
     */
    private Map<HttpRoute, Queue<MessageContext>> waitingMessages =
            new ConcurrentHashMap<HttpRoute, Queue<MessageContext>>();

    /** The connection management */
    private TargetConnections targetConnections;

    /** Configuration of the sender */
    private TargetConfiguration targetConfiguration;

    /** Proxy config */
    private ProxyConfig proxyConfig;
    
    /** The maximum number of messages that can wait for a connection */
    private int maxWaitingMessages;

    private TargetErrorHandler targetErrorHandler;

    /** Lock for synchronizing access */
    private Lock lock = new ReentrantLock();

    /**
     * Create a delivery agent with the target configuration and connection management.
     *
     * @param targetConfiguration configuration of the sender
     * @param targetConnections connection management
     */
    public DeliveryAgent(TargetConfiguration targetConfiguration,
                         TargetConnections targetConnections,
                         ProxyConfig proxyConfig) {
        this.targetConfiguration = targetConfiguration;
        this.targetConnections = targetConnections;
        this.proxyConfig = proxyConfig;
        this.targetErrorHandler = new TargetErrorHandler(targetConfiguration);
        PassThroughConfiguration conf = PassThroughConfiguration.getInstance();
        this.maxWaitingMessages = conf.getIntProperty(PassThroughConfigPNames.MAX_MESSAGES_PER_HOST_PORT,
                Integer.MAX_VALUE);
    }


    /**
     * This method queues the message for delivery. If a connection is already existing for
     * the destination epr, the message will be delivered immediately. Otherwise message has
     * to wait until a connection is established. In this case this method will inform the
     * system about the need for a connection.
     *
     * @param msgContext the message context to be sent
     * @param epr the endpoint to which the message should be sent
     * @throws AxisFault if an error occurs
     *
     * @return false if connection can not be acquired due to connection limit exceeds or queue limit exceeds
     *         else return true
     */
    public boolean submit(MessageContext msgContext, EndpointReference epr)
            throws AxisFault {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Submitting request for MessageID: " + msgContext.getMessageID());
            }
            URL url = new URL(epr.getAddress());
            String scheme = url.getProtocol() != null ? url.getProtocol() : "http";
            String hostname = url.getHost();
            int port = url.getPort();
            if (port == -1) {
                // use default
                if ("http".equals(scheme)) {
                    port = 80;
                } else if ("https".equals(scheme)) {
                    port = 443;
                }
            }
            HttpHost target = new HttpHost(hostname, port, scheme);
            boolean secure = "https".equalsIgnoreCase(target.getSchemeName());

            HttpHost proxy = proxyConfig.selectProxy(target);
            msgContext.setProperty(PassThroughConstants.PROXY_PROFILE_TARGET_HOST, target.getHostName());

            HttpRoute route;
            if (proxy != null) {
                route = new HttpRoute(target, null, proxy, secure);
            } else {
                route = new HttpRoute(target, null, secure);
            }

            // first we queue the message
            Queue<MessageContext> queue = null;
            NHttpClientConnection conn = null;
            lock.lock();
            try {
                queue = waitingMessages.get(route);
                if (queue == null) {
                    queue = new ConcurrentLinkedQueue<MessageContext>();
                    waitingMessages.put(route, queue);
                }
                if (queue.size() >= maxWaitingMessages) {
                    msgContext.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                            PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                    log.warn("Delivery agent queue length exceeds the maximum number of waiting messages");
                    if (msgContext != null) {
                        targetErrorHandler.handleError(msgContext, ErrorCodes.CONNECTION_TIMEOUT,
                                "Number of queued messages exceeds the limit",
                                null, ProtocolState.REQUEST_READY);
                    }
                    return false;
                }

                queue.add(msgContext);
                conn = targetConnections.getConnection(route, msgContext, targetErrorHandler, queue);
                if (conn == null && msgContext != null && "true".equalsIgnoreCase(
                        (String) msgContext.getProperty(PassThroughConstants.CONNECTION_LIMIT_EXCEEDS))) {
                    msgContext.removeProperty(PassThroughConstants.CONNECTION_LIMIT_EXCEEDS);
                    return false;
                }


            } finally {
                lock.unlock();
            }

            if (conn != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Connection found from pool for MessageID: " + msgContext.getMessageID() +
                            ", conn: " + conn.toString());
                }
            	conn.resetInput();
            	conn.resetOutput();
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    tryNextMessage(messageContext, route, conn);
                }
            }

        } catch (MalformedURLException e) {
            handleException("Malformed URL in the target EPR", e);
        }
        return true;
    }

    public void errorConnecting(HttpRoute route, int errorCode, String message, Exception exceptionToRaise) {
        Queue<MessageContext> queue = waitingMessages.get(route);
        if (queue != null) {
            MessageContext msgCtx = queue.poll();

            if (msgCtx != null) {
                msgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                        PassThroughConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
                targetErrorHandler.handleError(msgCtx, errorCode,
                        "Error connecting to the back end", exceptionToRaise,
                        ProtocolState.REQUEST_READY);
                synchronized (msgCtx) {
                    msgCtx.setProperty(PassThroughConstants.WAIT_BUILDER_IN_STREAM_COMPLETE,
                            Boolean.TRUE);
                    msgCtx.notifyAll();
                }
            }
        } else {
            throw new IllegalStateException("Queue cannot be null for: " + route);
        }
    }

	public void errorConnecting(HttpRoute route, int errorCode, String message) {
		errorConnecting(route, errorCode, message, null);
	}

    /**
     * Notification for a connection availability. When this occurs a message in the
     * queue for delivery will be tried.
     *
     * @param host name of the remote host
     * @param port remote port number
     */
    public void connected(HttpRoute route, NHttpClientConnection conn) {
        if (log.isDebugEnabled()) {
            log.debug("Connection established conn: " + conn.toString());
        }
        Queue<MessageContext> queue = null;
        lock.lock();
        try {
            queue = waitingMessages.get(route);
        } finally {
            lock.unlock();
        }

        while (queue.size() > 0) {
            if(conn == null) {
                conn = targetConnections.getExistingConnection(route);
            }
            if (conn != null) {
                MessageContext messageContext = queue.poll();

                if (messageContext != null) {
                    messageContext.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_CONNECTION, conn);
                    messageContext.setProperty(PassThroughConstants.PASS_THROUGH_TARGET_CONFIGURATION,
                            targetConfiguration);
                    tryNextMessage(messageContext, route, conn);
                    conn = null;
                }
            } else {
                break;
            }
        }
        //releasing the connection to pool when connection is not null and message queue is empty,
        //otherwise connection remains in busyConnections pool till it gets timeout and It is not used.
        if(conn != null && TargetContext.getState(conn) == ProtocolState.REQUEST_READY) {
            targetConfiguration.getConnections().releaseConnection(conn);
        }
    }

    private void tryNextMessage(MessageContext messageContext, HttpRoute route, NHttpClientConnection conn) {
        if (conn != null) {
            try {
                HttpContext ctx = conn.getContext();
                /*
                * If the flow is SSE we need to set references to target connection and targetConnections
                * in the source connection to access them and shut down target connection when source connection is
                * broken.
                * */
                if (SSE.equals(messageContext.getProperty(SYNAPSE_ARTIFACT_TYPE))) {
                    LoggingNHttpServerConnection sourceConnection = (LoggingNHttpServerConnection)
                            ((ServerWorker) messageContext.getProperty(
                            "OutTransportInfo")).getRequestContext().getProperty
                                    (PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                    if (sourceConnection != null) {
                        sourceConnection.getIOSession().setAttribute(SSE_TARGET_CONNECTIONS, targetConnections);
                        sourceConnection.getIOSession().setAttribute(SSE_TARGET_CONNECTION, conn);
                    }
                }
                ctx.setAttribute(CorrelationConstants.CORRELATION_ID,
                                 messageContext.getProperty(CorrelationConstants.CORRELATION_ID));

                ctx.setAttribute(PassThroughConstants.REQUEST_MESSAGE_CONTEXT, messageContext);
                TargetContext.updateState(conn, ProtocolState.REQUEST_READY);
                TargetContext.get(conn).setRequestMsgCtx(messageContext);
                if (log.isDebugEnabled()) {
                    log.debug("Trying to send the message, MessageID:" + messageContext.getMessageID() +
                            ", conn:" + conn.toString());
                }
                submitRequest(conn, route, messageContext);
            } catch (AxisFault e) {
                log.error("IO error while sending the request out", e);
            }
        }
    }

    private void submitRequest(NHttpClientConnection conn, HttpRoute route, MessageContext msgContext) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Submitting new request MessageID:"
                    + msgContext.getMessageID() +" to the connection: " + conn);
        }

        if (SynapseDebugInfoHolder.getInstance().isDebuggerEnabled()) {
            conn.getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY,
                                           msgContext.getProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY));
            conn.getContext().setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_MEDIATOR_ID_PROPERTY,
                                           msgContext.getProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_MEDIATOR_ID_PROPERTY));
        }
        TargetRequest request = TargetRequestFactory.create(msgContext, route, targetConfiguration);
        TargetContext.setRequest(conn, request);

        Pipe pipe = (Pipe) msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null) {
            pipe.attachConsumer(conn);
            request.connect(pipe);
            if (Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
                synchronized (msgContext) {
                    OutputStream out = pipe.getOutputStream();
                    msgContext.setProperty(PassThroughConstants.BUILDER_OUTPUT_STREAM, out);
                    msgContext.setProperty(PassThroughConstants.WAIT_BUILDER_IN_STREAM_COMPLETE, Boolean.TRUE);
                    msgContext.notifyAll();
                }
                return;
            }
        }

        conn.requestOutput();
    }    

    /**
     * Throws an AxisFault if an error occurs at this level
     * @param s a message describing the error
     * @param e original exception leads to the error condition
     * @throws AxisFault wrapping the original exception
     */
    private void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }
}

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
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.transport.base.threads.WorkerPool;
// Axis2 transport tracker imports
import org.apache.axis2.transport.base.tracker.AxisServiceFilter;
import org.apache.axis2.transport.base.tracker.AxisServiceTracker;
import org.apache.axis2.transport.base.tracker.AxisServiceTrackerListener;
// Synapse imports for logging
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
// HttpCore 5 imports
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.impl.io.DefaultBHttpServerConnection;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.impl.io.HttpService;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.support.BasicHttpServerRequestHandler;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.SessionContextUtil;

// for out vt blocking socket handling, we use Java 21+ Virtual Threads and HttpCore 5's blocking I/O APIs
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A fully blocking HTTP transport listener that uses Java 21+ Virtual Threads
 * and Apache HttpCore 5 for HTTP protocol handling.
 */

public class VTPassThroughHttpListener implements TransportListener {

    protected Log log = LogFactory.getLog(this.getClass());

    /** Server socket accepting inbound connections */
    private ServerSocket serverSocket;

    /** Virtual-thread executor for handling accepted connections */
    private ExecutorService vtExecutor;

    /** Limits concurrently accepted virtual-thread connections */
    private Semaphore connectionSemaphore;

    /** Flag to signal the accept-loop to stop */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** The accept-loop thread (a platform thread) */
    private Thread acceptThread;

    /** HttpCore 5 blocking HTTP service — handles protocol processing */
    private HttpService httpService;

    /** SourceConfiguration (reused from existing PT transport)  */
    private SourceConfiguration sourceConfiguration;

    /** Protocol scheme */
    private Scheme scheme;

    /** Transport name prefix */
    private String namePrefix;

    /** Operating port */
    private int operatingPort;

    /** Axis2 ConfigurationContext */
    private ConfigurationContext configurationContext;

    /** Transport In Description */
    private TransportInDescription transportInDescription;

    /** Service URI maps */
    private Map<String, String> serviceNameToEPRMap = new HashMap<>();
    private Map<String, String> eprToServiceNameMap = new HashMap<>();

    /** Service tracker */
    private AxisServiceTracker serviceTracker;

    /** Listener state  prevent invalid execution  by having shared state*/
    private volatile int state = BaseConstants.STOPPED;

    /** Socket configuration */
    private int soTimeout = VTConstants.DEFAULT_SO_TIMEOUT;
    private int backlog = VTConstants.DEFAULT_BACKLOG;
    private boolean tcpNoDelay = VTConstants.DEFAULT_TCP_NODELAY;

    
    protected Scheme initScheme() {
        return new Scheme("http", 80, false);
    }

    /**
     * Create the ServerSocket.
     */
    protected ServerSocket createServerSocket(InetSocketAddress bindAddress) throws IOException {
        ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket();
        ss.setReuseAddress(true);
        ss.bind(bindAddress, backlog);
        return ss;
    }

    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportIn) throws AxisFault {
        log.info("Initializing Virtual Thread Pass-through HTTP/S Listener (HttpCore 5)...");

        this.configurationContext = cfgCtx;
        this.transportInDescription = transportIn;
        this.namePrefix = transportIn.getName().toUpperCase(Locale.US);
        this.scheme = initScheme();

        // Compute operating port with offset
        int portOffset = Integer.parseInt(System.getProperty("portOffset", "0"));
        Parameter portParam = transportIn.getParameter(VTConstants.PARAM_PORT);
        int port = Integer.parseInt(portParam.getValue().toString());
        operatingPort = port + portOffset;
        portParam.setValue(String.valueOf(operatingPort));
        portParam.getParameterElement().setText(String.valueOf(operatingPort));
        
        System.setProperty(transportInDescription.getName() + ".VT.port", String.valueOf(operatingPort));
        // Read optional parameters
        //socket timout
        Parameter soTimeoutParam = transportIn.getParameter(VTConstants.PARAM_SO_TIMEOUT);
        if (soTimeoutParam != null) {
            soTimeout = Integer.parseInt(soTimeoutParam.getValue().toString());
        }
        //tcp connection that can be queued unitl accept
        Parameter backlogParam = transportIn.getParameter(VTConstants.PARAM_BACKLOG);
        if (backlogParam != null) {
            backlog = Integer.parseInt(backlogParam.getValue().toString());
        }
        // disables nagles algorithm, which can reduce latency for small messages at the cost of potentially more packets
        Parameter tcpNoDelayParam = transportIn.getParameter(VTConstants.PARAM_TCP_NODELAY);
        if (tcpNoDelayParam != null) {
            tcpNoDelay = Boolean.parseBoolean(tcpNoDelayParam.getValue().toString());
        }

        int maxConnections = VTConstants.getSystemInt(
                VTConstants.VT_MAX_ACCEPT_CONNECTIONS,
                VTConstants.DEFAULT_VT_MAX_ACCEPT_CONNECTIONS);
        connectionSemaphore = new Semaphore(maxConnections);

        // Create the shared virtual-thread executor
        vtExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name(VTConstants.VT_THREAD_PREFIX + namePrefix.toLowerCase() + "-", 0)
                        .factory()
        );

        // Wrap the VT executor as an Axis2 WorkerPool
        WorkerPool workerPool;
        Object obj = cfgCtx.getProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        if (obj != null) {
            workerPool = (WorkerPool) obj;
            log.info("Reusing existing shared WorkerPool for " + namePrefix);
        } else {
            workerPool = new VirtualThreadWorkerPool(vtExecutor);
            log.info("Created Virtual Thread WorkerPool (shared executor) for " + namePrefix);
        }

        PassThroughTransportMetricsCollector metrics =
                new PassThroughTransportMetricsCollector(true, scheme.getName());

        sourceConfiguration = new SourceConfiguration(cfgCtx, transportIn, scheme, workerPool, metrics);
        sourceConfiguration.build();

        // EPR map
        @SuppressWarnings("unchecked")
        Map<String, String> existingMap = (Map<String, String>) cfgCtx.getProperty(
                PassThroughConstants.EPR_TO_SERVICE_NAME_MAP);
        if (existingMap != null) {
            this.eprToServiceNameMap = existingMap;
        } else {
            cfgCtx.setProperty(PassThroughConstants.EPR_TO_SERVICE_NAME_MAP, eprToServiceNameMap);
        }

        cfgCtx.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                sourceConfiguration.getWorkerPool());

        // ---- Build HttpCore 5 HTTP service ----
        HttpRequestHandler requestHandler = (ClassicHttpRequest request,
                                             ClassicHttpResponse response,
                                             HttpContext context) -> {
            VTBlockingServerWorker worker = new VTBlockingServerWorker(
                    request, response, context,
                    sourceConfiguration, configurationContext);
            worker.process();
        };

        // Wrap as HttpRequestMapper that routes all requests to our handler
        org.apache.hc.core5.http.HttpRequestMapper<HttpRequestHandler> mapper =
                (req, ctx) -> requestHandler;

        httpService = new HttpService(
                HttpProcessors.server(),
                new BasicHttpServerRequestHandler(mapper));

        // Service tracker
        serviceTracker = new AxisServiceTracker(
                cfgCtx.getAxisConfiguration(),
                new AxisServiceFilter() {
                    @Override
                    public boolean matches(AxisService service) {
                        return !service.getName().startsWith("__")
                                && BaseUtils.isUsingTransport(service,
                                transportInDescription.getName());
                    }
                },
                new AxisServiceTrackerListener() {
                    @Override
                    public void serviceAdded(AxisService service) {
                        addToServiceURIMap(service);
                    }

                    @Override
                    public void serviceRemoved(AxisService service) {
                        removeServiceFromURIMap(service);
                    }
                });
    }

    @Override
    public void start() throws AxisFault {
        serviceTracker.start();
        log.info("Starting Virtual Thread Pass-through " + namePrefix
                + " Listener (HttpCore 5) on port " + operatingPort);

        // Determine bind address
        InetSocketAddress bindAddress;
        Parameter bindParam = transportInDescription.getParameter(VTConstants.PARAM_BIND_ADDRESS);
        if (bindParam != null) {
            try {
                InetAddress addr = InetAddress.getByName((String) bindParam.getValue());
                bindAddress = new InetSocketAddress(addr, operatingPort);
            } catch (UnknownHostException e) {
                throw new AxisFault("Invalid bind address", e);
            }
        } else {
            bindAddress = new InetSocketAddress(operatingPort);
        }

        try {
            serverSocket = createServerSocket(bindAddress);
        } catch (IOException e) {
            throw new AxisFault("Failed to bind server socket on " + bindAddress, e);
        }

        // flag to control the accept loop and state
        running.set(true);
        
        state = BaseConstants.STARTED;

        // Accept loop runs on a platform thread
        acceptThread = new Thread(this::acceptLoop,
                VTConstants.VT_THREAD_PREFIX + namePrefix + "-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        log.info("Virtual Thread Pass-through " + namePrefix
                + " Listener started on port " + operatingPort);
    }

    /**
     * Main accept loop. Runs on a single platform thread.
     * For each accepted socket, dispatches a connection handler to the VT
     * executor.  HttpCore 5 handles HTTP parsing, keep-alive, chunked
     * encoding, and response serialization within each VT.
     */
    private void acceptLoop() {
        while (running.get()) {
            Socket clientSocket = null;
            boolean permitAcquired = false;
            try {
                connectionSemaphore.acquire();
                permitAcquired = true;

                clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(soTimeout);
                clientSocket.setTcpNoDelay(tcpNoDelay);

                Socket acceptedSocket = clientSocket;
                vtExecutor.submit(() -> {
                    try {
                        handleConnection(acceptedSocket);
                    } finally {
                        connectionSemaphore.release();
                    }
                });
            } catch (IOException e) {
                if (permitAcquired) {
                    connectionSemaphore.release();
                }
                closeClientSocket(clientSocket);
                if (running.get()) {
                    log.error("Error accepting connection on " + namePrefix + " listener", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (permitAcquired) {
                    connectionSemaphore.release();
                }
                closeClientSocket(clientSocket);
                if (running.get()) {
                    log.error("Interrupted while waiting for connection permit on " + namePrefix + " listener", e);
                }
                break;
            } catch (RejectedExecutionException e) {
                if (permitAcquired) {
                    connectionSemaphore.release();
                }
                closeClientSocket(clientSocket);
                if (running.get()) {
                    log.error("Virtual-thread executor rejected connection on " + namePrefix + " listener", e);
                }
            }
        }
    }

    private void closeClientSocket(Socket clientSocket) {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Handle a single TCP connection using HttpCore 5's blocking HTTP service.
     */
    private void handleConnection(Socket clientSocket) {
        DefaultBHttpServerConnection conn = null;
        try {
            // config to define stream buffer size 
            Http1Config h1Config = Http1Config.custom()
                    .setBufferSize(VTConstants.STREAM_BUFFER_SIZE)
                    .build();

            conn = new DefaultBHttpServerConnection(scheme.getName(), h1Config);
            conn.bind(clientSocket);

            // Store socket addresses in context for the handler
            //Synapse sets this in MessageContext, but http core 5 didn't expose it to handlers, so we set it in the HttpContext here.
            BasicHttpContext context = new BasicHttpContext();
            SocketAddress remoteAddr = clientSocket.getRemoteSocketAddress();
            SocketAddress localAddr = clientSocket.getLocalSocketAddress();
            context.setAttribute(VTConstants.CTX_REMOTE_ADDRESS, remoteAddr);
            context.setAttribute(VTConstants.CTX_LOCAL_ADDRESS, localAddr);
            context.setAttribute(VTConstants.CTX_LOCAL_PORT, clientSocket.getLocalPort());

            while (conn.isOpen()) {
                //handleRequest handle ont request-response cycle then returns, after it blocks until next request in socket.
                httpService.handleRequest(conn, context);
            }
        } catch (ConnectionClosedException e) {
            // Normal — client closed connection
            if (log.isDebugEnabled()) {
                log.debug("Client closed connection: "
                        + clientSocket.getRemoteSocketAddress());
            }
        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Connection ended: "
                        + clientSocket.getRemoteSocketAddress()
                        + " - " + e.getMessage());
            }
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (IOException ignore) { }
            }
            try {
                if (!clientSocket.isClosed()) { clientSocket.close(); }
            } catch (IOException ignore) { }
        }
    }

    @Override
    public void stop() throws AxisFault {
        if (state == BaseConstants.STOPPED) return;
        log.info("Stopping Virtual Thread Pass-through " + namePrefix + " Listener...");
        running.set(false);
        state = BaseConstants.STOPPED;

        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server socket", e);
        }
        if (vtExecutor != null) vtExecutor.shutdown();
        if (acceptThread != null) {
            try { acceptThread.join(5000); }
            catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
        }
        serviceTracker.stop();
        log.info("Virtual Thread Pass-through " + namePrefix + " Listener stopped.");
    }

    @Override
    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        String trailer = "";
        if (serviceName.indexOf('/') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("/"));
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        if (serviceName.indexOf('.') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("."));
            serviceName = serviceName.substring(0, serviceName.indexOf('.'));
        }
        if (serviceNameToEPRMap.containsKey(serviceName)) {
            return new EndpointReference(
                    sourceConfiguration.getCustomEPRPrefix()
                            + serviceNameToEPRMap.get(serviceName) + trailer);
        } else {
            return new EndpointReference(
                    sourceConfiguration.getServiceEPRPrefix() + serviceName + trailer);
        }
    }

    @Override
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        return new EndpointReference[] { getEPRForService(serviceName, ip) };
    }

    @Override
    public SessionContext getSessionContext(MessageContext messageContext) {
        return SessionContextUtil.createSessionContext(messageContext);
    }

    @Override
    public void destroy() {
        log.info("Destroying VTPassThroughHttpListener");
        sourceConfiguration.getMetrics().destroy();
    }

    // ---- Service URI mapping ----

    private void addToServiceURIMap(AxisService service) {
        Parameter param = service.getParameter(PassThroughConstants.SERVICE_URI_LOCATION);
        if (param != null) {
            String uriLocation = param.getValue().toString();
            if (uriLocation.startsWith("/")) {
                uriLocation = uriLocation.substring(1);
            }
            serviceNameToEPRMap.put(service.getName(), uriLocation);
            eprToServiceNameMap.put(uriLocation, service.getName());
        }
    }

    private void removeServiceFromURIMap(AxisService service) {
        eprToServiceNameMap.remove(serviceNameToEPRMap.get(service.getName()));
        serviceNameToEPRMap.remove(service.getName());
    }

    // ---- Accessors ----

    public SourceConfiguration getSourceConfiguration() { return sourceConfiguration; }
    public int getOperatingPort() { return operatingPort; }
    public int getState() { return state; }
    public ConfigurationContext getConfigurationContext() { return configurationContext; }
    public ExecutorService getVtExecutor() { return vtExecutor; }
}

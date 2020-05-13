/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.apache.synapse.transport.passthru.core;

import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.log4j.Logger;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.ServerIODispatch;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.core.ssl.MultiListenerSSLServerIODispatch;
import org.apache.synapse.transport.passthru.core.ssl.SSLConfiguration;
import org.apache.synapse.transport.passthru.core.ssl.SSLConnectionUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PassThroughListeningIOReactorManager {

    private static final Logger log = Logger.getLogger(PassThroughListeningIOReactorManager.class);

    /**
     * Singleton Object of class PassThroughListeningIOReactorManager
     */
    private static PassThroughListeningIOReactorManager passThroughListeningIOReactorManager = new PassThroughListeningIOReactorManager();

    /**
     * Reference for shared ListeningIOReactor which can use by external endpoints
     */
    private ListeningIOReactor sharedListeningIOReactor;

    /**
     * Reference for shared SSL ListeningIOReactor which can be used by external SSL configured endpoints
     */
    private ListeningIOReactor sharedSSLListeningIOReactor;

    /**
     * Configuration used in shared IO Reactor
     */
    private PassThroughSharedListenerConfiguration passThroughListenerConfiguration;

    /**
     * Configuration used in shared SSL IO Reactor
     */
    private  PassThroughSharedListenerConfiguration sslPassThroughListenerConfiguration;

    /**
     * Map keeps EventHandler and port mapping
     */
    private Map<Integer, NHttpServerEventHandler> portServerHandlerMapper;

    /**
     * Map keeps ListeningEndpoint and external port mapping
     */
    private Map<Integer, ListenerEndpoint> dynamicPTTListeningEndpointMapper;

    /**
     * Keep map between PTT Listeners and IOReactors.
     */
    private Map<Integer, ListeningIOReactor> passThroughListenerIOReactorMapper;

    /**
     * Keep map between ServerIODispatch and PTT Listeners
     */
    private Map<Integer, ServerIODispatch> passThroughListenerServerIODispatchMapper;

    /**
     * keep map between port , tenant and serverConnectionFactory used in Inbound Endpoints with SSL
     */
    private Map<Integer, ServerConnFactory> serverConnectionFactoryMapper;

    private AtomicBoolean isSharedIOReactorInitiated;
    private IOReactorSharingMode ioReactorSharingMode;

    private AtomicBoolean isSharedSSLIOReactorInitiated;

    /**
     * Default value for verification timeout to validate whether port is closed successfully.
     */
    private static final int DEFAULT_PORT_CLOSE_VERIFY_TIMEOUT = 10;

    private PassThroughListeningIOReactorManager() {
        portServerHandlerMapper = new ConcurrentHashMap<Integer, NHttpServerEventHandler>();
        dynamicPTTListeningEndpointMapper = new ConcurrentHashMap<Integer, ListenerEndpoint>();
        passThroughListenerIOReactorMapper = new ConcurrentHashMap<Integer, ListeningIOReactor>();
        passThroughListenerServerIODispatchMapper = new ConcurrentHashMap<Integer, ServerIODispatch>();
        serverConnectionFactoryMapper = new ConcurrentHashMap<Integer, ServerConnFactory>();

        isSharedIOReactorInitiated = new AtomicBoolean(false);
        isSharedSSLIOReactorInitiated = new AtomicBoolean(false);
        ioReactorSharingMode = PassThroughConfiguration.getInstance().isListeningIOReactorShared() ?
                               IOReactorSharingMode.SHARED : IOReactorSharingMode.UNSHARED;
    }

    /**
     * Provide manager object for internal services
     *
     * @return PassThroughIOReactorManager
     */
    public static PassThroughListeningIOReactorManager getInstance() {
        return passThroughListeningIOReactorManager;
    }

    /**
     * Start PTT Endpoint which is given by axis2.xml
     *
     * @param inetSocketAddress         Socket Address of starting endpoint
     * @param defaultListeningIOReactor IO Reactor which  starts Endpoint
     * @param namePrefix                name specified for endpoint
     * @return Is started
     */
    public boolean startPTTEndpoint(InetSocketAddress inetSocketAddress,
                                    DefaultListeningIOReactor defaultListeningIOReactor, String namePrefix) {
        try {
            return startEndpoint(inetSocketAddress, defaultListeningIOReactor, namePrefix) != null;
        } catch (Exception e) {
            log.error("Cannot Start PassThroughListeningEndpoint for port " + inetSocketAddress.getPort(), e);
            return false;
        }
    }

    /**
     * Start Endpoint in IOReactor which is external to PTT Axis2 Listeners started at server startup
     *
     * @param inetSocketAddress       Socket Address of starting endpoint
     * @param nHttpServerEventHandler ServerHandler responsible for handle events of port
     * @param endpointName            Endpoint Name
     * @return Is Endpoint started
     */
    public boolean startDynamicPTTEndpoint(InetSocketAddress inetSocketAddress,
                                           NHttpServerEventHandler nHttpServerEventHandler, String endpointName) {
        try {
            // get Shared IO Reactor and Start Endpoint
            ListenerEndpoint endpoint = startEndpoint(inetSocketAddress,
                                                      getSharedIOReactor(nHttpServerEventHandler, endpointName), endpointName);
            if (endpoint != null) {
                portServerHandlerMapper.put(inetSocketAddress.getPort(), nHttpServerEventHandler);
                dynamicPTTListeningEndpointMapper.put(inetSocketAddress.getPort(), endpoint);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("Cannot Start Endpoint for " + endpointName, e);
            return false;
        }
    }

    /**
     * Start SSL endpoint in IO reactor which is external to PTT Axis Listeners started at server startup
     * @param inetSocketAddress InetSocketAddress
     * @param nHttpServerEventHandler  ServerHandler responsible for handle events of port
     * @param endpointName   Endpoint Name
     * @param sslConfiguration SSL information for create secure connection
     * @return
     */
    public boolean startDynamicPTTSSLEndpoint(InetSocketAddress inetSocketAddress,
                                              NHttpServerEventHandler nHttpServerEventHandler, String endpointName,
                                              SSLConfiguration sslConfiguration) {
        try {
            // get Shared IO Reactor and Start Endpoint
            ListenerEndpoint endpoint = startEndpoint(inetSocketAddress,
                                                      getSharedSSLIOReactor(nHttpServerEventHandler, endpointName,
                                                                             inetSocketAddress.getPort(),
                                                                            sslConfiguration), endpointName);
            if (endpoint != null) {
                portServerHandlerMapper.put(inetSocketAddress.getPort(), nHttpServerEventHandler);
                dynamicPTTListeningEndpointMapper.put(inetSocketAddress.getPort(), endpoint);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("Cannot Start Endpoint for " + endpointName, e);
            return false;
        }
    }

    /**
     * Create IOReactor with given configuration
     *
     * @param port                                   Port of the Endpoint for axis2 Listener
     * @param nHttpServerEventHandler                Server Handler responsible for handle events of port
     * @param passThroughSharedListenerConfiguration configuration related to create and start IOReactor
     * @return IOReactor
     */
    public ListeningIOReactor initIOReactor(int port, NHttpServerEventHandler nHttpServerEventHandler,
                                            PassThroughSharedListenerConfiguration passThroughSharedListenerConfiguration)
            throws IOReactorException {
        ListeningIOReactor defaultListeningIOReactor;
        try {
            ServerIODispatch serverIODispatch;

            // PassThroughListenerConfiguration to be used by Shared IO Reactor
            synchronized (this) {
                if (this.passThroughListenerConfiguration == null &&
                    !passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                    this.passThroughListenerConfiguration = passThroughSharedListenerConfiguration;
                }

                if (this.sslPassThroughListenerConfiguration == null &&
                    passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                    this.sslPassThroughListenerConfiguration = passThroughSharedListenerConfiguration;
                }
            }

            //If IOReactor is in shared mode and if it is  not initialized, initialize it for HTTP Protocol
            if (ioReactorSharingMode == IOReactorSharingMode.SHARED && !isSharedIOReactorInitiated.get()
                && !passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                synchronized (this) {
                    portServerHandlerMapper.put(port, nHttpServerEventHandler);
                    serverIODispatch = new MultiListenerServerIODispatch
                            (portServerHandlerMapper, nHttpServerEventHandler,
                             passThroughSharedListenerConfiguration.getServerConnFactory());
                    // Create IOReactor for Listener make it shareable with Inbounds
                    defaultListeningIOReactor = createListeningIOReactor(passThroughSharedListenerConfiguration);
                    log.info("IO Reactor for port " + port + " initiated on shared mode which will be used by non axis2 " +
                             "Transport Listeners ");
                    sharedListeningIOReactor = defaultListeningIOReactor;
                    isSharedIOReactorInitiated.compareAndSet(false, true);
                }

            } else if(ioReactorSharingMode == IOReactorSharingMode.SHARED && !isSharedSSLIOReactorInitiated.get()
                      && passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                synchronized (this) {
                    serverConnectionFactoryMapper.put(port,passThroughSharedListenerConfiguration.getServerConnFactory());
                    portServerHandlerMapper.put(port, nHttpServerEventHandler);
                    serverIODispatch = new MultiListenerSSLServerIODispatch(
                               portServerHandlerMapper , nHttpServerEventHandler, serverConnectionFactoryMapper);
                    // Create IOReactor for Listener make it shareable with Inbounds
                    defaultListeningIOReactor = createListeningIOReactor(passThroughSharedListenerConfiguration);
                    log.info("IO Reactor for port " + port + " initiated on shared mode which will be used by non axis2 " +
                             "Transport SSL Listeners ");
                    sharedSSLListeningIOReactor = defaultListeningIOReactor;
                    isSharedSSLIOReactorInitiated.compareAndSet(false, true);
                  }
                }else {
                // Create un shareable IOReactors for axis2 Listeners and assign IOReactor Config for later
                // create IOReactor for Inbounds
                serverIODispatch = new ServerIODispatch(nHttpServerEventHandler,
                                                        passThroughSharedListenerConfiguration.getServerConnFactory());
                defaultListeningIOReactor = createListeningIOReactor(passThroughSharedListenerConfiguration);

            }
            passThroughListenerServerIODispatchMapper.put(port, serverIODispatch);
            passThroughListenerIOReactorMapper.put(port, defaultListeningIOReactor);
        } catch (IOReactorException e) {
            throw new IOReactorException("Error occurred when trying to init IO Reactor", e);
        }
        return defaultListeningIOReactor;
    }

    /**
     * Close external endpoints listen in shared IO Reactor
     *
     * @param port Port of the endpoint need to close
     * @return Is endpoint closed
     */
    public boolean closeDynamicPTTEndpoint(int port) {
        int portCloseVerifyTimeout =
                System.getProperty(PassThroughConstants.SYSTEMPROP_PORT_CLOSE_VERIFY_TIMEOUT) == null ?
                        DEFAULT_PORT_CLOSE_VERIFY_TIMEOUT :
                        Integer.parseInt(
                                System.getProperty(PassThroughConstants.SYSTEMPROP_PORT_CLOSE_VERIFY_TIMEOUT));
        try {
            log.info("Closing Endpoint Listener for port "+port);
            dynamicPTTListeningEndpointMapper.get(port).close();
        } catch (Exception e) {
            log.error("Cannot close  Endpoint relevant to port " + port, e);
            return false;
        } finally {
            if(serverConnectionFactoryMapper.containsKey(port)){
                serverConnectionFactoryMapper.remove(port);
            }

            /*
             * validate whether port is closed successfully. Here we wait till port is successfully get closed
             * iteratively trying to bind a ServerSocket to the relevant port. As default time check for 10s and log
             * warning and move on.
             *
             * We need to do this since even though we close the
             * org.apache.http.nio.reactor.ListenerEndpoint above, on some operating systems it takes time to release
             * the port. And at redeployment of a inbound endpoint redeployment happens immediately and in some environments
             * redeployment fails due to this issue.
             *
             * If 10s timeout is not enough (depends on the environment) the timeout can be tuned using
             * "synapse.transport.portCloseVerifyTimeout" system property
             */
            if (isPortCloseSuccess(port, portCloseVerifyTimeout)) {
                log.info("Successfully closed Endpoint Listener for port " + port);
            } else {
                log.warn("Port close verify timeout " + portCloseVerifyTimeout
                        + "s exceeded. Endpoint Listener for port " + port + " still bound to the ListenerEndpoint.");
            }
            dynamicPTTListeningEndpointMapper.remove(port);
        }
        return true;
    }

    /**
     * Close all endpoints started by PTT Listeners.
     *
     * @param port Port of the Endpoint for PTT axis2 Listener
     * @return is all Endpoints closed
     */
    public boolean closeAllPTTListenerEndpoints(int port) {
        try {
            ListeningIOReactor listeningIOReactor = passThroughListenerIOReactorMapper.get(port);
            if (listeningIOReactor != null) {
                Set<ListenerEndpoint> endpoints = listeningIOReactor.getEndpoints();
                // If it is shared IO Reactor then only close endpoints related to PTT Listener
                if (passThroughListenerServerIODispatchMapper.get(port) instanceof MultiListenerServerIODispatch) {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        if (listenerEndpoint.getAddress() instanceof InetSocketAddress) {
                            int endPointPort = ((InetSocketAddress) listenerEndpoint.getAddress()).getPort();
                            if (dynamicPTTListeningEndpointMapper.containsKey(endPointPort)) {
                                continue;
                            }
                            log.info("Closing Endpoint Listener for port "+port);
                            listenerEndpoint.close();
                            log.info("Successfully closed Endpoint Listener for port "+port);
                        }
                    }
                } else {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        log.info("Closing Endpoint Listener for port "+port);
                        listenerEndpoint.close();
                        log.info("Successfully closed Endpoint Listener for port "+port);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error occurred when closing Endpoint in PassThrough Transport Related to port " + port, e);
            return false;
        }
    }

    /**
     * Close specific endpoints started by PTT Listeners using give set of bind addresses.
     *
     * @param port          Port of the listener
     * @param bindAddresses bind address list of endpoints to be closed
     * @return true if successfully closed, false if any error
     */
    public boolean closeSpecificPTTListenerEndpoints(int port,
                                                     Set<InetSocketAddress> bindAddresses) {
        try {
            ListeningIOReactor listeningIOReactor = passThroughListenerIOReactorMapper.get(port);
            if (listeningIOReactor != null) {
                Set<ListenerEndpoint> endpoints = listeningIOReactor.getEndpoints();
                // If it is shared IO Reactor then only close endpoints related to PTT Listener
                if (passThroughListenerServerIODispatchMapper.get(port) instanceof MultiListenerServerIODispatch) {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        if (listenerEndpoint.getAddress() instanceof InetSocketAddress) {
                            int endPointPort = ((InetSocketAddress) listenerEndpoint.getAddress()).getPort();
                            if (dynamicPTTListeningEndpointMapper.containsKey(endPointPort)) {
                                continue;
                            }

                            for (InetSocketAddress inetSocketAddress : bindAddresses) {
                                if (inetSocketAddress.getHostName().equalsIgnoreCase(((InetSocketAddress) listenerEndpoint.getAddress()).getHostName())) {
                                    listenerEndpoint.close();
                                }
                            }
                        }
                    }
                } else {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        for (InetSocketAddress inetSocketAddress : bindAddresses) {
                            if (inetSocketAddress.getHostName().equalsIgnoreCase(((InetSocketAddress) listenerEndpoint.getAddress()).getHostName())) {
                                listenerEndpoint.close();
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Error occurred when closing Endpoint in PassThrough Transport Related to port " + port, e);
            return false;
        }
    }


    /**
     * Return ServerIODispatch registered under given port
     *
     * @param port Port of  axis2 PTT Listener
     * @return ServerIODispatch
     */
    public ServerIODispatch getServerIODispatch(int port) {
            return passThroughListenerServerIODispatchMapper.get(port);
    }

    /**
     * @return source configuration used by shared IO Reactor
     */
    public SourceConfiguration getSharedPassThroughSourceConfiguration() {
        if (passThroughListenerConfiguration != null) {
            return passThroughListenerConfiguration.getSourceConfiguration();
        }
        return null;
    }

    /**
     * @return source configuration used by shared SSL IO Reactor
     */
    public SourceConfiguration getSharedSSLPassThroughSourceConfiguration() {
        if (sslPassThroughListenerConfiguration != null) {
            return sslPassThroughListenerConfiguration.getSourceConfiguration();
        }
        return null;
    }

    /**
     * ShutdownIOReactor which is registered by HTTPListener running on given port
     *
     * @param port                Port of  axis2 PTT Listener
     * @param sourceConfiguration Source Configuration from Source handler
     * @param timeout             Maximum timeout value for shutdown
     * @throws IOException Exception throwing when Shutdown
     */
    public void shutdownIOReactor(int port, SourceConfiguration sourceConfiguration, long timeout)
            throws IOException, InterruptedException {
        if (sourceConfiguration.getMetrics().getUnServedRequestCount() > 0) {
            log.info("Waiting to cleanup active connections on port " + port + ": " +
                    sourceConfiguration.getMetrics().getUnServedRequestCount());
        }
        long startTime = System.currentTimeMillis();
        long timeoutTime = startTime + timeout;
        while (sourceConfiguration.getMetrics().getUnServedRequestCount() > 0) {
            Thread.sleep(1000);
            if (System.currentTimeMillis() > timeoutTime) {
                log.info("Shutting down listener since Socket Timeout exceeded");
                break;
            }
        }
        shutdownIOReactor(port);
    }

    /**
     * ShutdownIOReactor which is registered by HTTPListener running on given port
     *
     * @param port Port of axis2 PTT Listener
     * @throws IOException Exception throwing when Shutdown
     */
    public void shutdownIOReactor(int port) throws IOException {
        ListeningIOReactor ioReactor = shutdownReactor(port);
        if (ioReactor != null) {
            try {
                ioReactor.shutdown();
            } catch (IOException e) {
                throw new IOException(
                        "IOException occurred when shutting down IOReactor for Listener started on port " + port, e);
            }
            passThroughListenerIOReactorMapper.remove(port);
            passThroughListenerServerIODispatchMapper.remove(port);

        }
    }

    /**
     * ShutdownIOReactor which is registered by HTTPListener running on given port
     *
     * @param port        Port of  axis2 PTT Listener
     * @param miliSeconds Waiting Time before close IO Reactor
     * @throws IOException Exception throwing when Shutdown
     */
    public void shutdownIOReactor(int port, long miliSeconds) throws IOException {
        ListeningIOReactor ioReactor = shutdownReactor(port);
        if (ioReactor != null) {
            try {
                ioReactor.shutdown(miliSeconds);
            } catch (IOException e) {
                throw new IOException(
                        "IOException occurred when shutting down IOReactor for Listener started on port " + port, e);
            }
            passThroughListenerIOReactorMapper.remove(port);
            passThroughListenerServerIODispatchMapper.remove(port);
        }
    }

    /**
     * Pause IO Reactor which is registered by HTTPListener running on given port
     *
     * @param port Port of  axis2 PTT Listener
     * @throws IOException Exception throwing when pausing
     */
    public void pauseIOReactor(int port) throws IOException {
        ListeningIOReactor listeningIOReactor = passThroughListenerIOReactorMapper.get(port);
        ServerIODispatch serverIODispatch = passThroughListenerServerIODispatchMapper.get(port);
        if (listeningIOReactor != null) {
            if (serverIODispatch instanceof MultiListenerServerIODispatch ||
                serverIODispatch instanceof  MultiListenerSSLServerIODispatch) {
                log.info("Pausing shared IO Reactor bind for port " + port + " will be caused for pausing non " +
                         "axis2 Listeners ");
            } else {
                log.info("Pausing  IO Reactor bind for port " + port);
            }
            listeningIOReactor.pause();
        } else {
            log.error("Cannot find Pass Through Listener for port " + port);
        }
    }

    /**
     * Resume IO Reactor which is registered by HTTPListener running on given port
     *
     * @param port Port of  axis2 PTT Listener
     * @throws IOException Exception throwing when pausing
     */
    public void resumeIOReactor(int port) throws IOException {
        ListeningIOReactor listeningIOReactor = passThroughListenerIOReactorMapper.get(port);
        if (listeningIOReactor != null) {
            listeningIOReactor.resume();
        } else {
            log.error("Cannot find Pass Through Listener for port " + port);
        }
    }

    /**
     * StartIOReactor with given ServerIODispatch
     *
     * @param listeningIOReactor Listening IO Reactor to be start
     * @param serverIODispatch   underlying Event Dispatcher for Reactor
     * @param prefix             HTTP/HTTPS
     */
    public void startIOReactor(final ListeningIOReactor listeningIOReactor, final ServerIODispatch serverIODispatch,
                               final String prefix) {
        Thread reactorThread = new Thread(new Runnable() {
            public void run() {
                try {
                    listeningIOReactor.execute(serverIODispatch);
                } catch (Exception e) {
                    log.fatal("Exception encountered in the " + prefix + " Listener. " +
                              "No more connections will be accepted by this transport", e);
                } finally {
                    log.info(prefix + " Listener shutdown.");
                    if (serverIODispatch instanceof MultiListenerServerIODispatch) {
                        log.info("Shutting down shared IO Reactor");
                    }
                }

            }
        }, "PassThrough " + prefix + " Listener");
        reactorThread.start();
    }

    private ListeningIOReactor getSharedIOReactor(NHttpServerEventHandler nHttpServerEventHandler,
                                                  String endpointName) throws Exception {

        if (!isSharedIOReactorInitiated.get()) {

            if (passThroughListenerConfiguration != null) {
                //create separate IO Reactor for external non axis2 transports and share among them

                try {
                    synchronized (this) {
                        sharedListeningIOReactor = createListeningIOReactor(passThroughListenerConfiguration);
                        ServerIODispatch serverIODispatch = new MultiListenerServerIODispatch(portServerHandlerMapper,
                                                                                              nHttpServerEventHandler,
                                                                                              passThroughListenerConfiguration.getServerConnFactory());
                        startIOReactor(sharedListeningIOReactor, serverIODispatch, "HTTP");
                        isSharedIOReactorInitiated.compareAndSet(false, true);
                    }

                } catch (IOReactorException e) {
                    throw new IOReactorException("Error occurred when creating shared IO Reactor for non axis2 Listener "
                                                 + endpointName, e);
                }
            } else {
                throw new Exception("Cannot start Endpoint for" + endpointName +
                                    "Axis2 Transport Listeners for PassThrough transport" + " not started correctly or not created the " +
                                    "IOReactor Configuration");
            }
        }
        return sharedListeningIOReactor;
    }

    private ListeningIOReactor getSharedSSLIOReactor(NHttpServerEventHandler nHttpServerEventHandler,
                                                     String endpointName, int port,
                                                     SSLConfiguration sslConfiguration) throws Exception {

        if (!isSharedSSLIOReactorInitiated.get()) {

            if (sslPassThroughListenerConfiguration != null) {
                //create separate IO Reactor for external non axis2 transports and share among them

                try {
                    synchronized (this) {
                        sharedSSLListeningIOReactor = createListeningIOReactor(sslPassThroughListenerConfiguration);
                        ServerIODispatch serverIODispatch = new MultiListenerSSLServerIODispatch(portServerHandlerMapper,
                                                                                                 nHttpServerEventHandler,
                                                                                                 serverConnectionFactoryMapper);
                        startIOReactor(sharedSSLListeningIOReactor, serverIODispatch, "HTTPS");
                        isSharedSSLIOReactorInitiated.compareAndSet(false, true);
                    }

                } catch (IOReactorException e) {
                    throw new IOReactorException("Error occurred when creating shared IO Reactor for non axis2 Listener "
                                                 + endpointName, e);
                }
            } else {
                throw new Exception("Cannot start Endpoint for" + endpointName +
                                    "Axis2 SSL Transport Listeners for PassThrough transport" + " not started correctly or not created the " +
                                    "IOReactor Configuration");
            }
        }
        ServerConnFactory serverConnFactory = SSLConnectionUtils.
                   getServerConnectionFactory(endpointName, sslPassThroughListenerConfiguration,
                                              sslConfiguration);
        if (serverConnectionFactoryMapper.get(port) != null) {
            throw new Exception("Cannot create ServerConnectionFactory for " + endpointName + "in port " + port +
                                "already registered a server connection factory ");
        } else {
            serverConnectionFactoryMapper.put(port, serverConnFactory);
        }
        return sharedSSLListeningIOReactor;
    }


    private ListeningIOReactor createListeningIOReactor(
            PassThroughSharedListenerConfiguration passThroughSharedListenerConfiguration) throws IOReactorException {
        try {
            return new DefaultListeningIOReactor(
                    passThroughSharedListenerConfiguration.getSourceConfiguration().getIOReactorConfig(),
                    passThroughSharedListenerConfiguration.getThreadFactory());
        } catch (IOReactorException e) {
            throw new IOReactorException
                    ("Error creating DefaultListingIOReactor, ioReactorConfig or thread factory may have problems", e);
        }
    }


    private ListenerEndpoint startEndpoint(InetSocketAddress inetSocketAddress,
                                           ListeningIOReactor defaultListeningIOReactor, String endPointName)
            throws Exception {
        ListenerEndpoint endpoint = defaultListeningIOReactor.listen(inetSocketAddress);
        try {
            endpoint.waitFor();
            InetSocketAddress address = (InetSocketAddress) endpoint.getAddress();
            if (!address.isUnresolved()) {
                log.info((endPointName != null ? "Pass-through " + endPointName : " Pass-through Http ") +
                         " Listener started on " + address.getHostName() + ":" + address.getPort());
            } else {
                log.info((endPointName != null ? "Pass-through " + endPointName : " Pass-through Http ") +
                         " Listener started on " + address);
            }
        } catch (Exception e) {
            throw new Exception("Endpoint does not start for port " + inetSocketAddress.getPort() +
                                "May be IO Reactor not started or endpoint binding exception ", e);

        }
        return endpoint;
    }

    private ListeningIOReactor shutdownReactor(int port) {
        ListeningIOReactor listeningIOReactor = passThroughListenerIOReactorMapper.get(port);
        ServerIODispatch serverIODispatch = passThroughListenerServerIODispatchMapper.get(port);
        if (listeningIOReactor != null) {
            if (serverIODispatch instanceof MultiListenerServerIODispatch ||
                serverIODispatch instanceof  MultiListenerSSLServerIODispatch) {
                log.info("Shutting down shared IO Reactor bind for port " + port + " will be caused for shutdown non " +
                         "axis2 Listeners ");
            } else {
                log.info("Shutting down IO Reactor bind for port " + port);
            }
        } else {
            log.error("Cannot find Pass Through Listener for port " + port);
        }
        return listeningIOReactor;
    }

    /**
     * Check whether dynamic endpoint is running for a particular port
     *
     * @param port port
     * @return whether dynamic endpoint is running for a particular port
     */
    public boolean isDynamicEndpointRunning(int port) {
        return dynamicPTTListeningEndpointMapper.get(port) != null ;
    }

    /**
     * Function to wait and verify iteratively whether port is successfully closed, till timeout reaches
     *
     * @param port target port
     * @param portCloseVerifyTimeout iterative verification timeout
     * @return
     */
    private boolean isPortCloseSuccess(int port, int portCloseVerifyTimeout) {
        boolean portCloseSuccess = false;
        for (int i = 0; i < portCloseVerifyTimeout; i++) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Verify port [" + port +"] close status. Attempt: " + i);
                }
                //Try to bind ServerSocket to port and verify whether port is successfully closed
                ServerSocket srv = new ServerSocket(port);
                srv.close();
                srv = null;
                //If reach here, port close successful
                portCloseSuccess = true;
                break;
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("The port " + port + " is not closed yet, verify again after waiting 1s", e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptException) {
                    //log and ignore
                }
            }
        }
        return portCloseSuccess;
    }
}
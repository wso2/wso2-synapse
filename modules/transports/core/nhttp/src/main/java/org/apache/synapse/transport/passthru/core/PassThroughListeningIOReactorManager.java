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
import org.apache.synapse.transport.passthru.ServerIODispatch;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PassThroughListeningIOReactorManager {

    private static final Logger logger = Logger.getLogger(PassThroughListeningIOReactorManager.class);

    private static PassThroughListeningIOReactorManager passThroughListeningIOReactorManager;

    private ListeningIOReactor sharedListeningIOReactor;
    private PassThroughSharedListenerConfiguration sharedIOReactorConfig;

    private final Map<Integer, NHttpServerEventHandler> portServerHandlerMapper;
    private final Map<Integer, ListenerEndpoint> nonAxis2PTTPortListeningEndpointMapper;
    private final Map<Integer, ListeningIOReactor> axis2ListenerIOReactorMapper;
    private final Map<Integer, ServerIODispatch> axis2ListenerServerIODispatchMapper;

    private AtomicBoolean isSharedIOReactorInitiated;
    private final IOReactorSharingMode ioReactorSharingMode;

    private PassThroughListeningIOReactorManager(IOReactorSharingMode ioReactorSharingMode) {
        portServerHandlerMapper = new ConcurrentHashMap<Integer, NHttpServerEventHandler>();
        nonAxis2PTTPortListeningEndpointMapper = new ConcurrentHashMap<Integer, ListenerEndpoint>();
        axis2ListenerIOReactorMapper = new ConcurrentHashMap<Integer, ListeningIOReactor>();
        axis2ListenerServerIODispatchMapper = new ConcurrentHashMap<Integer, ServerIODispatch>();

        isSharedIOReactorInitiated = new AtomicBoolean(false);
        this.ioReactorSharingMode = ioReactorSharingMode;
    }

    /**
     * Start Endpoint in IOReactor which is external to axis2 Listeners started at server startup
     * @param inetSocketAddress Socket Address of starting endpoint
     * @param nHttpServerEventHandler ServerHandler responsible for handle events of port
     * @param endpointName Endpoint Name
     * @return Is Endpoint started
     */
    public boolean startDynamicPTTEndpoint
    (InetSocketAddress inetSocketAddress, NHttpServerEventHandler nHttpServerEventHandler, String endpointName) {
        if (isSharedIOReactorInitiated.get()) {
            // if already SharedIOReactor initiated then use it for start endpoints
            ListenerEndpoint endpoint = startEndpoint(inetSocketAddress, sharedListeningIOReactor, endpointName);
            if (endpoint != null) {
                portServerHandlerMapper.put(inetSocketAddress.getPort(), nHttpServerEventHandler);
                nonAxis2PTTPortListeningEndpointMapper.put(inetSocketAddress.getPort(), endpoint);
                return true;
            } else {
                return false;
            }
        } else if (sharedIOReactorConfig != null) {
            //create separate IO Reactor for non axis2 transports and share among them

                try {
                    synchronized (this) {
                        sharedListeningIOReactor = initiateIOReactor(sharedIOReactorConfig);
                        isSharedIOReactorInitiated.compareAndSet(false, true);
                    }
                    ServerIODispatch serverIODispatch = new MultiListenerServerIODispatch
                            (portServerHandlerMapper, nHttpServerEventHandler, sharedIOReactorConfig.getServerConnFactory());
                    startIOReactor(sharedListeningIOReactor, serverIODispatch, "HTTP");
                    ListenerEndpoint endpoint = startEndpoint(inetSocketAddress, sharedListeningIOReactor, endpointName);
                    if (endpoint != null) {
                        portServerHandlerMapper.put(inetSocketAddress.getPort(), nHttpServerEventHandler);
                        nonAxis2PTTPortListeningEndpointMapper.put(inetSocketAddress.getPort(), endpoint);
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOReactorException e) {
                    logger.error("Error occurred when creating shared IO Reactor for non axis2 Listener " + endpointName, e);
                    return false;
                }
        } else {
            logger.error("Cannot start Endpoint for" + endpointName + "Axis2 Transport Listeners for PassThrough transport" +
                    " not started correctly ");
            return false;
        }
    }


    /**
     * Start PTT Endpoint which is given by axis2.xml
     * @param inetSocketAddress Socket Address of starting endpoint
     * @param defaultListeningIOReactor IO Reactor which  starts Endpoint
     * @param namePrefix name specified for endpoint
     * @return  Is started
     */
    public boolean startPTTEndpoint
    (InetSocketAddress inetSocketAddress, DefaultListeningIOReactor defaultListeningIOReactor, String namePrefix) {
        return startEndpoint(inetSocketAddress, defaultListeningIOReactor, namePrefix) != null;
    }

    /**
     * Close external endpoints listen in shared IO Reactor
     * @param port port of the endpoint need to close
     * @return is endpoint closed
     */
    public boolean closeDynamicPTTEndpoint(int port) {
        try {
            nonAxis2PTTPortListeningEndpointMapper.get(port).close();
        } catch (Exception e) {
            logger.error("Cannot close  Endpoint relevant to port " + port, e);
            return false;
        } finally {
            if (nonAxis2PTTPortListeningEndpointMapper.containsKey(port)) {
                nonAxis2PTTPortListeningEndpointMapper.remove(port);
            }
            if (portServerHandlerMapper.containsKey(port)) {
                portServerHandlerMapper.remove(port);
            }
        }
        return true;
    }

    /**
     * @param ioReactorSharingMode Mode of IO Reactor Sharing can be SHARED or UNSHARED
     * @return PassThroughIOReactorManager
     */
    public static PassThroughListeningIOReactorManager getInstance(IOReactorSharingMode ioReactorSharingMode) {
        if (passThroughListeningIOReactorManager == null) {
            synchronized (PassThroughListeningIOReactorManager.class) {
                passThroughListeningIOReactorManager = new PassThroughListeningIOReactorManager(ioReactorSharingMode);
                return passThroughListeningIOReactorManager;
            }
        }
        return passThroughListeningIOReactorManager;
    }

    /**
     * @return PassThroughIOReactorManager
     */
    public static PassThroughListeningIOReactorManager getInstance(){
        if (passThroughListeningIOReactorManager != null) {
            return passThroughListeningIOReactorManager;
        } else {
            logger.error("PassThroughIOReactorManager is not initiated Properly When PassThrough " +
                    "Axis2 Listeners are Starting or Axis2Listeners are not Started");
            return null;
        }
    }

    /**
     * Create IOReactor with given configuration
     * @param port Port of the Endpoint for axis2 Listener
     * @param nHttpServerEventHandler Server Handler responsible for handle events of port
     * @param passThroughSharedListenerConfiguration configuration related to create and start IOReactor
     * @return IOReactor
     */
    public ListeningIOReactor initIOReactor
    (int port, NHttpServerEventHandler nHttpServerEventHandler, PassThroughSharedListenerConfiguration passThroughSharedListenerConfiguration)
            throws IOReactorException {
        ListeningIOReactor defaultListeningIOReactor;
        ServerIODispatch serverIODispatch;
        try {
            if (ioReactorSharingMode == IOReactorSharingMode.SHARED && !isSharedIOReactorInitiated.get()
                    && !passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                // Create IOReactor for Listener make it shareable with Inbounds
                portServerHandlerMapper.put(port, nHttpServerEventHandler);
                synchronized (this){
                    serverIODispatch = new MultiListenerServerIODispatch
                            (portServerHandlerMapper, nHttpServerEventHandler, passThroughSharedListenerConfiguration.getServerConnFactory());
                    defaultListeningIOReactor = initiateIOReactor(passThroughSharedListenerConfiguration);
                    logger.info("IO Reactor for port " + port + " initiated on shared mode which will be used by non axis2 Transport " +
                            " Listeners ");
                    sharedListeningIOReactor = defaultListeningIOReactor;
                    isSharedIOReactorInitiated.compareAndSet(false, true);
                }
            } else {
                // Create un shareable IOReactors for axis2 Listeners and assign IOReactor Config for later create IOReactor for Inbounds
                serverIODispatch = new ServerIODispatch(nHttpServerEventHandler, passThroughSharedListenerConfiguration.getServerConnFactory());
                defaultListeningIOReactor = initiateIOReactor(passThroughSharedListenerConfiguration);

                synchronized (this) {
                    if (sharedIOReactorConfig == null && !passThroughSharedListenerConfiguration.getSourceConfiguration().getScheme().isSSL()) {
                        sharedIOReactorConfig = passThroughSharedListenerConfiguration;
                    }
                }
            }
            axis2ListenerServerIODispatchMapper.put(port, serverIODispatch);
            axis2ListenerIOReactorMapper.put(port, defaultListeningIOReactor);
        } catch (IOReactorException e) {
            throw new IOReactorException("Error occurred when trying to init IO Reactor", e);
        }
        return defaultListeningIOReactor;
    }


    /**
     * Close all endpoints started by PTT Listeners.
     * @param port Port of the Endpoint for PTT axis2 Listener
     * @return  is all Endpoints closed
     */
    public boolean closeAllStaticEndpoints(int port) {
        try {
            if (axis2ListenerIOReactorMapper.containsKey(port)) {
                ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
                Set<ListenerEndpoint> endpoints = listeningIOReactor.getEndpoints();
                if (axis2ListenerServerIODispatchMapper.get(port) instanceof MultiListenerServerIODispatch) {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        if (listenerEndpoint.getAddress() instanceof InetSocketAddress) {
                            int endPointPort = ((InetSocketAddress) listenerEndpoint.getAddress()).getPort();
                            if (nonAxis2PTTPortListeningEndpointMapper.containsKey(endPointPort)) continue;
                            listenerEndpoint.close();
                        }
                    }
                } else {
                    for (ListenerEndpoint listenerEndpoint : endpoints) {
                        listenerEndpoint.close();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error occurred when closing Endpoint in PassThrough Transport Related to port " + port, e);
            return false;
        }
    }

    /**
     * Return ServerIODispatch registered under given port
     * @param port Port of  axis2 PTT Listener
     * @return ServerIODispatch
     */
    public ServerIODispatch getServerIODispatch(int port) {
        if (axis2ListenerServerIODispatchMapper.containsKey(port)) {
            return axis2ListenerServerIODispatchMapper.get(port);
        }
        return null;
    }

    /**
     * @return  source configuration used by shared IO Reactor
     */
    public SourceConfiguration getSharedPassThroughSourceConfiguration() {
        if (sharedIOReactorConfig != null) {
            return sharedIOReactorConfig.getSourceConfiguration();
        }
        return null;
    }

    /**
     * ShutdownIOReactor which is registered by HTTPListener running on given port
     * @param port Port of  axis2 PTT Listener
     * @throws IOException Exception throwing when Shutdown
     */
    public void shutdownIOReactor(int port) throws IOException {
        ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
        ServerIODispatch serverIODispatch = axis2ListenerServerIODispatchMapper.get(port);
        if (listeningIOReactor != null) {
            if (serverIODispatch instanceof MultiListenerServerIODispatch) {
                logger.warn("Shutting down shared IO Reactor bind for port " + port + " will be caused for shutdown non " +
                        "axis2 Listeners ");
            } else {
                logger.info("Shutting down IO Reactor bind for port " + port);
            }
            listeningIOReactor.shutdown();
            axis2ListenerIOReactorMapper.remove(port);
            axis2ListenerServerIODispatchMapper.remove(port);
        } else {
            logger.error("Cannot found Pass Through Listener for port " + port);
        }
    }

    /**
     * ShutdownIOReactor which is registered by HTTPListener running on given port
     * @param port Port of  axis2 PTT Listener
     * @param miliSeconds Waiting Time before close IO Reactor
     * @throws IOException Exception throwing when Shutdown
     */
    public void shutdownIOReactor(int port, long miliSeconds) throws IOException {
        ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
        ServerIODispatch serverIODispatch = axis2ListenerServerIODispatchMapper.get(port);
        if (listeningIOReactor != null) {
            if (serverIODispatch instanceof MultiListenerServerIODispatch) {
                logger.warn("Shutting down shared IO Reactor bind for port " + port + " will be caused for shutdown non " +
                        "axis2 Listeners ");
            } else {
                logger.info("Shutting down IO Reactor bind for port " + port);
            }
            listeningIOReactor.shutdown(miliSeconds);
            axis2ListenerIOReactorMapper.remove(port);
            axis2ListenerServerIODispatchMapper.remove(port);
        } else {
            logger.error("Cannot found Pass Through Listener for port " + port);
        }
    }

    /**
     * Pause IO Reactor which is registered by HTTPListener running on given port
     * @param port Port of  axis2 PTT Listener
     * @throws IOException Exception throwing when pausing
     */
    public void pauseIOReactor(int port) throws IOException {
        ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
        ServerIODispatch serverIODispatch = axis2ListenerServerIODispatchMapper.get(port);
        if (listeningIOReactor != null) {
            if (serverIODispatch instanceof MultiListenerServerIODispatch) {
                logger.warn("Pausing shared IO Reactor bind for port " + port + " will be caused for pausing non " +
                        "axis2 Listeners ");
            } else {
                logger.info("Shutting down IO Reactor bind for port " + port);
            }
            listeningIOReactor.pause();
        } else {
            logger.error("Cannot found Pass Through Listener for port " + port);
        }
    }

    /**
     * Resume IO Reactor which is registered by HTTPListener running on given port
     * @param port Port of  axis2 PTT Listener
     * @throws IOException Exception throwing when pausing
     */
    public void resume(int port) throws IOException {
        ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
        if (listeningIOReactor != null) {
            listeningIOReactor.resume();
        } else {
            logger.error("Cannot found Pass Through Listener for port " + port);
        }
    }


    private ListeningIOReactor initiateIOReactor(final PassThroughSharedListenerConfiguration passThroughSharedListenerConfiguration)
            throws IOReactorException {
        try {
            return new DefaultListeningIOReactor
                    (passThroughSharedListenerConfiguration.getSourceConfiguration().getIOReactorConfig(),
                                                            passThroughSharedListenerConfiguration.getThreadFactory());
        } catch (IOReactorException e) {
            throw new IOReactorException
                    ("Error creating DefaultListingIOReactor, ioReactorConfig or thread factory may have problems", e);
        }
    }

    /**
     * StartIOReactor with given ServerIODispatch
     * @param listeningIOReactor Listening IO Reactor to be start
     * @param serverIODispatch underlying Event Dispatcher for Reactor
     * @param prefix HTTP/HTTPS
     */
    public void startIOReactor(final ListeningIOReactor listeningIOReactor, final ServerIODispatch serverIODispatch, final String prefix) {
        Thread reactorThread = new Thread(new Runnable() {
            public void run() {
                try {
                    listeningIOReactor.execute(serverIODispatch);
                } catch (Exception e) {
                    logger.fatal("Exception encountered in the " + prefix + " Listener. " +
                            "No more connections will be accepted by this transport", e);
                } finally {
                    logger.info(prefix + " Listener shutdown.");
                    if (serverIODispatch instanceof MultiListenerServerIODispatch) {
                        logger.warn("Shutting down shared IO Reactor");
                    }
                }

            }
        }, "PassThrough " + prefix + " Listener");
        reactorThread.start();
    }


    private ListenerEndpoint startEndpoint
            (InetSocketAddress inetSocketAddress, ListeningIOReactor defaultListeningIOReactor, String endPointName) {
        ListenerEndpoint endpoint = defaultListeningIOReactor.listen(inetSocketAddress);
        try {
            endpoint.waitFor();
            if (logger.isInfoEnabled()) {
                InetSocketAddress address = (InetSocketAddress) endpoint.getAddress();
                if (!address.isUnresolved()) {
                    logger.info((endPointName != null ? "Pass-through " + endPointName : " Pass-through Http ") + " Listener started on " +
                            address.getHostName() + ":" + address.getPort());
                } else {
                    logger.info((endPointName != null ? "Pass-through " + endPointName : " Pass-through Http ") + " Listener started on " + address);
                }
            }
        } catch (InterruptedException e) {
            logger.error("Endpoint does not start for port " + inetSocketAddress.getPort() +
                    "May be IO Reactor not started or endpoint binding exception ", e);
        }
        return endpoint;
    }
}

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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PassThroughIOReactorManager {

    private static final Logger logger = Logger.getLogger(PassThroughIOReactorManager.class);

    private static PassThroughIOReactorManager passThroughIOReactorManager;

    private DefaultListeningIOReactor sharedListeningIOReactor;
    private PassThroughIOReactorConfig sharedIOReactorConfig;

    private final Map<Integer, NHttpServerEventHandler> portServerHandlerMapper;
    private final Map<Integer, ListenerEndpoint> nonAxis2PTTPortListeningEndpointMapper;
    private final Map<Integer, DefaultListeningIOReactor> axis2ListenerIOReactorMapper;
    private final Map<Integer, ServerIODispatch> axis2ListenerServerIODispatchMapper;

    private AtomicBoolean isSharedIOReactorInitiated;
    private final IOReactorSharingMode ioReactorSharingMode;

    private PassThroughIOReactorManager(IOReactorSharingMode ioReactorSharingMode) {
        portServerHandlerMapper = new ConcurrentHashMap<Integer, NHttpServerEventHandler>();
        nonAxis2PTTPortListeningEndpointMapper = new ConcurrentHashMap<Integer, ListenerEndpoint>();
        axis2ListenerIOReactorMapper = new ConcurrentHashMap<Integer, DefaultListeningIOReactor>();
        axis2ListenerServerIODispatchMapper = new ConcurrentHashMap<Integer, ServerIODispatch>();

        isSharedIOReactorInitiated = new AtomicBoolean(false);
        this.ioReactorSharingMode = ioReactorSharingMode;
    }

    /**
     * @param inetSocketAddress       <>Socket Address of starting endpoint</>
     * @param nHttpServerEventHandler <>Server Handler responsible for handle events of port</>
     * @param endpointName            <>Endpoint Name</>
     * @return <>is Endpoint started</>
     */
    public boolean startNonAxis2PTTEndpoint
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
            DefaultListeningIOReactor defaultListeningIOReactor =
                    initiateIOReactor
                            (sharedIOReactorConfig, new MultiListenerServerIODispatch
                                               (portServerHandlerMapper, sharedIOReactorConfig.getServerConnFactory()));
            if (defaultListeningIOReactor != null) {
                synchronized (this) {
                    sharedListeningIOReactor = defaultListeningIOReactor;
                }
                isSharedIOReactorInitiated.compareAndSet(false, true);
            } else {
                logger.error("Cannot start Endpoint for" + endpointName);
            }
        } else {
            logger.error("Cannot start Endpoint for" + endpointName + "Axis2 Transport Listeners for PassThrough transport" +
                    "not started correctly");
        }
        return true;
    }


    /**
     * @param inetSocketAddress         <>Socket Address of starting endpoint</>
     * @param defaultListeningIOReactor <>IO Reactor for start the Endpoint</>
     * @return <>Is endpoint started</>
     */
    public boolean startAxis2PTTEndpoint(InetSocketAddress inetSocketAddress, DefaultListeningIOReactor defaultListeningIOReactor) {
        return startEndpoint(inetSocketAddress, defaultListeningIOReactor, null) != null;
    }

    /**
     * @param port <>port of the Endpoint need to close</>
     * @return <>is endpoint closed</>
     */
    public boolean closeNonAxis2PTTEndpoint(int port) {
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
     * @param ioReactorSharingMode <>Mode of IO Reactor Sharing can be SHARED or UNSHARED</>
     * @return <>PassThroughIOReactorManager</>
     */
    public PassThroughIOReactorManager getInstance(IOReactorSharingMode ioReactorSharingMode) {
        if (passThroughIOReactorManager == null) {
            synchronized (this) {
                passThroughIOReactorManager = new PassThroughIOReactorManager(ioReactorSharingMode);
                return passThroughIOReactorManager;
            }
        }
        return passThroughIOReactorManager;
    }

    /**
     * @return <>PassThroughIOReactorManager</>
     */
    public PassThroughIOReactorManager getInstance() {
        if (passThroughIOReactorManager != null) {
            return passThroughIOReactorManager;
        } else {
            logger.error("PassThroughIOReactorManager is not initiated");
            return null;
        }
    }

    /**
     * @param port                       <>Port of the Endpoint for axis2 Listener</>
     * @param nHttpServerEventHandler    <>Server Handler responsible for handle events of port</>
     * @param passThroughIOReactorConfig <>configuration related to create and start IOReactor</>
     * @return <>IOReactor</>
     */
    public DefaultListeningIOReactor getListeningIOReactor
    (int port, NHttpServerEventHandler nHttpServerEventHandler, PassThroughIOReactorConfig passThroughIOReactorConfig) {
        DefaultListeningIOReactor defaultListeningIOReactor;
        ServerIODispatch serverIODispatch;

        if (ioReactorSharingMode == IOReactorSharingMode.SHARED && !isSharedIOReactorInitiated.get()) {
            // Create IOReactor for Listener make it shareable with Inbounds
            portServerHandlerMapper.put(port, nHttpServerEventHandler);
            serverIODispatch = new MultiListenerServerIODispatch
                    (portServerHandlerMapper, passThroughIOReactorConfig.getServerConnFactory());
            defaultListeningIOReactor = initiateIOReactor(passThroughIOReactorConfig, serverIODispatch);
            if (defaultListeningIOReactor != null) {
                synchronized (this) {
                    sharedListeningIOReactor = defaultListeningIOReactor;
                }
                isSharedIOReactorInitiated.compareAndSet(false, true);
            }
        } else {
            // Create un shareable IOReactors for axis2 Listeners and assign IOReactor Config for later create IOReactor for Inbounds
            serverIODispatch = new ServerIODispatch(nHttpServerEventHandler, passThroughIOReactorConfig.getServerConnFactory());
            defaultListeningIOReactor = initiateIOReactor(passThroughIOReactorConfig, serverIODispatch);

            synchronized (this) {
                if (sharedIOReactorConfig == null) {
                    sharedIOReactorConfig = passThroughIOReactorConfig;
                }
            }
        }

        if (defaultListeningIOReactor != null) {
            axis2ListenerServerIODispatchMapper.put(port, serverIODispatch);
            axis2ListenerIOReactorMapper.put(port, defaultListeningIOReactor);
        }
        return defaultListeningIOReactor;
    }


    /**
     * @param port <>Port of the Endpoint for PTT axis2 Listener</>
     * @return <>is all Endpoints closed</>
     */
    public boolean closeAllAxi2PTTRelatedEndpoints(int port) {
        try {
            if (axis2ListenerIOReactorMapper.containsKey(port)) {
                ListeningIOReactor listeningIOReactor = axis2ListenerIOReactorMapper.get(port);
                Set<ListenerEndpoint> endpoints = listeningIOReactor.getEndpoints();
                if (axis2ListenerServerIODispatchMapper.get(port) instanceof MultiListenerServerIODispatch){
                    for (ListenerEndpoint listenerEndpoint : endpoints){
                        if (listenerEndpoint.getAddress() instanceof InetSocketAddress){
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
            logger.error("Error occurred when closing Endpoint in PassThrough Transport Related to port" + port, e);
            return false;
        }
    }

    /**
     * @param port <>Port of  axis2 PTT Listener</>
     * @return <>ServerIODispatch</>
     */
    public ServerIODispatch getServerIODispatch(int port){
         if(axis2ListenerServerIODispatchMapper.containsKey(port)){
             return axis2ListenerServerIODispatchMapper.get(port);
         }
        return null;
    }


    private DefaultListeningIOReactor initiateIOReactor
            (final PassThroughIOReactorConfig passThroughIOReactorConfig, final ServerIODispatch serverIODispatch) {
        try {
            final DefaultListeningIOReactor defaultListeningIOReactor =
                    new DefaultListeningIOReactor
                            (passThroughIOReactorConfig.getIoReactorConfig(), passThroughIOReactorConfig.getThreadFactory());
            Thread reactorThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        defaultListeningIOReactor.execute(serverIODispatch);
                    } catch (Exception e) {
                        logger.fatal("Exception encountered in the " + passThroughIOReactorConfig.getNamePrefix() + " Listener. " +
                                "No more connections will be accepted by this transport", e);
                        return;
                    }
                    logger.info(passThroughIOReactorConfig.getNamePrefix() + " Listener shutdown.");
                }
            }, "PassThrough" + passThroughIOReactorConfig.getNamePrefix() + "Listener");
            reactorThread.start();
            return defaultListeningIOReactor;
        } catch (IOReactorException e) {
            logger.error
                    ("Error creating DefaultListingIOReactor, ioReactorConfig or thread factory may have problems", e);
            return null;
        }
    }



    private ListenerEndpoint startEndpoint
            (InetSocketAddress inetSocketAddress, DefaultListeningIOReactor defaultListeningIOReactor, String endPointName) {
        ListenerEndpoint endpoint = defaultListeningIOReactor.listen(inetSocketAddress);
        try {
            endpoint.waitFor();
            if (logger.isInfoEnabled()) {
                InetSocketAddress address = (InetSocketAddress) endpoint.getAddress();
                if (!address.isUnresolved()) {
                    logger.info(endPointName != null ? endPointName + "Http" : "Http" + " Listener started on " +
                            address.getHostName() + ":" + address.getPort());
                } else {
                    logger.info(endPointName != null ? endPointName + "Http" : "Http" + " Listener started on" + address);
                }
            }
        } catch (InterruptedException e) {
            logger.error("Endpoint does not start for port " + inetSocketAddress.getPort() +
                    "May be IO Reactor not started or endpoint binding exception", e);
            return null;
        }
        return endpoint;

    }
}

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

package org.apache.synapse.transport.passthru.api;

import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.core.PassThroughListeningIOReactorManager;

import java.net.InetSocketAddress;

/**
 * API class for access PassThrough Core Inbound Endpoint management classes
 */
public class PassThroughInboundEndpointHandler {

    /**
     * Start Endpoint Listen and events related to Endpoint handle by  given NHttpServerEventHandler
     * @param inetSocketAddress Socket Address of the Endpoint need to be start by underlying IOReactor
     * @param nHttpServerEventHandler Event Handler for handle events for Endpoint
     * @param endpointName Name of the Endpoint
     * @return  Is Endpoint started successfully
     */
    public static boolean startEndpoint(InetSocketAddress inetSocketAddress,
                                         NHttpServerEventHandler nHttpServerEventHandler, String endpointName) {
        return PassThroughListeningIOReactorManager.getInstance().startDynamicPTTEndpoint(inetSocketAddress,
                                                                                 nHttpServerEventHandler, endpointName);
    }
    /**
     * close ListeningEndpoint running on the given port
     * @param port Port of  ListeningEndpoint to be closed
     * @return  IS successfully closed
     */
    public static boolean closeEndpoint(int port) {
        return PassThroughListeningIOReactorManager.getInstance().closeDynamicPTTEndpoint(port);
    }
    /**
     * @return  Pass Through SourceConfiguration registered by shared IO Reactor of  PTT Listener
     */
    public static SourceConfiguration getPassThroughSourceConfiguration() throws Exception {
        SourceConfiguration sourceConfiguration = PassThroughListeningIOReactorManager.getInstance().
                                                                              getSharedPassThroughSourceConfiguration();
        if ( sourceConfiguration != null) {
            return sourceConfiguration;
        } else {
            throw new Exception("PassThroughSharedListenerConfiguration  is not initiated correctly when  " +
                                                                                  "PassThroughListeners  are starting");
        }
    }
}

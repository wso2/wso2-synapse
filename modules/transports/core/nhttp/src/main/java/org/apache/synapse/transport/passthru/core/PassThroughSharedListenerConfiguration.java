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

import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;

import java.util.concurrent.ThreadFactory;

/**
 * This class is a representation of configuration used when creating ListeningIOReactor
 */
public class PassThroughSharedListenerConfiguration {

    /**
     * ThreadFactory Used by IOReactor
     */
    private ThreadFactory threadFactory;

    /**
     * ServerConnFactory used by EventDispatcher to create connections
     */
    private ServerConnFactory serverConnFactory;

    /**
     * SourceConfiguration used by IOReactor to initiate
     */
    private SourceConfiguration sourceConfiguration;

    /**
     * @param threadFactory       ThreadFactory used by IO Reactor
     * @param serverConnFactory   ServerConnectionFactory used by IODispatcher to create connections
     * @param sourceConfiguration SourceConfiguration of the shared IOReactor initiated PTT Listener
     */
    public PassThroughSharedListenerConfiguration(ThreadFactory threadFactory, ServerConnFactory serverConnFactory,
                                                  SourceConfiguration sourceConfiguration) {
        this.threadFactory = threadFactory;
        this.serverConnFactory = serverConnFactory;
        this.sourceConfiguration = sourceConfiguration;
    }

    /**
     * @return ThreadFactory
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * @return ServerConnFactory
     */
    public ServerConnFactory getServerConnFactory() {
        return serverConnFactory;
    }

    /**
     * @return SourceConfiguration
     */
    public SourceConfiguration getSourceConfiguration() {
        return sourceConfiguration;
    }

}

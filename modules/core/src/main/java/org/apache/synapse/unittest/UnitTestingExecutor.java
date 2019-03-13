/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest;

import org.apache.log4j.Logger;
import org.apache.synapse.config.SynapseConfiguration;

/**
 * Class of executing unit test agent parallel to the Synapse engine.
 * Class extends with Thread class
 */
public class UnitTestingExecutor extends Thread {

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());
    public SynapseConfiguration synapseConfiguration;
    private static UnitTestingExecutor initializeThread = null;
    private static int executingPort;

    /**
     * Initializing initializeThread if it is null.
     */
    public static synchronized UnitTestingExecutor getExecuteInstance() {

        if (initializeThread == null) {
            initializeThread = new UnitTestingExecutor();
        }

        return initializeThread;
    }

    /**
     * Get receiving server TCP port and assign it to executingPort
     *
     * @param port receiving port
     */
    public void setServerPort(int port) {
        executingPort = port;
    }

    /**
     * Method of executing thread of agent.
     */
    public void run() {
        logger.info("Unit testing agent started");
        TCPServer tcpConnection = new TCPServer();
        tcpConnection.initialize(executingPort);
    }

    /**
     * get the current SynapseConfiguration.
     * @return SynapseConfiguration
     */
    public SynapseConfiguration getSynapseConfiguration() {

        return this.synapseConfiguration;
    }

    /**
     * set the current SynapseConfiguration.
     */
    public void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {

        this.synapseConfiguration = synapseConfiguration;

    }
}

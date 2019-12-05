/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Class is responsible for configurations of TCP connection.
 */
public class TCPServer {

    private static Logger log = Logger.getLogger(UnitTestingExecutor.class.getName());

    private ServerSocket serverSocket;
    private boolean isUnitTestingOver = false;

    /**
     * Initializing TCP server for main unit testing server.
     */
    public void initialize() {
        //check unit test received port, if its null then set default port as 9008
        String requestPort = System.getProperty("synapseTestPort");
        if (requestPort == null || requestPort.isEmpty()) {
            requestPort = "9008";
        }

        //check port for TCP server connection and initialized socket
        try {
            int unitTestingAgentPort = Integer.parseInt(requestPort);
            serverSocket = new ServerSocket(unitTestingAgentPort);
            log.info("Synapse unit testing agent has been established on port " + unitTestingAgentPort);
            if (log.isDebugEnabled()) {
                log.debug("Waiting for client request");
            }

            //allow for the client requests
            acceptConnection();
        } catch (NumberFormatException e) {
            log.error("Given TCP port \"" + requestPort + "\" is not in valid format, " +
                    "failed to start unit testing framework", e);
        } catch (IOException e) {
            log.error("Error in initializing TCP connection in given port " + requestPort, e);
        }
    }

    /**
     * Create RequestHandler threads for load balancing.
     */
    private void acceptConnection() throws IOException {
        //start thread for shutdown hook
        shutDown();

        while (!isUnitTestingOver) {
            Socket socket = serverSocket.accept();
            RequestHandler requestHandler = new RequestHandler(socket);
            Thread threadForClient = new Thread(requestHandler);
            threadForClient.start();
        }
    }

    /**
     * Shut down unit testing framework.
     */
    private void shutDown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Shutting down unit testing framework");
                isUnitTestingOver = true;
            }
        });
    }
}

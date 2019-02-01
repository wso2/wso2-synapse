/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {

    /**
     * Class responsible for initializing a socket, reading and writing data to the socket
     */

    private static ServerSocket serverSocket;
    private static Socket clientSocket;

    private static PrintWriter printWriter;
    private static BufferedReader bufferedReader;
    static Logger log = Logger.getLogger(TCPServer.class.getName());

    /**
     * Constructor for initializing a TCPServer instance
     */

    public TCPServer() {

        try {
            serverSocket = new ServerSocket(6666);
            clientSocket = serverSocket.accept();
            log.info("Connection established");
        } catch (IOException e) {
            log.error("Exception in initializing the socket", e);
        }
    }

    /**
     * Method for reading data from the socket
     *
     * @param agent
     */

    public void readData(Agent agent) {

        try {
            log.info("Waiting for the client");
            printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String message = bufferedReader.readLine();
            while (message != null) {
                log.info("Message received:" + message);
                agent.processData(message);
                message = bufferedReader.readLine();
            }
//            log.info("Closing resources");
//            clientSocket.close();
//            printWriter.close();
//            bufferedReader.close();

        } catch (IOException e) {
            log.error("Exception in reading data from the buffer reader", e);

        }

    }

    /**
     * Method for writing data to the socket
     *
     * @param result
     */

    public void writeData(String result) {

        printWriter.println(result);
        log.info("Result sent:" + result);
    }
}

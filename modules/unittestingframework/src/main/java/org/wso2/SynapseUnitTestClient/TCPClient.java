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
package org.wso2.SynapseUnitTestClient;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;

/**
 * TCP client for initializing the socket and sending and receiving data through the socket
 */
public class TCPClient {

    private static Socket clientSocket;
    private static PrintWriter printWriter;
    private static BufferedReader bufferedReader;
    private static Logger log = Logger.getLogger(TCPClient.class.getName());

    /**
     * Initializing the socket
     *
     * @param synapseHost
     * @param port
     */
    public TCPClient(String synapseHost, String port) {

        try {
            clientSocket = new Socket(synapseHost, Integer.parseInt(port));
            log.info("Connection established");
        } catch (IOException e) {
            log.error("Exception in initializing the socket", e);
        }
    }

    /**
     * Method responsible for sending the artifactId and test data to the server
     */
    public String writeData(String messageToBeSent) {

        try {
            printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            printWriter.println(messageToBeSent);
            String result = bufferedReader.readLine();
            return result;

        } catch (IOException e) {
            log.error("Exception in writing data to the socket", e);
            return null;
        }

    }

    public void closeResources(){
        try {

            clientSocket.close();
            bufferedReader.close();
            printWriter.close();
        } catch (IOException e){
            log.error("Exception in closing resources");
        }
    }

}
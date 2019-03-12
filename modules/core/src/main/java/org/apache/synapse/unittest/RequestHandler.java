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

import javafx.util.Pair;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

/**
 * Class is implements with Runnable.
 * Handle multiple requests from the client
 * Process receive message and response to the particular client
 */
public class RequestHandler implements Runnable {

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    private Socket socket;
    private JSONObject responseToClient;

    /**
     * Initializing RequestHandler withe the client socket connection
     */
    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            readData();
            writeData(responseToClient);

        } catch (Exception e) {
            logger.error(e);

        } finally {
            closeSocket();
        }
    }

    /**
     * Read input data from the unit testing client
     */
    private void readData() throws Exception {
        TestingAgent agent = new TestingAgent();
        InputStream inputStream = socket.getInputStream();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        //receiving message from the client
        String receivedMessage = (String) objectInputStream.readObject();

        //convert the message into the JSON format
        JSONObject jsonObject = new JSONObject(receivedMessage);

        //get results of artifact deployer
        Pair<Boolean, String> artifactDeployed = agent.processArtifact(jsonObject);

        //check artifact deployer is success or not
        if (artifactDeployed.getKey()) {

            //performs test cases through the deployed synapse configuration
            Pair<JSONObject, String> testCasesMediated = agent.processTestCases(jsonObject);

            //check mediation or invoke is success or failed
            if (testCasesMediated.getValue() == null) {
                responseToClient = testCasesMediated.getKey();
            } else {
                responseToClient = new JSONObject("{'mediation':'failed', 'exception':'"
                        + testCasesMediated.getValue() + "'}");
            }

        } else if (!artifactDeployed.getKey() && artifactDeployed.getValue() != null) {
            responseToClient = new JSONObject("{'deployment':'failed', 'exception':'"
                    + artifactDeployed.getValue() + "'}");
        } else {
            responseToClient = new JSONObject("{'deployment':'failed'}");
        }
    }

    /**
     * Write output data to the unit testing client
     */
    private void writeData(JSONObject jsonObject) throws IOException {

        OutputStream out = socket.getOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(out);
        o.writeObject(jsonObject.toString());
        out.flush();
    }

    /**
     * close the TCP connection with client
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

}

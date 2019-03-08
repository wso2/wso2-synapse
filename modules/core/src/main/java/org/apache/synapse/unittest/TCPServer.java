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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 */
public class TCPServer {

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    private ServerSocket serverSocket;

    public void initialize(int port) {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("####################################################");
            logger.info("Synapse unit testing agent has been established on port " + port);
            logger.info("Waiting for client request");
            logger.info("####################################################");
            acceptConnection();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void acceptConnection() throws IOException {
        while (true) {
            Socket socket = serverSocket.accept();
            RequestHandler requestHandler = new RequestHandler(socket);
            Thread t = new Thread(requestHandler);
            t.start();
        }
    }
}


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
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;

public class RequestHandler implements Runnable{

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    private Socket socket;

    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            handleClientRequest();

        } catch (IOException e) {
            logger.error(e);
        } finally {
            closeSocket();
        }
    }

    public void closeSocket(){
        try {
            socket.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }


    public void handleClientRequest() throws IOException{
        String receivedMessage;

        DataInputStream in = new DataInputStream(socket.getInputStream());
        receivedMessage = in.readUTF();
        //create JSON object from input message
        JSONObject jsonObject = new JSONObject(receivedMessage);

        System.out.println(" ");
        System.out.println(" ");
        System.out.println("");
        System.out.println(jsonObject);
        System.out.println("");
        System.out.println("");
        System.out.println("");
        boolean isArtifactDeployed = TestingAgent.processArtifact(jsonObject);

        if(isArtifactDeployed){
            JSONObject resultOfTestCases = TestingAgent.processTestCases(jsonObject);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(resultOfTestCases.toString());
        }else{
            //send response to the client about artifact deployment failed
        }
    }

}
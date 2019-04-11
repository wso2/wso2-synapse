/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.synapse.unittest.testcase.data.classes.SynapseTestCase;
import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.MockServiceData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
     * Initializing RequestHandler withe the client socket connection.
     */
    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

            logger.info("\n");
            logger.info("---------------------START TEST-CASE--------------------------\n");
            String receivedData = readData();
            SynapseTestCase synapseTestCases = preProcessingData(receivedData);

            if (synapseTestCases != null) {
                runTestingAgent(synapseTestCases);
            } else {
                logger.error("Reading Synapse testcase data failed");
                responseToClient = new JSONObject("{'test-cases':'Failed while reading synapseTestCase data'}");
            }

            writeData(responseToClient);
            MockServiceCreator.stopServices();

            logger.info("---------------------END TEST-CASE--------------------------\n");
        } catch (Exception e) {
            logger.error(e);
        } finally {
            closeSocket();
        }
    }

    /**
     * Read input data from the unit testing client.
     *
     * @return read data from the client
     */
    private String readData() {
        String inputFromClient = null;

        try {
            InputStream inputStream = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            inputFromClient = (String) objectInputStream.readObject();

        } catch (Exception e) {
            logger.error("Failed to get input stream from TCP connection", e);
        }

        //receiving message from the client
        return inputFromClient;

    }

    /**
     * Processed received message data and stores those data in relevant data holders.
     * Uses configModifier if there are some mock services to start
     *
     * @param receivedMessage received synapseTestcase data message as String
     * @return SynapaseTestCase object contains artifact, test cases and mock services data
     */
    private SynapseTestCase preProcessingData(String receivedMessage) {

        try {
            //Read relevant data from the received message and add to the relevant data holders
            SynapseTestcaseDataReader synapseTestcaseDataReader = new SynapseTestcaseDataReader(receivedMessage);

            ArtifactData readArtifactData = synapseTestcaseDataReader.readAndStoreArtifactData();
            TestCaseData readTestCaseData = synapseTestcaseDataReader.readAndStoreTestCaseData();
            MockServiceData readMockServiceData = synapseTestcaseDataReader.readAndStoreMockServiceData();

            //configure the artifact if there are mock-services to append
            if (readMockServiceData.getMockServicesCount() > 0) {
                ConfigModifier.endPointModifier(readArtifactData, readMockServiceData);
            }

            //wrap the artifact data, testcase data and mock service data as one
            SynapseTestCase synapseTestCases = new SynapseTestCase();
            synapseTestCases.setArtifacts(readArtifactData);
            synapseTestCases.setTestCases(readTestCaseData);

            return synapseTestCases;

        } catch (Exception e) {
            logger.error("Error while reading data from received message", e);
            return null;
        }

    }

    /**
     * Execute test agent for artifact deployment and mediation using receiving JSON message.
     *
     * @param synapseTestCase test cases data received from client
     */
    private void runTestingAgent(SynapseTestCase synapseTestCase) {

        TestingAgent agent = new TestingAgent();
        Pair<Boolean, String> supportiveArtifactDeployment = new Pair<>(false, null);
        Pair<Boolean, String> testArtifactDeployment = new Pair<>(false, null);

        //get results of supportive-artifact deployment if exists
        if (synapseTestCase.getArtifacts().getSupportiveArtifactCount() > 0) {
            logger.info("Supportive artifacts deployment started");
            supportiveArtifactDeployment = agent.processSupportiveArtifacts(synapseTestCase);
        }

        //check supportive-artifact deployment is success or not
        if (supportiveArtifactDeployment.getKey() || synapseTestCase.getArtifacts().getSupportiveArtifactCount() == 0) {
            logger.info("Test artifact deployment started");
            testArtifactDeployment = agent.processTestArtifact(synapseTestCase);

        } else if (!supportiveArtifactDeployment.getKey() && supportiveArtifactDeployment.getValue() != null) {
            responseToClient = new JSONObject("{'deployment':'failed', 'exception':'"
                    + supportiveArtifactDeployment.getValue() + "'}");
        } else {
            responseToClient = new JSONObject("{'supportive-deployment':'failed'}");
        }

        //check test-artifact deployment is success or not
        if (testArtifactDeployment.getKey()) {

            logger.info("Synapse testing agent ready to mediate test cases through deployments");
            //performs test cases through the deployed synapse configuration
            Pair<JSONObject, String> testCasesMediated = agent.processTestCases(synapseTestCase);

            //check mediation or invoke is success or failed
            if (testCasesMediated.getValue() == null) {
                responseToClient = testCasesMediated.getKey();
            } else {
                responseToClient = new JSONObject("{'mediation':'failed', 'exception':'"
                        + testCasesMediated.getValue() + "'}");
            }

        } else if (!testArtifactDeployment.getKey() && testArtifactDeployment.getValue() != null) {
            responseToClient = new JSONObject("{'deployment':'failed', 'exception':'"
                    + testArtifactDeployment.getValue() + "'}");
        } else {
            responseToClient = new JSONObject("{'test-deployment':'failed'}");
        }
    }

    /**
     * Write output data to the unit testing client.
     */
    private void writeData(JSONObject jsonObject) throws IOException {

        OutputStream out = socket.getOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(out);
        o.writeObject(jsonObject.toString());
        out.flush();
    }

    /**
     * close the TCP connection with client.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

}

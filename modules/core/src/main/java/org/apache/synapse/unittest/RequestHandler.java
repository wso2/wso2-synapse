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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.util.Pair;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.testcase.data.classes.SynapseTestCase;
import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.MockServiceData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Class is implements with Runnable.
 * Handle multiple requests from the client
 * Process receive message and response to the particular client
 */
public class RequestHandler implements Runnable {

    private static Logger log = Logger.getLogger(UnitTestingExecutor.class.getName());

    private Socket socket;
    private JsonObject responseToClient;
    private boolean isTransportPassThroughPortChecked = false;
    private String exception;

    /**
     * Initializing RequestHandler withe the client socket connection.
     */
    RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

            log.info("\n");
            log.info("---------------------START TEST-CASE--------------------------\n");
            checkTransportPassThroughPortAvailability();

            String receivedData = readData();
            SynapseTestCase synapseTestCases = preProcessingData(receivedData);

            if (synapseTestCases != null) {
                runTestingAgent(synapseTestCases);
            } else {
                log.error("Reading Synapse testcase data failed");
                responseToClient = new JsonParser()
                        .parse("{'test-cases':'Failed while reading synapseTestCase data','Exception':'"
                                + exception + "'}").getAsJsonObject();
            }

            writeData(responseToClient);
            MockServiceCreator.stopServices();

            log.info("---------------------END TEST-CASE--------------------------\n");
        } catch (Exception e) {
            log.error("Error while running client request in test agent", e);
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
            log.error("Failed to get input stream from TCP connection", e);
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
            log.error("Error while reading data from received message", e);
            exception = e.toString();
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
            log.info("Supportive artifacts deployment started");
            supportiveArtifactDeployment = agent.processSupportiveArtifacts(synapseTestCase);
        }

        //check supportive-artifact deployment is success or not
        if (supportiveArtifactDeployment.getKey() || synapseTestCase.getArtifacts().getSupportiveArtifactCount() == 0) {
            log.info("Test artifact deployment started");
            testArtifactDeployment = agent.processTestArtifact(synapseTestCase);

        } else if (!supportiveArtifactDeployment.getKey() && supportiveArtifactDeployment.getValue() != null) {
            responseToClient = new JsonParser()
                    .parse("{'deployment':'failed', 'exception':'"
                            + supportiveArtifactDeployment.getValue() + "'}").getAsJsonObject();
        } else {
            responseToClient = new JsonParser()
                    .parse("{'supportive-deployment':'failed'}").getAsJsonObject();
        }

        //check test-artifact deployment is success or not
        if (testArtifactDeployment.getKey()) {

            log.info("Synapse testing agent ready to mediate test cases through deployments");
            //performs test cases through the deployed synapse configuration
            Pair<JsonObject, String> testCasesMediated = agent.processTestCases(synapseTestCase);

            //check mediation or invoke is success or failed
            if (testCasesMediated.getValue() == null) {
                responseToClient = testCasesMediated.getKey();
            } else {
                responseToClient = new JsonParser()
                        .parse("{'mediation':'failed', 'exception':'"
                                + testCasesMediated.getValue() + "'}").getAsJsonObject();
            }

        } else if (!testArtifactDeployment.getKey() && testArtifactDeployment.getValue() != null) {
            responseToClient = new JsonParser()
                    .parse("{'deployment':'failed', 'exception':'"
                            + testArtifactDeployment.getValue() + "'}").getAsJsonObject();
        } else {
            responseToClient = new JsonParser()
                    .parse("{'test-deployment':'failed'}").getAsJsonObject();
        }
    }

    /**
     * Write output data to the unit testing client.
     */
    private void writeData(JsonObject jsonObject) throws IOException {

        OutputStream out = socket.getOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(out);
        o.writeObject(jsonObject.toString());
        out.flush();
    }

    /**
     * Check transport pass through port is started or not.
     * Waits until port started to receive the request from client
     */
    private void checkTransportPassThroughPortAvailability() throws IOException {

        if (!isTransportPassThroughPortChecked) {
            log.info("Unit testing agent checks transport Pass-through HTTP Listener port");

            boolean isPassThroughPortNotOccupied = true;
            int transportPassThroughPort = Integer.parseInt(System.getProperty("http.nio.port"));
            long timeoutExpiredMs = System.currentTimeMillis() + 10000;

            while (isPassThroughPortNotOccupied) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isPassThroughPortNotOccupied = checkPortAvailability(transportPassThroughPort);

                if (waitMillis <= 0) {
                    // timeout expired
                    throw new IOException("Connection refused for http Pass-through HTTP Listener port - "
                            + transportPassThroughPort);
                }
            }

            isTransportPassThroughPortChecked = true;
        }
    }

    /**
     * Check port availability.
     *
     * @param port port which want to check availability
     * @return if available true else false
     */
    private boolean checkPortAvailability(int port) {
        boolean isPortAvailable;
        try (Socket socketTester = new Socket()) {
            socketTester.connect(new InetSocketAddress("127.0.0.1", port));
            isPortAvailable = false;
        } catch (IOException e) {
            isPortAvailable = true;
        }

        return isPortAvailable;
    }

    /**
     * close the TCP connection with client.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            log.error("Error when closing socket connection", e);
        }
    }

}

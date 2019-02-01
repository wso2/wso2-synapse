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

import javafx.util.Pair;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;

import java.io.File;

/**
 * Class responsible for receiving test data and and maintaining the test execution flow
 */
public class Agent extends Thread {

    private static Agent agent = null;
    public String artifactType = null;

    public static synchronized Agent getInstance() {

        if (agent == null) {
            agent = new Agent();
        }
        return agent;
    }

    private static Logger log = Logger.getLogger(Agent.class.getName());
    private TCPServer tcpServer = new TCPServer();
    private SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
    private String key = null;

    /**
     * Method for initializing the TCPServer instance
     */

    public void initialize() {

        tcpServer.readData(Agent.getInstance());
    }

    /**
     * Method for maintaining the test execution flow
     *
     * @param message
     */

    public void processData(String message) {

        try {

            if (message.startsWith("|") && message.endsWith("|")) {
                String operation = MessageFormatUtils.getOperation(message);

                if (operation.equals("deploy")) {
                    String[] deploymentData = MessageFormatUtils.getDeploymentData(message);
                    String artifact = deploymentData[0];
                    String fileName = deploymentData[1];
                    String artifactType = deploymentData[2];
                    this.artifactType = artifactType;
                    String fileString = FileUtils.readFileToString(new File(artifact));
                    OMElement xmlFile = AXIOMUtil.stringToOM(fileString);

                    if (artifactType.equals("sequence")) {
                        Pair<SynapseConfiguration, String> pair = new Deployer().deploySequence(xmlFile, fileName);
                        synapseConfiguration = pair.getKey();
                        key = pair.getValue();
                        log.info(key);
                        log.info(fileName);

                    } else if (artifactType.equals("proxy")) {
                        Pair<SynapseConfiguration, String> pair = new Deployer().deployProxy(xmlFile, fileName);
                        synapseConfiguration = pair.getKey();
                        key = pair.getValue();
                        log.info(key);

                    } else {
                        String error = "Operation not identified";
                        log.error(error);
                        tcpServer.writeData(MessageFormatUtils.generateResultMessage(error));
                    }

                    if (key.equals(fileName)) {
                        String deploymentResult = "Artifact is deployed successfully";
                        log.info("Artifact deployed successfully");
                        tcpServer.writeData(MessageFormatUtils.generateResultMessage(deploymentResult));

                    } else {
                        String error = "Artifact not deployed";
                        log.error(error);
                        tcpServer.writeData(error);

                    }

                } else if (operation.equals("executeTest")) {

                    log.info("Test data received unit testing begins");
                    String[] testDataValues = MessageFormatUtils.getTestData(message);
                    String inputXmlPayload = testDataValues[0];
                    String expectedPayload = testDataValues[1];
                    String expectedPropVal = testDataValues[2];

                    if (artifactType.equals("sequence")) {
                        Pair<Boolean, MessageContext> pair = new TestExecutor().sequenceMediate(inputXmlPayload, synapseConfiguration, key);
                        Boolean mediationResult = pair.getKey();
                        MessageContext messageContext = pair.getValue();
                        if (mediationResult) {
                            String unitTestResult = new TestExecutor().doAssertions(expectedPayload, expectedPropVal, messageContext);
                            tcpServer.writeData(MessageFormatUtils.generateResultMessage(unitTestResult));
                            new SynapseArtifactDeploymentStore().removeArtifactWithFileName(key);
                        } else {
                            String mediationError = "Sequence cannot be found";
                            log.error(mediationError);
                            tcpServer.writeData(MessageFormatUtils.generateResultMessage(mediationError));
                        }
                    } else if (artifactType.equals("proxy")) {
                        String propertySet = new TestExecutor().invokeProxyService(key, inputXmlPayload);

                        if (propertySet.equals("Exception in invoking the proxy service")) {
                            String result = "Exception in invoking the proxy service";
                            tcpServer.writeData(MessageFormatUtils.generateResultMessage(result));
                        } else {
                            String assertionResult = new TestExecutor().assertProperties(expectedPropVal, propertySet);
                            tcpServer.writeData(MessageFormatUtils.generateResultMessage(assertionResult));

                        }

                    }
                } else {
                    String error = "Operation not identified";
                    log.error(error);
                    tcpServer.writeData(MessageFormatUtils.generateResultMessage(error));
                }

            } else {
                String error = "This is not a valid message";
                log.error(error);
                tcpServer.writeData(MessageFormatUtils.generateResultMessage(error));

            }
        } catch (Exception e) {
            String deploymentError = "Exception in mediating the message through the deployed artifact: ";
            tcpServer.writeData(MessageFormatUtils.generateResultMessage(deploymentError + e));
        }
    }
}

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
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.namespace.QName;


import static org.apache.synapse.unittest.Constants.*;

/**
 * Testing agent deploy receiving artifact data in relevant deployer and mediate test cases on it.
 * Returns the results of artifact deployment and test case mediation to the RequestHandler
 */
class TestingAgent {

    private Logger logger = Logger.getLogger(TestingAgent.class.getName());
    private SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
    private String artifactType = null;
    private String key = null;
    private String context = null;
    private String resourceMethod = null;
    private String exception = null;
    private ArrayList<Boolean> testCasesResult = new ArrayList<>();

    /**
     * Check artifact type and pass the artifact data to the relevant deployment mechanism.
     *
     * @param receivedMessage message received from unit testing client
     * @return Result of the deployment and exception status
     */
    Pair<Boolean, String> processArtifact(JSONObject receivedMessage) {
        artifactType = MessageDecoder.getArtifactType(receivedMessage);
        String artifactName = MessageDecoder.getArtifactName(receivedMessage);
        OMElement artifact = MessageDecoder.getConfigurationArtifact(receivedMessage);
        boolean isArtifactDeployed = false;
        ConfigurationDeployer config = new ConfigurationDeployer();

        try {

            //check artifact type and pass the artifact to the relevant deployment
            switch (artifactType) {
                case TYPE_SEQUENCE :
                    Pair<SynapseConfiguration, String> pairOfSequenceDeployment =
                            config.deploySequenceArtifact(artifact, artifactName);
                    synapseConfiguration = pairOfSequenceDeployment.getKey();
                    key = pairOfSequenceDeployment.getValue();

                    if (key.equals(artifactName)) {
                        isArtifactDeployed = true;
                        logger.info("Sequence artifact deployed successfully");
                    } else {
                        logger.error("Sequence deployment failed");
                    }
                    break;

                case TYPE_PROXY :
                    Pair<SynapseConfiguration, String> pairofProxyDeployment =
                            config.deployProxyArtifact(artifact, artifactName);
                    synapseConfiguration = pairofProxyDeployment.getKey();
                    key = pairofProxyDeployment.getValue();

                    if (key.equals(artifactName)) {
                        isArtifactDeployed = true;
                        logger.info("Proxy artifact deployed successfully");
                    } else {
                        logger.error("Proxy deployment failed");
                    }
                    break;

                case TYPE_API :
                    Pair<SynapseConfiguration, String> pairofApiDeployment =
                            config.deployApiArtifact(artifact, artifactName);
                    synapseConfiguration = pairofApiDeployment.getKey();
                    key = pairofApiDeployment.getValue();
                    context = artifact.getAttributeValue(new QName(API_CONTEXT));
                    resourceMethod = artifact.getFirstElement().getAttributeValue(new QName(RESOURCE_METHODS));

                    if (key.equals(artifactName)) {
                        isArtifactDeployed = true;
                        logger.info("API artifact deployed successfully");
                    } else {
                        logger.error("API deployment failed");
                    }
                    break;

                default:
                    throw new IOException("Undefined operation type in unit testing agent");

            }
        } catch (Exception e) {
            logger.error("Artifact deployment failed", e);
            exception = e.toString();
        }

        return new Pair<>(isArtifactDeployed, exception);
    }

    /**
     * Check artifact type and pass the test case data to the relevant mediation.
     *
     * @param receivedMessage message received from unit testing client
     * @return Result of the mediation and exception status
     */
    Pair<JSONObject, String> processTestCases(JSONObject receivedMessage) {
        boolean isAssert = false;
        JSONObject resultOfTestCases = new JSONObject();
        int testCaseCount = MessageDecoder.getTestCasesCount(receivedMessage);
        ArrayList<ArrayList<String>> testCasesData;
        testCasesData = MessageDecoder.getTestCasesData(receivedMessage);

        logger.info(testCaseCount + " Test case(s) ready to execute");

        try {
            //execute test cases with synapse configurations and test data
            for (int i = 0; i < testCaseCount; i++) {
                ArrayList<String> currentTestCase = testCasesData.get(i);

                switch (artifactType) {
                    case TYPE_SEQUENCE :
                        Pair<Boolean, MessageContext> mediateResult =
                                TestCasesMediator.sequenceMediate(currentTestCase.get(0), synapseConfiguration, key);

                        Boolean mediationResult = mediateResult.getKey();
                        MessageContext resultedMessageContext = mediateResult.getValue();

                        //check whether mediation is success or not
                        isAssert = checkAssertionWithSequenceMediation(mediationResult, resultedMessageContext, currentTestCase, i);
                        break;

                    case TYPE_PROXY :
                        String invokedProxyResult = TestCasesMediator
                                .proxyServiceExecutor(testCasesData.get(i).get(0), key);

                        isAssert = checkAssertionWithProxyMediation(invokedProxyResult, currentTestCase, i);
                        break;

                    case TYPE_API :
                        String invokedApiResult = TestCasesMediator.apiResourceExecutor
                                (currentTestCase.get(0), context, resourceMethod);

                        isAssert = checkAssertionWithAPIMediation(invokedApiResult, currentTestCase, i);
                        break;

                    default :
                        break;
                }
                resultOfTestCases.put("test-case " + (i + 1), isAssert);
                testCasesResult.add(isAssert);
            }
        } catch (Exception e) {
            logger.error("Error occurred while running test cases", e);
            exception = e.toString();
        }

        //check all test cases are success

        return new Pair<>(checkAllTestCasesCorrect(resultOfTestCases), exception);
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param mediationResult result of mediation of sequence
     * @param resultedMessageContext message context of mediation of sequence
     * @param currentTestCase current running test case data
     * @param index index of current running test case data
     * @return result of assertion or mediation result
     */
    private boolean checkAssertionWithSequenceMediation(boolean mediationResult, MessageContext resultedMessageContext,
                                                        ArrayList<String> currentTestCase, int index) {
        boolean isAssert = false;
        if (mediationResult) {
            isAssert = Assertor.doAssertionSequence(currentTestCase.get(1),
                    currentTestCase.get(2), resultedMessageContext, index + 1);

        } else {
            logger.error("Sequence mediation failed");
        }

        return isAssert;
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedProxyResult result of proxy invoke
     * @param currentTestCase current running test case data
     * @param index index of current running test case data
     * @return result of assertion or invoke result
     */
    private boolean checkAssertionWithProxyMediation(String invokedProxyResult,
                                                     ArrayList<String> currentTestCase, int index) {
        boolean isAssert = false;
        if (!invokedProxyResult.equals("failed")) {
            isAssert = Assertor.doAssertionService
                    (currentTestCase.get(1), invokedProxyResult, index + 1);

        } else {
            logger.error("Proxy service invoke failed");
        }

        return isAssert;
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedApiResult result of API invoke
     * @param currentTestCase current running test case data
     * @param index index of current running test case data
     * @return result of assertion or invoke result
     */
    private boolean checkAssertionWithAPIMediation(String invokedApiResult,
                                                   ArrayList<String> currentTestCase, int index) {
        boolean isAssert = false;

        if (!invokedApiResult.equals("failed")) {
            isAssert = Assertor.doAssertionService
                    (currentTestCase.get(1), invokedApiResult, index + 1);
        } else {
            logger.error("API resource invoke failed");
        }

        return isAssert;
    }

    /**
     * Check all the test cases are run correctly as expected.
     *
     * @param currentAllTestCaseResult test cases results
     * @return sucess message as a JSON
     */
    private JSONObject checkAllTestCasesCorrect(JSONObject currentAllTestCaseResult) {
        boolean isAllSuccess = true;
        JSONObject resultOfTestCases;

        for (boolean testCase : testCasesResult) {
            if (!testCase) {
                isAllSuccess = false;
                break;
            }
        }

        if (isAllSuccess) {
            resultOfTestCases = new JSONObject("{'test-cases':'SUCCESS'}");
        } else {
            resultOfTestCases = currentAllTestCaseResult;
        }

        return resultOfTestCases;
    }
}

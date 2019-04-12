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
import org.apache.axiom.om.OMElement;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.unittest.testcase.data.classes.SynapseTestCase;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;
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
    private OMElement artifactNode = null;
    private String exception = null;
    private ArrayList<Boolean> testCasesResult = new ArrayList<>();

    /**
     * Check artifact type and pass the test-artifact data to the relevant deployment mechanism.
     *
     * @param synapseTestCase test cases data received from client
     * @return Result of the deployment and exception status
     */
    Pair<Boolean, String> processTestArtifact(SynapseTestCase synapseTestCase) {
        artifactType = synapseTestCase.getArtifacts().getTestArtifact().getArtifactType();
        String artifactNameOrKey = synapseTestCase.getArtifacts().getTestArtifact().getArtifactNameOrKey();
        OMElement artifact = synapseTestCase.getArtifacts().getTestArtifact().getArtifact();
        boolean isArtifactDeployed = false;
        ConfigurationDeployer config = new ConfigurationDeployer();

        try {
            //check artifact type and pass the artifact to the relevant deployment
            switch (artifactType) {

                case TYPE_SEQUENCE:
                    Pair<SynapseConfiguration, String> pairOfSequenceDeployment =
                            config.deploySequenceArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairOfSequenceDeployment.getKey();
                    key = pairOfSequenceDeployment.getValue();

                    if (key.equals(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        logger.info("Sequence artifact deployed successfully");
                    } else {
                        logger.error("Sequence deployment failed");
                    }
                    break;

                case TYPE_PROXY:
                    Pair<SynapseConfiguration, String> pairOfProxyDeployment =
                            config.deployProxyArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairOfProxyDeployment.getKey();
                    key = pairOfProxyDeployment.getValue();

                    if (key.equals(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        logger.info("Proxy artifact deployed successfully");
                    } else {
                        logger.error("Proxy deployment failed");
                    }
                    break;

                case TYPE_API:
                    Pair<SynapseConfiguration, String> pairofApiDeployment =
                            config.deployApiArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairofApiDeployment.getKey();
                    key = pairofApiDeployment.getValue();
                    artifactNode = artifact;

                    if (key.equals(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        logger.info("API artifact deployed successfully");
                    } else {
                        logger.error("API deployment failed");
                    }
                    break;

                default:
                    throw new IOException("Undefined operation type for <test-artifact> given in unit testing agent");

            }
        } catch (Exception e) {
            logger.error("Artifact deployment failed", e);
            exception = e.toString();
        }

        return new Pair<>(isArtifactDeployed, exception);
    }

    /**
     * Check artifact type and pass the supportive-artifact data to the relevant deployment mechanism.
     *
     * @param synapseTestCase test cases data received from client
     * @return Result of the deployment and exception status
     */
    Pair<Boolean, String> processSupportiveArtifacts(SynapseTestCase synapseTestCase) {
        boolean isArtifactDeployed = true;

        for (int x = 0; x < synapseTestCase.getArtifacts().getSupportiveArtifactCount(); x++) {
            if (isArtifactDeployed) {
                try {
                    //check artifact type and pass the artifact to the relevant deployment
                    isArtifactDeployed = processForArtifactTypes(synapseTestCase, x);

                } catch (Exception e) {
                    logger.error("Artifact deployment failed", e);
                    exception = e.toString();
                }
            } else {
                logger.error(synapseTestCase.getArtifacts().getSupportiveArtifact(x).getArtifactType()
                        + " artifact deployment failed");
                break;
            }
        }

        return new Pair<>(isArtifactDeployed, exception);
    }

    /**
     * Check artifact type and pass the supportive-artifact data to the relevant deployment mechanism.
     *
     * @param synapseTestCase test cases data received from client
     * @param index           index of supportive artifact
     * @return true if supportive artifact deployed else return false
     */
    private boolean processForArtifactTypes(SynapseTestCase synapseTestCase, int index) throws IOException {

        String supportiveArtifactType = synapseTestCase.getArtifacts().getSupportiveArtifact(index).getArtifactType();
        String artifactNameOrKey = synapseTestCase.getArtifacts().getSupportiveArtifact(index).getArtifactNameOrKey();
        OMElement artifact = synapseTestCase.getArtifacts().getSupportiveArtifact(index).getArtifact();
        ConfigurationDeployer config = new ConfigurationDeployer();

        switch (supportiveArtifactType) {
            case TYPE_SEQUENCE:
                Pair<SynapseConfiguration, String> pairOfSequenceDeployment =
                        config.deploySequenceArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfSequenceDeployment.getKey();
                key = pairOfSequenceDeployment.getValue();
                break;

            case TYPE_PROXY:
                Pair<SynapseConfiguration, String> pairofProxyDeployment =
                        config.deployProxyArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairofProxyDeployment.getKey();
                key = pairofProxyDeployment.getValue();
                break;

            case TYPE_API:
                Pair<SynapseConfiguration, String> pairofApiDeployment =
                        config.deployApiArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairofApiDeployment.getKey();
                key = pairofApiDeployment.getValue();
                break;

            case TYPE_ENDPOINT:
                Pair<SynapseConfiguration, String> pairOfEndpointDeployment =
                        config.deployEndpointArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfEndpointDeployment.getKey();
                key = pairOfEndpointDeployment.getValue();
                break;

            case TYPE_LOCAL_ENTRY:
                Pair<SynapseConfiguration, String> pairOfLocalEntryDeployment =
                        config.deployLocalEntryArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfLocalEntryDeployment.getKey();
                key = pairOfLocalEntryDeployment.getValue();
                break;

            default:
                throw new IOException("Undefined operation type for <test-artifact> given in unit testing agent");
        }

        if (key.equals(artifactNameOrKey)) {
            logger.info(supportiveArtifactType + " artifact deployed successfully");
            return true;

        } else {
            return false;
        }
    }

    /**
     * Check artifact type and pass the test case data to the relevant mediation.
     *
     * @param synapseTestCase test cases data received from client
     * @return Result of the mediation and exception status
     */
    Pair<JSONObject, String> processTestCases(SynapseTestCase synapseTestCase) {
        boolean isAssert = false;
        JSONObject resultOfTestCases = new JSONObject();
        int testCaseCount = synapseTestCase.getTestCases().getTestCaseCount();

        logger.info(testCaseCount + " Test case(s) ready to execute");

        try {
            //execute test cases with synapse configurations and test data
            for (int i = 0; i < testCaseCount; i++) {
                TestCase currentTestCase = synapseTestCase.getTestCases().getTestCase(i);

                switch (artifactType) {
                    case TYPE_SEQUENCE:
                        Pair<Boolean, MessageContext> mediateResult =
                                TestCasesMediator.sequenceMediate(currentTestCase, synapseConfiguration, key);

                        Boolean mediationResult = mediateResult.getKey();
                        MessageContext resultedMessageContext = mediateResult.getValue();

                        //check whether mediation is success or not
                        Pair<Boolean, String> assertSeqResult = checkAssertionWithSequenceMediation
                                (mediationResult, resultedMessageContext, currentTestCase, i);
                        isAssert = assertSeqResult.getKey();
                        exception = assertSeqResult.getValue();
                        break;

                    case TYPE_PROXY:
                        HttpResponse invokedProxyResult = TestCasesMediator
                                .proxyServiceExecutor(currentTestCase, key);

                        Pair<Boolean, String> assertProxyResult = checkAssertionWithProxyMediation(invokedProxyResult, currentTestCase, i);
                        isAssert = assertProxyResult.getKey();
                        exception = assertProxyResult.getValue();
                        break;

                    case TYPE_API:
                        String context = artifactNode.getAttributeValue(new QName(API_CONTEXT));
                        String resourceMethod = currentTestCase.getRequestMethod();
                        HttpResponse invokedApiResult = TestCasesMediator.apiResourceExecutor
                                (currentTestCase, context, resourceMethod);

                        Pair<Boolean, String> assertAPIResult = checkAssertionWithAPIMediation(invokedApiResult, currentTestCase, i);
                        isAssert = assertAPIResult.getKey();
                        exception = assertAPIResult.getValue();
                        break;

                    default:
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
     * @param mediationResult        result of mediation of sequence
     * @param resultedMessageContext message context of mediation of sequence
     * @param currentTestCase        current running test case data
     * @return result of assertion or mediation result
     */
    private Pair<Boolean, String> checkAssertionWithSequenceMediation(
            boolean mediationResult, MessageContext resultedMessageContext, TestCase currentTestCase, int index) {
        boolean isAssert = false;
        String assertMessage;
        if (mediationResult) {
            Pair<Boolean, String> assertOfSequence = Assertor.doAssertionSequence(currentTestCase, resultedMessageContext, index + 1);
            isAssert = assertOfSequence.getKey();
            assertMessage = assertOfSequence.getValue();

        } else {
            assertMessage = "Sequence mediation failed";
            logger.error("Sequence mediation failed");
        }

        return new Pair<>(isAssert, assertMessage);
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedProxyResult result of proxy invoke
     * @param currentTestCase    current running test case data
     * @return result of assertion or invoke result
     */
    private Pair<Boolean, String> checkAssertionWithProxyMediation(HttpResponse invokedProxyResult,
                                                                   TestCase currentTestCase, int index) {
        boolean isAssert = false;
        String assertMessage;
        if (invokedProxyResult != null) {
            Pair<Boolean, String> assertOfProxy = Assertor.doAssertionService
                    (currentTestCase, invokedProxyResult, index + 1);

            isAssert = assertOfProxy.getKey();
            assertMessage = assertOfProxy.getValue();
        } else {
            assertMessage = "Proxy service invoke failed";
            logger.error("Proxy service invoke failed");
        }

        return new Pair<>(isAssert, assertMessage);
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedApiResult result of API invoke
     * @param currentTestCase  current running test case data
     * @return result of assertion or invoke result
     */
    private Pair<Boolean, String> checkAssertionWithAPIMediation(HttpResponse invokedApiResult,
                                                                 TestCase currentTestCase, int index) {
        boolean isAssert = false;
        String assertMessage;
        if (invokedApiResult != null) {
            Pair<Boolean, String> assertOfApi = Assertor.doAssertionService
                    (currentTestCase, invokedApiResult, index + 1);

            isAssert = assertOfApi.getKey();
            assertMessage = assertOfApi.getValue();
        } else {
            assertMessage = "API resource invoke failed";
            logger.error("API resource invoke failed");
        }

        return new Pair<>(isAssert, assertMessage);
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

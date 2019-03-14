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

import java.util.ArrayList;
import javax.xml.namespace.QName;


import static org.apache.synapse.unittest.Constants.*;

/**
 * Testing agent deploy receiving artifact data in relevant deployer and mediate test cases on it.
 * Returns the results of artifact deployment and test case mediation to the RequestHandler
 */
public class TestingAgent {

    private Logger logger = Logger.getLogger(TestingAgent.class.getName());
    private SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
    private String artifactType = null;
    private String key = null;
    private String context = null;
    private String resourceMethod = null;
    private String exception = null;

    /**
     * Check artifact type and pass the artifact data to the relevant deployment mechanism.
     *
     * @param receivedMessage message received from unit testing client
     * @return Result of the deployment and exception status
     */
    public Pair<Boolean, String> processArtifact(JSONObject receivedMessage) {
        artifactType = MessageDecoder.getArtifactType(receivedMessage);
        String artifactName = MessageDecoder.getArtifactName(receivedMessage);
        OMElement artifact = MessageDecoder.getConfigurationArtifact(receivedMessage);
        boolean isArtifactDeployed = false;


        try {
            //check artifact type and pass the artifact to the relevant deployment
            if (artifactType.equals(TYPE_SEQUENCE)) {

                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deploySequenceArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();

                if (key.equals(artifactName)) {
                    isArtifactDeployed = true;
                    logger.info("Sequence artifact deployed successfully");
                } else {
                    logger.error("Sequence deployment failed");
                }

            } else if (artifactType.equals(TYPE_PROXY)) {
                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deployProxyArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();

                if (key.equals(artifactName)) {
                    isArtifactDeployed = true;
                    logger.info("Proxy artifact deployed successfully");
                } else {
                    logger.error("Proxy deployment failed");
                }


            } else if (artifactType.equals(TYPE_API)) {

                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deployApiArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();
                context = artifact.getAttributeValue(new QName(API_CONTEXT));
                resourceMethod = artifact.getFirstElement().getAttributeValue(new QName(RESOURCE_METHODS));

                if (key.equals(artifactName)) {
                    isArtifactDeployed = true;
                    logger.info("API artifact deployed successfully");
                } else {
                    logger.error("API deployment failed");
                }

            } else {
                logger.error("Undefined operation type in unit testing agent");
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
    public Pair<JSONObject, String> processTestCases(JSONObject receivedMessage) {
        boolean isAssert = false;
        JSONObject resultOfTestCases = new JSONObject();
        int testCaseCount = MessageDecoder.getTestCasesCount(receivedMessage);
        ArrayList<ArrayList<String>> testCasesData;
        testCasesData = MessageDecoder.getTestCasesData(receivedMessage);

        logger.info(testCaseCount + " Test case(s) ready to execute");

        try {
            //execute test cases with synapse configurations and test data
            for (int i = 0; i < testCaseCount; i++) {

                if (artifactType.equals(TYPE_SEQUENCE)) {
                    Pair<Boolean, MessageContext> mediateResult =
                            TestCasesMediator.sequenceMediate(testCasesData.get(i).get(0), synapseConfiguration, key);
                    Boolean mediationResult = mediateResult.getKey();
                    MessageContext resultedMessageContext = mediateResult.getValue();

                    //check whether mediation is success or not
                    if (mediationResult) {
                        isAssert = Assert.doAssertionSequence(testCasesData.get(i).get(1),
                                testCasesData.get(i).get(2), resultedMessageContext, i + 1);

                    } else {
                        logger.error("Sequence mediation failed");
                    }

                } else if (artifactType.equals(TYPE_PROXY)) {
                    String invokedResult = TestCasesMediator.proxyServiceExecutor(testCasesData.get(i).get(0), key);

                    if (!invokedResult.equals("failed")) {
                        isAssert = Assert.doAssertionService(testCasesData.get(i).get(1), invokedResult, i + 1);

                    } else {
                        logger.error("Proxy service invoke failed");
                    }

                } else if (artifactType.equals(TYPE_API)) {

                    String invokedResult = TestCasesMediator.apiResourceExecutor
                            (testCasesData.get(i).get(0), context, resourceMethod);

                    if (!invokedResult.equals("failed")) {
                        isAssert = Assert.doAssertionService(testCasesData.get(i).get(1), invokedResult, i + 1);

                    } else {
                        logger.error("API resource invoke failed");
                    }
                } else {
                    break;
                }

                resultOfTestCases.put("test-case " + (i + 1), isAssert);
            }

        } catch (Exception e) {
            logger.error("Error occured while running test cases", e);
            exception = e.toString();
        }

        return new Pair<>(resultOfTestCases, exception);
    }
}

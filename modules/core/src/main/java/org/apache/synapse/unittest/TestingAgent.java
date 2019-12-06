/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.deployers.APIDeployer;
import org.apache.synapse.deployers.EndpointDeployer;
import org.apache.synapse.deployers.LocalEntryDeployer;
import org.apache.synapse.deployers.ProxyServiceDeployer;
import org.apache.synapse.deployers.SequenceDeployer;
import org.apache.synapse.unittest.testcase.data.classes.SynapseTestCase;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;
import org.apache.synapse.unittest.testcase.data.classes.TestCaseSummary;
import org.apache.synapse.unittest.testcase.data.classes.TestSuiteSummary;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

import static org.apache.synapse.unittest.Constants.API_CONTEXT;
import static org.apache.synapse.unittest.Constants.TYPE_API;
import static org.apache.synapse.unittest.Constants.TYPE_ENDPOINT;
import static org.apache.synapse.unittest.Constants.TYPE_LOCAL_ENTRY;
import static org.apache.synapse.unittest.Constants.TYPE_PROXY;
import static org.apache.synapse.unittest.Constants.TYPE_SEQUENCE;


/**
 * Testing agent deploy receiving artifact data in relevant deployer and mediate test cases on it.
 * Returns the results of artifact deployment and test case mediation to the RequestHandler
 */
class TestingAgent {

    private Logger log = Logger.getLogger(TestingAgent.class.getName());
    private SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
    private String artifactType = null;
    private String proxyTransportMethod = null;
    private String key = null;
    private OMElement artifactNode = null;
    private String exception = null;
    private Map<String, String> deploymentStats = new HashMap<>();

    /**
     * Check artifact type and pass the test-artifact data to the relevant deployment mechanism.
     *
     * @param synapseTestCase test cases data received from client
     * @return Result of the deployment and exception status
     */
    Map.Entry<Boolean, TestSuiteSummary> processTestArtifact(SynapseTestCase synapseTestCase,
                                                             TestSuiteSummary testSuiteSummary) {
        artifactType = synapseTestCase.getArtifacts().getTestArtifact().getArtifactType();
        proxyTransportMethod = synapseTestCase.getArtifacts().getTestArtifact().getTransportMethod();
        String artifactNameOrKey = synapseTestCase.getArtifacts().getTestArtifact().getArtifactNameOrKey();
        OMElement artifact = synapseTestCase.getArtifacts().getTestArtifact().getArtifact();
        boolean isArtifactDeployed = false;
        ConfigurationDeployer config = new ConfigurationDeployer();

        try {
            //check artifact type and pass the artifact to the relevant deployment
            switch (artifactType) {

                case TYPE_SEQUENCE:
                    Map.Entry<SynapseConfiguration, String> pairOfSequenceDeployment =
                            config.deploySequenceArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairOfSequenceDeployment.getKey();
                    key = pairOfSequenceDeployment.getValue();

                    if (key.contains(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        deploymentStats.put(key, TYPE_SEQUENCE);
                        testSuiteSummary.setDeploymentStatus(Constants.PASSED_KEY);
                        log.info("Sequence artifact deployed successfully");
                    } else {
                        String errorMessage = "Sequence " + artifactNameOrKey + " deployment failed";
                        log.error(errorMessage);
                        testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
                        testSuiteSummary.setDeploymentException(errorMessage);
                    }
                    break;

                case TYPE_PROXY:
                    Map.Entry<SynapseConfiguration, String> pairOfProxyDeployment =
                            config.deployProxyArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairOfProxyDeployment.getKey();
                    key = pairOfProxyDeployment.getValue();

                    if (key.contains(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        deploymentStats.put(key, TYPE_PROXY);
                        testSuiteSummary.setDeploymentStatus(Constants.PASSED_KEY);
                        log.info("Proxy artifact deployed successfully");
                    } else {
                        String errorMessage = "Proxy " + artifactNameOrKey + " deployment failed";
                        log.error(errorMessage);
                        testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
                        testSuiteSummary.setDeploymentException(errorMessage);
                    }
                    break;

                case TYPE_API:
                    Map.Entry<SynapseConfiguration, String> pairofApiDeployment =
                            config.deployApiArtifact(artifact, artifactNameOrKey);
                    synapseConfiguration = pairofApiDeployment.getKey();
                    key = pairofApiDeployment.getValue();
                    artifactNode = artifact;

                    if (key.contains(artifactNameOrKey)) {
                        isArtifactDeployed = true;
                        deploymentStats.put(key, TYPE_API);
                        testSuiteSummary.setDeploymentStatus(Constants.PASSED_KEY);
                        log.info("API artifact deployed successfully");
                    } else {
                        String errorMessage = "API " + artifactNameOrKey + " deployment failed";
                        log.error(errorMessage);
                        testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
                        testSuiteSummary.setDeploymentException(errorMessage);
                    }
                    break;

                default:
                    throw new IOException("Undefined operation type for <test-artifact> given in unit testing agent");

            }
        } catch (Exception e) {
            log.error("Artifact deployment failed", e);
            exception = CommonUtils.stackTraceToString(e);
            testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
            testSuiteSummary.setDeploymentException(exception);
        }

        return new AbstractMap.SimpleEntry<>(isArtifactDeployed, testSuiteSummary);
    }

    /**
     * Check artifact type and pass the supportive-artifact data to the relevant deployment mechanism.
     *
     * @param synapseTestCase test cases data received from client
     * @return Result of the deployment and exception status
     */
    Map.Entry<Boolean, TestSuiteSummary> processSupportiveArtifacts(SynapseTestCase synapseTestCase,
                                                                    TestSuiteSummary testSuiteSummary) {
        boolean isArtifactDeployed = true;

        for (int x = 0; x < synapseTestCase.getArtifacts().getSupportiveArtifactCount(); x++) {
            if (isArtifactDeployed) {
                try {
                    //check artifact type and pass the artifact to the relevant deployment
                    isArtifactDeployed = processForArtifactTypes(synapseTestCase, x);

                } catch (Exception e) {
                    log.error("Artifact deployment failed", e);
                    exception = CommonUtils.stackTraceToString(e);
                    testSuiteSummary.setDeploymentStatus(Constants.FAILED_KEY);
                    testSuiteSummary.setDeploymentException(exception);
                }
            } else {
                log.error(synapseTestCase.getArtifacts().getSupportiveArtifact(x).getArtifactType()
                        + " artifact deployment failed");
                break;
            }
        }

        return new AbstractMap.SimpleEntry<>(isArtifactDeployed, testSuiteSummary);
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
                Map.Entry<SynapseConfiguration, String> pairOfSequenceDeployment =
                        config.deploySequenceArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfSequenceDeployment.getKey();
                key = pairOfSequenceDeployment.getValue();
                break;

            case TYPE_PROXY:
                Map.Entry<SynapseConfiguration, String> pairofProxyDeployment =
                        config.deployProxyArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairofProxyDeployment.getKey();
                key = pairofProxyDeployment.getValue();
                break;

            case TYPE_API:
                Map.Entry<SynapseConfiguration, String> pairofApiDeployment =
                        config.deployApiArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairofApiDeployment.getKey();
                key = pairofApiDeployment.getValue();
                break;

            case TYPE_ENDPOINT:
                Map.Entry<SynapseConfiguration, String> pairOfEndpointDeployment =
                        config.deployEndpointArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfEndpointDeployment.getKey();
                key = pairOfEndpointDeployment.getValue();
                break;

            case TYPE_LOCAL_ENTRY:
                Map.Entry<SynapseConfiguration, String> pairOfLocalEntryDeployment =
                        config.deployLocalEntryArtifact(artifact, artifactNameOrKey);
                synapseConfiguration = pairOfLocalEntryDeployment.getKey();
                key = pairOfLocalEntryDeployment.getValue();
                break;

            default:
                throw new IOException("Undefined operation type for <test-artifact> given in unit testing agent");
        }

        if (key.contains(artifactNameOrKey)) {
            log.info(key + " - " + supportiveArtifactType + " artifact deployed successfully");
            deploymentStats.put(key, supportiveArtifactType);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check artifact type and pass the test case data to the relevant mediation.
     *
     * @param synapseTestCase test cases data received from client
     */
    void processTestCases(SynapseTestCase synapseTestCase, TestSuiteSummary testSuiteSummary) {
        int testCaseCount = synapseTestCase.getTestCases().getTestCaseCount();
        log.info(testCaseCount + " Test case(s) ready to execute");
        String currentTestCaseName = null;

        try {
            //execute test cases with synapse configurations and test data
            for (int i = 0; i < testCaseCount; i++) {
                TestCaseSummary testSummary = new TestCaseSummary();
                TestCase currentTestCase = synapseTestCase.getTestCases().getTestCase(i);
                testSummary.setTestCaseName(currentTestCase.getTestCaseName());
                testSuiteSummary.setRecentTestCaseName(currentTestCaseName);
                currentTestCaseName = currentTestCase.getTestCaseName();

                switch (artifactType) {
                    case TYPE_SEQUENCE:
                        Map.Entry<Boolean, MessageContext> mediateResult =
                                TestCasesMediator.sequenceMediate(currentTestCase, synapseConfiguration, key);

                        testSuiteSummary.setMediationStatus(Constants.PASSED_KEY);
                        Boolean mediationResult = mediateResult.getKey();
                        MessageContext resultedMessageContext = mediateResult.getValue();

                        //check whether mediation is success or not
                        checkAssertionWithSequenceMediation
                                (mediationResult, resultedMessageContext, currentTestCase, i, testSummary);
                        break;

                    case TYPE_PROXY:
                        HttpResponse invokedProxyResult = TestCasesMediator
                                .proxyServiceExecutor(currentTestCase, proxyTransportMethod, key);

                        testSuiteSummary.setMediationStatus(Constants.PASSED_KEY);
                        checkAssertionWithProxyMediation(invokedProxyResult, currentTestCase, i, testSummary);
                        break;

                    case TYPE_API:
                        String context = artifactNode.getAttributeValue(new QName(API_CONTEXT));
                        String resourceMethod = currentTestCase.getRequestMethod();
                        HttpResponse invokedApiResult = TestCasesMediator.apiResourceExecutor
                                (currentTestCase, context, resourceMethod);

                        testSuiteSummary.setMediationStatus(Constants.PASSED_KEY);
                        checkAssertionWithAPIMediation(invokedApiResult, currentTestCase, i, testSummary);

                        break;

                    default:
                        break;
                }

                testSuiteSummary.addTestCaseSumamry(testSummary);
            }
        } catch (Exception e) {
            log.error("Error occurred while running test cases", e);
            exception = CommonUtils.stackTraceToString(e);
            testSuiteSummary.setRecentTestCaseName(currentTestCaseName);
            testSuiteSummary.setMediationStatus(Constants.FAILED_KEY);
            testSuiteSummary.setMediationException(exception);
        }
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param mediationResult        result of mediation of sequence
     * @param resultedMessageContext message context of mediation of sequence
     * @param currentTestCase        current running test case data
     */
    private void checkAssertionWithSequenceMediation(
            boolean mediationResult, MessageContext resultedMessageContext, TestCase currentTestCase,
            int index, TestCaseSummary testSummary) {
        String assertMessage;
        if (mediationResult) {
            testSummary.setMediationStatus(Constants.PASSED_KEY);
            Assertor.doAssertionSequence(currentTestCase, resultedMessageContext, index + 1, testSummary);
        } else {
            assertMessage = "Sequence mediation failed";
            log.error(assertMessage);
            testSummary.setMediationStatus(Constants.FAILED_KEY);
            testSummary.setTestException(assertMessage);
        }
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedProxyResult result of proxy invoke
     * @param currentTestCase    current running test case data
     */
    private void checkAssertionWithProxyMediation(HttpResponse invokedProxyResult,
                                                                        TestCase currentTestCase, int index,
                                                                        TestCaseSummary testSummary) {

        String assertMessage;
        if (invokedProxyResult != null) {
            testSummary.setMediationStatus(Constants.PASSED_KEY);
            Assertor.doAssertionService
                    (currentTestCase, invokedProxyResult, index + 1, testSummary);
        } else {
            assertMessage = "Proxy service invoke failed";
            log.error(assertMessage);
            testSummary.setMediationStatus(Constants.FAILED_KEY);
            testSummary.setTestException(assertMessage);
        }
    }

    /**
     * Check assertion results with results of sequence mediation.
     *
     * @param invokedApiResult result of API invoke
     * @param currentTestCase  current running test case data
     */
    private void checkAssertionWithAPIMediation(HttpResponse invokedApiResult,
                                                                      TestCase currentTestCase, int index,
                                                                      TestCaseSummary testSummary) {
        String assertMessage;
        if (invokedApiResult != null) {
            testSummary.setMediationStatus(Constants.PASSED_KEY);
            Assertor.doAssertionService
                    (currentTestCase, invokedApiResult, index + 1, testSummary);
        } else {
            assertMessage = "API resource invoke failed";
            log.error(assertMessage);
            testSummary.setMediationStatus(Constants.FAILED_KEY);
            testSummary.setTestException(assertMessage);
        }
    }

    /**
     * Undeploy all the artifacts used in the recent unit test.
     */
    void artifactUndeployer() {
        try {
            //create a synapse configuration and set all axis2 configuration to it
            SynapseConfiguration synapseConfig = UnitTestingExecutor.getExecuteInstance().getSynapseConfiguration();
            AxisConfiguration axisConfiguration = synapseConfig.getAxisConfiguration();
            ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
            SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(configurationContext, synapseConfig);

            axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
            axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfig));
            configurationContext.setAxisConfiguration(axisConfiguration);

            for (Map.Entry<String, String> unDeployEntry : deploymentStats.entrySet()) {
                String artifactName = unDeployEntry.getKey();
                String unDeployableArtifactType = unDeployEntry.getValue();

                switch (unDeployableArtifactType) {
                    case TYPE_SEQUENCE:
                        SequenceDeployer sequenceDeployer = new SequenceDeployer();
                        sequenceDeployer.init(configurationContext);
                        sequenceDeployer.undeploySynapseArtifact(artifactName);
                        break;

                    case TYPE_PROXY:
                        ProxyServiceDeployer proxyDeployer = new ProxyServiceDeployer();
                        proxyDeployer.init(configurationContext);
                        proxyDeployer.undeploySynapseArtifact(artifactName);
                        break;

                    case TYPE_API:
                        APIDeployer apiDeployer = new APIDeployer();
                        apiDeployer.init(configurationContext);
                        apiDeployer.undeploySynapseArtifact(artifactName);
                        break;

                    case TYPE_ENDPOINT:
                        EndpointDeployer endpointDeployer = new EndpointDeployer();
                        endpointDeployer.init(configurationContext);
                        endpointDeployer.undeploySynapseArtifact(artifactName);
                        break;

                    case TYPE_LOCAL_ENTRY:
                        LocalEntryDeployer localEntryDeployer = new LocalEntryDeployer();
                        localEntryDeployer.init(configurationContext);
                        localEntryDeployer.undeploySynapseArtifact(artifactName);
                        break;

                    default:
                        break;
                }
            }
            log.info("Undeployed all the deployed test and supportive artifacts");

        } catch (AxisFault e) {
            log.error("Error while undeploying the artifacts", e);
        }
    }
}

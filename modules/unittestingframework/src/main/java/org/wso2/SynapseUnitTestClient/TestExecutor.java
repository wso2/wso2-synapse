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
package org.wso2.SynapseUnitTestClient;

import org.apache.log4j.Logger;

/**
 * Main class for initializing the test framework and maintaining the execution flow
 */
public class TestExecutor {

    private static Logger log = Logger.getLogger(TestExecutor.class.getName());

    public static void main(String[] args) {

        String descriptorFilePath = args[0];
        String synapseHost = args[1];
        String port = args[2];

        DescriptorFileReader descriptorFileReader = new DescriptorFileReader();
        TestDataHolder uniTestDataHolder = descriptorFileReader.readArtifactData(descriptorFilePath);
        TCPClient tcpClient = new TCPClient(synapseHost, port);

        String deploymentMessage = MessageFormatUtils.generateDeployMessage(uniTestDataHolder);
        String result = tcpClient.writeData(deploymentMessage);
        String message = MessageFormatUtils.getResultMessage(result);
        log.info(message);

        if (message.equals("Artifact is deployed successfully")) {

            int noOfTestCases = MessageFormatUtils.getNumberOfTestCases(uniTestDataHolder);
            log.info(noOfTestCases);

            for (int i = 1; i <= noOfTestCases; i++) {
                uniTestDataHolder = descriptorFileReader.readTestCaseData(descriptorFilePath, i);
                String testDataMessage = MessageFormatUtils.generateTestDataMessage(uniTestDataHolder);
                log.info("Sending test data:" + testDataMessage);
                String finalResult = MessageFormatUtils.getResultMessage(tcpClient.writeData(testDataMessage));
                log.info("Unit Test Result of test suite" + i + ":" + finalResult);
            }

        } else if (result.equals("Sequence is not deployed")) {
            log.info("Sequence not deployed");
        } else log.info("Deployment result not received:" + message);
        tcpClient.closeResources();
    }
}



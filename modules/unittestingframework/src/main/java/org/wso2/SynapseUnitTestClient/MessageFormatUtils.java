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

import java.util.Base64;

public class MessageFormatUtils {

    /**
     * Util class for encrypting the messages to be sent and decrypting the received messages
     */

    private static final String OPERATION = "operation";
    private static final String DEPLOY = "deploy";
    private static final String EXECUTETEST = "executeTest";
    private static final String ARTIFACT = "artifact";
    private static final String INPUTXMLPAYLOAD = "inputXmlPayload";
    private static final String EXPECTEDPAYLOAD = "expectedPayload";
    private static final String EXPECTEDPROPVAL = "expectedPropVal";
    private static final String FILENAME = "fileName";
    private static final String ARTIFACTTYPE = "artifactType";

    private static Logger log = Logger.getLogger(MessageFormatUtils.class.getName());

    /**
     * Method for encrypting the artifact details and formatting the deployment message to be sent
     *
     * @param unitTestDataHolder
     * @return deployMessage
     */

    public static int getNumberOfTestCases(TestDataHolder unitTestDataHolder){

        return unitTestDataHolder.getNoOfTestCases();
    }

    public static String generateDeployMessage(TestDataHolder unitTestDataHolder) {

        String artifact = unitTestDataHolder.getArtifact();
        String encodedArtifact = Base64.getEncoder().encodeToString(artifact.getBytes());
        String fileName = unitTestDataHolder.getFileName();
        String encodedFileName = Base64.getEncoder().encodeToString(fileName.getBytes());
        String artifactType = unitTestDataHolder.getArtifactType();
        String encodedArtifactType = Base64.getEncoder().encodeToString(artifactType.getBytes());
        String deployMessage = "|" + OPERATION + "-" + DEPLOY + "," + ARTIFACT + "-" + encodedArtifact + "," + FILENAME + "-" + encodedFileName + "," + ARTIFACTTYPE + "-" + encodedArtifactType + ",|";

        return deployMessage;
    }

    /**
     * Method for encrypting the test data to be sent and formatting the test data messagee
     *
     * @param unitTestDataHolder
     * @return testData
     */

    public static String generateTestDataMessage(TestDataHolder unitTestDataHolder) {

        String inputXmlPayload = unitTestDataHolder.getInputXmlPayload();
        String expectedPayload = unitTestDataHolder.getExpectedPayload();
        String expectedPropVal = unitTestDataHolder.getExpectedPropVal();
        String decodedInputXmlPayload = Base64.getEncoder().encodeToString(inputXmlPayload.getBytes());
        String decodedExpectedPayload = Base64.getEncoder().encodeToString(expectedPayload.getBytes());
        String decodedExpectedPropval = Base64.getEncoder().encodeToString(expectedPropVal.getBytes());

        String testData = "|" + OPERATION + "-" + EXECUTETEST + "," + INPUTXMLPAYLOAD + "-" + decodedInputXmlPayload + "," + EXPECTEDPAYLOAD + "-" + decodedExpectedPayload + "," + EXPECTEDPROPVAL + "-" + decodedExpectedPropval + ",|";

        return testData;
    }

    /**
     * Method for extracting the result from the received messages
     *
     * @param result
     * @return
     */

    public static String getResultMessage(String result) {

        String[] parts1 = result.split("-");
        String[] parts2 = parts1[1].split("-");

        return parts2[0];
    }

}

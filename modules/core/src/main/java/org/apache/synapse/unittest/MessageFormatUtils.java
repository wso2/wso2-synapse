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

import org.apache.log4j.Logger;

import java.util.Base64;

/**
 * Util class for encrypting the messages to be sent and decrypting the received messages
 */

public class MessageFormatUtils {

    private static Logger log = Logger.getLogger(MessageFormatUtils.class.getName());

    public static String generateResultMessage(String result) {

        return "|result-" + result + "-|";
    }

    /**
     * Method used for identifying the operation of the received messages
     *
     * @param message
     * @return operation
     */
    public static String getOperation(String message) {

        String[] parts = message.split(",");
        String subString1 = parts[0];
        String[] parts2 = subString1.split("-");
        String operation = parts2[1];
        log.info("get operation" + operation);
        return operation;
    }

    /**
     * Method for extracting the synapse artifact to be deployed from the received messages
     *
     * @param message
     * @return decodedArtifact
     */

    public static String[] getDeploymentData(String message) {

        String[] parts = message.split(",");
        String subString1 = parts[1];
        String subString2 = parts[2];
        String subString3 = parts[3];
        String[] parts1 = subString1.split("-");
        String[] parts2 = subString2.split("-");
        String[] parts3 = subString3.split("-");
        String encodedArtifact = parts1[1];
        String encodedFileName = parts2[1];
        String encodedArtifactType = parts3[1];
        byte[] decodedBytesArtifact = Base64.getDecoder().decode(encodedArtifact);
        String decodedArtifact = new String(decodedBytesArtifact);
        byte[] decodedBytesFileName = Base64.getDecoder().decode(encodedFileName);
        String decodedFileName = new String(decodedBytesFileName);
        byte[] decodedBytesArtifactType = Base64.getDecoder().decode(encodedArtifactType);
        String decodedArtifactType = new String(decodedBytesArtifactType);

        return new String[]{decodedArtifact, decodedFileName, decodedArtifactType};
    }

    /**
     * Method for extracting test data from the received messages
     *
     * @param message
     * @return decodedInputXmlPayload, decodedExpectedPayload, decodedExpectedPropVal
     */
    public static String[] getTestData(String message) {

        String[] parts = message.split(",");
        String subString1 = parts[1];
        String subString2 = parts[2];
        String subString3 = parts[3];
        String[] parts1 = subString1.split("-");
        String[] parts2 = subString2.split("-");
        String[] parts3 = subString3.split("-");
        String encodedInputXmlPayload = parts1[1];
        String encodedExpectedPayload = parts2[1];
        String encodedExpectedPropVal = parts3[1];

        byte[] decodedBytesInputXmlPayload = Base64.getDecoder().decode(encodedInputXmlPayload);
        String decodedInputXmlPayload = new String(decodedBytesInputXmlPayload);
        log.info("Decoded message:" + decodedInputXmlPayload);
        byte[] decodedBytesExpectedpayload = Base64.getDecoder().decode(encodedExpectedPayload);
        String decodedExpectedPayload = new String(decodedBytesExpectedpayload);
        log.info("Decoded message:" + decodedExpectedPayload);
        byte[] decodedBytesExpectedPropVal = Base64.getDecoder().decode(encodedExpectedPropVal);
        String decodedExpectedPropVal = new String(decodedBytesExpectedPropVal);
        log.info("Decoded message:" + decodedExpectedPropVal);

        return new String[]{decodedInputXmlPayload, decodedExpectedPayload, decodedExpectedPropVal};

    }

}

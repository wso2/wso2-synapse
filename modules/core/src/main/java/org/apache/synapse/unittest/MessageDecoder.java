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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.apache.synapse.unittest.Constants.*;

/**
 * Class responsible for the decode relevant data from received message.
 */
public class MessageDecoder {

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    /**
     * Get artifact type from the received message from unit testing client
     *
     * @param inputMessage received JSON message from unit testing client
     * @return artifact type
     */
    public static String getArtifactType(JSONObject inputMessage) {
        String artifactType = null;

        try {
            artifactType = inputMessage.getString(ARTIFACT_TYPE);
        } catch (Exception e) {
            logger.error(e);
        }

        return artifactType;
    }

    /**
     * Get artifact from the received message from unit testing client
     *
     * @param inputMessage received JSON message from unit testing client
     * @return artifact
     */
    public static OMElement getConfigurationArtifact(JSONObject inputMessage) {
        OMElement artifact = null;

        try {
            String artifactAsString = inputMessage.getString(ARTIFACT);
            artifact = AXIOMUtil.stringToOM(artifactAsString);
        } catch (Exception e) {
            logger.error(e);
        }

        return artifact;
    }

    /**
     * Get artifact name from the received message from unit testing client
     *
     * @param inputMessage received JSON message from unit testing client
     * @return artifact name
     */
    public static String getArtifactName(JSONObject inputMessage) {
        String artifactName = null;

        try {
            artifactName = inputMessage.getString(ARTIFACT_NAME);

        } catch (Exception e) {
            logger.error(e);
        }

        return artifactName;
    }

    /**
     * Get test cases count from the received message from unit testing client
     *
     * @param inputMessage received JSON message from unit testing client
     * @return test cases count
     */
    public static int getTestCasesCount(JSONObject inputMessage) {
        int testCasesCount = 0;

        try {
            testCasesCount = inputMessage.getInt(TEST_CASES_COUNT);

        } catch (Exception e) {
            logger.error(e);
        }

        return testCasesCount;
    }

    /**
     * Get test cases data from the received message from unit testing client
     *
     * @param inputMessage received JSON message from unit testing client
     * @return test cases data as ArrayList of ArrayList
     */
    public static ArrayList<ArrayList<String>> getTestCasesData(JSONObject inputMessage) {
        ArrayList<ArrayList<String>> testCasesData = new ArrayList<ArrayList<String>>();
        JSONArray testCases = inputMessage.getJSONArray(TEST_CASES);

        try {
            for (int x = 0; x < testCases.length(); x++) {
                ArrayList<String> testCaseData = new ArrayList<String>();

                testCaseData.add(testCases.getJSONObject(x).getString(INPUT_XML_PAYLOAD));
                testCaseData.add(testCases.getJSONObject(x).getString(EXPECTED_PAYLOAD));
                testCaseData.add(testCases.getJSONObject(x).getString(EXPECTED_PROPERTY_VALUES));

                testCasesData.add(testCaseData);
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return testCasesData;
    }
}

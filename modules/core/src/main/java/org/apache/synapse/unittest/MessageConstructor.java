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

import org.apache.log4j.Logger;
import org.apache.synapse.unittest.data.holders.ArtifactData;
import org.apache.synapse.unittest.data.holders.MockServiceData;
import org.apache.synapse.unittest.data.holders.TestCaseData;

import org.json.JSONObject;

import static org.apache.synapse.unittest.Constants.ARTIFACT;
import static org.apache.synapse.unittest.Constants.ARTIFACT_NAME;
import static org.apache.synapse.unittest.Constants.ARTIFACT_TYPE;

/**
 * Class of the message constructor for Synapse unit test framework.
 * Create deployable JSON object from data holders
 * Update deployable JSON object as Config Modifier
 */
class MessageConstructor {

    private static Logger logger = Logger.getLogger(MessageDecoder.class.getName());
    private MessageConstructor(){}
    /**
     * Read artifact data from the artifactDataHolder.
     * Append artifact data into the JSON object
     *
     * @param artifactDataHolder object of ArtifactData which contains artifact data read from descriptor data
     * @return JSONObject which is ready to deploy via TCP server
     */
     static JSONObject generateDeployableMessage(ArtifactData artifactDataHolder, TestCaseData testCaseDataHolder,
                                            MockServiceData mockServiceData) {

        JSONConstructor jsonDataHolder = new JSONConstructor();
        String configuredArtifact;

        //configure the artifact if there are mock-services to append
        if (mockServiceData.getMockServicesCount() > 0) {
            configuredArtifact = ConfigModifier.endPointModifier(artifactDataHolder.getTestArtifact(), mockServiceData);

        } else {
            configuredArtifact = artifactDataHolder.getTestArtifact();
        }

        if (configuredArtifact != null) {
            try {
                jsonDataHolder.initialize();

                //Add artifact data from data holder to json object
                jsonDataHolder.setAttribute(ARTIFACT , configuredArtifact);
                jsonDataHolder.setAttribute(ARTIFACT_TYPE , artifactDataHolder.getTestArtifactType());
                jsonDataHolder.setAttribute(ARTIFACT_NAME , artifactDataHolder.getTestArtifactNameOrKey());
                jsonDataHolder.setAttribute(testCaseDataHolder.getTestCaseCount());

                //Add  test-case data from data holder to json object
                JSONConstructor jsonTestCaseDataHolderArray = new JSONConstructor();
                jsonTestCaseDataHolderArray.initializeArray();

                for (int i = 0; i < testCaseDataHolder.getTestCaseCount(); i++) {
                    JSONConstructor jsonTestCaseDataHolder = new JSONConstructor();
                    jsonTestCaseDataHolder.initialize();

//                    jsonTestCaseDataHolder.setAttribute(INPUT_PAYLOAD, testCaseDataHolder.getInputXmlPayload(i));
//                    jsonTestCaseDataHolder.setAttribute(ASSERT_EXPECTED_PROPERTIES,
//                            testCaseDataHolder.getExpectedPropertyValues(i));
//                    jsonTestCaseDataHolder.setAttribute(ASSERT_EXPECTED_PAYLOAD, testCaseDataHolder.getExpectedPayload(i));

                    //Add test-case attributes to JSON array
                    jsonTestCaseDataHolderArray.setAttributeForArray(jsonTestCaseDataHolder.getJSONDataHolder());
                }

                jsonDataHolder.setAttribute(jsonTestCaseDataHolderArray.getJSONArrayDataHolder());

                logger.info("Deployable JSON artifact data object created");

            } catch (Exception e) {
                logger.error(e);
            }

        } else {
            return null;
        }

        return jsonDataHolder.getJSONDataHolder();
    }
}

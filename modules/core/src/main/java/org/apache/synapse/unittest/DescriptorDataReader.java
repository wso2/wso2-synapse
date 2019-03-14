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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.data.holders.ArtifactData;
import org.apache.synapse.unittest.data.holders.MockServiceData;
import org.apache.synapse.unittest.data.holders.TestCaseData;

import javax.xml.namespace.QName;
import java.io.File;

import static org.apache.synapse.unittest.Constants.TEST_CASES_COUNT;
import static org.apache.synapse.unittest.Constants.ARTIFACT;
import static org.apache.synapse.unittest.Constants.ARTIFACT_FILE;
import static org.apache.synapse.unittest.Constants.ARTIFACT_TYPE;
import static org.apache.synapse.unittest.Constants.ARTIFACT_NAME_ATTRIBUTE;
import static org.apache.synapse.unittest.Constants.TEST_CASES;
import static org.apache.synapse.unittest.Constants.TEST_CASE;
import static org.apache.synapse.unittest.Constants.INPUT_XML_PAYLOAD;
import static org.apache.synapse.unittest.Constants.EXPECTED_PAYLOAD;
import static org.apache.synapse.unittest.Constants.EXPECTED_PROPERTY_VALUES;
import static org.apache.synapse.unittest.Constants.MOCK_SERVICE_COUNT;
import static org.apache.synapse.unittest.Constants.MOCK_SERVICES;
import static org.apache.synapse.unittest.Constants.MOCK_SERVICE;
import static org.apache.synapse.unittest.Constants.SERVICE_NAME;
import static org.apache.synapse.unittest.Constants.SERVICE_HOST;
import static org.apache.synapse.unittest.Constants.SERVICE_PORT;
import static org.apache.synapse.unittest.Constants.SERVICE_PATH;
import static org.apache.synapse.unittest.Constants.SERVICE_TYPE;
import static org.apache.synapse.unittest.Constants.SERVICE_PAYLOAD;
import static org.apache.synapse.unittest.Constants.SERVICE_RESPONSE;

/**
 * Descriptor file read class in unit test framework.
 */
public class DescriptorDataReader {

    private static Logger logger = Logger.getLogger(DescriptorDataReader.class.getName());
    private OMElement importXMLFile = null;

    /**
     * Constructor of the DescriptorDataReader class.
     * @param descriptorData defines the descriptor data of the received message
     */
    DescriptorDataReader(String descriptorData) {
        try {
            this.importXMLFile = AXIOMUtil.stringToOM(descriptorData);

        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * Read artifact data from the descriptor file.
     * Append artifact data into the data holder object
     *
     * @return dataHolder object with artifact data
     */
    ArtifactData readArtifactData() throws NullPointerException {
        ArtifactData artifactDataHolder = new ArtifactData();

        //Read test case count from descriptor file
        QName qualifiedTestCaseCount = new QName("", TEST_CASES_COUNT, "");
        OMElement testCaseCountNode = importXMLFile.getFirstChildWithName(qualifiedTestCaseCount);
        int testCasesCount = Integer.parseInt(testCaseCountNode.getText());
        artifactDataHolder.setTestCaseCount(testCasesCount);

        //Read artifact from descriptor file
        QName qualifiedArtifact = new QName("", ARTIFACT, "");
        OMElement artifactNode = importXMLFile.getFirstChildWithName(qualifiedArtifact);

        //check if artifact or artifact-file available in descriptor file
        if (artifactNode == null) {

            QName qualifiedArtifactFile = new QName("", ARTIFACT_FILE, "");
            OMElement artifactFileNode = importXMLFile.getFirstChildWithName(qualifiedArtifactFile);
            String artifactFilePath = artifactFileNode.getText();

            try {
                String artifactFileAsString = FileUtils.readFileToString(new File(artifactFilePath));
                artifactNode = AXIOMUtil.stringToOM(artifactFileAsString);

                if (artifactNode == null) {
                    throw new NullPointerException();
                }
            } catch (Exception e) {
                logger.error("Artifact file reading failed", e);
            }
        }

        String artifact = artifactNode.getFirstElement().toString();
        artifactDataHolder.setArtifact(artifact);

        //Read artifact type from descriptor file
        QName qualifiedArtifactType = new QName("", ARTIFACT_TYPE, "");
        OMElement artifactTypeNode = importXMLFile.getFirstChildWithName(qualifiedArtifactType);
        String artifactType = (artifactTypeNode.getText());
        artifactDataHolder.setArtifactType(artifactType);

        //Read artifact name from descriptor file
        String artifactName = artifactNode.getFirstElement().getAttributeValue(new QName(ARTIFACT_NAME_ATTRIBUTE));
        artifactDataHolder.setArtifactName(artifactName);

        logger.info("Artifact data from descriptor file read successfully");
        return artifactDataHolder;
    }

    /**
     * Read test-case data from the descriptor file.
     * Append test-case data into the test data holder object
     *
     * @return testCaseDataHolder object with test case data
     */
    TestCaseData readTestCaseData() throws NullPointerException {

        TestCaseData testCaseDataHolder = new TestCaseData();

        //Read test case count from descriptor file
        QName qualifiedTestCaseCount = new QName("", TEST_CASES_COUNT, "");
        OMElement testCaseCountNode = importXMLFile.getFirstChildWithName(qualifiedTestCaseCount);
        int testCasesCount = Integer.parseInt(testCaseCountNode.getText());

        //Read test cases from descriptor file
        QName qualifiedTestCases = new QName("", TEST_CASES, "");
        OMElement testCasesNode = importXMLFile.getFirstChildWithName(qualifiedTestCases);

        //Iterate through test-cases in descriptor file
        for (int i = 0; i < testCasesCount; i++) {
            //Read test case from test-cases parent
            QName qualifiedTestCase = new QName("", TEST_CASE + "-" + (i + 1), "");
            OMElement testCaseNode = testCasesNode.getFirstChildWithName(qualifiedTestCase);

            //Read input-xml-payload child attribute from test-case node
            QName qualifiedInputXMLPayload = new QName("", INPUT_XML_PAYLOAD, "");
            OMElement inputXMLPayloadNode = testCaseNode.getFirstChildWithName(qualifiedInputXMLPayload);
            String inputXMLPayload = inputXMLPayloadNode.getText();
            testCaseDataHolder.addInputXmlPayload(inputXMLPayload);

            //Read expected-payload child attribute from test-case node
            QName qualifiedExpectedPayload = new QName("", EXPECTED_PAYLOAD, "");
            OMElement expectedPayloadNode = testCaseNode.getFirstChildWithName(qualifiedExpectedPayload);
            String expectedPayload = expectedPayloadNode.getText();
            testCaseDataHolder.addExpectedPayload(expectedPayload);

            //Read expected-property-values child attribute from test-case node
            QName qualifiedExpectedPropertyValues = new QName("", EXPECTED_PROPERTY_VALUES, "");
            OMElement expectedPropertyValuesNode = testCaseNode
                    .getFirstChildWithName(qualifiedExpectedPropertyValues);
            String expectedPropertyValues = expectedPropertyValuesNode.getText();
            testCaseDataHolder.addExpectedPropertyValues(expectedPropertyValues);
        }

        logger.info("Test case data from descriptor file read successfully");
        return testCaseDataHolder;
    }

    /**
     * Read mock-service data from the descriptor file.
     * Append mock-service data into the test data holder object
     *
     * @return mockServiceDataHolder object with test case data
     */
    MockServiceData readMockServiceData() throws NullPointerException {

        MockServiceData mockServiceDataHolder = new MockServiceData();

        //Read mock-services count from descriptor file
        QName qualifiedMockServiceCount = new QName("", MOCK_SERVICE_COUNT, "");
        OMElement mockServiceCountNode = importXMLFile.getFirstChildWithName(qualifiedMockServiceCount);
        int mockServiceCount = Integer.parseInt(mockServiceCountNode.getText());
        mockServiceDataHolder.setMockServicesCount(mockServiceCount);

        //Read test cases from descriptor file
        QName qualifiedMockServices = new QName("", MOCK_SERVICES, "");
        OMElement mockServicesNode = importXMLFile.getFirstChildWithName(qualifiedMockServices);

        //Iterate through mock-service in descriptor file
        for (int i = 0; i < mockServiceCount; i++) {
            //Read mock-service from mock services parent
            QName qualifiedMockService = new QName("", MOCK_SERVICE + "-" + (i + 1), "");
            OMElement mockServiceNode = mockServicesNode.getFirstChildWithName(qualifiedMockService);

            //Read service name child attribute from mock service node
            QName qualifiedServiceName = new QName("", SERVICE_NAME, "");
            OMElement serviceNameNode = mockServiceNode.getFirstChildWithName(qualifiedServiceName);
            String serviceName = serviceNameNode.getText();
            mockServiceDataHolder.addServiceName(serviceName, i);

            //Read service host child attribute from mock service node
            QName qualifiedServiceHost = new QName("", SERVICE_HOST, "");
            OMElement serviceHostNode = mockServiceNode.getFirstChildWithName(qualifiedServiceHost);
            String serviceHost = serviceHostNode.getText();
            mockServiceDataHolder.addServiceHost(serviceHost);

            //Read service port child attribute from mock service node
            QName qualifiedServicePort = new QName("", SERVICE_PORT, "");
            OMElement servicePortNode = mockServiceNode.getFirstChildWithName(qualifiedServicePort);
            int servicePort = Integer.parseInt(servicePortNode.getText());
            mockServiceDataHolder.addServicePort(servicePort);

            //Read service path child attribute from mock service node
            QName qualifiedServicePath = new QName("", SERVICE_PATH, "");
            OMElement servicePathNode = mockServiceNode.getFirstChildWithName(qualifiedServicePath);
            String servicePath = servicePathNode.getText();
            mockServiceDataHolder.addServicePath(servicePath);

            //Read service type child attribute from mock service node
            QName qualifiedServiceType = new QName("", SERVICE_TYPE, "");
            OMElement serviceTypeNode = mockServiceNode.getFirstChildWithName(qualifiedServiceType);
            String serviceType = serviceTypeNode.getText();
            mockServiceDataHolder.addServiceType(serviceType);

            //Read service input payload child attribute from mock service node
            QName qualifiedServicePayload = new QName("", SERVICE_PAYLOAD, "");
            OMElement servicePayloadeNode = mockServiceNode.getFirstChildWithName(qualifiedServicePayload);
            String servicePayload = servicePayloadeNode.getText();
            mockServiceDataHolder.addServicePayload(servicePayload);

            //Read service response child attribute from mock service node
            QName qualifiedServiceResponse = new QName("", SERVICE_RESPONSE, "");
            OMElement serviceResponseNode = mockServiceNode.getFirstChildWithName(qualifiedServiceResponse);
            String serviceResponse = serviceResponseNode.getText();
            mockServiceDataHolder.addServiceResponse(serviceResponse);
        }

        logger.info("Mock service data from descriptor file read successfully");
        return mockServiceDataHolder;
    }
}

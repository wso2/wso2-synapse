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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.data.holders.ArtifactData;
import org.apache.synapse.unittest.data.holders.MockServiceData;
import org.apache.synapse.unittest.data.holders.TestCaseData;

import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.namespace.QName;

import static org.apache.synapse.unittest.Constants.*;

/**
 * descriptor data read class in unit test framework.
 */
class SynapseTestcaseDataReader {

    private static Logger logger = Logger.getLogger(SynapseTestcaseDataReader.class.getName());
    private OMElement importXMLFile = null;

    /**
     * Constructor of the SynapseTestcaseDataReader class.
     * @param descriptorData defines the descriptor data of the received message
     */
    SynapseTestcaseDataReader(String descriptorData) {
        try {
            this.importXMLFile = AXIOMUtil.stringToOM(descriptorData);

        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * Read artifact data from the descriptor data.
     * Append artifact data into the data holder object
     *
     * @return dataHolder object with artifact data
     */
    ArtifactData readArtifactData() {
        ArtifactData artifactDataHolder = new ArtifactData();

        //Read artifact from descriptor data
        QName qualifiedArtifacts = new QName("", ARTIFACTS, "");
        OMElement artifactNode = importXMLFile.getFirstChildWithName(qualifiedArtifacts);

        QName qualifiedTestArtifact = new QName("", TEST_ARTIFACT, "");
        OMElement testArtifactNode = artifactNode.getFirstChildWithName(qualifiedTestArtifact);

        QName qualifiedArtifact = new QName("", ARTIFACT, "");
        OMElement testArtifactDataNode = testArtifactNode.getFirstChildWithName(qualifiedArtifact);
        String testArtifactData = testArtifactDataNode.getFirstElement().toString();
        artifactDataHolder.setTestArtifact(testArtifactData);

        //Read test artifact type from synapse test data
        String testArtifactType = testArtifactDataNode.getFirstElement().getLocalName();
        artifactDataHolder.setTestArtifactType(testArtifactType);

        //Read artifact name from descriptor data
        String testArtifactNameOrKey;
        if (testArtifactType.equals(TYPE_LOCAL_ENTRY)) {
            testArtifactNameOrKey
                    = testArtifactDataNode.getFirstElement().getAttributeValue(new QName(ARTIFACT_KEY_ATTRIBUTE));
        } else {
            testArtifactNameOrKey
                    = testArtifactDataNode.getFirstElement().getAttributeValue(new QName(ARTIFACT_NAME_ATTRIBUTE));
        }
        artifactDataHolder.setTestArtifactNameOrKey(testArtifactNameOrKey);

        //Read supportive test cases data
        QName qualifiedSupportiveTestArtifact = new QName("", SUPPORTIVE_ARTIFACTS, "");
        OMElement supportiveArtifactsNode = artifactNode.getFirstChildWithName(qualifiedSupportiveTestArtifact);

        Iterator artifactIterator = supportiveArtifactsNode.getChildElements();
        while (artifactIterator.hasNext()) {
            OMElement artifact = (OMElement) artifactIterator.next();

            //Read supportive artifact from synapse test data
            String supportiveArtifactData = artifact.getFirstElement().toString();
            artifactDataHolder.addSupportiveArtifact(supportiveArtifactData);

            //Read supportive artifact type from synapse test data
            String supportiveArtifactType = artifact.getFirstElement().getLocalName();
            artifactDataHolder.addSupportiveArtifactType(supportiveArtifactType);

            //Read artifact name from descriptor data
            String supportiveArtifactNameOrKey;
            if (testArtifactType.equals(TYPE_LOCAL_ENTRY)) {
                supportiveArtifactNameOrKey
                        = artifact.getFirstElement().getAttributeValue(new QName(ARTIFACT_KEY_ATTRIBUTE));
            } else {
                supportiveArtifactNameOrKey
                        = artifact.getFirstElement().getAttributeValue(new QName(ARTIFACT_NAME_ATTRIBUTE));
            }
            artifactDataHolder.addSupportiveArtifactNameOrKey(supportiveArtifactNameOrKey);

        }

        logger.info("Artifact data from descriptor data read successfully");
        return artifactDataHolder;
    }

    /**
     * Read test-case data from the descriptor data.
     * Append test-case data into the test data holder object
     *
     * @return testCaseDataHolder object with test case data
     */
    TestCaseData readTestCaseData() {

        TestCaseData testCaseDataHolder = new TestCaseData();

        //Set test case count as zero
        int testCasesCount = 0;

        //Read test cases from descriptor data
        QName qualifiedTestCases = new QName("", TEST_CASES, "");
        OMElement testCasesNode = importXMLFile.getFirstChildWithName(qualifiedTestCases);

        if (testCasesNode != null) {
            //Iterate through test-cases in descriptor data
            Iterator<?> iterator = testCasesNode.getChildElements();

            while (iterator.hasNext()) {
                OMElement testCaseNode = (OMElement) (iterator.next());

                //Read input-xml-payload child attribute from test-case node
                QName qualifiedInputXMLPayload = new QName("", INPUT_PAYLOAD, "");
                OMElement inputXMLPayloadNode = testCaseNode.getFirstChildWithName(qualifiedInputXMLPayload);
                String inputXMLPayload = inputXMLPayloadNode.getText();
                testCaseDataHolder.addInputXmlPayload(inputXMLPayload);

                //Read assertion tag from test-case node
                QName qualifiedAssertion = new QName("", ASSERTION, "");
                OMElement assertionNode = testCaseNode.getFirstChildWithName(qualifiedAssertion);

                //Read expected-payload child attribute from test-case node
                QName qualifiedExpectedPayload = new QName("", ASSERT_EXPECTED_PAYLOAD, "");
                OMElement expectedPayloadNode = assertionNode.getFirstChildWithName(qualifiedExpectedPayload);
                String expectedPayload = expectedPayloadNode.getText();
                testCaseDataHolder.addExpectedPayload(expectedPayload);

                //Read expected-property-values child attribute from test-case node
                QName qualifiedExpectedPropertyValues = new QName("", ASSERT_EXPECTED_PROPERTIES, "");
                OMElement expectedPropertyValuesNode = assertionNode
                        .getFirstChildWithName(qualifiedExpectedPropertyValues);
                String expectedPropertyValues = expectedPropertyValuesNode.getText();
                testCaseDataHolder.addExpectedPropertyValues(expectedPropertyValues);

                testCasesCount++;
            }
        }

        //Set test case count in test data holder
        testCaseDataHolder.setTestCaseCount(testCasesCount);

        logger.info("Test case data from descriptor data read successfully");
        return testCaseDataHolder;
    }

    /**
     * Read mock-service data from the descriptor data.
     * Append mock-service data into the test data holder object
     *
     * @return mockServiceDataHolder object with test case data
     */
    MockServiceData readMockServiceData() {

        MockServiceData mockServiceDataHolder = new MockServiceData();

        //Set mock service count as zero
        int mockServiceCount = 0;

        //Read mock services from descriptor data
        QName qualifiedMockServices = new QName("", MOCK_SERVICES, "");
        OMElement mockServicesNode = importXMLFile.getFirstChildWithName(qualifiedMockServices);

        //check whether descriptor data has mock services
        if (mockServicesNode != null) {
            //Iterate through mock-service in descriptor data
            Iterator<?> iterator = mockServicesNode.getChildElements();

            while (iterator.hasNext()) {
                OMElement mockServiceNode = (OMElement) (iterator.next());

                //Read service name child attribute from mock service node
                QName qualifiedServiceName = new QName("", SERVICE_NAME, "");
                OMElement serviceNameNode = mockServiceNode.getFirstChildWithName(qualifiedServiceName);
                String serviceName = serviceNameNode.getText();
                mockServiceDataHolder.addServiceName(serviceName, mockServiceCount);

                //Read service port child attribute from mock service node
                QName qualifiedServicePort = new QName("", SERVICE_PORT, "");
                OMElement servicePortNode = mockServiceNode.getFirstChildWithName(qualifiedServicePort);
                int servicePort = Integer.parseInt(servicePortNode.getText());
                mockServiceDataHolder.addServicePort(servicePort);

                //Read service path child attribute from mock service node
                QName qualifiedServicePath = new QName("", SERVICE_CONTEXT, "");
                OMElement servicePathNode = mockServiceNode.getFirstChildWithName(qualifiedServicePath);
                String servicePath = servicePathNode.getText();
                mockServiceDataHolder.addServiceContext(servicePath);

                //Read resource of the mock service
                QName qualifiedServiceResource = new QName("", SERVICE_RESOURCE, "");
                OMElement serviceResourceNode = mockServiceNode.getFirstChildWithName(qualifiedServiceResource);

                //Read service type child attribute from mock service node
                QName qualifiedServiceMethod = new QName("", SERVICE_RESOURCE_METHOD, "");
                OMElement serviceMethodNode = serviceResourceNode.getFirstChildWithName(qualifiedServiceMethod);
                String serviceMethod = serviceMethodNode.getText();
                mockServiceDataHolder.addServiceType(serviceMethod);

                //Read service request data of payload and headers
                readMockServicesRequest(serviceResourceNode,mockServiceDataHolder);

                //Read service response data of payload and headers
                readMockServicesResponse(serviceResourceNode,mockServiceDataHolder);

                mockServiceCount++;
            }
        }

        //Set mock service count in mock service data holder
        mockServiceDataHolder.setMockServicesCount(mockServiceCount);

        logger.info("Mock service data from descriptor data read successfully");
        return mockServiceDataHolder;
    }

    /**
     * Read mock service request
     *
     * @param mockServiceDataHolder object with test case data
     * @param serviceResourceNode OMElement of resource node
     */
    private void readMockServicesRequest(OMElement serviceResourceNode, MockServiceData mockServiceDataHolder) {
        QName qualifiedServiceRequest = new QName("", SERVICE_RESOURCE_REQUEST, "");
        OMElement serviceRequestNode = serviceResourceNode.getFirstChildWithName(qualifiedServiceRequest);

        if (serviceRequestNode != null) {
            QName qualifiedServiceRequestPayload =
                    new QName("", SERVICE_RESOURCE_PAYLOAD, "");
            OMElement serviceRequestPayloadNode =
                    serviceRequestNode.getFirstChildWithName(qualifiedServiceRequestPayload);

            if (serviceRequestPayloadNode != null) {
                String serviceRequestPayload = serviceRequestPayloadNode.getText();
                mockServiceDataHolder.addServiceRequestPayload(serviceRequestPayload);
            }

            QName qualifiedServiceRequestHeaders =
                    new QName("", SERVICE_RESOURCE_HEADERS, "");
            OMElement serviceRequestHeaders =
                    serviceRequestNode.getFirstChildWithName(qualifiedServiceRequestHeaders);

            if (serviceRequestHeaders != null) {
                Iterator<?> iterateHeaders = serviceRequestHeaders.getChildElements();
                ArrayList<Pair<String, String>> headers = new ArrayList<>();

                while (iterateHeaders.hasNext()) {
                    OMElement mockServiceRequestHeader = (OMElement) (iterateHeaders.next());
                    String headerName =
                            mockServiceRequestHeader.getAttributeValue(new QName(SERVICE_RESOURCE_HEADER_NAME));
                    String headerValue =
                            mockServiceRequestHeader.getAttributeValue(new QName(SERVICE_RESOURCE_HEADER_VALUE));

                    headers.add(new Pair<>(headerName, headerValue));
                }

                mockServiceDataHolder.addServiceRequestHeaders(headers);
            }
        }
    }

    /**
     * Read mock service response
     *
     * @param mockServiceDataHolder object with test case data
     * @param serviceResourceNode OMElement of resource node
     */
    private void readMockServicesResponse(OMElement serviceResourceNode, MockServiceData mockServiceDataHolder) {
        QName qualifiedServiceResponse = new QName("", SERVICE_RESOURCE_RESPONSE, "");
        OMElement serviceResponseNode = serviceResourceNode.getFirstChildWithName(qualifiedServiceResponse);

        if (serviceResponseNode != null) {
            QName qualifiedServiceResponsePayload =
                    new QName("", SERVICE_RESOURCE_PAYLOAD, "");
            OMElement serviceResponsePayloadNode =
                    serviceResponseNode.getFirstChildWithName(qualifiedServiceResponsePayload);

            if (serviceResponsePayloadNode != null) {
                String serviceResponsePayload = serviceResponsePayloadNode.getText();
                mockServiceDataHolder.addServiceResponsePayload(serviceResponsePayload);
            }

            QName qualifiedServiceResponseHeaders =
                    new QName("", SERVICE_RESOURCE_HEADERS, "");
            OMElement serviceResponseHeaders =
                    serviceResponseNode.getFirstChildWithName(qualifiedServiceResponseHeaders);

            if (serviceResponseHeaders != null) {
                Iterator<?> iterateHeaders = serviceResponseHeaders.getChildElements();
                ArrayList<Pair<String, String>> headers = new ArrayList<>();

                while (iterateHeaders.hasNext()) {
                    OMElement mockServiceResponseHeader = (OMElement) (iterateHeaders.next());
                    String headerName =
                            mockServiceResponseHeader.getAttributeValue(new QName(SERVICE_RESOURCE_HEADER_NAME));
                    String headerValue =
                            mockServiceResponseHeader.getAttributeValue(new QName(SERVICE_RESOURCE_HEADER_VALUE));

                    headers.add(new Pair<>(headerName, headerValue));
                }

                mockServiceDataHolder.addServiceResponseHeaders(headers);
            }
        }
    }
}

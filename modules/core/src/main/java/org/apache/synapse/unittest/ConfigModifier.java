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

import org.apache.log4j.Logger;

import org.apache.synapse.unittest.testcase.data.classes.Artifact;
import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.MockServiceData;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.apache.synapse.unittest.Constants.END_POINT;
import static org.apache.synapse.unittest.Constants.HTTP;
import static org.apache.synapse.unittest.Constants.NAME_ATTRIBUTE;
import static org.apache.synapse.unittest.Constants.SERVICE_HOST;
import static org.apache.synapse.unittest.Constants.URI;
import static org.apache.synapse.unittest.Constants.URI_TEMPLATE;


/**
 * Class responsible for modify the endpoint configuration data if mock-service data exists.
 * creates mock services and start those as in descriptor data.
 */
class ConfigModifier {

    private ConfigModifier() {
    }

    private static Logger log = Logger.getLogger(ConfigModifier.class.getName());

    private static final String EMPTY_XMLNS = "xmlns=\"\"";
    private static final String NULL_XMLNS = "xmlns=\"NULL\"";

    /**
     * Method parse the artifact data received and replaces actual endpoint urls with mock urls.
     * Call mock service creator with relevant mock service data.
     * Thread waits until all mock services are starts by checking port availability
     *
     * @param artifactData    artifact data received from descriptor data
     * @param mockServiceData mock service data received from descriptor data
     * @return return a exception of error occurred while creating mock services
     */
    static String endPointModifier(ArtifactData artifactData, MockServiceData mockServiceData) {
        ArrayList<Integer> mockServicePorts = new ArrayList<>();
        ArrayList<Artifact> allArtifacts = new ArrayList<>();
        allArtifacts.add(artifactData.getTestArtifact());

        for (int x = 0; x < artifactData.getSupportiveArtifactCount(); x++) {
            allArtifacts.add(artifactData.getSupportiveArtifact(x));
        }

        for (Artifact artifact : allArtifacts) {
            try {
                String artifactNode = artifact.getArtifact().toString();
                artifactNode = artifactNode.replaceAll(EMPTY_XMLNS, NULL_XMLNS);
                //Build document using artifact data to parse the XML
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document document = docBuilder
                        .parse(new InputSource(new StringReader(artifactNode)));

                //Find relevant endpoint and update actual one. Start the mock service
                Document parsedDocument =
                        configureEndpointsAndStartService(document, mockServiceData, mockServicePorts);

                //Transform the document to the string and store it in artifact data holder
                TransformerFactory tf = TransformerFactory.newInstance();
                tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(parsedDocument), new StreamResult(writer));
                artifact.setArtifact(writer.getBuffer().toString().replaceAll(EMPTY_XMLNS, "")
                        .replaceAll(NULL_XMLNS, EMPTY_XMLNS));

                //check services are ready to serve by checking the ports
                if (!mockServicePorts.isEmpty()) {
                    checkServiceStatus(mockServicePorts);
                }

            } catch (Exception e) {
                String errorMessage = "Error while creating mock service for " + artifact.getArtifactNameOrKey();
                log.error(errorMessage , e);
                return CommonUtils.stackTraceToString(e, errorMessage);
            }
        }
        return null;
    }


    /**
     * Check endpoints in given artifacts.
     * call updateEndPoint method to replace actual with mock URL
     *
     * @param document         document of artifacts
     * @param mockServiceData  mock service data holder
     * @param mockServicePorts mock service port array
     */
    private static Document configureEndpointsAndStartService(Document document, MockServiceData mockServiceData,
                                                          ArrayList<Integer> mockServicePorts) {
        NodeList xmlElementNodes = document.getElementsByTagName("*");

        for (int i = 0; i < xmlElementNodes.getLength(); i++) {
            Node endPointNode = xmlElementNodes.item(i);

            if (endPointNode.getNodeName().equals(END_POINT)) {

                NamedNodeMap attributeListOfEndPoint = endPointNode.getAttributes();

                String valueOfName;
                if (attributeListOfEndPoint.getNamedItem(NAME_ATTRIBUTE) != null) {
                    valueOfName = attributeListOfEndPoint.getNamedItem(NAME_ATTRIBUTE).getNodeValue();
                } else {
                    continue;
                }

                //check service name is exists in mock service data holder map
                boolean isServiceExists = mockServiceData.isServiceNameExist(valueOfName);

                if (isServiceExists) {
                    int serviceElementIndex = mockServiceData.getServiceNameIndex(valueOfName);
                    int port = mockServiceData.getMockServices(serviceElementIndex).getPort();
                    String context = mockServiceData.getMockServices(serviceElementIndex).getContext();
                    String serviceURL = HTTP + SERVICE_HOST + ":" + port + context;

                    mockServicePorts.add(port);
                    updateEndPoint(endPointNode, serviceURL);

                    log.info("Mock service creator ready to start service for " + valueOfName);
                    MockServiceCreator.startMockServiceServer(valueOfName, SERVICE_HOST, port, context,
                            mockServiceData.getMockServices(serviceElementIndex).getResources());
                }
            }
        }

        return document;
    }

    /**
     * Update the endpoint node element attribute with mock service urls.
     * Checks endpoint has uri or uri-template attributes
     *
     * @param endPointNode endpoint node in document
     * @param serviceURL   mock service url
     */
    private static void updateEndPoint(Node endPointNode, String serviceURL) {
        NodeList childNodesOfEndPoint = endPointNode.getChildNodes();
        Node addressNode = childNodesOfEndPoint.item(1);

        NamedNodeMap attributeListOfAddress = addressNode.getAttributes();

        boolean isFound = false;
        for (int y = 0; y < attributeListOfAddress.getLength(); y++) {
            Attr attribute = (Attr) attributeListOfAddress.item(y);

            if (attribute.getNodeName().equals(URI)) {
                attributeListOfAddress.getNamedItem(URI).setNodeValue(serviceURL);
                isFound = true;

            } else if (attribute.getNodeName().equals(URI_TEMPLATE)) {
                attributeListOfAddress.getNamedItem(URI_TEMPLATE).setNodeValue(serviceURL);
                isFound = true;
            }

            if (isFound) {
                break;
            }
        }
    }

    /**
     * Check services are ready to serve in given ports.
     *
     * @param mockServicePorts mock service port array
     */
    private static void checkServiceStatus(ArrayList<Integer> mockServicePorts) throws IOException {
        log.info("Thread waiting for mock service(s) starting");

        for (int port : mockServicePorts) {
            boolean isAvailable = true;
            long timeoutExpiredMs = System.currentTimeMillis() + 5000;
            while (isAvailable) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isAvailable = checkPortAvailability(port);

                if (waitMillis <= 0) {
                    // timeout expired
                    throw new IOException("Connection refused for service in port - " + port);
                }
            }
        }

        log.info("Mock service(s) are started with given ports");
    }

    /**
     * Thread wait until all services are started by checking the ports.
     *
     * @param port mock service port
     * @return boolean value of port availability
     */
    private static boolean checkPortAvailability(int port) {
        boolean isAvailable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port));
            isAvailable = false;
        } catch (IOException e) {
            isAvailable = true;
        }

        return isAvailable;
    }

}

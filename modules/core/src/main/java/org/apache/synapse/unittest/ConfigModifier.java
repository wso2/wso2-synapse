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
import org.apache.log4j.Logger;

import org.apache.synapse.unittest.data.classes.Artifact;
import org.apache.synapse.unittest.data.holders.ArtifactData;
import org.apache.synapse.unittest.data.holders.MockServiceData;
import org.apache.synapse.unittest.mock.services.MockServiceCreator;
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
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.apache.synapse.unittest.Constants.*;


/**h
 * Class responsible for modify the artifact data.
 * creates mock services as in descriptor data.
 */
class ConfigModifier {

    private ConfigModifier() {}

    private static Logger logger = Logger.getLogger(ConfigModifier.class.getName());

    /**
     * Method parse the artifact data received and replaces actual endpoint urls with mock urls.
     * Call mock service creator with relevant mock service data.
     * Thread waits until all mock services are starts by checking port availability
     *
     * @param artifactData artifact data received from descriptor data
     * @param mockServiceData mock service data received from descriptor data
     */
    static void endPointModifier(ArtifactData artifactData, MockServiceData mockServiceData) {
        ArrayList<Integer> mockServicePorts = new ArrayList<>();
        ArrayList<Artifact> allArtifacts = new ArrayList<>();
        allArtifacts.add(artifactData.getTestArtifact());

        for (int x = 0; x < artifactData.getSupportiveArtifactCount(); x++) {
            allArtifacts.add(artifactData.getSupportiveArtifact(x));
        }

        for (Artifact artifact : allArtifacts) {
            try {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document document = docBuilder.parse(new InputSource(new StringReader(artifact.getArtifact())));

                NodeList xmlElementNodes = document.getElementsByTagName("*");

                for (int i = 0; i < xmlElementNodes.getLength(); i++) {
                    Node endPointNode = xmlElementNodes.item(i);

                    if (endPointNode.getNodeName().equals(END_POINT)) {

                        NamedNodeMap attributeListOfEndPoint = endPointNode.getAttributes();
                        String valueOfName = attributeListOfEndPoint.getNamedItem(ARTIFACT_NAME_ATTRIBUTE).getNodeValue();

                        //check service name is exists in mock service data holder map
                        boolean isServiceExists = mockServiceData.isServiceNameExist(valueOfName);

                        if (isServiceExists) {
                            int serviceElementIndex = mockServiceData.getServiceNameIndex(valueOfName);
                            String serviceMethod = mockServiceData.getMockServices(serviceElementIndex).getMethod();
                            int port = mockServiceData.getMockServices(serviceElementIndex).getPort();
                            String path = mockServiceData.getMockServices(serviceElementIndex).getContext();
                            String method = mockServiceData.getMockServices(serviceElementIndex).getMethod();
                            String inputPayloadWithoutWhitespace = mockServiceData.getMockServices(serviceElementIndex).getRequestPayload()
                                    .replaceAll(WHITESPACE_REGEX, "");
                            String responseWithoutWhitespace = mockServiceData.getMockServices(serviceElementIndex).getResponsePayload()
                                    .replaceAll(WHITESPACE_REGEX, "");
                            String serviceURL = HTTP + SERVICE_HOST + ":" + port + path;

                            List<Pair<String,String>> requestHeaders = mockServiceData.getMockServices(serviceElementIndex).getRequestHeaders();
                            List<Pair<String,String>> responseHeaders = mockServiceData.getMockServices(serviceElementIndex).getResponseHeaders();
                            mockServicePorts.add(port);

                            updateEndPoint(endPointNode, serviceURL, serviceMethod);

                            logger.info("Mock service creator ready to start service for " + valueOfName);
                            MockServiceCreator.startServer(valueOfName, SERVICE_HOST, port, path, method,
                                    inputPayloadWithoutWhitespace, responseWithoutWhitespace, requestHeaders,responseHeaders);
                        }
                    }
                }


                TransformerFactory tf = TransformerFactory.newInstance();
                tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(writer));
                artifact.setArtifact(writer.getBuffer().toString().replaceAll("xmlns=\"\"", ""));

                //check services are ready to serve
                logger.info("Thread waiting for mock service(s) starting");

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

                logger.info("Mock service(s) started");

            } catch (Exception e) {
                logger.error(e);
            }
        }

    }

    /**
     * Update the endpoint node element attribute with mock service urls.
     * Checks endpoint has uri or uri-template attributes
     *
     * @param endPointNode endpoint node in document
     * @param serviceURL mock service url
     * @param serviceMethod mock service method
     */
    private static void updateEndPoint(Node endPointNode, String serviceURL, String serviceMethod) {
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
                attributeListOfAddress.getNamedItem(METHOD).setNodeValue(serviceMethod);
                isFound = true;
            }

            if (isFound) {
                break;
            }
        }
    }

    /**
     * Thread wait until all services are started by checking the ports
     *
     * @param port mock service port
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


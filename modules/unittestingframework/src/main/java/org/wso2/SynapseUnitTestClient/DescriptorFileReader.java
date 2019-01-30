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
package org.wso2.SynapseUnitTestClient;

import java.io.*;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * Class responsible for reading data from the test descriptor file
 */
public class DescriptorFileReader {

    private static Logger log = Logger.getLogger(DescriptorFileReader.class.getName());
    TestDataHolder dataHolder = new TestDataHolder();

    public String inputXmlPayload = null;
    public String artifact = null;
    public String fileName = null;
    public String properties = null;
    public String expectedPropVal = null;
    public String expectedPayload = null;
    public String artifactType = null;

    public TestDataHolder readArtifactData(String descriptorFilePath) {

        BasicConfigurator.configure();

        try {
            String fileString = FileUtils.readFileToString(new File(descriptorFilePath));
            OMElement xmlFile = AXIOMUtil.stringToOM(fileString);

            QName qName = new QName("", "NoOfTestCases", "");
            OMElement noOfTestCases = xmlFile.getFirstChildWithName(qName);
            int noofTestCases = Integer.parseInt(noOfTestCases.getText());

            QName qName1 = new QName("", "artifact", "");
            OMElement artifact1 = xmlFile.getFirstChildWithName(qName1);
            artifact = artifact1.getText();

            QName qName2 = new QName("", "artifactType", "");
            OMElement artifactType1 = xmlFile.getFirstChildWithName(qName2);
            artifactType = artifactType1.getText();

            QName qName3 = new QName("", "fileName", "");
            OMElement fileName1 = xmlFile.getFirstChildWithName(qName3);
            fileName = fileName1.getText();

            QName qName4 = new QName("", "properties", "");
            OMElement properties1 = xmlFile.getFirstChildWithName(qName4);
            properties = properties1.getText();

            dataHolder.setArtifact(artifact);
            dataHolder.setArtifactType(artifactType);
            dataHolder.setFileName(fileName);
            dataHolder.setProperties(properties);
            dataHolder.setNoOfTestCases(noofTestCases);

            return dataHolder;

        } catch (FileNotFoundException e) {
            log.error("File not found");
        } catch (XMLStreamException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }

    public TestDataHolder readTestCaseData(String descriptorFilePath, int x) {

        try {
            String fileString = FileUtils.readFileToString(new File(descriptorFilePath));
            OMElement xmlFile = AXIOMUtil.stringToOM(fileString);

            String testCase = "testCase" + x;
            log.info(testCase);

            QName qName5 = new QName("", testCase, "");
            OMElement testData = xmlFile.getFirstChildWithName(qName5);

            QName qName6 = new QName("", "set-inputXmlPayload", "");
            OMElement inputXmlPayload1 = testData.getFirstChildWithName(qName6);
            inputXmlPayload = inputXmlPayload1.getText();

            QName qName7 = new QName("", "expectedPropVal", "");
            OMElement expectedPropVal1 = testData.getFirstChildWithName(qName7);
            expectedPropVal = expectedPropVal1.getText();

            QName qName8 = new QName("", "expectedPayload", "");
            OMElement expectedPayload1 = testData.getFirstChildWithName(qName8);
            expectedPayload = expectedPayload1.getText();

            dataHolder.setInputXmlPayload(inputXmlPayload);
            dataHolder.setExpectedPropVal(expectedPropVal);
            dataHolder.setExpectedPayload(expectedPayload);

            return dataHolder;

        } catch (FileNotFoundException e) {
            log.error("File not found");
        } catch (XMLStreamException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }
}
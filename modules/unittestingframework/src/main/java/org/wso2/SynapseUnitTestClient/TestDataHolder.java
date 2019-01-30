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

/**
 * Data holder class for encapsulating the data read from the test descriptor file
 */
public class TestDataHolder {

    private String inputXmlPayload;
    private String artifact;
    private String filename;
    private String properties;
    private String expectedPayload;
    private String expectedPropVal;
    private String artifactType;
    private int noOfTestCases;

    public String getInputXmlPayload() {

        return this.inputXmlPayload;
    }

    public void setInputXmlPayload(String inputXmlPayload) {

        this.inputXmlPayload = inputXmlPayload;
    }

    public String getArtifact() {

        return this.artifact;
    }

    public void setArtifact(String artifact) {

        this.artifact = artifact;
    }

    public String getArtifactType() {

        return this.artifactType;
    }

    public void setArtifactType(String artifactType) {

        this.artifactType = artifactType;
    }

    public String getFileName() {

        return this.filename;
    }

    public void setFileName(String fileName) {

        this.filename = fileName;
    }

    public String getProperties() {

        return this.properties;
    }

    public void setProperties(String properties) {

        this.properties = properties;
    }

    public String getExpectedPayload() {

        return this.expectedPayload;
    }

    public void setExpectedPayload(String expectedPayload) {

        this.expectedPayload = expectedPayload;
    }

    public String getExpectedPropVal() {

        return this.expectedPropVal;
    }

    public void setExpectedPropVal(String expectedPropVal) {

        this.expectedPropVal = expectedPropVal;
    }

    public int getNoOfTestCases() {

        return this.noOfTestCases;
    }

    public void setNoOfTestCases(int noOfTestSuits){

        this.noOfTestCases = noOfTestSuits;
    }

}


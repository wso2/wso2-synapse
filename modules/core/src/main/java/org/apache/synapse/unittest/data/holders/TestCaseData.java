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
package org.apache.synapse.unittest.data.holders;

import java.util.ArrayList;

/**
 * Class responsible for the holding the data of test case data.
 */
public class TestCaseData {

    private ArrayList<String> inputXmlPayloadArray = new ArrayList<String>();
    private ArrayList<String> expectedPropertyValuesArray = new ArrayList<String>();
    private ArrayList<String> expectedPayloadArray = new ArrayList<String>();

    /**
     * Get input payload of particular test case.
     *
     * @param elementIndex index value of the test case
     * @return input payload of requested test case
     */
    public String getInputXmlPayload(int elementIndex) {
        return inputXmlPayloadArray.get(elementIndex);
    }

    /**
     * Get expected property values of particular test case.
     *
     * @param elementIndex index value of the test case
     * @return expected property values of requested test case
     */
    public String getExpectedPropertyValues(int elementIndex) {
        return expectedPropertyValuesArray.get(elementIndex);
    }

    /**
     * Get expected payload values of particular test case.
     *
     * @param elementIndex index value of the test case
     * @return expected payload of requested test case
     */
    public String getExpectedPayload(int elementIndex) {
        return expectedPayloadArray.get(elementIndex);
    }

    /**
     * Add input payload into a ArrayList.
     *
     * @param inputXmlPayload input payload of a particular test case
     */
    public void addInputXmlPayload(String inputXmlPayload) {
        this.inputXmlPayloadArray.add(inputXmlPayload);
    }

    /**
     * Add expected property values into a ArrayList.
     *
     * @param expectedPropertyValues expected property values of a particular test case
     */
    public void addExpectedPropertyValues(String expectedPropertyValues) {
        this.expectedPropertyValuesArray.add(expectedPropertyValues);
    }

    /**
     * Add expected payload into a ArrayList.
     *
     * @param expectedPayload expected payload of a particular test case
     */
    public void addExpectedPayload(String expectedPayload) {
        this.expectedPayloadArray.add(expectedPayload);
    }

}

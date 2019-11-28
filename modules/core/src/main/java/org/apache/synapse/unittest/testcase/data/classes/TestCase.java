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

package org.apache.synapse.unittest.testcase.data.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCase {
    private String testCaseName;
    private String requestPath;
    private String requestMethod;
    private String inputPayload;
    private List<Map<String, String>> propertyMap = new ArrayList<>();
    private List<AssertEqual> assertEquals = new ArrayList<>();
    private List<AssertNotNull> assertNotNull = new ArrayList<>();

    /**
     * Get name of the test case.
     *
     * @return name of the requested test case
     */
    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * Get input payload of particular test case.
     *
     * @return input payload of requested test case
     */
    public String getInputPayload() {
        return inputPayload;
    }

    /**
     * Get input property values of particular test case.
     *
     * @return expected property values of requested test case
     */
    public List<Map<String, String>> getPropertyMap() {
        return propertyMap;
    }

    /**
     * Get assert equals set of particular test case.
     *
     * @return assert equals set of requested test case
     */
    public List<AssertEqual> getAssertEquals() {
        return assertEquals;
    }

    /**
     * Get assert not null set of particular test case.
     *
     * @return assert not null set of requested test case
     */
    public List<AssertNotNull> getAssertNotNull() {
        return assertNotNull;
    }

    /**
     * Get request path of particular test case.
     *
     * @return request path of requested test case
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Get request method of particular test case.
     *
     * @return request method of requested test case
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * Add input payload into a ArrayList.
     *
     * @param inputPayload input payload of a particular test case
     */
    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    /**
     * Add input property values into a ArrayList.
     *
     * @param propertyMap input property values of a particular test case
     */
    public void setPropertyMap(List<Map<String, String>> propertyMap) {
        this.propertyMap = propertyMap;
    }

    /**
     * Add assertEquals set into a ArrayList.
     *
     * @param assertEquals assertEquals set of a particular test case
     */
    public void setAssertEquals(List<AssertEqual> assertEquals) {
        this.assertEquals = assertEquals;
    }

    /**
     * Add assertNotNull set into a ArrayList.
     *
     * @param assertNotNull assertNotNull set of a particular test case
     */
    public void setAssertNotNull(List<AssertNotNull> assertNotNull) {
        this.assertNotNull = assertNotNull;
    }

    /**
     * Add requestPath.
     *
     * @param requestPath requestPath of a particular test case
     */
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    /**
     * Add requestMethod.
     *
     * @param requestMethod requestMethod of a particular test case
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * Set test case name.
     *
     * @param testCaseName name of the particular test case
     */
    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }
}

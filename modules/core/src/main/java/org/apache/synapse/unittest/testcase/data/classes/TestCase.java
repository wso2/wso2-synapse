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

package org.apache.synapse.unittest.testcase.data.classes;

import java.util.ArrayList;
import java.util.Map;

public class TestCase {
    private String requestPath;
    private String inputPayload;
    private ArrayList<Map<String, String>> propertyMap = new ArrayList<>();
    private ArrayList<AssertEqual> assertEquals = new ArrayList<>();
    private ArrayList<AssertNotNull> assertNotNull = new ArrayList<>();

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
    public ArrayList<Map<String, String>> getPropertyMap() {
        return propertyMap;
    }

    /**
     * Get assert equals set of particular test case.
     *
     * @return assert equals set of requested test case
     */
    public ArrayList<AssertEqual> getAssertEquals() {
        return assertEquals;
    }

    /**
     * Get assert not null set of particular test case.
     *
     * @return assert not null set of requested test case
     */
    public ArrayList<AssertNotNull> getAssertNotNull() {
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
    public void setPropertyMap(ArrayList<Map<String, String>> propertyMap) {
        this.propertyMap = propertyMap;
    }

    /**
     * Add assertEquals set into a ArrayList.
     *
     * @param assertEquals assertEquals set of a particular test case
     */
    public void setAssertEquals(ArrayList<AssertEqual> assertEquals) {
        this.assertEquals = assertEquals;
    }

    /**
     * Add assertNotNull set into a ArrayList.
     *
     * @param assertNotNull assertNotNull set of a particular test case
     */
    public void setAssertNotNull(ArrayList<AssertNotNull> assertNotNull) {
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
}

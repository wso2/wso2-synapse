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

public class TestCaseSummary {

    private static final String SKIPPED_STATE = "SKIPPED";
    private static final String FAILED_STATE = "FAILED";
    private String testCaseName;
    private String exception;
    private String mediationStatus = SKIPPED_STATE;
    private String assertionStatus = SKIPPED_STATE;

    /**
     * Get failure exception.
     *
     * @return failure exception
     */
    public String getTestException() {
        return exception;
    }

    /**
     * Get test case mediation status.
     *
     * @return test cases mediation status
     */
    public String getMediationStatus() {
        return mediationStatus;
    }

    /**
     * Get test case assertion status.
     *
     * @return test cases assertion status
     */
    public String getAssertionStatus() {
        return assertionStatus;
    }

    /**
     * Get test case name.
     *
     * @return test cases name
     */
    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * Set test case failure exception.
     *
     * @param exception test case exception
     */
    public void setTestException(String exception) {
        this.exception = exception;
    }

    /**
     * Set test case mediation status.
     *
     * @param mediationStatus test cases mediation status
     */
    public void setMediationStatus(String mediationStatus) {
        this.mediationStatus = mediationStatus;
    }

    /**
     * Set test case assertion status.
     *
     * @param assertionStatus test cases assertion status
     */
    public void setAssertionStatus(String assertionStatus) {
        this.assertionStatus = assertionStatus;
    }

    /**
     * Set test case name.
     *
     * @param testCaseName test cases name
     */
    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    /**
     * Get test case passed or failed state.
     *
     * @return boolean value of test case pass or not
     */
    public boolean isTestCasePassed() {
        if (mediationStatus.equals(FAILED_STATE) || mediationStatus.equals(SKIPPED_STATE)) {
            return false;
        }

        if (assertionStatus.equals(FAILED_STATE) || assertionStatus.equals(SKIPPED_STATE)) {
            return false;
        }

        return true;
    }
}

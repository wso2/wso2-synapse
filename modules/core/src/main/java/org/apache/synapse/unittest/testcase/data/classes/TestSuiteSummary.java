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

public class TestSuiteSummary {

    private static final String SKIPPED_STATE = "SKIPPED";
    private static final String FAILED_STATE = "FAILED";
    private String exception;
    private String description;
    private String deploymentStatus = SKIPPED_STATE;
    private String mediationStatus = SKIPPED_STATE;
    private String recentTestCaseName;
    private String mediationException;
    private List<TestCaseSummary> testCases = new ArrayList<>();

    /**
     * Get test case deployment status.
     *
     * @return test cases deployment status
     */
    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    /**
     * Set test case deployment status.
     *
     * @param deploymentStatus test cases deployment status
     */
    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    /**
     * Get failure exception.
     *
     * @return failure exception
     */
    public String getDeploymentException() {
        return exception;
    }

    /**
     * Set test case failure exception.
     *
     * @param exception test case exception
     */
    public void setDeploymentException(String exception) {
        this.exception = exception;
    }

    /**
     * Get test case suite passed or failed state.
     *
     * @return boolean value of test case suite pass or not
     */
    public boolean isTestSuiteDeploymentSuccess() {
        if (deploymentStatus.equals(SKIPPED_STATE) || deploymentStatus.equals(FAILED_STATE)) {
            return false;
        }

        return true;
    }

    /**
     * Add test case summary object.
     *
     * @param summary test cases summary object
     */
    public void addTestCaseSumamry(TestCaseSummary summary) {
        testCases.add(summary);
    }

    /**
     * Get test case summary object list.
     *
     * @return  summary test cases summary object list
     */
    public List<TestCaseSummary> getTestCaseSumamryList() {
        return testCases;
    }

    /**
     * Get test suite description.
     *
     * @return test suite description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get test suite name which failed in mediation state.
     *
     * @return test case name
     */
    public String getRecentTestCaseName() {
        return recentTestCaseName;
    }

    /**
     * Set test suite description.
     *
     * @param description test suite description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get test mediation status.
     *
     * @return test mediation status
     */
    public String getMediationStatus() {
        return mediationStatus;
    }

    /**
     * Get test mediation exception.
     *
     * @return test mediation exception
     */
    public String getMediationException() {
        return mediationException;
    }

    /**
     * Set test case mediation status.
     *
     * @param mediationStatus test case mediation status
     */
    public void setMediationStatus(String mediationStatus) {
        this.mediationStatus = mediationStatus;
    }

    /**
     * Set test case mediation exception.
     *
     * @param mediationException test case mediation exception
     */
    public void setMediationException(String mediationException) {
        this.mediationException = mediationException;
    }

    /**
     * Set test case name which run recently.
     *
     * @param recentTestCaseName test case name
     */
    public void setRecentTestCaseName(String recentTestCaseName) {
        this.recentTestCaseName = recentTestCaseName;
    }

    /**
     * Get test case suite mediate passed or failed state.
     *
     * @return boolean value of test case suite mediate pass or not
     */
    public boolean isTestSuiteMediateSuccess() {
        if (mediationStatus.equals(SKIPPED_STATE) || mediationStatus.equals(FAILED_STATE)) {
            return false;
        }

        return true;
    }
}

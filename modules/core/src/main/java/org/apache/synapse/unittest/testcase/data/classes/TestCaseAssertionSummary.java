/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest.testcase.data.classes;

/**
 * Class responsible for store the assert failure details.
 */
public class TestCaseAssertionSummary {

    private String assertionActualValue;
    private String assertionExpectedValue;
    private String assertionExpression;
    private String assertionErrorMessage;
    private String assertionType;
    private String assertionDescription;

    /**
     * Get assert actual value.
     *
     * @return assert actual value
     */
    public String getAssertionActualValue() {
        return assertionActualValue;
    }

    /**
     * Set assert actual value.
     *
     * @param assertionActualValue actual value
     */
    public void setAssertionActualValue(String assertionActualValue) {
        this.assertionActualValue = assertionActualValue;
    }

    /**
     * Get assert expected value.
     *
     * @return assert expected value
     */
    public String getAssertionExpectedValue() {
        return assertionExpectedValue;
    }

    /**
     * Set assert expected value.
     *
     * @param assertionExpectedValue expected value
     */
    public void setAssertionExpectedValue(String assertionExpectedValue) {
        this.assertionExpectedValue = assertionExpectedValue;
    }

    /**
     * Get assert expression.
     *
     * @return assert expression
     */
    public String getAssertionExpression() {
        return assertionExpression;
    }

    /**
     * Set assert expression.
     *
     * @param assertionExpression assert expression
     */
    public void setAssertionExpression(String assertionExpression) {
        this.assertionExpression = assertionExpression;
    }

    /**
     * Get assert message.
     *
     * @return assert message
     */
    public String getAssertionErrorMessage() {
        return assertionErrorMessage;
    }

    /**
     * Set assert message.
     *
     * @param assertionErrorMessage assert message
     */
    public void setAssertionErrorMessage(String assertionErrorMessage) {
        this.assertionErrorMessage = assertionErrorMessage;
    }

    /**
     * Get assert type.
     *
     * @return assert type
     */
    public String getAssertionType() {
        return assertionType;
    }

    /**
     * Set assert type.
     *
     * @param assertionType assert type
     */
    public void setAssertionType(String assertionType) {
        this.assertionType = assertionType;
    }

    /**
     * Get assert description.
     *
     * @return assert description
     */
    public String getAssertionDescription() {
        return assertionDescription;
    }

    /**
     * Set assert description.
     *
     * @param assertionDescription assert description
     */
    public void setAssertionDescription(String assertionDescription) {
        this.assertionDescription = assertionDescription;
    }
}

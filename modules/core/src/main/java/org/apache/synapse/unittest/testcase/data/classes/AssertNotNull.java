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

/**
 * Class responsible for manage assertNotNull data.
 *
 */
public class AssertNotNull {

    private String actual;
    private String message;

    /**
     * Get assert actual value.
     *
     * @return  actual value of assert
     */
    public String getActual() {
        return actual;
    }

    /**
     * Get assert message.
     *
     * @return  actual value of assert
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set assert actual value.
     *
     * @param  actual value of assert
     */
    public void setActual(String actual) {
        this.actual = actual;
    }

    /**
     * Set message value.
     *
     * @param  message value of message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}

/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.core.axis2;

/**
 * Class to keep the response's state
 */
public class ResponseState {
    private boolean isRespondDone = false;

    /**
     * Return the response is done or not.
     *
     * @return isRespondDone {boolean}
     */
    public boolean isRespondDone() {
        return isRespondDone;
    }

    /**
     * Set the response to done or not.
     */
    public void setRespondDone() {
        isRespondDone = true;
    }
}

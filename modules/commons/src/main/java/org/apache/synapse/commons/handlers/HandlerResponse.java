/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.commons.handlers;

/**
 * DTO class to hold response information of an Inbound endpoint handler execution.
 */
public class HandlerResponse {

    boolean isError;
    int errorCode;
    String errorMessage;
    boolean closeConnection;

    public HandlerResponse(boolean isError, int errorCode, String errorMessage, boolean closeConnection) {
        this.isError = isError;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.closeConnection = closeConnection;
    }

    public HandlerResponse() {
        this(false, 0, null, false);
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isCloseConnection() {
        return closeConnection;
    }

    public void setCloseConnection(boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorResponseString() {
        return "Error code: " + errorCode + ". Reason: " + errorMessage;
    }
}

/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.opa;

import org.apache.http.HttpStatus;

/**
 * Exception to be thrown when a OPA mediator related error occurs
 */
public class OPASecurityException extends Exception {

    public static final int MEDIATOR_ERROR = 90100;
    public static final String MEDIATOR_ERROR_MESSAGE = "OPA Mediator: Unexpected mediator failure";
    public static final int OPA_REQUEST_ERROR = 90101;
    public static final String OPA_REQUEST_ERROR_MESSAGE = "OPA Mediator: Error with the OPA validation request";
    public static final int OPA_RESPONSE_ERROR = 90102;
    public static final String OPA_RESPONSE_ERROR_MESSAGE = "OPA Mediator: Error with the OPA validation response";
    public static final int ACCESS_REVOKED = 90103;
    public static final String ACCESS_REVOKED_MESSAGE = "OPA mediator: Access Revoked";

    private int errorCode;

    public OPASecurityException(String message) {

        super(message);
    }

    public OPASecurityException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OPASecurityException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public OPASecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public OPASecurityException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    /**
     * returns an String that corresponds to errorCode passed in
     * @param errorCode - error code
     * @return String
     */
    public static String getAuthenticationFailureMessage(int errorCode) {
        String errorMessage;
        switch (errorCode) {
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                errorMessage = "Unexpected error occurred in the OPA mediator";
                break;
            case HttpStatus.SC_FORBIDDEN:
                errorMessage = "Access revoked by OPA";
                break;
            case HttpStatus.SC_BAD_REQUEST:
                errorMessage = "Bad request";
                break;
            default:
                errorMessage = "Unexpected error";
                break;
        }
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

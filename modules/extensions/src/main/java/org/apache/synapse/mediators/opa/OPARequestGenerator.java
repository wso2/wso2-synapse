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

import org.apache.synapse.MessageContext;

import java.util.Map;

/**
 * OPA request generator interface to handle OPA policy validation payload and validation response
 */
public interface OPARequestGenerator {

    /**
     * Generate the OPA request payload from the provided message context and the additional Properties Map
     *
     * @param messageContext     The message to be validated with OPA server
     * @param advancedProperties Advanced properies that can be used to construct the opa payload
     *
     * @return json input as a string and this will be sent to the OPA server for validation
     * @throws OPASecurityException If an authentication failure or some other error occurs
     */
    String createRequest(String policyName, String rule, Map<String, Object> advancedProperties,
                         MessageContext messageContext) throws OPASecurityException;

    /**
     * Authenticates the given request using the authenticators which have been initialized.
     *
     * @param messageContext The message to be authenticated
     * @param opaResponse The message to be authenticated
     *
     * @return true if the authentication is successful (never returns false)
     * @throws OPASecurityException If an authentication failure or some other error occurs
     */
    boolean handleResponse(String policyName, String rule, String opaResponse, MessageContext messageContext)
            throws OPASecurityException;

}

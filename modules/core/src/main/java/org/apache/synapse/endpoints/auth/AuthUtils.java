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

package org.apache.synapse.endpoints.auth;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.auth.basicauth.BasicAuthHandler;
import org.apache.synapse.endpoints.auth.oauth.OAuthUtils;

import javax.xml.namespace.QName;

public class AuthUtils {

    private static final Log log = LogFactory.getLog(AuthUtils.class);

    /**
     * This method will return an AuthHandler instance depending on the auth configs
     *
     * @param httpElement Element containing http configs
     * @return AuthHandler object
     * @throws AuthException throw exception for invalid auth configs
     */
    public static AuthHandler getAuthHandler(OMElement httpElement) throws AuthException {

        if (httpElement != null) {
            OMElement authElement = httpElement.getFirstChildWithName(
                    new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.AUTHENTICATION));

            if (authElement != null) {
                OMElement oauthElement = authElement.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.OAUTH));
                OMElement basicAuthElement = authElement.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.BASIC_AUTH));

                AuthHandler authHandler = null;
                if (oauthElement != null) {
                    authHandler = OAuthUtils.getSpecificOAuthHandler(oauthElement);
                } else if (basicAuthElement != null) {
                    authHandler = getBasicAuthHandler(basicAuthElement);
                }

                // invalid auth configuration
                if (authHandler != null) {
                    return authHandler;
                } else {
                    throw new AuthException("Authentication configuration is invalid");
                }
            }
        }
        return null;
    }

    private static AuthHandler getBasicAuthHandler(OMElement basicAuthElement) {

        String username = OAuthUtils.getChildValue(basicAuthElement, AuthConstants.BASIC_AUTH_USERNAME);
        String password = OAuthUtils.getChildValue(basicAuthElement, AuthConstants.BASIC_AUTH_PASSWORD);

        if (username == null || password == null) {
            log.error("Invalid basicAuth configurations provided");
            return null;
        }
        return new BasicAuthHandler(username, password);
    }
}

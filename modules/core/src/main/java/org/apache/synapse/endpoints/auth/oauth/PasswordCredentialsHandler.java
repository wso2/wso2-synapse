/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.endpoints.auth.oauth;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;

/**
 * This class is used to handle Password Credentials grant oauth.
 */
public class PasswordCredentialsHandler extends OAuthHandler {

    private final String username;
    private final String password;

    protected PasswordCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String username,
                                         String password) {

        super(tokenApiUrl, clientId, clientSecret);
        this.username = username;
        this.password = password;
    }

    @Override
    protected String buildTokenRequestPayload(MessageContext messageContext) throws AuthException {

        StringBuilder payload = new StringBuilder();

        payload.append(AuthConstants.PASSWORD_CRED_GRANT_TYPE);
        payload.append(AuthConstants.PARAM_USERNAME).append(OAuthUtils.resolveExpression(username, messageContext));
        payload.append(AuthConstants.PARAM_PASSWORD).append(OAuthUtils.resolveExpression(password, messageContext));
        String requestParams = getRequestParametersAsString(messageContext);
        payload.append(requestParams);

        return payload.toString();
    }

    @Override
    protected OMElement serializeSpecificOAuthConfigs(OMFactory omFactory) {

        OMElement passwordCredentials = omFactory.createOMElement(
                AuthConstants.PASSWORD_CREDENTIALS,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        passwordCredentials.addChild(OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.OAUTH_USERNAME,
                username));
        passwordCredentials.addChild(OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.OAUTH_PASSWORD,
                password));
        return passwordCredentials;
    }

    public String getUsername() {

        return username;
    }

    public String getPassword() {

        return password;
    }
}

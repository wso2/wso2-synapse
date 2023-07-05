/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;

/**
 * This class is used to handle Authorization code grant oauth.
 */
public class AuthorizationCodeHandler extends OAuthHandler {

    private final String refreshToken;

    public AuthorizationCodeHandler(String tokenApiUrl, String clientId, String clientSecret,
                                    String refreshToken, String authMode, int connectionTimeout,
                                    int connectionRequestTimeout, int socketTimeout) {

        super(tokenApiUrl, clientId, clientSecret, authMode, connectionTimeout, connectionRequestTimeout,
                socketTimeout);
        this.refreshToken = refreshToken;
    }

    @Override
    protected String buildTokenRequestPayload(MessageContext messageContext) throws AuthException {

        StringBuilder payload = new StringBuilder();

        payload.append(AuthConstants.REFRESH_TOKEN_GRANT_TYPE)
                .append(AuthConstants.PARAM_REFRESH_TOKEN)
                .append(OAuthUtils.resolveExpression(refreshToken, messageContext));
        if (StringUtils.isNotBlank(getAuthMode()) &&
                "payload".equalsIgnoreCase(OAuthUtils.resolveExpression(getAuthMode(), messageContext))) {
            payload.append(AuthConstants.PARAM_CLIENT_ID)
                    .append(OAuthUtils.resolveExpression(getClientId(), messageContext));
            payload.append(AuthConstants.PARAM_CLIENT_SECRET)
                    .append(OAuthUtils.resolveExpression(getClientSecret(), messageContext));
        }
        payload.append(getRequestParametersAsString(messageContext));

        return payload.toString();
    }

    @Override
    protected OMElement serializeSpecificOAuthConfigs(OMFactory omFactory) {

        OMElement authCode = omFactory.createOMElement(
                AuthConstants.AUTHORIZATION_CODE,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        authCode.addChild(
                OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.OAUTH_REFRESH_TOKEN, getRefreshToken()));
        return authCode;
    }

    /**
     * Return the refresh token secret relevant to the Authorization Code Handler.
     *
     * @return String refresh token
     */
    public String getRefreshToken() {

        return refreshToken;
    }
}

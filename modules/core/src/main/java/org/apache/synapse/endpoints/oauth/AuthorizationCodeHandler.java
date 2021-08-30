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

package org.apache.synapse.endpoints.oauth;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.synapse.MessageContext;

/**
 * This class is used to handle Authorization code grant oauth
 */
public class AuthorizationCodeHandler extends OAuthHandler {

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    public AuthorizationCodeHandler(String tokenApiUrl, String clientId, String clientSecret,
                                    String refreshToken) {

        super(tokenApiUrl);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    @Override
    protected String buildTokenRequestPayload(MessageContext messageContext) throws OAuthException {

        StringBuilder payload = new StringBuilder();

        payload.append(OAuthConstants.REFRESH_TOKEN_GRANT_TYPE)
                .append(OAuthConstants.PARAM_REFRESH_TOKEN)
                .append(OAuthUtils.resolveExpression(refreshToken, messageContext));
        payload.append(OAuthConstants.PARAM_CLIENT_ID).append(OAuthUtils.resolveExpression(clientId, messageContext));
        payload.append(OAuthConstants.PARAM_CLIENT_SECRET)
                .append(OAuthUtils.resolveExpression(clientSecret, messageContext));
        payload.append(getRequestParametersAsString(messageContext));

        return payload.toString();
    }

    @Override
    protected String getEncodedCredentials(MessageContext messageContext) throws OAuthException {

        return Base64Utils.encode((OAuthUtils.resolveExpression(clientId, messageContext) + ":" +
                OAuthUtils.resolveExpression(clientSecret, messageContext)).getBytes());
    }

    /**
     * Return the client id relevant to the Authorization Code Handler
     *
     * @return String client id
     */
    public String getClientId() {

        return clientId;
    }

    /**
     * Return the client secret relevant to the Authorization Code Handler
     *
     * @return String client secret
     */
    public String getClientSecret() {

        return clientSecret;
    }

    /**
     * Return the refresh token secret relevant to the Authorization Code Handler
     *
     * @return String refresh token
     */
    public String getRefreshToken() {

        return refreshToken;
    }
}

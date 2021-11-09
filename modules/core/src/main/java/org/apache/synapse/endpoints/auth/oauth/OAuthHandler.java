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

import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.AuthHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * This abstract class is to be used by OAuth handlers
 * This class checks validity of tokens, request for tokens
 * and add tokens to in-memory cache
 */

public abstract class OAuthHandler implements AuthHandler {

    private final String id;

    private final String tokenApiUrl;

    protected OAuthHandler(String tokenApiUrl) {

        this.id = OAuthUtils.getRandomOAuthHandlerID();
        this.tokenApiUrl = tokenApiUrl;
    }

    @Override
    public String getAuthType() {
        return AuthConstants.OAUTH;
    }

    @Override
    public void setAuthHeader(MessageContext messageContext) throws AuthException {
        setAuthorizationHeader(messageContext, getToken());
    }

    /**
     * This method returns a token string
     *
     * @return token String
     * @throws AuthException In the event of errors when generating new token
     */
    private String getToken() throws AuthException {

        try {
            return TokenCache.getInstance().getToken(id, new Callable<String>() {
                @Override
                public String call() throws AuthException, IOException {

                    return OAuthClient.generateToken(tokenApiUrl, buildTokenRequestPayload(), getEncodedCredentials());
                }
            });
        } catch (ExecutionException e) {
            throw new AuthException(e);
        }
    }

    /**
     * Method to set the Authorization header
     *
     * @param messageContext Message Context of the request
     * @param accessToken    Access token to be set
     */
    private void setAuthorizationHeader(MessageContext messageContext, String accessToken) {

        Map<String, Object> transportHeaders = (Map<String, Object>) ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        transportHeaders.put(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BEARER + accessToken);
    }

    /**
     * Method to remove the token from the cache when the endpoint is destroyed
     */
    public void removeTokenFromCache() {

        TokenCache.getInstance().removeToken(id);
    }

    /**
     * Return the token server url relevant to the OAuth handler.
     *
     * @return String Token server url
     */
    public String getTokenUrl() {

        return tokenApiUrl;
    }

    /**
     * Return the request payload relevant to the OAuth handler.
     *
     * @return String payload
     */
    protected abstract String buildTokenRequestPayload();

    /**
     * Return the base 64 encoded clientId:clientSecret relevant to the OAuth handler.
     *
     * @return String payload
     */
    protected abstract String getEncodedCredentials();

}

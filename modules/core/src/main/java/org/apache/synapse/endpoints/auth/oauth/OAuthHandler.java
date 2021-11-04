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
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.AuthHandler;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
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
    private final String clientId;
    private final String clientSecret;
    private Map<String, String> requestParametersMap;

    protected OAuthHandler(String tokenApiUrl, String clientId, String clientSecret) {

        this.id = OAuthUtils.getRandomOAuthHandlerID();
        this.tokenApiUrl = tokenApiUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getAuthType() {
        return AuthConstants.OAUTH;
    }

    @Override
    public void setAuthHeader(MessageContext messageContext) throws AuthException {
        setAuthorizationHeader(messageContext, getToken(messageContext));
    }

    /**
     * This method returns a token string.
     *
     * @return token String
     * @throws AuthException In the event of errors when generating new token
     */
    private String getToken(final MessageContext messageContext) throws AuthException {

        try {
            return TokenCache.getInstance().getToken(id, new Callable<String>() {
                @Override
                public String call() throws AuthException, IOException {

                    return OAuthClient.generateToken(tokenApiUrl, buildTokenRequestPayload(messageContext),
                            getEncodedCredentials(messageContext));
                }
            });
        } catch (ExecutionException e) {
            throw new AuthException(e.getCause());
        }
    }

    /**
     * Method to set the Authorization header.
     *
     * @param messageContext Message Context of the request
     * @param accessToken    Access token to be set
     */
    private void setAuthorizationHeader(MessageContext messageContext, String accessToken) {

        Object transportHeaders = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null && transportHeaders instanceof Map) {
            Map transportHeadersMap = (Map) transportHeaders;
            transportHeadersMap.put(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BEARER + accessToken);
        } else {
            Map<String, Object> transportHeadersMap = new TreeMap<>(new Comparator<String>() {
                public int compare(String o1, String o2) {

                    return o1.compareToIgnoreCase(o2);
                }
            });
            transportHeadersMap.put(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BEARER + accessToken);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                    .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, transportHeadersMap);
        }
    }

    /**
     * Method to remove the token from the cache when the endpoint is destroyed.
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
     * Return the client id relevant to the OAuth Handler.
     *
     * @return String client id
     */
    public String getClientId() {

        return clientId;
    }

    /**
     * Return the client secret relevant to the OAuth Handler.
     *
     * @return String client secret
     */
    public String getClientSecret() {

        return clientSecret;
    }

    /**
     * Return the request payload relevant to the OAuth handler.
     *
     * @return String payload
     */
    protected abstract String buildTokenRequestPayload(MessageContext messageContext) throws AuthException;

    /**
     * Return the OMElement for OAuth configuration relevant to the OAuth handler.
     *
     * @return OMElement OAuth configuration
     */
    protected abstract OMElement serializeSpecificOAuthConfigs(OMFactory omFactory);

    /**
     * This method returns an OMElement containing the OAuth configuration.
     *
     * @return OMElement OAuth configuration
     */
    public OMElement serializeOAuthConfiguration(OMFactory omFactory) {

        OMElement oauthCredentials = serializeSpecificOAuthConfigs(omFactory);
        oauthCredentials.addChild(OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.OAUTH_CLIENT_ID,
                clientId));
        oauthCredentials.addChild(OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.OAUTH_CLIENT_SECRET,
                clientSecret));
        oauthCredentials.addChild(OAuthUtils.createOMElementWithValue(omFactory, AuthConstants.TOKEN_API_URL,
                tokenApiUrl));
        if (requestParametersMap != null && !requestParametersMap.isEmpty()) {
            OMElement requestParameters = OAuthUtils.createOMRequestParams(omFactory, requestParametersMap);
            oauthCredentials.addChild(requestParameters);
        }
        return oauthCredentials;
    }

    /**
     * Return the base 64 encoded clientId:clientSecret relevant to the OAuth handler.
     *
     * @param messageContext Message Context of the request which will be used to resolve dynamic expressions
     * @return String payload
     * @throws AuthException In the event of errors when resolving the dynamic expressions
     */
    protected String getEncodedCredentials(MessageContext messageContext) throws AuthException {

        return Base64Utils.encode((OAuthUtils.resolveExpression(clientId, messageContext) + ":" +
                OAuthUtils.resolveExpression(clientSecret, messageContext)).getBytes());
    }

    /**
     * Return the request parameters as a string.
     *
     * @return String request parameters
     */
    protected String getRequestParametersAsString(MessageContext messageContext) throws AuthException {

        if (requestParametersMap == null) {
            return "";
        }
        StringBuilder payload = new StringBuilder();
        for (Map.Entry<String, String> entry : requestParametersMap.entrySet()) {
            String value = OAuthUtils.resolveExpression(entry.getValue(), messageContext);
            payload.append(AuthConstants.AMPERSAND).append(entry.getKey()).append(AuthConstants.EQUAL_MARK)
                    .append(value);
        }
        return payload.toString();
    }

    /**
     * Method to set the request parameter map.
     *
     * @param requestParameters the request parameter map
     */
    public void setRequestParameters(Map<String, String> requestParameters) {

        this.requestParametersMap = requestParameters;
    }

    public Map<String, String> getRequestParametersMap() {

        return requestParametersMap;
    }
}

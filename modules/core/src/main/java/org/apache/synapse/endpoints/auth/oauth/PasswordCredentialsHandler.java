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
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.TrustStoreConfigs;
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;

import java.util.Objects;

/**
 * This class is used to handle Password Credentials grant oauth.
 */
public class PasswordCredentialsHandler extends OAuthHandler {

    private final String username;
    private final String password;

    /**
     * Backward compatibility constructor without TrustStoreConfigs
     * @deprecated Use constructor with TrustStoreConfigs parameter for enhanced SSL configuration support
     */
    @Deprecated
    protected PasswordCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String username,
                                         String password, String authMode, boolean useGlobalConnectionTimeoutConfigs,
                                         int connectionTimeout, int connectionRequestTimeout, int socketTimeout,
                                         TokenCacheProvider tokenCacheProvider, boolean useGlobalProxyConfigs,
                                         ProxyConfigs proxyConfigs) {
        this(tokenApiUrl, clientId, clientSecret, username, password, authMode, useGlobalConnectionTimeoutConfigs,
                connectionTimeout, connectionRequestTimeout, socketTimeout, tokenCacheProvider,
                useGlobalProxyConfigs, proxyConfigs, null);
    }

    protected PasswordCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String username,
                                         String password, String authMode, boolean useGlobalConnectionTimeoutConfigs,
                                         int connectionTimeout, int connectionRequestTimeout, int socketTimeout,
                                         TokenCacheProvider tokenCacheProvider, boolean useGlobalProxyConfigs,
                                         ProxyConfigs proxyConfigs, TrustStoreConfigs trustStoreConfigs) {

        super(tokenApiUrl, clientId, clientSecret, authMode, useGlobalConnectionTimeoutConfigs, connectionTimeout,
                connectionRequestTimeout, socketTimeout, tokenCacheProvider, useGlobalProxyConfigs, proxyConfigs,
                trustStoreConfigs);
        this.username = username;
        this.password = password;
    }

    @Override
    protected String buildTokenRequestPayload(MessageContext messageContext) throws AuthException {

        StringBuilder payload = new StringBuilder();

        payload.append(AuthConstants.PASSWORD_CRED_GRANT_TYPE);
        payload.append(AuthConstants.PARAM_USERNAME).append(OAuthUtils.resolveExpression(username, messageContext));
        payload.append(AuthConstants.PARAM_PASSWORD).append(OAuthUtils.resolveExpression(password, messageContext));
        if (StringUtils.isNotBlank(getAuthMode()) &&
                "payload".equalsIgnoreCase(OAuthUtils.resolveExpression(getAuthMode(), messageContext))) {
            payload.append(AuthConstants.PARAM_CLIENT_ID)
                    .append(OAuthUtils.resolveExpression(getClientId(), messageContext));
            payload.append(AuthConstants.PARAM_CLIENT_SECRET)
                    .append(OAuthUtils.resolveExpression(getClientSecret(), messageContext));
        }
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

    @Override
    protected int getHash(MessageContext messageContext) throws AuthException {
        return Objects.hash(messageContext.getTo().getAddress(), OAuthUtils.resolveExpression(getTokenUrl(), messageContext),
                OAuthUtils.resolveExpression(getClientId(), messageContext), OAuthUtils.resolveExpression(getClientSecret(),
                        messageContext), OAuthUtils.resolveExpression(getUsername(), messageContext),
                OAuthUtils.resolveExpression(getPassword(), messageContext),
                getRequestParametersAsString(messageContext), getResolvedCustomHeadersMap(getCustomHeadersMap(),
                        messageContext));
    }

    public String getUsername() {

        return username;
    }

    public String getPassword() {

        return password;
    }
}

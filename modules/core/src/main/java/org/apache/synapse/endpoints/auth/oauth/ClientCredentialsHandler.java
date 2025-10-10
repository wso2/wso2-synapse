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
import org.apache.synapse.endpoints.TrustStoreConfigs;
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;

import java.util.Objects;

/**
 * This class is used to handle Client Credentials grant oauth.
 */
public class ClientCredentialsHandler extends OAuthHandler {

    /**
     * Backward compatibility constructor without TrustStoreConfigs
     * @deprecated Use constructor with TrustStoreConfigs parameter for enhanced SSL configuration support
     */
    @Deprecated
    public ClientCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String authMode,
                                    boolean useGlobalConnectionTimeoutConfigs, int connectionTimeout,
                                    int connectionRequestTimeout, int socketTimeout,
                                    TokenCacheProvider tokenCacheProvider, boolean useGlobalProxyConfigs,
                                    ProxyConfigs proxyConfigs) {
        this(tokenApiUrl, clientId, clientSecret, authMode, useGlobalConnectionTimeoutConfigs,
                connectionTimeout, connectionRequestTimeout, socketTimeout, tokenCacheProvider,
                useGlobalProxyConfigs, proxyConfigs, null);
    }

    public ClientCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String authMode,
                                    boolean useGlobalConnectionTimeoutConfigs, int connectionTimeout,
                                    int connectionRequestTimeout, int socketTimeout,
                                    TokenCacheProvider tokenCacheProvider, boolean useGlobalProxyConfigs,
                                    ProxyConfigs proxyConfigs, TrustStoreConfigs trustStoreConfigs) {

        super(tokenApiUrl, clientId, clientSecret, authMode, useGlobalConnectionTimeoutConfigs, connectionTimeout,
                connectionRequestTimeout, socketTimeout, tokenCacheProvider, useGlobalProxyConfigs, proxyConfigs,
                trustStoreConfigs);
    }

    @Override
    protected String buildTokenRequestPayload(MessageContext messageContext) throws AuthException {

        StringBuilder payload = new StringBuilder();

        payload.append(AuthConstants.CLIENT_CRED_GRANT_TYPE);
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

        return omFactory.createOMElement(AuthConstants.CLIENT_CREDENTIALS, SynapseConstants.SYNAPSE_OMNAMESPACE);
    }

    @Override
    protected int getHash(MessageContext messageContext) throws AuthException {
        return Objects.hash(messageContext.getTo().getAddress(), OAuthUtils.resolveExpression(getTokenUrl(), messageContext),
                OAuthUtils.resolveExpression(getClientId(), messageContext), OAuthUtils.resolveExpression(getClientSecret(),
                        messageContext), getRequestParametersAsString(messageContext),
                getResolvedCustomHeadersMap(getCustomHeadersMap(), messageContext));
    }
}

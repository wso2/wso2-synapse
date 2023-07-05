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
 * This class is used to handle Client Credentials grant oauth.
 */
public class ClientCredentialsHandler extends OAuthHandler {

    public ClientCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret, String authMode,
                                    int connectionTimeout, int connectionRequestTimeout, int socketTimeout) {

        super(tokenApiUrl, clientId, clientSecret, authMode, connectionTimeout, connectionRequestTimeout, socketTimeout);
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
}

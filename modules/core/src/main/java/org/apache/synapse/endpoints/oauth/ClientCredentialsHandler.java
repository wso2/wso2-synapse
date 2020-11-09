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

/**
 * This class is used to handle Client Credentials grant oauth
 */
public class ClientCredentialsHandler extends OAuthHandler {

    private final String clientId;
    private final String clientSecret;

    public ClientCredentialsHandler(String tokenApiUrl, String clientId, String clientSecret) {

        super(tokenApiUrl);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    protected String buildTokenRequestPayload() {

        StringBuilder payload = new StringBuilder();

        payload.append(OAuthConstants.CLIENT_CRED_GRANT_TYPE);
        payload.append(OAuthConstants.PARAM_CLIENT_ID).append(clientId);
        payload.append(OAuthConstants.PARAM_CLIENT_SECRET).append(clientSecret);

        return payload.toString();
    }

    @Override
    protected String getEncodedCredentials() {

        return Base64Utils.encode((clientId + ":" + clientSecret).getBytes());
    }

    /**
     * Return the client id relevant to the Client Credentials Handler
     *
     * @return String client id
     */
    public String getClientId() {

        return clientId;
    }

    /**
     * Return the client secret relevant to the Client Credentials Handler
     *
     * @return String client secret
     */
    public String getClientSecret() {

        return clientSecret;
    }
}

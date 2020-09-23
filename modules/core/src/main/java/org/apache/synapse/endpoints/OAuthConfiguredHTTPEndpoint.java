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

package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.oauth.OAuthException;
import org.apache.synapse.endpoints.oauth.OAuthHandler;

/**
 * This class represents a http endpoint with oauth configured
 * This will configure the oauth headers and call the send method in HTTP endpoint
 */
public class OAuthConfiguredHTTPEndpoint extends HTTPEndpoint {

    private OAuthHandler oauthHandler;

    @Override
    public void send(MessageContext synCtx) {

        processOAuth(synCtx);
        super.send(synCtx);
    }

    private void processOAuth(MessageContext synCtx) {

        try {
            oauthHandler.setOAuthHeader(synCtx);
        } catch (OAuthException e) {
            log.warn("Could not generate access token for endpoint " + this.getName() + " : " + e.getMessage());
        }
    }

    @Override
    public void destroy() {

        oauthHandler.removeTokenFromCache();
        super.destroy();
    }

    public OAuthHandler getOauthHandler() {

        return oauthHandler;
    }

    public void setOauthHandler(OAuthHandler oauthHandler) {

        this.oauthHandler = oauthHandler;
    }
}

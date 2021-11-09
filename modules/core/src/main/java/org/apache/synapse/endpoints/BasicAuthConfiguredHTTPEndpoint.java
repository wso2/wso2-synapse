/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints;

import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.AuthHandler;
import org.apache.synapse.endpoints.auth.basicauth.BasicAuthHandler;
import org.apache.synapse.endpoints.auth.oauth.OAuthUtils;

/**
 * This class represents a http endpoint with basic auth configured
 * This will configure the basic auth headers and call the send method in HTTP endpoint
 */
public class BasicAuthConfiguredHTTPEndpoint extends HTTPEndpoint  {

    private final BasicAuthHandler basicAuthHandler;

    public BasicAuthConfiguredHTTPEndpoint(AuthHandler authHandler) {
        this.basicAuthHandler = (BasicAuthHandler) authHandler;
    }

    @Override
    public void send(MessageContext synCtx) {
        try {
            basicAuthHandler.setAuthHeader(synCtx);
            super.send(synCtx);

        } catch (AuthException e) {
            handleError(synCtx,"Error while setting basic auth header", e);
        }
    }

    public BasicAuthHandler getBasicAuthHandler() {
        return basicAuthHandler;
    }

    /**
     * This method will send a Internal Server Error to the client and throw a Synapse exception
     *
     * @param synCtx    Original Synapse MessageContext that went through this endpoint
     * @param exception Exception
     * @param message   Error message
     */
    private void handleError(MessageContext synCtx, String message, Exception exception) {

        OAuthUtils.sendOAuthFault(synCtx);
        handleException(message, exception);
    }
}

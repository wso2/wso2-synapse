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

import com.damnhandy.uri.template.VariableExpansionException;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.auth.AuthHandler;
import org.apache.synapse.endpoints.auth.oauth.MessageCache;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.oauth.OAuthHandler;
import org.apache.synapse.endpoints.auth.oauth.OAuthUtils;
import org.apache.synapse.util.MessageHelper;

/**
 * This class represents a http endpoint with oauth configured
 * This will configure the oauth headers and call the send method in HTTP endpoint
 */
public class OAuthConfiguredHTTPEndpoint extends HTTPEndpoint {

    private final OAuthHandler oAuthHandler;

    public OAuthConfiguredHTTPEndpoint(AuthHandler authHandler) {

        this.oAuthHandler = (OAuthHandler) authHandler;
    }

    @Override
    public void send(MessageContext synCtx) {

        try {
            setResolvedUrlTemplate(synCtx);
            oAuthHandler.setAuthHeader(synCtx);

            // If this a blocking call, add 401 as a non error http status code
            if (synCtx.getProperty(SynapseConstants.BLOCKING_MSG_SENDER) != null) {
                OAuthUtils.append401HTTPSC(synCtx);
            }

            // Clone the original MessageContext and save it to do a retry after a token refresh
            MessageContext cloneMessageContext = MessageHelper.cloneMessageContext(synCtx);
            MessageCache.getInstance().addMessageContext(synCtx.getMessageID(), cloneMessageContext);

            super.send(synCtx);

        } catch (AuthException e) {
            handleError(synCtx,
                    "Could not generate access token for oauth configured http endpoint " + this.getName(), e);
        } catch (AxisFault axisFault) {
            handleError(synCtx,
                    "Error cloning the message context for oauth configured http endpoint " + this.getName(),
                    axisFault);
        }
    }

    /**
     * This method is called when we need to retry a call to the resource with a new token
     *
     * @param synCtx Original Synapse MessageContext that went through this endpoint
     * @return MessageContext response obtained after a retry
     */
    public MessageContext retryCallWithNewToken(MessageContext synCtx) {
        // remove the existing token from the cache so that a new token is generated
        try {
            // set RETRIED_ON_OAUTH_FAILURE property to true
            synCtx.setProperty(AuthConstants.RETRIED_ON_OAUTH_FAILURE, true);
            oAuthHandler.removeTokenFromCache(synCtx);
            send(synCtx);
        } catch (AuthException e) {
            handleError(synCtx,
                    "Error removing access token for oauth configured http endpoint " + this.getName(), e);
        }
        return synCtx;
    }

    @Override
    public void destroy() {

        oAuthHandler.removeTokensFromCache();
        super.destroy();
    }

    public OAuthHandler getOauthHandler() {

        return oAuthHandler;
    }

    /**
     * This method will log the error and call the fault sequence
     *
     * @param synCtx    Original Synapse MessageContext that went through this endpoint
     * @param exception Exception
     * @param message   Error message
     */
    private void handleError(MessageContext synCtx, String message, Exception exception) {

        String errorMsg = message + " " + exception.getMessage();
        log.error(errorMsg);
        informFailure(synCtx, SynapseConstants.ENDPOINT_AUTH_FAILURE, errorMsg);
    }

    private void setResolvedUrlTemplate(MessageContext messageContext) {
        String resolvedUrl = resolveUrlTemplate(messageContext);
        if (resolvedUrl != null) {
            messageContext.setTo(new EndpointReference(resolvedUrl));
            if (super.getDefinition() != null) {
                messageContext.setProperty(EndpointDefinition.DYNAMIC_URL_VALUE, resolvedUrl);
            }
        }
    }

    @Override
    protected void processUrlTemplate(MessageContext synCtx) throws VariableExpansionException {
        // Since we set the resolved URL in the OAuthConfiguredHTTPEndpoint.send method
        // return the processUrlTemplate to skip re-resolving at HTTPEndpoint.
        return;
    }
}

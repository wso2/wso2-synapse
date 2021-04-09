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

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.oauth.MessageCache;
import org.apache.synapse.endpoints.oauth.OAuthConstants;
import org.apache.synapse.endpoints.oauth.OAuthException;
import org.apache.synapse.endpoints.oauth.OAuthHandler;
import org.apache.synapse.util.MessageHelper;

/**
 * This class represents a http endpoint with oauth configured
 * This will configure the oauth headers and call the send method in HTTP endpoint
 */
public class OAuthConfiguredHTTPEndpoint extends HTTPEndpoint {

    private final OAuthHandler oAuthHandler;

    public OAuthConfiguredHTTPEndpoint(OAuthHandler oAuthHandler) {

        this.oAuthHandler = oAuthHandler;
    }

    @Override
    public void send(MessageContext synCtx) {

        try {
            oAuthHandler.setOAuthHeader(synCtx);

            // If this a blocking call, add 401 as a non error http status code
            if (synCtx.getProperty(SynapseConstants.BLOCKING_MSG_SENDER) != null) {
                org.apache.axis2.context.MessageContext axis2Ctx =
                        ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                        String.valueOf(OAuthConstants.HTTP_SC_UNAUTHORIZED));
            }

            // Clone the original MessageContext and save it to do a retry after a token refresh
            MessageContext cloneMessageContext = MessageHelper.cloneMessageContext(synCtx);
            MessageCache.getInstance().addMessageContext(synCtx.getMessageID(), cloneMessageContext);

            super.send(synCtx);

        } catch (OAuthException e) {
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
        oAuthHandler.removeTokenFromCache();
        // set RETRIED_ON_OAUTH_FAILURE property to true
        synCtx.setProperty(OAuthConstants.RETRIED_ON_OAUTH_FAILURE, true);
        send(synCtx);
        return synCtx;
    }

    @Override
    public void destroy() {

        oAuthHandler.removeTokenFromCache();
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
        informFailure(synCtx, SynapseConstants.ENDPOINT_OAUTH_FAILURE, errorMsg);
    }
}

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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.resolvers.ResolverFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.transport.passthru.PassThroughConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * Helper class to build OAuth handlers using the synapse configuration of the endpoints
 */
public class OAuthUtils {

    private static final Log log = LogFactory.getLog(OAuthUtils.class);

    /**
     * This method will return an OAuthHandler instance depending on the oauth configs
     *
     * @param httpElement Element containing http configs
     * @return OAuthHandler object
     * @throws OAuthException throw exception for invalid oauth configs
     */
    public static OAuthHandler getOAuthHandler(OMElement httpElement) throws OAuthException {

        if (httpElement != null) {
            OMElement authElement = httpElement.getFirstChildWithName(
                    new QName(SynapseConstants.SYNAPSE_NAMESPACE, OAuthConstants.AUTHENTICATION));

            if (authElement != null) {
                OMElement oauthElement = authElement.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, OAuthConstants.OAUTH));

                if (oauthElement != null) {

                    OAuthHandler oAuthHandler = getSpecificOAuthHandler(oauthElement);
                    if (oAuthHandler != null) {
                        return oAuthHandler;
                    } else {
                        throw new OAuthException("Invalid OAuth configuration");
                    }
                }
            }
        }
        return null;
    }

    /**
     * This method will return an OAuthHandler instance depending on the oauth configs
     *
     * @param oauthElement Element containing OAuth configs
     * @return OAuthHandler object
     */
    private static OAuthHandler getSpecificOAuthHandler(OMElement oauthElement) {

        OAuthHandler oAuthHandler = null;

        OMElement authCodeElement = oauthElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, OAuthConstants.AUTHORIZATION_CODE));

        OMElement clientCredentialsElement = oauthElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, OAuthConstants.CLIENT_CREDENTIALS));

        if (authCodeElement != null && clientCredentialsElement != null) {
            if (log.isDebugEnabled()) {
                log.error("Invalid OAuth configuration: AuthorizationCode and ClientCredentials grants are not " +
                        "allowed together");
            }
            return null;
        }

        if (authCodeElement != null) {
            oAuthHandler = getAuthorizationCodeHandler(authCodeElement);
        }

        if (clientCredentialsElement != null) {
            oAuthHandler = getClientCredentialsHandler(clientCredentialsElement);
        }
        return oAuthHandler;
    }

    /**
     * Method to get a AuthorizationCodeHandler
     *
     * @param authCodeElement Element containing authorization code configs
     * @return AuthorizationCodeHandler object
     */
    private static AuthorizationCodeHandler getAuthorizationCodeHandler(OMElement authCodeElement) {

        String clientId = getChildValue(authCodeElement, OAuthConstants.OAUTH_CLIENT_ID);
        String clientSecret = getChildValue(authCodeElement, OAuthConstants.OAUTH_CLIENT_SECRET);
        String refreshToken = getChildValue(authCodeElement, OAuthConstants.OAUTH_REFRESH_TOKEN);
        String tokenApiUrl = getChildValue(authCodeElement, OAuthConstants.TOKEN_API_URL);

        if (clientId == null || clientSecret == null || refreshToken == null || tokenApiUrl == null) {
            if (log.isDebugEnabled()) {
                log.error("Invalid AuthorizationCode configuration");
            }
            return null;
        }
        AuthorizationCodeHandler handler = new AuthorizationCodeHandler(tokenApiUrl, clientId, clientSecret,
                refreshToken);
        if (hasRequestParameters(authCodeElement)) {
            Map<String, String> requestParameters = getRequestParameters(authCodeElement);
            if (requestParameters == null) {
                return null;
            }
            handler.setRequestParameters(requestParameters);
        }
        return handler;
    }

    /**
     * Method to get a ClientCredentialsHandler
     *
     * @param clientCredentialsElement Element containing client credentials configs
     * @return ClientCredentialsHandler object
     */
    private static ClientCredentialsHandler getClientCredentialsHandler(
            OMElement clientCredentialsElement) {

        String clientId = getChildValue(clientCredentialsElement, OAuthConstants.OAUTH_CLIENT_ID);
        String clientSecret = getChildValue(clientCredentialsElement, OAuthConstants.OAUTH_CLIENT_SECRET);
        String tokenApiUrl = getChildValue(clientCredentialsElement, OAuthConstants.TOKEN_API_URL);

        if (clientId == null || clientSecret == null || tokenApiUrl == null) {
            if (log.isDebugEnabled()) {
                log.error("Invalid ClientCredentials configuration");
            }
            return null;
        }
        ClientCredentialsHandler handler = new ClientCredentialsHandler(tokenApiUrl, clientId, clientSecret);
        if (hasRequestParameters(clientCredentialsElement)) {
            Map<String, String> requestParameters = getRequestParameters(clientCredentialsElement);
            if (requestParameters == null) {
                return null;
            }
            handler.setRequestParameters(requestParameters);
        }
        return handler;
    }

    /**
     * Method to return the request parameters as a Map
     *
     * @param oauthElement OAuth config OMElement
     * @return Map<String, String> containing request parameters
     */
    private static Map<String, String> getRequestParameters(OMElement oauthElement) {

        HashMap<String, String> parameterMap = new HashMap<>();

        OMElement requestParametersElement = oauthElement.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        OAuthConstants.REQUEST_PARAMETERS));

        Iterator parameters =
                requestParametersElement.getChildrenWithName(
                        new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, OAuthConstants.REQUEST_PARAMETER));

        while (parameters.hasNext()) {
            OMElement parameter = (OMElement) parameters.next();
            String paramName = parameter.getAttributeValue(new QName(OAuthConstants.NAME));
            String paramValue = parameter.getText().trim();
            if (StringUtils.isBlank(paramName) || StringUtils.isBlank(paramValue)) {
                if (log.isDebugEnabled()) {
                    log.error("Invalid Request Parameters in OAuth configuration");
                }
                return null;
            }
            paramValue = ResolverFactory.getInstance().getResolver(paramValue).resolve();
            parameterMap.put(paramName, paramValue);
        }
        return parameterMap;
    }

    /**
     * Method to check whether there are request parameters are defined in the OAuth config
     *
     * @param oauthElement OAuth config OMElement
     * @return true if there are request parameters in the oauth element
     */
    private static boolean hasRequestParameters(OMElement oauthElement) {

        OMElement requestParametersElement = oauthElement.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        OAuthConstants.REQUEST_PARAMETERS));
        return (requestParametersElement != null && requestParametersElement.getChildrenWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        OAuthConstants.REQUEST_PARAMETER)).hasNext());
    }

    /**
     * Method to get the value inside a child element
     *
     * @param parentElement Parent OMElement
     * @param childName     name of the child
     * @return String containing the value of the child
     */
    private static String getChildValue(OMElement parentElement, String childName) {

        OMElement childElement = parentElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, childName));

        if (hasANonEmptyValue(childElement)) {
            return ResolverFactory.getInstance().getResolver(childElement.getText().trim()).resolve();
        }
        return null;
    }

    /**
     * Method to check whether a non empty value is present inside an OMelement
     *
     * @param childElement OMElement
     * @return true if there is a non empty value inside the element
     */
    private static boolean hasANonEmptyValue(OMElement childElement) {

        return childElement != null && StringUtils.isNotBlank(childElement.getText());
    }

    /**
     * Method to generate a random id for each OAuth handler
     *
     * @return String containing random id
     */
    public static String getRandomOAuthHandlerID() {

        String uuid = UIDGenerator.generateUID();
        return OAuthConstants.OAUTH_PREFIX + uuid;
    }

    /**
     * Method to check whether retry is needed
     *
     * @param httpEndpoint     OAuth Configured HTTP Endpoint related to the message context
     * @param synapseInMsgCtx  MessageContext that has been received
     * @param synapseOutMsgCtx Corresponding outgoing Synapse MessageContext for the above received MessageContext
     * @return true if the call needs to be retried
     */
    public static boolean retryOnOAuthFailure(OAuthConfiguredHTTPEndpoint httpEndpoint, MessageContext synapseInMsgCtx,
                                              MessageContext synapseOutMsgCtx) {

        Boolean hasRetried = (Boolean) synapseOutMsgCtx.getProperty(OAuthConstants.RETRIED_ON_OAUTH_FAILURE);
        if (hasRetried != null && hasRetried) {
            return false;
        }

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();

        Object statusCode = axis2MessageContext.getProperty(PassThroughConstants.HTTP_SC);

        if (statusCode != null) {
            try {
                int httpStatus =
                        Integer.parseInt(axis2MessageContext.getProperty(PassThroughConstants.HTTP_SC).toString());
                if (httpStatus == OAuthConstants.HTTP_SC_UNAUTHORIZED) {
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Unable to set the HTTP status code from the property " + PassThroughConstants.HTTP_SC
                        + " with value: " + statusCode);
            }
        }
        return false;
    }

    /**
     * Method to append 401 status code to NON_ERROR_HTTP_STATUS_CODES property
     *
     * @param synCtx MessageContext of the request
     */
    public static void append401HTTPSC(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext axis2Ctx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Object nonErrorCodesInMsgCtx = axis2Ctx.getProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES);
        if (nonErrorCodesInMsgCtx instanceof Set) {
            Set<Integer> nonErrorCodes = (Set<Integer>) nonErrorCodesInMsgCtx;
            nonErrorCodes.add(OAuthConstants.HTTP_SC_UNAUTHORIZED);
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    nonErrorCodes);
        } else if (nonErrorCodesInMsgCtx instanceof String) {
            String strNonErrorCodes = ((String) nonErrorCodesInMsgCtx).trim();
            if (strNonErrorCodes.contains(String.valueOf(OAuthConstants.HTTP_SC_UNAUTHORIZED))) {
                return;
            }
            if (!strNonErrorCodes.endsWith(",")) {
                strNonErrorCodes += ",";
            }
            strNonErrorCodes += String.valueOf(OAuthConstants.HTTP_SC_UNAUTHORIZED);
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    strNonErrorCodes);
        } else {
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    String.valueOf(OAuthConstants.HTTP_SC_UNAUTHORIZED));
        }
    }
}

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
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * Helper class to build OAuth handlers using the synapse configuration of the endpoints.
 */
public class OAuthUtils {

    private static final Log log = LogFactory.getLog(OAuthUtils.class);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(\\{[^\"<>}\\]]+})");

    /**
     * This method will return an OAuthHandler instance depending on the oauth configs.
     *
     * @param oauthElement Element containing OAuth configs
     * @return OAuthHandler object
     */
    public static OAuthHandler getSpecificOAuthHandler(OMElement oauthElement) {

        OAuthHandler oAuthHandler = null;

        OMElement authCodeElement = oauthElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.AUTHORIZATION_CODE));

        OMElement clientCredentialsElement = oauthElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.CLIENT_CREDENTIALS));

        OMElement passwordCredentialsElement = oauthElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.PASSWORD_CREDENTIALS));

        if (hasMultipleOAuthConfigs(authCodeElement, clientCredentialsElement, passwordCredentialsElement)) {
            log.error("Invalid OAuth configuration: Multiple OAuth configurations are defined");
            return null;
        }

        if (authCodeElement != null) {
            oAuthHandler = getAuthorizationCodeHandler(authCodeElement);
        } else if (clientCredentialsElement != null) {
            oAuthHandler = getClientCredentialsHandler(clientCredentialsElement);
        } else if (passwordCredentialsElement != null) {
            oAuthHandler = getPasswordCredentialsHandler(passwordCredentialsElement);
        }
        return oAuthHandler;
    }

    /**
     * Method to check whether there are multiple OAuth config defined.
     *
     * @param authCodeElement            OAuth config for authorization code
     * @param clientCredentialsElement   OAuth config for client credentials
     * @param passwordCredentialsElement OAuth config for password credentials
     * @return true if there are multiple OAuth config defined
     */
    private static boolean hasMultipleOAuthConfigs(OMElement authCodeElement, OMElement clientCredentialsElement,
                                                   OMElement passwordCredentialsElement) {

        return authCodeElement != null ?
                (clientCredentialsElement != null || passwordCredentialsElement != null) :
                (clientCredentialsElement != null && passwordCredentialsElement != null);
    }

    /**
     * Method to get a AuthorizationCodeHandler.
     *
     * @param authCodeElement Element containing authorization code configs
     * @return AuthorizationCodeHandler object
     */
    private static AuthorizationCodeHandler getAuthorizationCodeHandler(OMElement authCodeElement) {

        String clientId = getChildValue(authCodeElement, AuthConstants.OAUTH_CLIENT_ID);
        String clientSecret = getChildValue(authCodeElement, AuthConstants.OAUTH_CLIENT_SECRET);
        String refreshToken = getChildValue(authCodeElement, AuthConstants.OAUTH_REFRESH_TOKEN);
        String tokenApiUrl = getChildValue(authCodeElement, AuthConstants.TOKEN_API_URL);
        String authMode = getChildValue(authCodeElement, AuthConstants.OAUTH_AUTHENTICATION_MODE);

        if (clientId == null || clientSecret == null || refreshToken == null || tokenApiUrl == null) {
            log.error("Invalid AuthorizationCode configuration");
            return null;
        }
        AuthorizationCodeHandler handler = new AuthorizationCodeHandler(tokenApiUrl, clientId, clientSecret,
                refreshToken, authMode);
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
     * Method to get a ClientCredentialsHandler.
     *
     * @param clientCredentialsElement Element containing client credentials configs
     * @return ClientCredentialsHandler object
     */
    private static ClientCredentialsHandler getClientCredentialsHandler(
            OMElement clientCredentialsElement) {

        String clientId = getChildValue(clientCredentialsElement, AuthConstants.OAUTH_CLIENT_ID);
        String clientSecret = getChildValue(clientCredentialsElement, AuthConstants.OAUTH_CLIENT_SECRET);
        String tokenApiUrl = getChildValue(clientCredentialsElement, AuthConstants.TOKEN_API_URL);
        String authMode = getChildValue(clientCredentialsElement, AuthConstants.OAUTH_AUTHENTICATION_MODE);

        if (clientId == null || clientSecret == null || tokenApiUrl == null) {
            log.error("Invalid ClientCredentials configuration");
            return null;
        }
        ClientCredentialsHandler handler = new ClientCredentialsHandler(tokenApiUrl, clientId, clientSecret, authMode);
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
     * Method to get a PasswordCredentialsHandler.
     *
     * @param passwordCredentialsElement Element containing password credentials configs
     * @return PasswordCredentialsHandler object
     */
    private static PasswordCredentialsHandler getPasswordCredentialsHandler(
            OMElement passwordCredentialsElement) {

        String clientId = getChildValue(passwordCredentialsElement, AuthConstants.OAUTH_CLIENT_ID);
        String clientSecret = getChildValue(passwordCredentialsElement, AuthConstants.OAUTH_CLIENT_SECRET);
        String username = getChildValue(passwordCredentialsElement, AuthConstants.OAUTH_USERNAME);
        String password = getChildValue(passwordCredentialsElement, AuthConstants.OAUTH_PASSWORD);
        String tokenApiUrl = getChildValue(passwordCredentialsElement, AuthConstants.TOKEN_API_URL);
        String authMode = getChildValue(passwordCredentialsElement, AuthConstants.OAUTH_AUTHENTICATION_MODE);

        if (username == null || password == null || tokenApiUrl == null || clientId == null || clientSecret == null) {
            log.error("Invalid PasswordCredentials configuration");
            return null;
        }
        PasswordCredentialsHandler handler = new PasswordCredentialsHandler(tokenApiUrl, clientId, clientSecret,
                username, password, authMode);
        if (hasRequestParameters(passwordCredentialsElement)) {
            Map<String, String> requestParameters = getRequestParameters(passwordCredentialsElement);
            if (requestParameters == null) {
                return null;
            }
            handler.setRequestParameters(requestParameters);
        }
        return handler;
    }

    /**
     * Method to return the request parameters as a Map.
     *
     * @param oauthElement OAuth config OMElement
     * @return Map<String, String> containing request parameters
     */
    private static Map<String, String> getRequestParameters(OMElement oauthElement) {

        HashMap<String, String> parameterMap = new HashMap<>();

        OMElement requestParametersElement = oauthElement.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        AuthConstants.REQUEST_PARAMETERS));

        Iterator parameters =
                requestParametersElement.getChildrenWithName(
                        new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, AuthConstants.REQUEST_PARAMETER));

        while (parameters.hasNext()) {
            OMElement parameter = (OMElement) parameters.next();
            String paramName = parameter.getAttributeValue(new QName(AuthConstants.NAME));
            String paramValue = parameter.getText().trim();
            if (StringUtils.isBlank(paramName) || StringUtils.isBlank(paramValue)) {
                log.error("Invalid Request Parameters in OAuth configuration");
                return null;
            }
            paramValue = ResolverFactory.getInstance().getResolver(paramValue).resolve();
            parameterMap.put(paramName, paramValue);
        }
        return parameterMap;
    }

    /**
     * Method to check whether there are request parameters are defined in the OAuth config.
     *
     * @param oauthElement OAuth config OMElement
     * @return true if there are request parameters in the oauth element
     */
    private static boolean hasRequestParameters(OMElement oauthElement) {

        OMElement requestParametersElement = oauthElement.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        AuthConstants.REQUEST_PARAMETERS));
        return (requestParametersElement != null && requestParametersElement.getChildrenWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        AuthConstants.REQUEST_PARAMETER)).hasNext());
    }

    /**
     * Method to get the value inside a child element.
     *
     * @param parentElement Parent OMElement
     * @param childName     name of the child
     * @return String containing the value of the child
     */
    public static String getChildValue(OMElement parentElement, String childName) {

        OMElement childElement = parentElement.getFirstChildWithName(new QName(
                SynapseConstants.SYNAPSE_NAMESPACE, childName));

        if (hasANonEmptyValue(childElement)) {
            return ResolverFactory.getInstance().getResolver(childElement.getText().trim()).resolve();
        }
        return null;
    }

    /**
     * Method to check whether a non empty value is present inside an OMelement.
     *
     * @param childElement OMElement
     * @return true if there is a non empty value inside the element
     */
    private static boolean hasANonEmptyValue(OMElement childElement) {

        return childElement != null && StringUtils.isNotBlank(childElement.getText());
    }

    /**
     * Method to generate a random id for each OAuth handler.
     *
     * @return String containing random id
     */
    public static String getRandomOAuthHandlerID() {

        String uuid = UIDGenerator.generateUID();
        return AuthConstants.OAUTH_PREFIX + uuid;
    }

    /**
     * Method to check whether retry is needed.
     *
     * @param httpEndpoint     OAuth Configured HTTP Endpoint related to the message context
     * @param synapseInMsgCtx  MessageContext that has been received
     * @param synapseOutMsgCtx Corresponding outgoing Synapse MessageContext for the above received MessageContext
     * @return true if the call needs to be retried
     */
    public static boolean retryOnOAuthFailure(OAuthConfiguredHTTPEndpoint httpEndpoint, MessageContext synapseInMsgCtx,
                                              MessageContext synapseOutMsgCtx) {

        Boolean hasRetried = (Boolean) synapseOutMsgCtx.getProperty(AuthConstants.RETRIED_ON_OAUTH_FAILURE);
        if (hasRetried != null && hasRetried) {
            synapseInMsgCtx.setProperty(AuthConstants.RETRIED_ON_OAUTH_FAILURE, false);
            return false;
        }

        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();

        Object statusCode = axis2MessageContext.getProperty(PassThroughConstants.HTTP_SC);

        if (statusCode != null) {
            try {
                int httpStatus =
                        Integer.parseInt(axis2MessageContext.getProperty(PassThroughConstants.HTTP_SC).toString());
                if (httpStatus == AuthConstants.HTTP_SC_UNAUTHORIZED) {
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
     * Method to check whether parameter value is an expression.
     *
     * @param value String
     * @return true if the value is an expression
     */
    private static boolean isExpression(String value) {

        Matcher matcher = EXPRESSION_PATTERN.matcher(value);
        return matcher.find();
    }

    /**
     * Method to check whether parameter value is a JSON Path.
     *
     * @param value String
     * @return true if the value is a JSON Path
     */
    private static boolean isJSONPath(String value) {

        return value.startsWith("json-eval(");
    }

    /**
     * Method to evaluate the expression.
     *
     * @param expressionStr  expression String
     * @param messageContext MessageContext of the request
     * @return evaluated String value
     */
    private static String evaluateExpression(String expressionStr, MessageContext messageContext)
            throws AuthException {

        Value expression;
        try {
            if (isJSONPath(expressionStr)) {
                expression = new Value(new SynapseJsonPath(expressionStr.substring(10, expressionStr.length() - 1)));
            } else {
                expression = new Value(new SynapseXPath(expressionStr));
            }
            return expression.evaluateValue(messageContext);
        } catch (JaxenException e) {
            throw new AuthException("Error while building the expression : " + expressionStr);
        }
    }

    /**
     * This method evaluate the value as an expression or return the value.
     *
     * @param value          String parameter value
     * @param messageContext MessageContext of the request
     * @return evaluated String value or the passed value itself
     */
    public static String resolveExpression(String value, MessageContext messageContext) throws AuthException {

        if (isExpression(value)) {
            String expressionStr = value.substring(1, value.length() - 1);
            return evaluateExpression(expressionStr, messageContext);
        }
        return value;
    }

    /**
     * Method to append 401 status code to NON_ERROR_HTTP_STATUS_CODES property.
     *
     * @param synCtx MessageContext of the request
     */
    public static void append401HTTPSC(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext axis2Ctx =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();

        Object nonErrorCodesInMsgCtx = axis2Ctx.getProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES);
        if (nonErrorCodesInMsgCtx instanceof Set) {
            Set<Integer> nonErrorCodes = (Set<Integer>) nonErrorCodesInMsgCtx;
            nonErrorCodes.add(AuthConstants.HTTP_SC_UNAUTHORIZED);
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    nonErrorCodes);
        } else if (nonErrorCodesInMsgCtx instanceof String) {
            String strNonErrorCodes = ((String) nonErrorCodesInMsgCtx).trim();
            if (strNonErrorCodes.contains(String.valueOf(AuthConstants.HTTP_SC_UNAUTHORIZED))) {
                return;
            }
            if (!strNonErrorCodes.endsWith(",")) {
                strNonErrorCodes += ",";
            }
            strNonErrorCodes += String.valueOf(AuthConstants.HTTP_SC_UNAUTHORIZED);
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    strNonErrorCodes);
        } else {
            axis2Ctx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                    String.valueOf(AuthConstants.HTTP_SC_UNAUTHORIZED));
        }
    }

    /**
     * This method returns an OMElement containing the elementValue encapsulated by the elementName.
     *
     * @param elementName  Name of the OMElement
     * @param elementValue Value of the OMElement
     * @return OMElement containing the value encapsulated by the elementName
     */
    public static OMElement createOMElementWithValue(OMFactory omFactory, String elementName, String elementValue) {

        OMElement element = omFactory.createOMElement(elementName, SynapseConstants.SYNAPSE_OMNAMESPACE);
        element.setText(elementValue);
        return element;
    }

    /**
     * Create an OMElement for request parameter map.
     *
     * @param requestParametersMap input parameter map.
     * @return OMElement of parameter map.
     */
    public static OMElement createOMRequestParams(OMFactory omFactory, Map<String, String> requestParametersMap) {

        OMElement requestParameters =
                omFactory.createOMElement(AuthConstants.REQUEST_PARAMETERS, SynapseConstants.SYNAPSE_OMNAMESPACE);
        for (Map.Entry<String, String> entry : requestParametersMap.entrySet()) {
            OMElement parameter =
                    omFactory.createOMElement(AuthConstants.REQUEST_PARAMETER, SynapseConstants.SYNAPSE_OMNAMESPACE);
            parameter.addAttribute(AuthConstants.NAME, entry.getKey(), null);
            parameter.setText(entry.getValue());
            requestParameters.addChild(parameter);
        }
        return requestParameters;
    }
}

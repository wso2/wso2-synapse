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

package org.apache.synapse.endpoints.auth;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.auth.basicauth.BasicAuthHandler;
import org.apache.synapse.endpoints.auth.oauth.OAuthUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthUtils {

    private static final Log log = LogFactory.getLog(AuthUtils.class);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("(\\{[^\"<>}\\]]+})");

    /**
     * This method will return an AuthHandler instance depending on the auth configs
     *
     * @param httpElement Element containing http configs
     * @return AuthHandler object
     * @throws AuthException throw exception for invalid auth configs
     */
    public static AuthHandler getAuthHandler(OMElement httpElement) throws AuthException {

        if (httpElement != null) {
            OMElement authElement = httpElement.getFirstChildWithName(
                    new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.AUTHENTICATION));

            if (authElement != null) {
                OMElement oauthElement = authElement.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.OAUTH));
                OMElement basicAuthElement = authElement.getFirstChildWithName(
                        new QName(SynapseConstants.SYNAPSE_NAMESPACE, AuthConstants.BASIC_AUTH));

                AuthHandler authHandler = null;
                if (oauthElement != null) {
                    authHandler = OAuthUtils.getSpecificOAuthHandler(oauthElement);
                } else if (basicAuthElement != null) {
                    authHandler = getBasicAuthHandler(basicAuthElement);
                }

                // invalid auth configuration
                if (authHandler != null) {
                    return authHandler;
                } else {
                    throw new AuthException("Authentication configuration is invalid");
                }
            }
        }
        return null;
    }

    private static AuthHandler getBasicAuthHandler(OMElement basicAuthElement) {

        String username = OAuthUtils.getChildValue(basicAuthElement, AuthConstants.BASIC_AUTH_USERNAME);
        String password = OAuthUtils.getChildValue(basicAuthElement, AuthConstants.BASIC_AUTH_PASSWORD);

        if (username == null || password == null) {
            log.error("Invalid basicAuth configurations provided");
            return null;
        }
        return new BasicAuthHandler(username, password);
    }

    /**
     * This method evaluate the value as an expression or return the value
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
     * Method to evaluate the expression
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
     * Method to check whether parameter value is an expression
     *
     * @param value String
     * @return true if the value is an expression
     */
    private static boolean isExpression(String value) {

        Matcher matcher = EXPRESSION_PATTERN.matcher(value);
        return matcher.find();
    }

    /**
     * Method to check whether parameter value is a JSON Path
     *
     * @param value String
     * @return true if the value is a JSON Path
     */
    private static boolean isJSONPath(String value) {

        return value.startsWith("json-eval(");
    }
}

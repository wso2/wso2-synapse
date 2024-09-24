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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.AuthUtils;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for OAuthUtils which contains helper functions used to generate OAuth handlers
 * from synapse configurations
 */

@RunWith(Enclosed.class)
public class OAuthUtilsTest {

    private final static String clientId = "clientId";
    private final static String clientSecret = "clientSecret";
    private final static String username = "username";
    private final static String password = "password";
    private final static String refreshToken = "refreshToken";
    private final static String tokenUrl = "tokenUrl";

    /**
     * Tests if the getOAuthHandler method generate the proper OAuthHandler instance
     */
    @RunWith(Parameterized.class)
    public static class OAuthHandlerGeneration {

        private final String configs;
        private final Class handler;

        public OAuthHandlerGeneration(String configs, Class handler) {

            this.configs = configs;
            this.handler = handler;
        }

        @Parameterized.Parameters
        public static Collection provideConfigsForOAuthHandlerTests() {

            String authorizationCodeConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><authorizationCode>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + refreshToken + ">refresh_token</" + refreshToken + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></authorizationCode></oauth></authentication></http>";

            String authorizationCodeConfigWithRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><authorizationCode>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + refreshToken + ">refresh_token</" + refreshToken + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter name=\"account_id\">1234</parameter>" +
                            "</requestParameters>" +
                            "</authorizationCode></oauth></authentication></http>";

            String clientCredentialsConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></clientCredentials></oauth></authentication></http>";

            String clientCredentialsConfigWithRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter name=\"account_id\">{$ctx:accountID}</parameter>" +
                            "</requestParameters>" +
                            "</clientCredentials></oauth></authentication></http>";

            String passwordCredentialsConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><passwordCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + username + ">tester123</" + username + ">" +
                            "<" + password + ">abc@123</" + password + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></passwordCredentials></oauth></authentication></http>";

            String passwordCredentialsConfigWithRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><passwordCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + username + ">tester123</" + username + ">" +
                            "<" + password + ">abc@123</" + password + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter name=\"account_id\">{$ctx:accountID}</parameter>" +
                            "</requestParameters>" +
                            "</passwordCredentials></oauth></authentication></http>";

            return Arrays.asList(new Object[][]{
                    {authorizationCodeConfig, AuthorizationCodeHandler.class},
                    {authorizationCodeConfigWithRequestParams, AuthorizationCodeHandler.class},
                    {clientCredentialsConfig, ClientCredentialsHandler.class},
                    {clientCredentialsConfigWithRequestParams, ClientCredentialsHandler.class},
                    {passwordCredentialsConfig, PasswordCredentialsHandler.class},
                    {passwordCredentialsConfigWithRequestParams, PasswordCredentialsHandler.class}
            });
        }

        @Test
        public void testOAuthHandlerGeneration() throws Exception {

            OMElement oauthElement = AXIOMUtil.stringToOM(configs);

            OAuthHandler oAuthHandler = (OAuthHandler) AuthUtils.getAuthHandler(oauthElement);

            assertThat(oAuthHandler, instanceOf(handler));
        }
    }

    /**
     * Tests if the getOAuthHandler method throw OAuthException exceptions
     */
    @RunWith(Parameterized.class)
    public static class OAuthHandlerGenerationException {

        private final String configs;

        public OAuthHandlerGenerationException(String configs) {

            this.configs = configs;
        }

        @Parameterized.Parameters
        public static Collection provideConfigsForOAuthHandlerExceptionTests() {

            String authorizationCodeConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><authorizationCode>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<refreshTok>refresh_token</refreshTok>" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></authorizationCode></oauth></authentication></http>";

            String authorizationCodeConfigWithInvalidRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><authorizationCode>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + refreshToken + ">refresh_token</" + refreshToken + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter name=\"account_id\"></parameter>" +
                            "</requestParameters>" +
                            "</authorizationCode></oauth></authentication></http>";

            String clientCredentialsConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></clientCredentials></oauth></authentication></http>";

            String clientCredentialsConfigWithInvalidRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter>abc@123</parameter>" +
                            "</requestParameters>" +
                            "</clientCredentials></oauth></authentication></http>";

            String passwordCredentialsConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><passwordCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + password + ">abc@123</" + password + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></passwordCredentials></oauth></authentication></http>";

            String passwordCredentialsConfigWithInvalidRequestParams =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><passwordCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + username + ">tester123</" + username + ">" +
                            "<" + password + ">abc@123</" + password + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "<requestParameters>" +
                            "   <parameter>abc@123</parameter>" +
                            "</requestParameters>" +
                            "</passwordCredentials></oauth></authentication></http>";

            String multipleOAuthConfig =
                    "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><passwordCredentials>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + username + ">tester123</" + username + ">" +
                            "<" + password + ">abc@123</" + password + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                            "></passwordCredentials>" +
                            "<authorizationCode>" +
                            "<" + clientId + ">client_id</" + clientId + ">" +
                            "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                            "<" + refreshToken + ">refresh_token</" + refreshToken + ">" +
                            "<" + tokenUrl + ">oauth_server_url</" + tokenUrl + ">" +
                            "</authorizationCode></oauth></authentication></http>";

            return Arrays.asList(new Object[][]{
                    {authorizationCodeConfig},
                    {authorizationCodeConfigWithInvalidRequestParams},
                    {clientCredentialsConfig},
                    {clientCredentialsConfigWithInvalidRequestParams},
                    {passwordCredentialsConfig},
                    {passwordCredentialsConfigWithInvalidRequestParams},
                    {multipleOAuthConfig}
            });
        }

        @Test
        public void testOAuthHandlerGenerationException() throws Exception {

            OMElement oauthElement = AXIOMUtil.stringToOM(configs);

            try {
                OAuthHandler oAuthHandler = (OAuthHandler) AuthUtils.getAuthHandler(oauthElement);
                Assert.fail("This method must throw an OAuthException");
            } catch (AuthException e) {
                Assert.assertEquals("Authentication configuration is invalid", e.getMessage());
            }
        }
    }

    /**
     * Tests retryOnOauthFailure method
     */
    @RunWith(Parameterized.class)
    public static class RetryOnOAuthFailure {

        private final OAuthConfiguredHTTPEndpoint httpEndpoint;
        private final MessageContext messageContextIn;
        private final MessageContext messageContextOut;
        private final boolean expected;

        public RetryOnOAuthFailure(OAuthConfiguredHTTPEndpoint httpEndpoint, MessageContext messageContextIn,
                                   MessageContext messageContextOut,
                                   boolean expected) {

            this.httpEndpoint = httpEndpoint;
            this.messageContextIn = messageContextIn;
            this.messageContextOut = messageContextOut;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection provideDataForRetryOnOauthFailureTests() throws AxisFault {

            OAuthHandler oAuthHandler =
                    new AuthorizationCodeHandler("oauth_server_url", "client_id", "client_secret",
                            "refresh_token", "header", -1, -1, -1, TokenCache.getInstance());

            OAuthConfiguredHTTPEndpoint httpEndpoint = new OAuthConfiguredHTTPEndpoint(oAuthHandler);

            MessageContext messageContextIn = createMessageContext();
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContextIn).getAxis2MessageContext();
            axis2MessageContext.setProperty(PassThroughConstants.HTTP_SC, 401);
            MessageContext messageContextOut1 = createMessageContext();
            MessageContext messageContextOut2 = createMessageContext();

            messageContextOut2.setProperty(AuthConstants.RETRIED_ON_OAUTH_FAILURE, true);

            return Arrays.asList(new Object[][]{
                    {httpEndpoint, messageContextIn, messageContextOut1, true},
                    {httpEndpoint, messageContextIn, messageContextOut2, false}
            });
        }

        @Test
        public void testRetryOnOAuthFailure() {

            assertEquals(expected,
                    OAuthUtils.retryOnOAuthFailure(httpEndpoint, messageContextIn, messageContextOut));

        }
    }

    /**
     * Tests resolveExpression method
     */
    @RunWith(Parameterized.class)
    public static class ResolveExpression {

        private final MessageContext messageContext;
        private final String expression;
        private final String resolvedValue;

        public ResolveExpression(MessageContext messageContext,
                                 String expression, String resolvedValue) {

            this.messageContext = messageContext;
            this.expression = expression;
            this.resolvedValue = resolvedValue;
        }

        @Parameterized.Parameters
        public static Collection provideDataForResolveExpressionTests() throws AxisFault {

            MessageContext messageContext = createMessageContext();
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            messageContext.setProperty("user_role", "tester");
            messageContext.setProperty("client_id", "q4306sjzviquusd23ojcldr");
            setJsonPayLoad(axis2MessageContext, "{\"account_id\":\"1234\"}");

            return Arrays.asList(new Object[][]{
                    {messageContext, "abc#&123", "abc#&123"},
                    {messageContext, "{$ctx:user_role}", "tester"},
                    {messageContext, "{get-property('client_id')}", "q4306sjzviquusd23ojcldr"},
                    {messageContext, "{json-eval($.account_id)}", "1234"},
            });
        }

        @Test
        public void testResolveExpression() throws AuthException {

            assertEquals(resolvedValue,
                    OAuthUtils.resolveExpression(expression, messageContext));

        }
    }

    /**
     * Tests if the resolveExpression method throw OAuthException exceptions
     */
    @RunWith(Parameterized.class)
    public static class ResolveExpressionException {

        private final MessageContext messageContext;
        private final String expression;
        private final String resolvedValue;

        public ResolveExpressionException(MessageContext messageContext,
                                          String expression, String resolvedValue) {

            this.messageContext = messageContext;
            this.expression = expression;
            this.resolvedValue = resolvedValue;
        }

        @Parameterized.Parameters
        public static Collection provideDataForResolveExpressionExceptionTests() throws AxisFault {

            MessageContext messageContext = createMessageContext();
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            messageContext.setProperty("user_role", "tester");
            messageContext.setProperty("client_id", "q4306sjzviquusd23ojcldr");
            setJsonPayLoad(axis2MessageContext, "{\"account_id\":\"1234\"}");

            return Arrays.asList(new Object[][]{
                    {messageContext, "{ctx:user_role}", "tester"},
                    {messageContext, "{get_property('client_id')}", "q4306sjzviquusd23ojcldr"},
                    {messageContext, "{json-eval.($.account_id)}", "1234"},
            });
        }

        @Test
        public void testResolveExpressionException() throws Exception {

            try {
                OAuthUtils.resolveExpression(expression, messageContext);
                Assert.fail("This method must throw an OAuthException");
            } catch (SynapseException e) {
                assertThat(e.getMessage(), containsString("resulted in an error"));
            } catch (AuthException e) {
                assertThat(e.getMessage(), containsString("Error while building the expression"));
            }
        }
    }

    /**
     * Tests retryOnOauthFailure method
     */
    @RunWith(Parameterized.class)
    public static class Append401HTTPSC {

        private final MessageContext messageContext;
        private final String expected;

        public Append401HTTPSC(MessageContext messageContext, String expected) {

            this.messageContext = messageContext;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection provideDataForAppend401HTTPSCTests() throws AxisFault {

            MessageContext messageContext = createMessageContext();

            MessageContext messageContext2 = createMessageContext();
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContext2).getAxis2MessageContext();
            axis2MessageContext.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES, "403");

            return Arrays.asList(new Object[][]{
                    {messageContext, "401"},
                    {messageContext2, "401, 403"},
            });
        }

        @Test
        public void testAppend401HTTPSC() {

            OAuthUtils.append401HTTPSC(messageContext);

            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            Object nonErrorCodesInMsgCtx = axis2MessageContext.getProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES);

            assertTrue(nonErrorCodesInMsgCtx instanceof String);
            String strNonErrorCodes = ((String) nonErrorCodesInMsgCtx).trim();
            for (String strRetryErrorCode : expected.split(",")) {
                assertTrue(strNonErrorCodes.contains(strRetryErrorCode.trim()));
            }
        }
    }

    /**
     * Create a empty message context
     *
     * @return A context with empty message
     * @throws AxisFault on an error creating a context
     */
    private static MessageContext createMessageContext() throws AxisFault {

        Axis2SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(new SynapseConfiguration());
        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        axis2MC.setTransportIn(new TransportInDescription("http"));
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }

    /**
     * Set a json payload to the message context
     *
     * @param axis2MessageContext Test message context
     * @param jsonString          Json payload in string format
     * @throws AxisFault on an error when setting json payload
     */
    public static void setJsonPayLoad(org.apache.axis2.context.MessageContext axis2MessageContext, String jsonString)
            throws AxisFault {

        JsonUtil.getNewJsonPayload(axis2MessageContext, jsonString, true, true);
    }
}

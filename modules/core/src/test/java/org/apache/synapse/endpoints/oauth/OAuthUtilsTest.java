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
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for OAuthUtils which contains helper functions used to generate OAuth handlers
 * from synapse configurations
 */

public class OAuthUtilsTest {

    String clientId = "clientId";
    String clientSecret = "clientSecret";
    String refreshToken = "refreshToken";
    String tokenUrl = "tokenUrl";

    /**
     * Tests if the getOAuthHandler method generate an AuthorizationCodeHandler instance
     *
     * @throws XMLStreamException if error occurs when building the sample OMElement
     */
    @Test
    public void testAuthorizationCodeHandler() throws Exception {

        String input =
                "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><authorizationCode>" +
                        "<" + clientId + ">client_id</" + clientId + ">" +
                        "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                        "<" + refreshToken + ">refresh_token</" + refreshToken + ">" +
                        "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                        "></authorizationCode></oauth></authentication></http>";

        OMElement oauthElement = AXIOMUtil.stringToOM(input);

        OAuthHandler oAuthHandler = OAuthUtils.getOAuthHandler(oauthElement);

        assertTrue(oAuthHandler instanceof AuthorizationCodeHandler);
    }

    /**
     * Tests if the getOAuthHandler method generate an ClientCredentialsHandler instance
     *
     * @throws XMLStreamException if error occurs when building the sample OMElement
     */
    @Test
    public void testClientCredentialsHandler() throws Exception {

        String input =
                "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                        "<" + clientId + ">client_id</" + clientId + ">" +
                        "<" + clientSecret + ">client_secret</" + clientSecret + ">" +
                        "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                        "></clientCredentials></oauth></authentication></http>";

        OMElement oauthElement = AXIOMUtil.stringToOM(input);

        OAuthHandler oAuthHandler = OAuthUtils.getOAuthHandler(oauthElement);

        assertTrue(oAuthHandler instanceof ClientCredentialsHandler);
    }

    /**
     * Tests if the getOAuthHandler method generate an ClientCredentialsHandler instance
     *
     * @throws XMLStreamException if error occurs when building the sample OMElement
     */
    @Test
    public void testClientCredentialsHandlerException() throws XMLStreamException {

        String input =
                "<http xmlns=\"http://ws.apache.org/ns/synapse\"><authentication><oauth><clientCredentials>" +
                        "<" + clientId + ">client_id</" + clientId + ">" +
                        "<" + tokenUrl + ">oauth_server_url</" + tokenUrl +
                        "></clientCredentials></oauth></authentication></http>";

        OMElement oauthElement = AXIOMUtil.stringToOM(input);

        try {
            OAuthHandler oAuthHandler = OAuthUtils.getOAuthHandler(oauthElement);
            Assert.fail("This method must throw an OAuthException");
        } catch (OAuthException e) {
            Assert.assertEquals("Invalid OAuth configuration", e.getMessage());
        }
    }

    /**
     * Tests retryOnOauthFailure when 401 HTTP status received
     *
     * @throws AxisFault on an error creating a context
     */
    @Test
    public void testRetryOnOauthFailureWithUnauthorizedStatus() throws AxisFault {

        OAuthHandler oAuthHandler = new AuthorizationCodeHandler("1", "oauth_server_url", "client_id", "client_secret",
                "refresh_token");

        OAuthConfiguredHTTPEndpoint httpEndpoint = new OAuthConfiguredHTTPEndpoint();
        httpEndpoint.setOauthHandler(oAuthHandler);

        MessageContext messageContextIn = createMessageContext();
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContextIn).getAxis2MessageContext();
        axis2MessageContext.setProperty(PassThroughConstants.HTTP_SC, 401);
        MessageContext messageContextOut = createMessageContext();

        assertTrue(OAuthUtils.retryOnOauthFailure(httpEndpoint, messageContextIn, messageContextOut));
    }

    /**
     * Tests retryOnOauthFailure when a retry is already done
     *
     * @throws AxisFault on an error creating a context
     */
    @Test
    public void testRetryOnOauthFailureOnAlreadyRetriedCall() throws AxisFault {

        OAuthHandler oAuthHandler = new AuthorizationCodeHandler("1", "oauth_server_url", "client_id", "client_secret",
                "refresh_token");

        OAuthConfiguredHTTPEndpoint httpEndpoint = new OAuthConfiguredHTTPEndpoint();
        httpEndpoint.setOauthHandler(oAuthHandler);

        MessageContext messageContextIn = createMessageContext();
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContextIn).getAxis2MessageContext();
        axis2MessageContext.setProperty(PassThroughConstants.HTTP_SC, 401);

        MessageContext messageContextOut = createMessageContext();
        messageContextOut.setProperty(OAuthConstants.RETRIED_ON_OAUTH_FAILURE, true);

        assertFalse(OAuthUtils.retryOnOauthFailure(httpEndpoint, messageContextIn, messageContextOut));
    }

    /**
     * Create a empty message context
     *
     * @return A context with empty message
     * @throws AxisFault on an error creating a context
     */
    private MessageContext createMessageContext() throws AxisFault {

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
}

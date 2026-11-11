/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com/).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.endpoints;

import com.damnhandy.uri.template.UriTemplate;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.AsyncCallback;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.oauth.MessageCache;
import org.apache.synapse.endpoints.auth.oauth.OAuthHandler;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that {@link OAuthConfiguredHTTPEndpoint} hands the request clone kept for a token refresh retry only to the
 * callback of its send, so the clone is never held globally and is released with the request or its callback.
 */
public class OAuthConfiguredHTTPEndpointCloneHandOffTest {

    private static final String BACKEND_URL = "http://api.example.com/backend";

    /**
     * A sent request hands its clone to the callback of the send and leaves nothing on the message or in the cache.
     */
    @Test
    public void testSendHandsCloneToCallback() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        List<AsyncCallback> callbacks = registerCallbacksOnSend(environment);
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);

        Assert.assertEquals("One callback should be registered", 1, callbacks.size());
        MessageContext clone = callbacks.get(0).getOauthCachedMessageContext();
        Assert.assertNotNull("The callback should hold the clone for a token refresh retry", clone);
        Assert.assertNotSame("The callback should hold a clone, not the original context", synCtx, clone);
        assertNothingLeftBehind(synCtx);
    }

    /**
     * Bug scenario: a request to a suspended endpoint fails fast (303001) and must not leave its clone behind.
     */
    @Test
    public void testSendToSuspendedEndpointLeavesNothingBehind() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        endpoint.getContext().onFault();
        Assert.assertFalse("Endpoint should be suspended for this test", endpoint.readyToSend());
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);

        Assert.assertEquals("Request to a suspended endpoint should fail with ENDPOINT_ADDRESS_NONE_READY",
                SynapseConstants.ENDPOINT_ADDRESS_NONE_READY, synCtx.getProperty(SynapseConstants.ERROR_CODE));
        Mockito.verify(environment, Mockito.never()).send(Mockito.any(EndpointDefinition.class),
                Mockito.any(MessageContext.class));
        assertNothingLeftBehind(synCtx);
    }

    /**
     * A send that fails before a callback is registered propagates the failure and leaves nothing behind.
     */
    @Test
    public void testSendFailureLeavesNothingBehind() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        Mockito.doThrow(new SynapseException("dummy send failure")).when(environment)
                .send(Mockito.any(EndpointDefinition.class), Mockito.any(MessageContext.class));
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        MessageContext synCtx = createMessageContext(environment);

        try {
            endpoint.send(synCtx);
            Assert.fail("The send failure should be propagated");
        } catch (SynapseException expected) {
            // expected
        }

        assertNothingLeftBehind(synCtx);
    }

    /**
     * A send that registers no callback (for example out only) leaves nothing behind.
     */
    @Test
    public void testSendWithoutCallbackLeavesNothingBehind() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);

        Mockito.verify(environment).send(Mockito.any(EndpointDefinition.class), Mockito.eq(synCtx));
        assertNothingLeftBehind(synCtx);
    }

    /**
     * When the same message is sent again (failover), each send's callback holds its own clone.
     */
    @Test
    public void testEachSendHandsOverItsOwnClone() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        List<AsyncCallback> callbacks = registerCallbacksOnSend(environment);
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);
        endpoint.send(synCtx);

        Assert.assertEquals("Two callbacks should be registered", 2, callbacks.size());
        Assert.assertNotNull(callbacks.get(0).getOauthCachedMessageContext());
        Assert.assertNotNull(callbacks.get(1).getOauthCachedMessageContext());
        Assert.assertNotSame("Each send should hand over its own clone",
                callbacks.get(0).getOauthCachedMessageContext(), callbacks.get(1).getOauthCachedMessageContext());
        assertNothingLeftBehind(synCtx);
    }

    /**
     * With retry message building disabled no clone is made, so the callback holds nothing.
     */
    @Test
    public void testMessageBuildingDisabledHandsOverNothing() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        environment.getSynapseConfiguration().getProperties()
                .setProperty(AuthConstants.OAUTH_RETRY_MESSAGE_BUILDING_DISABLED, "true");
        List<AsyncCallback> callbacks = registerCallbacksOnSend(environment);
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(Mockito.mock(OAuthHandler.class), environment);
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);

        Assert.assertEquals("One callback should be registered", 1, callbacks.size());
        Assert.assertNull("No clone should be handed over", callbacks.get(0).getOauthCachedMessageContext());
        assertNothingLeftBehind(synCtx);
    }

    /**
     * When the access token cannot be obtained the request is not sent and nothing is left behind.
     */
    @Test
    public void testAuthFailureLeavesNothingBehind() throws Exception {

        Axis2SynapseEnvironment environment = getMockedSynapseEnvironment();
        OAuthHandler oAuthHandler = Mockito.mock(OAuthHandler.class);
        Mockito.doThrow(new AuthException("dummy token endpoint failure"))
                .when(oAuthHandler).setAuthHeader(Mockito.any(MessageContext.class));
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint(oAuthHandler, environment);
        MessageContext synCtx = createMessageContext(environment);

        endpoint.send(synCtx);

        Assert.assertEquals("Auth failure should be reported with ENDPOINT_AUTH_FAILURE",
                SynapseConstants.ENDPOINT_AUTH_FAILURE, synCtx.getProperty(SynapseConstants.ERROR_CODE));
        assertNothingLeftBehind(synCtx);
    }

    private void assertNothingLeftBehind(MessageContext synCtx) {

        Assert.assertNull("The clone hand-off property should be cleared",
                synCtx.getProperty(AuthConstants.OAUTH_CACHED_MESSAGE_CONTEXT));
        Assert.assertNull("No clone should be held in the global MessageCache",
                MessageCache.getInstance().removeMessageContext(synCtx.getMessageID()));
    }

    private List<AsyncCallback> registerCallbacksOnSend(Axis2SynapseEnvironment environment) {

        final List<AsyncCallback> callbacks = new ArrayList<>();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {

                MessageContext sent = (MessageContext) invocation.getArguments()[1];
                callbacks.add(new AsyncCallback(((Axis2MessageContext) sent).getAxis2MessageContext(), sent));
                return null;
            }
        }).when(environment).send(Mockito.any(EndpointDefinition.class), Mockito.any(MessageContext.class));
        return callbacks;
    }

    private OAuthConfiguredHTTPEndpoint createEndpoint(OAuthHandler oAuthHandler,
                                                       Axis2SynapseEnvironment environment) {

        OAuthConfiguredHTTPEndpoint endpoint = new OAuthConfiguredHTTPEndpoint(oAuthHandler);
        endpoint.setName("TestOAuthEndpoint");
        endpoint.setHttpMethod("GET");
        endpoint.setDefinition(new EndpointDefinition());
        endpoint.setUriTemplate(UriTemplate.fromTemplate(BACKEND_URL));
        endpoint.init(environment);
        return endpoint;
    }

    private Axis2SynapseEnvironment getMockedSynapseEnvironment() throws AxisFault {

        final Axis2SynapseEnvironment synapseEnvironment = Mockito.mock(Axis2SynapseEnvironment.class);
        final ConfigurationContext axis2ConfigurationContext = new ConfigurationContext(new AxisConfiguration());
        axis2ConfigurationContext.getAxisConfiguration().addParameter(SynapseConstants.SYNAPSE_ENV,
                synapseEnvironment);
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(axis2ConfigurationContext);
        // Read by OAuthConfiguredHTTPEndpoint.send() to decide whether to clone the request.
        Mockito.when(synapseEnvironment.getSynapseConfiguration()).thenReturn(new SynapseConfiguration());
        // Used by MessageHelper.cloneMessageContext when the endpoint clones the request.
        Mockito.when(synapseEnvironment.createMessageContext()).thenAnswer(new Answer<MessageContext>() {
            @Override
            public MessageContext answer(InvocationOnMock invocation) {

                org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
                axis2MC.setConfigurationContext(axis2ConfigurationContext);
                return new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
            }
        });
        return synapseEnvironment;
    }

    private MessageContext createMessageContext(Axis2SynapseEnvironment environment) throws AxisFault {

        org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));
        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        axis2MC.setTransportIn(new TransportInDescription("http"));
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), environment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }
}

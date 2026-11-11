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
package org.apache.synapse.core.axis2;

import com.damnhandy.uri.template.UriTemplate;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.oauth.OAuthHandler;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that a backend 401 is retried with a new token using the request clone handed over by the send, in
 * non-blocking and blocking calls, and that other responses are not retried.
 */
public class OAuthTokenRefreshRetryTest {

    private static final String BACKEND_URL = "http://api.example.com/backend";

    private Axis2SynapseEnvironment environment;
    private SynapseCallbackReceiver receiver;
    private OAuthHandler oAuthHandler;
    private OAuthConfiguredHTTPEndpoint endpoint;
    private final List<MessageContext> sentMessages = new ArrayList<>();
    private final List<AsyncCallback> callbacks = new ArrayList<>();

    @Before
    public void setUp() throws Exception {

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        environment = getMockedSynapseEnvironment(synapseConfiguration);
        receiver = new SynapseCallbackReceiver(synapseConfiguration, null);
        // Register a callback for each send, as Axis2FlexibleMEPClient does for a non-blocking call
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {

                MessageContext sent = (MessageContext) invocation.getArguments()[1];
                AsyncCallback callback = new AsyncCallback(((Axis2MessageContext) sent).getAxis2MessageContext(),
                        sent);
                sentMessages.add(sent);
                callbacks.add(callback);
                receiver.addCallback(sent.getMessageID(), callback);
                return null;
            }
        }).when(environment).send(Mockito.any(EndpointDefinition.class), Mockito.any(MessageContext.class));
        oAuthHandler = Mockito.mock(OAuthHandler.class);
        endpoint = new OAuthConfiguredHTTPEndpoint(oAuthHandler);
        endpoint.setName("TestOAuthEndpoint");
        endpoint.setHttpMethod("GET");
        endpoint.setDefinition(new EndpointDefinition());
        endpoint.setUriTemplate(UriTemplate.fromTemplate(BACKEND_URL));
        endpoint.init(environment);
    }

    /**
     * A backend 401 resends the clone held by the callback once, with a new token.
     */
    @Test
    public void testBackend401IsRetriedWithCallbackClone() throws Exception {

        MessageContext synCtx = createMessageContext();
        endpoint.send(synCtx);
        MessageContext clone = callbacks.get(0).getOauthCachedMessageContext();
        Assert.assertNotNull("The callback should hold the clone for a token refresh retry", clone);

        receiver.receive(createResponse(synCtx, 401));

        Assert.assertEquals("The 401 should be retried once", 2, sentMessages.size());
        Assert.assertSame("The retry should resend the clone held by the callback", clone, sentMessages.get(1));
        Assert.assertEquals(Boolean.TRUE, clone.getProperty(AuthConstants.RETRIED_ON_OAUTH_FAILURE));
        Mockito.verify(oAuthHandler).removeTokenFromCache(clone);
    }

    /**
     * Any other backend status, for example 503, is not retried.
     */
    @Test
    public void testBackend503IsNotRetried() throws Exception {

        MessageContext synCtx = createMessageContext();
        endpoint.send(synCtx);

        receiver.receive(createResponse(synCtx, 503));

        Assert.assertEquals("A 503 should not be retried", 1, sentMessages.size());
        Mockito.verify(oAuthHandler, Mockito.never()).removeTokenFromCache(Mockito.any(MessageContext.class));
    }

    /**
     * In a blocking call, a backend 401 resends the request with the clone handed over by the endpoint, once.
     */
    @Test
    public void testBlockingBackend401IsRetriedWithHandedOverClone() throws Exception {

        final List<String> sentBodies = new ArrayList<>();
        final List<Integer> statuses = new ArrayList<>();
        statuses.add(401);
        statuses.add(200);
        final BlockingMsgSender blockingMsgSender = createBlockingMsgSender(sentBodies, statuses);
        // Call the blocking sender with the same message, as Axis2FlexibleMEPClient does for a blocking call
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                blockingMsgSender.send((EndpointDefinition) invocation.getArguments()[0],
                        (MessageContext) invocation.getArguments()[1]);
                return null;
            }
        }).when(environment).send(Mockito.any(EndpointDefinition.class), Mockito.any(MessageContext.class));
        MessageContext synCtx = createMessageContext();
        synCtx.getEnvelope().getBody().addChild(OMAbstractFactory.getOMFactory().createOMElement("original", null));
        synCtx.setProperty(SynapseConstants.BLOCKING_MSG_SENDER, blockingMsgSender);
        synCtx.pushFaultHandler(Mockito.mock(FaultHandler.class));

        endpoint.send(synCtx);

        Assert.assertEquals("The 401 should be retried once", 2, sentBodies.size());
        Assert.assertTrue("The retry should resend the original request", sentBodies.get(1).contains("original"));
        Mockito.verify(oAuthHandler).removeTokenFromCache(synCtx);
        Assert.assertEquals("200", ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                .getProperty(SynapseConstants.HTTP_SC));
        Assert.assertNull("The clone hand-off property should be cleared",
                synCtx.getProperty(AuthConstants.OAUTH_CACHED_MESSAGE_CONTEXT));
    }

    private BlockingMsgSender createBlockingMsgSender(final List<String> sentBodies, final List<Integer> statuses)
            throws AxisFault {

        // A transport that records each request and answers with the next status code, instead of calling a backend
        TransportOutDescription transportOut = new TransportOutDescription("http");
        transportOut.setSender(new StubTransportSender(sentBodies, statuses));
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        axisConfiguration.addTransportOut(transportOut);
        BlockingMsgSender blockingMsgSender = new BlockingMsgSender();
        blockingMsgSender.setConfigurationContext(new ConfigurationContext(axisConfiguration));
        blockingMsgSender.init();
        return blockingMsgSender;
    }

    private static class StubTransportSender extends AbstractHandler implements TransportSender {

        private final List<String> sentBodies;
        private final List<Integer> statuses;

        StubTransportSender(List<String> sentBodies, List<Integer> statuses) {

            this.sentBodies = sentBodies;
            this.statuses = statuses;
        }

        @Override
        public InvocationResponse invoke(org.apache.axis2.context.MessageContext msgContext) throws AxisFault {

            sentBodies.add(msgContext.getEnvelope().getBody().toString());
            org.apache.axis2.context.MessageContext response = msgContext.getOperationContext()
                    .getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            response.setEnvelope(OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope());
            // Axis2 copies the status code from the request context to the response context
            msgContext.setProperty(SynapseConstants.HTTP_SENDER_STATUSCODE, statuses.get(sentBodies.size() - 1));
            return InvocationResponse.CONTINUE;
        }

        @Override
        public void cleanup(org.apache.axis2.context.MessageContext msgContext) {
        }

        @Override
        public void init(ConfigurationContext confContext, TransportOutDescription transportOut) {
        }

        @Override
        public void stop() {
        }
    }

    private org.apache.axis2.context.MessageContext createResponse(MessageContext request, int httpStatus)
            throws AxisFault {

        org.apache.axis2.context.MessageContext response = new org.apache.axis2.context.MessageContext();
        response.setConfigurationContext(environment.getAxis2ConfigurationContext());
        response.setMessageID(UIDGenerator.generateURNString());
        response.addRelatesTo(new RelatesTo(request.getMessageID()));
        response.setProperty(PassThroughConstants.HTTP_SC, httpStatus);
        response.setEnvelope(createEnvelope());
        return response;
    }

    private Axis2SynapseEnvironment getMockedSynapseEnvironment(SynapseConfiguration synapseConfiguration)
            throws AxisFault {

        final Axis2SynapseEnvironment synapseEnvironment = Mockito.mock(Axis2SynapseEnvironment.class);
        final ConfigurationContext axis2ConfigurationContext = new ConfigurationContext(new AxisConfiguration());
        axis2ConfigurationContext.getAxisConfiguration().addParameter(SynapseConstants.SYNAPSE_ENV,
                synapseEnvironment);
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(axis2ConfigurationContext);
        Mockito.when(synapseEnvironment.getSynapseConfiguration()).thenReturn(synapseConfiguration);
        Mockito.when(synapseEnvironment.createMessageContext()).thenAnswer(new Answer<MessageContext>() {
            @Override
            public MessageContext answer(InvocationOnMock invocation) {

                org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
                axis2MC.setConfigurationContext(axis2ConfigurationContext);
                return new Axis2MessageContext(axis2MC, synapseConfiguration, synapseEnvironment);
            }
        });
        return synapseEnvironment;
    }

    private MessageContext createMessageContext() throws AxisFault {

        org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(environment.getAxis2ConfigurationContext());
        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        axis2MC.setTransportIn(new TransportInDescription("http"));
        MessageContext mc = new Axis2MessageContext(axis2MC, environment.getSynapseConfiguration(), environment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(createEnvelope());
        return mc;
    }

    private SOAPEnvelope createEnvelope() {

        SOAPEnvelope envelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
        envelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return envelope;
    }
}

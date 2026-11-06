/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.algorithms.RoundRobin;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.oauth.OAuthHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests that a token failure (303003) in an OAuth configured HTTP endpoint that is a child of a failover or
 * load balance endpoint fails the request once, instead of being retried against the same endpoint with no bound,
 * and that genuine transport faults still fail over.
 */
public class OAuthConfiguredHTTPEndpointFailoverTest {

    private static final String TOKEN_ERROR_PREFIX = "Could not generate access token";
    /**
     * Upper bound on token requests, so that an unfixed endpoint fails the test instead of overflowing the stack.
     */
    private static final int MAX_TOKEN_CALLS = 10;

    private Axis2SynapseEnvironment synapseEnvironment;

    /**
     * Creates a mocked Synapse environment that the endpoints and cloned message contexts use.
     */
    @Before
    public void setUp() throws AxisFault {

        synapseEnvironment = Mockito.mock(Axis2SynapseEnvironment.class);
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(configurationContext);
        // Read by the endpoint to check whether message building is disabled for the OAuth retry flow
        Mockito.when(synapseEnvironment.getSynapseConfiguration()).thenReturn(new SynapseConfiguration());
        // Used when the endpoint clones the message context for a retry with a new token
        Mockito.when(synapseEnvironment.createMessageContext()).thenAnswer(new Answer<MessageContext>() {
            @Override
            public MessageContext answer(InvocationOnMock invocation) {

                return new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                        new SynapseConfiguration(), synapseEnvironment);
            }
        });
    }

    /**
     * A token failure in the only child of a failover endpoint fails the request once.
     */
    @Test
    public void testTokenFailureInSingleFailoverChildIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint child = createEndpoint("child1", failingTokenHandler(tokenCalls));
        FailoverEndpoint failover = createFailover(child);

        assertTokenFailureFailsOnce(failover, tokenCalls, 1);
    }

    /**
     * A token failure in the first child of a failover endpoint fails the request once without trying the second child.
     */
    @Test
    public void testTokenFailureInTwoFailoverChildrenIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        AtomicInteger secondTokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", failingTokenHandler(tokenCalls));
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", failingTokenHandler(secondTokenCalls));
        FailoverEndpoint failover = createFailover(child1, child2);

        assertTokenFailureFailsOnce(failover, tokenCalls, 1);
        Assert.assertEquals("The sibling endpoint must not be tried on a token failure", 0, secondTokenCalls.get());
    }

    /**
     * A token failure in the only child of a load balance endpoint fails the request once.
     */
    @Test
    public void testTokenFailureInSingleLoadbalanceChildIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint child = createEndpoint("child1", failingTokenHandler(tokenCalls));
        LoadbalanceEndpoint loadbalance = createLoadbalance(child);

        assertTokenFailureFailsOnce(loadbalance, tokenCalls, 1);
    }

    /**
     * A token failure in the selected child of a load balance endpoint fails the request once.
     */
    @Test
    public void testTokenFailureInTwoLoadbalanceChildrenIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", failingTokenHandler(tokenCalls));
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", failingTokenHandler(tokenCalls));
        LoadbalanceEndpoint loadbalance = createLoadbalance(child1, child2);

        // The two children share one counter: exactly one of them is selected and neither is retried
        assertTokenFailureFailsOnce(loadbalance, tokenCalls, 1);
    }

    /**
     * A failover endpoint nested in another failover endpoint must not be retried by the outer endpoint when its
     * OAuth child fails to get a token, and the outer endpoint's other child must not be tried.
     */
    @Test
    public void testTokenFailureInNestedFailoverIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        AtomicInteger siblingTokenCalls = new AtomicInteger();
        FailoverEndpoint inner = createFailover(createEndpoint("child1", failingTokenHandler(tokenCalls)));
        inner.setName("innerFailoverEP");
        OAuthConfiguredHTTPEndpoint sibling = createEndpoint("child2", failingTokenHandler(siblingTokenCalls));
        FailoverEndpoint outer = createFailover(inner, sibling);

        assertTokenFailureFailsOnce(outer, tokenCalls, 1);
        Assert.assertEquals("The outer sibling endpoint must not be tried on a token failure", 0,
                siblingTokenCalls.get());
    }

    /**
     * A load balance endpoint nested in a failover endpoint must not be retried by the failover endpoint when its
     * OAuth child fails to get a token.
     */
    @Test
    public void testTokenFailureInLoadbalanceNestedInFailoverIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        LoadbalanceEndpoint inner = createLoadbalance(createEndpoint("child1", failingTokenHandler(tokenCalls)));
        FailoverEndpoint outer = createFailover(inner);

        assertTokenFailureFailsOnce(outer, tokenCalls, 1);
    }

    /**
     * A failover endpoint nested in a load balance endpoint must not be retried by the load balance endpoint when
     * its OAuth child fails to get a token.
     */
    @Test
    public void testTokenFailureInFailoverNestedInLoadbalanceIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        FailoverEndpoint inner = createFailover(createEndpoint("child1", failingTokenHandler(tokenCalls)));
        LoadbalanceEndpoint outer = createLoadbalance(inner);

        assertTokenFailureFailsOnce(outer, tokenCalls, 1);
    }

    /**
     * isRetry returns false for an endpoint authentication failure and the default decision for other error codes.
     */
    @Test
    public void testIsRetryForAuthAndNonAuthErrorCodes() throws Exception {

        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint("child1", Mockito.mock(OAuthHandler.class));
        endpoint.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();

        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.ENDPOINT_AUTH_FAILURE);
        Assert.assertFalse(endpoint.isRetry(synCtx));

        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_FAILED);
        Assert.assertTrue(endpoint.isRetry(synCtx));

        synCtx.setProperty(SynapseConstants.ERROR_CODE, null);
        Assert.assertTrue(endpoint.isRetry(synCtx));
    }

    /**
     * A transport fault in an OAuth child of a failover endpoint still fails over to the next child.
     */
    @Test
    public void testTransportFaultInFailoverChildStillFailsOver() throws Exception {

        OAuthHandler handler1 = Mockito.mock(OAuthHandler.class);
        OAuthHandler handler2 = Mockito.mock(OAuthHandler.class);
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", handler1);
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", handler2);
        FailoverEndpoint failover = createFailover(child1, child2);

        assertTransportFaultFailsOver(failover, child1, handler1, handler2, null);
    }

    /**
     * A transport fault in an OAuth child of a load balance endpoint still fails over to the next child.
     */
    @Test
    public void testTransportFaultInLoadbalanceChildStillFailsOver() throws Exception {

        OAuthHandler handler1 = Mockito.mock(OAuthHandler.class);
        OAuthHandler handler2 = Mockito.mock(OAuthHandler.class);
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", handler1);
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", handler2);
        LoadbalanceEndpoint loadbalance = createLoadbalance(child1, child2);

        assertTransportFaultFailsOver(loadbalance, child1, handler1, handler2, null);
    }

    /**
     * An authentication error code left on the message from an earlier failure does not block failover on a later
     * transport fault.
     */
    @Test
    public void testStaleAuthErrorCodeDoesNotBlockFailoverOnTransportFault() throws Exception {

        OAuthHandler handler1 = Mockito.mock(OAuthHandler.class);
        OAuthHandler handler2 = Mockito.mock(OAuthHandler.class);
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", handler1);
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", handler2);
        FailoverEndpoint failover = createFailover(child1, child2);

        assertTransportFaultFailsOver(failover, child1, handler1, handler2, SynapseConstants.ENDPOINT_AUTH_FAILURE);
    }

    /**
     * A token failure in a standalone OAuth endpoint sets the same error properties as before.
     */
    @Test
    public void testStandaloneTokenFailureIsUnchanged() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint("standalone", failingTokenHandler(tokenCalls));
        endpoint.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        synCtx.pushFaultHandler(faultHandler);

        endpoint.send(synCtx);

        Assert.assertEquals(1, tokenCalls.get());
        Mockito.verify(faultHandler, Mockito.times(1)).handleFault(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.never()).send(Mockito.any(), Mockito.any());
        String expectedMessage = "Could not generate access token for oauth configured http endpoint standalone. "
                + "token endpoint returned 401";
        Assert.assertEquals(SynapseConstants.ENDPOINT_AUTH_FAILURE, synCtx.getProperty(SynapseConstants.ERROR_CODE));
        Assert.assertEquals(expectedMessage, synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
        Assert.assertEquals(expectedMessage, synCtx.getProperty(SynapseConstants.ERROR_DETAIL));
        Assert.assertEquals(expectedMessage, synCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
    }

    /**
     * A token failure while retrying with a new token under a failover endpoint is not retried.
     */
    @Test
    public void testTokenFailureOnRetryWithNewTokenUnderFailoverIsNotRetried() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthHandler handler = Mockito.mock(OAuthHandler.class);
        // First token request succeeds, every later one fails
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                int calls = tokenCalls.incrementAndGet();
                if (calls > MAX_TOKEN_CALLS) {
                    throw new IllegalStateException("Token requested " + calls + " times, retry is not bounded");
                }
                if (calls > 1) {
                    throw new AuthException("token endpoint returned 401");
                }
                return null;
            }
        }).when(handler).setAuthHeader(Mockito.any(MessageContext.class));
        OAuthConfiguredHTTPEndpoint child = createEndpoint("child1", handler);
        FailoverEndpoint failover = createFailover(child);
        failover.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        synCtx.pushFaultHandler(faultHandler);

        failover.send(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.times(1)).send(Mockito.any(), Mockito.eq(synCtx));
        // The response (backend 401) has arrived: the child endpoint is no longer the active fault handler
        Assert.assertSame(child, synCtx.getFaultStack().pop());

        child.retryCallWithNewToken(synCtx);

        Assert.assertEquals(2, tokenCalls.get());
        Mockito.verify(synapseEnvironment, Mockito.times(1)).send(Mockito.any(), Mockito.eq(synCtx));
        Mockito.verify(faultHandler, Mockito.times(1)).handleFault(synCtx);
        assertTokenError(synCtx);
    }

    /**
     * A failure to clone the message for a token retry under a failover endpoint fails the request once.
     */
    @Test
    public void testCloneFailureUnderFailoverIsNotRetried() throws Exception {

        OAuthHandler handler = Mockito.mock(OAuthHandler.class);
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", handler);
        OAuthHandler siblingHandler = Mockito.mock(OAuthHandler.class);
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", siblingHandler);
        FailoverEndpoint failover = createFailover(child1, child2);
        failover.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();
        AtomicInteger cloneCalls = new AtomicInteger();
        // Cloning the message context for a later token retry fails with an AxisFault
        Mockito.doAnswer(new Answer<MessageContext>() {
            @Override
            public MessageContext answer(InvocationOnMock invocation) throws Throwable {

                int calls = cloneCalls.incrementAndGet();
                if (calls > MAX_TOKEN_CALLS) {
                    throw new IllegalStateException("Clone attempted " + calls + " times, retry is not bounded");
                }
                throw new AxisFault("clone failed");
            }
        }).when(synapseEnvironment).createMessageContext();
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        synCtx.pushFaultHandler(faultHandler);

        failover.send(synCtx);

        Assert.assertEquals("Clone must be attempted only once", 1, cloneCalls.get());
        Mockito.verify(handler, Mockito.times(1)).setAuthHeader(synCtx);
        Mockito.verify(siblingHandler, Mockito.never()).setAuthHeader(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.never()).send(Mockito.any(), Mockito.any());
        Mockito.verify(faultHandler, Mockito.times(1)).handleFault(synCtx);
        Assert.assertEquals(SynapseConstants.ENDPOINT_AUTH_FAILURE, synCtx.getProperty(SynapseConstants.ERROR_CODE));
        Assert.assertTrue(String.valueOf(synCtx.getProperty(SynapseConstants.ERROR_MESSAGE))
                .startsWith("Error cloning the message context for oauth configured http endpoint child1."));
    }

    /**
     * An endpoint authentication failure is not retried even when its code is listed in the retry enabled error codes.
     */
    @Test
    public void testIsRetryAuthFailureIgnoresRetryEnabledErrorCodes() throws Exception {

        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint("child1", Mockito.mock(OAuthHandler.class));
        endpoint.getDefinition().addRetryEnabledErrorCode(SynapseConstants.ENDPOINT_AUTH_FAILURE);
        endpoint.getDefinition().addRetryEnabledErrorCode(SynapseConstants.NHTTP_CONNECTION_FAILED);
        endpoint.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();

        // A token failure is never retried, even when it is configured as a retry enabled error code
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.ENDPOINT_AUTH_FAILURE);
        Assert.assertFalse(endpoint.isRetry(synCtx));

        // Other codes keep the configured retry enabled behaviour
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_FAILED);
        Assert.assertTrue(endpoint.isRetry(synCtx));
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        Assert.assertFalse(endpoint.isRetry(synCtx));
    }

    /**
     * Retry disabled error codes are still honoured for errors other than an endpoint authentication failure.
     */
    @Test
    public void testIsRetryHonoursRetryDisabledErrorCodes() throws Exception {

        OAuthConfiguredHTTPEndpoint endpoint = createEndpoint("child1", Mockito.mock(OAuthHandler.class));
        endpoint.getDefinition().addRetryDisabledErrorCode(SynapseConstants.NHTTP_CONNECTION_FAILED);
        endpoint.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();

        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_FAILED);
        Assert.assertFalse(endpoint.isRetry(synCtx));
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_TIMEOUT);
        Assert.assertTrue(endpoint.isRetry(synCtx));
    }

    /**
     * After a token failure under a failover endpoint, the child error is kept, the child is not suspended and the next
     * request is sent.
     */
    @Test
    public void testTokenFailureUnderFailoverKeepsChildErrorAndDoesNotSuspendChild() throws Exception {

        OAuthHandler handler = Mockito.mock(OAuthHandler.class);
        AtomicInteger tokenCalls = new AtomicInteger();
        // The first token request fails, the next one succeeds (token endpoint recovered)
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                int calls = tokenCalls.incrementAndGet();
                if (calls > MAX_TOKEN_CALLS) {
                    throw new IllegalStateException("Token requested " + calls + " times, retry is not bounded");
                }
                if (calls == 1) {
                    throw new AuthException("token endpoint returned 401");
                }
                return null;
            }
        }).when(handler).setAuthHeader(Mockito.any(MessageContext.class));
        OAuthConfiguredHTTPEndpoint child = createEndpoint("child1", handler);
        FailoverEndpoint failover = createFailover(child);
        failover.init(synapseEnvironment);

        MessageContext firstCtx = createMessageContext();
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        firstCtx.pushFaultHandler(faultHandler);
        failover.send(firstCtx);

        // The parent's own failure must not overwrite the child's 303003 error with 303000
        Assert.assertSame(child, firstCtx.getProperty(SynapseConstants.LAST_ENDPOINT));
        assertTokenError(firstCtx);
        String expectedMessage = "Could not generate access token for oauth configured http endpoint child1. "
                + "token endpoint returned 401";
        Assert.assertEquals(expectedMessage, firstCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
        Assert.assertEquals(expectedMessage, firstCtx.getProperty(SynapseConstants.ERROR_DETAIL));
        Mockito.verify(faultHandler, Mockito.times(1)).handleFault(firstCtx);

        // A token failure is not a backend fault: the child stays active and serves the next request
        Assert.assertTrue("A token failure must not suspend the child", child.readyToSend());
        Assert.assertTrue(failover.readyToSend());
        MessageContext secondCtx = createMessageContext();
        FaultHandler secondFaultHandler = Mockito.mock(FaultHandler.class);
        secondCtx.pushFaultHandler(secondFaultHandler);
        failover.send(secondCtx);

        Assert.assertEquals(2, tokenCalls.get());
        Mockito.verify(synapseEnvironment, Mockito.times(1)).send(Mockito.any(), Mockito.eq(secondCtx));
        Mockito.verify(secondFaultHandler, Mockito.never()).handleFault(secondCtx);
    }

    /**
     * A token failure under a load balance endpoint with failover disabled fails the request once.
     */
    @Test
    public void testTokenFailureInLoadbalanceWithoutFailoverFailsOnce() throws Exception {

        AtomicInteger tokenCalls = new AtomicInteger();
        OAuthConfiguredHTTPEndpoint child1 = createEndpoint("child1", failingTokenHandler(tokenCalls));
        OAuthConfiguredHTTPEndpoint child2 = createEndpoint("child2", failingTokenHandler(tokenCalls));
        LoadbalanceEndpoint loadbalance = createLoadbalance(child1, child2);
        loadbalance.setFailover(false);

        assertTokenFailureFailsOnce(loadbalance, tokenCalls, 1);
    }

    /**
     * Sends through the parent endpoint and asserts the request fails once with the child's 303003 error.
     */
    private void assertTokenFailureFailsOnce(AbstractEndpoint parent, AtomicInteger tokenCalls, int expectedCalls)
            throws Exception {

        parent.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        synCtx.pushFaultHandler(faultHandler);

        parent.send(synCtx);

        Assert.assertEquals("Token must be requested only once", expectedCalls, tokenCalls.get());
        Mockito.verify(faultHandler, Mockito.times(1)).handleFault(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.never()).send(Mockito.any(), Mockito.any());
        Assert.assertTrue(synCtx.getFaultStack().isEmpty());
        assertTokenError(synCtx);
    }

    /**
     * Sends through the parent endpoint, fails the first child with a transport fault (101503) and asserts the
     * message is sent through the second child.
     */
    private void assertTransportFaultFailsOver(AbstractEndpoint parent, OAuthConfiguredHTTPEndpoint child1,
                                               OAuthHandler handler1, OAuthHandler handler2, Integer staleErrorCode)
            throws Exception {

        parent.init(synapseEnvironment);
        MessageContext synCtx = createMessageContext();
        if (staleErrorCode != null) {
            synCtx.setProperty(SynapseConstants.ERROR_CODE, staleErrorCode);
        }
        FaultHandler faultHandler = Mockito.mock(FaultHandler.class);
        synCtx.pushFaultHandler(faultHandler);

        parent.send(synCtx);
        Mockito.verify(handler1, Mockito.times(1)).setAuthHeader(synCtx);
        Mockito.verify(handler2, Mockito.never()).setAuthHeader(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.times(1)).send(Mockito.any(), Mockito.eq(synCtx));

        // Simulate a connection failure reported by the transport for the first child
        synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.NHTTP_CONNECTION_FAILED);
        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, "Connection refused");
        Object topFaultHandler = synCtx.getFaultStack().pop();
        Assert.assertSame(child1, topFaultHandler);
        ((FaultHandler) topFaultHandler).handleFault(synCtx);

        Assert.assertFalse("The failed child must be suspended", child1.readyToSend());
        Mockito.verify(handler1, Mockito.times(1)).setAuthHeader(synCtx);
        Mockito.verify(handler2, Mockito.times(1)).setAuthHeader(synCtx);
        Mockito.verify(synapseEnvironment, Mockito.times(2)).send(Mockito.any(), Mockito.eq(synCtx));
        Mockito.verify(faultHandler, Mockito.never()).handleFault(synCtx);
    }

    /**
     * Asserts that the message carries the endpoint authentication failure code and the token error message.
     */
    private void assertTokenError(MessageContext synCtx) {

        Assert.assertEquals(SynapseConstants.ENDPOINT_AUTH_FAILURE, synCtx.getProperty(SynapseConstants.ERROR_CODE));
        Object errorMessage = synCtx.getProperty(SynapseConstants.ERROR_MESSAGE);
        Assert.assertNotNull(errorMessage);
        Assert.assertTrue("Unexpected error message: " + errorMessage,
                errorMessage.toString().startsWith(TOKEN_ERROR_PREFIX));
    }

    /**
     * Creates an OAuth handler whose token request always fails, counting the calls and failing the test if they are
     * not bounded.
     */
    private OAuthHandler failingTokenHandler(AtomicInteger tokenCalls) throws AuthException {

        OAuthHandler handler = Mockito.mock(OAuthHandler.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                int calls = tokenCalls.incrementAndGet();
                if (calls > MAX_TOKEN_CALLS) {
                    throw new IllegalStateException("Token requested " + calls + " times, retry is not bounded");
                }
                throw new AuthException("token endpoint returned 401");
            }
        }).when(handler).setAuthHeader(Mockito.any(MessageContext.class));
        return handler;
    }

    /**
     * Creates an OAuth configured HTTP endpoint with the given name and handler.
     */
    private OAuthConfiguredHTTPEndpoint createEndpoint(String name, OAuthHandler handler) {

        OAuthConfiguredHTTPEndpoint endpoint = new OAuthConfiguredHTTPEndpoint(handler);
        endpoint.setName(name);
        EndpointDefinition definition = new EndpointDefinition();
        definition.setAddress("http://localhost:9999/" + name);
        endpoint.setDefinition(definition);
        endpoint.setUriTemplate(UriTemplate.fromTemplate("http://localhost:9999/" + name));
        return endpoint;
    }

    /**
     * Creates a failover endpoint with the given children.
     */
    private FailoverEndpoint createFailover(Endpoint... children) {

        FailoverEndpoint failover = new FailoverEndpoint();
        failover.setName("failoverEP");
        failover.setChildren(toChildren(failover, children));
        return failover;
    }

    /**
     * Creates a round robin load balance endpoint with failover enabled and the given children.
     */
    private LoadbalanceEndpoint createLoadbalance(Endpoint... children) {

        LoadbalanceEndpoint loadbalance = new LoadbalanceEndpoint();
        loadbalance.setName("loadbalanceEP");
        List<Endpoint> childList = toChildren(loadbalance, children);
        loadbalance.setChildren(childList);
        loadbalance.setAlgorithm(new RoundRobin(childList));
        loadbalance.setFailover(true);
        return loadbalance;
    }

    /**
     * Sets the given parent on each child endpoint and returns them as a list.
     */
    private List<Endpoint> toChildren(Endpoint parent, Endpoint... children) {

        List<Endpoint> childList = new ArrayList<>(Arrays.asList(children));
        for (Endpoint child : childList) {
            ((AbstractEndpoint) child).setParentEndpoint(parent);
        }
        return childList;
    }

    /**
     * Creates a Synapse message context with an empty SOAP envelope.
     */
    private MessageContext createMessageContext() throws AxisFault {

        org.apache.axis2.context.MessageContext axis2MC = new org.apache.axis2.context.MessageContext();
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

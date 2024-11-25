/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints.dynamic;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.ArrayList;
import java.util.List;

public class DynamicEndpointTest extends TestCase {

    public void testContextProperties() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$ctx:timeout");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutExpression(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeout", "90000");
        assertEquals(Long.valueOf((String) xpath.evaluate(synCtx)).longValue(),
                endpoint.getDefinition().evaluateDynamicEndpointTimeout(synCtx));
    }

    public void testContextPropertiesForInitialSuspendDuration() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:initialSuspendDuration");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicInitialSuspendDuration(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("initialSuspendDuration", "90000");
        assertEquals(endpoint.getDefinition().getResolvedInitialSuspendDuration(synCtx), 90000);
    }

    public void testContextPropertiesForNoInitialSuspendDuration() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:initialSuspendDuration");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicInitialSuspendDuration(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(-1, endpoint.getDefinition().getResolvedInitialSuspendDuration(synCtx));
    }

    public void testContextPropertiesForSuspendMaximumDuration() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendMaximumDuration");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendMaximumDuration(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendMaximumDuration", "90000");
        assertEquals(endpoint.getDefinition().getResolvedSuspendMaximumDuration(synCtx), 90000);
    }

    public void testContextPropertiesForNoSuspendMaximumDuration() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendMaximumDuration");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendMaximumDuration(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(Long.MAX_VALUE, endpoint.getDefinition().getResolvedSuspendMaximumDuration(synCtx));
    }

    public void testContextPropertiesForSuspendProgressionFactor() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendProgressionFactor");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendProgressionFactor(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendProgressionFactor", "2");
        assertEquals(endpoint.getDefinition().getResolvedSuspendProgressionFactor(synCtx), 2.0f);
    }

    public void testContextPropertiesForNoSuspendProgressionFactor() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendProgressionFactor");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendProgressionFactor(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(1.0f, endpoint.getDefinition().getResolvedSuspendProgressionFactor(synCtx));
    }

    public void testContextPropertiesForRetriesOnTimeoutBeforeSuspend() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:retriesOnTimeoutBeforeSuspend");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicRetriesOnTimeoutBeforeSuspend(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("retriesOnTimeoutBeforeSuspend", "3");
        assertEquals(endpoint.getDefinition().getResolvedRetriesOnTimeoutBeforeSuspend(synCtx), 3);
    }

    public void testContextPropertiesForNoRetriesOnTimeoutBeforeSuspend() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:retriesOnTimeoutBeforeSuspend");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicRetriesOnTimeoutBeforeSuspend(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(0, endpoint.getDefinition().getResolvedRetriesOnTimeoutBeforeSuspend(synCtx));
    }

    public void testContextPropertiesForRetryDurationOnTimeout() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:retryDurationOnTimeout");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicRetryDurationOnTimeout(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("retryDurationOnTimeout", "90000");
        assertEquals(endpoint.getDefinition().getResolvedRetryDurationOnTimeout(synCtx), 90000);
    }

    public void testContextPropertiesForNoRetryDurationOnTimeout() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:retryDurationOnTimeout");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicRetryDurationOnTimeout(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(0, endpoint.getDefinition().getResolvedRetryDurationOnTimeout(synCtx));
    }

    public void testContextPropertiesForTimeoutActionFault() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutAction");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutAction(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutAction", "fault");
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.DISCARD_AND_FAULT);
    }

    public void testContextPropertiesForTimeoutActionDiscard() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutAction");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutAction(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutAction", "discard");
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.DISCARD);
    }

    public void testContextPropertiesForNoTimeoutAction() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutAction");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutAction(xpath);
        MessageContext synCtx = new TestMessageContext();
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.NONE);
    }

    public void testContextPropertiesForTimeoutActionNever() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutAction");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutAction(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutAction", "never");
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.NONE);
    }

    public void testContextPropertiesForSuspendErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendErrorCodes", "101503,101504");

        List<Integer> actualSuspendErrorCodes = new ArrayList<>();
        actualSuspendErrorCodes.add(101503);
        actualSuspendErrorCodes.add(101504);
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.equals(actualSuspendErrorCodes));
    }

    public void testContextPropertiesForEmptySuspendErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendErrorCodes", "");

        List<Integer> actualSuspendErrorCodes = new ArrayList<>();
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.equals(actualSuspendErrorCodes));
    }

    public void testContextPropertiesForNoSuspendErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:suspendErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicSuspendErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.isEmpty());
    }

    public void testContextPropertiesForTimeoutErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutErrorCodes", "101503,101504");

        List<Integer> actualTimeoutErrorCodes = new ArrayList<>();
        actualTimeoutErrorCodes.add(101503);
        actualTimeoutErrorCodes.add(101504);
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.equals(actualTimeoutErrorCodes));
    }

    public void testContextPropertiesForEmptyTimeoutErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutErrorCodes", "");

        List<Integer> actualTimeoutErrorCodes = new ArrayList<>();
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.equals(actualTimeoutErrorCodes));
    }

    public void testContextPropertiesForNoTimeoutErrorCodes() throws Exception {

        SynapseXPath xpath = new SynapseXPath("$ctx:timeoutErrorCodes");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutErrorCodes(xpath);
        MessageContext synCtx = new TestMessageContext();
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.isEmpty());
    }
}

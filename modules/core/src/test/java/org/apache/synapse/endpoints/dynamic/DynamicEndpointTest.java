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


}

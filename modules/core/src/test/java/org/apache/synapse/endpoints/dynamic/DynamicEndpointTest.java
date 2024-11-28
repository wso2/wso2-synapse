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
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.xml.ValueFactory;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;

import java.util.ArrayList;
import java.util.List;

public class DynamicEndpointTest extends TestCase {

    public void testContextProperties() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<duration>{$ctx:timeout}</duration>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutDuration(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeout", "90000");
        assertEquals(90000,
                endpoint.getDefinition().evaluateDynamicEndpointTimeout(synCtx));
    }

    public void testContextPropertiesForInitialSuspendDuration() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<initialDuration>{$ctx:suspendInitialDuration}</initialDuration>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setInitialSuspendDuration(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendInitialDuration", "90000");
        assertEquals(endpoint.getDefinition().getResolvedInitialSuspendDuration(synCtx), 90000);
    }

    public void testContextPropertiesForNoInitialSuspendDuration() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<initialDuration>{$ctx:suspendInitialDuration}</initialDuration>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setInitialSuspendDuration(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(-1, endpoint.getDefinition().getResolvedInitialSuspendDuration(synCtx));
    }

    public void testContextPropertiesForSuspendMaximumDuration() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<maximumDuration>{$ctx:suspendMaximumDuration}</maximumDuration>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendMaximumDuration(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendMaximumDuration", "90000");
        assertEquals(endpoint.getDefinition().getResolvedSuspendMaximumDuration(synCtx), 90000);
    }

    public void testContextPropertiesForNoSuspendMaximumDuration() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<maximumDuration>{$ctx:suspendMaximumDuration}</maximumDuration>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendMaximumDuration(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(Long.MAX_VALUE, endpoint.getDefinition().getResolvedSuspendMaximumDuration(synCtx));
    }

    public void testContextPropertiesForSuspendProgressionFactor() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<progressionFactor>{$ctx:suspendProgressionFactor}</progressionFactor>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendProgressionFactor(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendProgressionFactor", "2");
        assertEquals(endpoint.getDefinition().getResolvedSuspendProgressionFactor(synCtx), 2.0f);
    }

    public void testContextPropertiesForNoSuspendProgressionFactor() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<progressionFactor>{$ctx:suspendProgressionFactor}</progressionFactor>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendProgressionFactor(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(1.0f, endpoint.getDefinition().getResolvedSuspendProgressionFactor(synCtx));
    }

    public void testContextPropertiesForRetriesOnTimeoutBeforeSuspend() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<retriesBeforeSuspension>{$ctx:retriesOnTimeoutBeforeSuspend}</retriesBeforeSuspension>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setRetriesOnTimeoutBeforeSuspend(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("retriesOnTimeoutBeforeSuspend", "3");
        assertEquals(endpoint.getDefinition().getResolvedRetriesOnTimeoutBeforeSuspend(synCtx), 3);
    }

    public void testContextPropertiesForNoRetriesOnTimeoutBeforeSuspend() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<retriesBeforeSuspension>{$ctx:retriesOnTimeoutBeforeSuspend}</retriesBeforeSuspension>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setRetriesOnTimeoutBeforeSuspend(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(0, endpoint.getDefinition().getResolvedRetriesOnTimeoutBeforeSuspend(synCtx));
    }

    public void testContextPropertiesForRetryDurationOnTimeout() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<retryDelay>{$ctx:retryDurationOnTimeout}</retryDelay>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setRetryDurationOnTimeout(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("retryDurationOnTimeout", "90000");
        assertEquals(endpoint.getDefinition().getResolvedRetryDurationOnTimeout(synCtx), 90000);
    }

    public void testContextPropertiesForNoRetryDurationOnTimeout() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<retryDelay>{$ctx:retryDurationOnTimeout}</retryDelay>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setRetryDurationOnTimeout(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(0, endpoint.getDefinition().getResolvedRetryDurationOnTimeout(synCtx));
    }

    public void testContextPropertiesForTimeoutActionFault() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<responseAction>{$ctx:timeoutAction}</responseAction>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutAction(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutAction", "fault");
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.DISCARD_AND_FAULT);
    }

    public void testContextPropertiesForTimeoutActionDiscard() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<responseAction>{get-property('timeoutAction')}</responseAction>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutAction(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutAction", "discard");
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.DISCARD);
    }

    public void testContextPropertiesForNoTimeoutAction() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<responseAction>{$ctx:timeoutAction}</responseAction>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutAction(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        assertEquals(endpoint.getDefinition().getResolvedTimeoutAction(synCtx), SynapseConstants.NONE);
    }

    public void testContextPropertiesForSuspendErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:suspendErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendErrorCodes", "101503,101504");

        List<Integer> actualSuspendErrorCodes = new ArrayList<>();
        actualSuspendErrorCodes.add(101503);
        actualSuspendErrorCodes.add(101504);
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.equals(actualSuspendErrorCodes));
    }

    public void testContextPropertiesForEmptySuspendErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:suspendErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("suspendErrorCodes", "");

        List<Integer> actualSuspendErrorCodes = new ArrayList<>();
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.equals(actualSuspendErrorCodes));
    }

    public void testContextPropertiesForNoSuspendErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:suspendErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setSuspendErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        List<Integer> expectedSuspendErrorCodes = endpoint.getDefinition().getResolvedSuspendErrorCodes(synCtx);
        assertTrue(expectedSuspendErrorCodes.isEmpty());
    }

    public void testContextPropertiesForTimeoutErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:timeoutErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutErrorCodes", "101503,101504");

        List<Integer> actualTimeoutErrorCodes = new ArrayList<>();
        actualTimeoutErrorCodes.add(101503);
        actualTimeoutErrorCodes.add(101504);
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.equals(actualTimeoutErrorCodes));
    }

    public void testContextPropertiesForEmptyTimeoutErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:timeoutErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeoutErrorCodes", "");

        List<Integer> actualTimeoutErrorCodes = new ArrayList<>();
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.equals(actualTimeoutErrorCodes));
    }

    public void testContextPropertiesForNoTimeoutErrorCodes() throws Exception {

        OMElement omElement = AXIOMUtil.stringToOM("<errorCodes>{$ctx:timeoutErrorCodes}</errorCodes>");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setTimeoutErrorCodes(new ValueFactory().createTextValue(omElement));
        MessageContext synCtx = new TestMessageContext();
        List<Integer> expectedTimeoutErrorCodes = endpoint.getDefinition().getResolvedTimeoutErrorCodes(synCtx);
        assertTrue(expectedTimeoutErrorCodes.isEmpty());
    }
}

/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators.eventing;

import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.eventing.SynapseEventSource;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for EventPublisherMediator class.
 */
public class EventPublisherMediatorTest {

    private EventPublisherMediator eventPublisherMediator = new EventPublisherMediator();

    /**
     * Testing eventPublisherMediator with mocked event source.
     */
    @Test
    public void testMediate() {
        final String EventSourceName = "testEventStore";
        SynapseEnvironment synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        MessageContext messageContext = new TestMessageContext();
        messageContext.setEnvironment(synapseEnvironment);
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        SynapseEventSource synapseEventSource = Mockito.mock(SynapseEventSource.class);
        Mockito.doNothing().when(synapseEventSource).dispatchEvents(Mockito.any(MessageContext.class));
        Map<String, SynapseEventSource> eventStoreMap = new HashMap<>();
        eventStoreMap.put(EventSourceName, synapseEventSource);
        synapseConfiguration.setEventSources(eventStoreMap);
        messageContext.setConfiguration(synapseConfiguration);
        eventPublisherMediator.setEventSourceName(EventSourceName);
        Assert.assertTrue("mediation successful", eventPublisherMediator.mediate(messageContext));
    }
}

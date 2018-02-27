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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.config.xml.eventing;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.builtin.CallMediator;
import org.apache.synapse.mediators.eventing.EventPublisherMediator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for EventPublisherMediatorSerializer
 */
public class EventPublisherMediatorSerializerTest {

    /**
     * Passing unsupported mediator.
     */
    @Test(expected = SynapseException.class)
    public void testCreateEventSource() {
        EventPublisherMediatorSerializer mediatorSerializer = new EventPublisherMediatorSerializer();
        Mediator mediator = new CallMediator();
        mediatorSerializer.serializeSpecificMediator(mediator);
    }

    /**
     * Mediator with no event source name.
     */
    @Test(expected = SynapseException.class)
    public void testCreateEventSource1() {
        EventPublisherMediatorSerializer mediatorSerializer = new EventPublisherMediatorSerializer();
        EventPublisherMediator mediator = new EventPublisherMediator();
        mediatorSerializer.serializeSpecificMediator(mediator);
    }

    /**
     * Test serializeSpecificMediator and asserting Name of OMElement.
     */
    @Test
    public void testCreateEventSource2() {
        EventPublisherMediatorSerializer mediatorSerializer = new EventPublisherMediatorSerializer();
        EventPublisherMediator mediator = new EventPublisherMediator();
        mediator.setEventSourceName("Test");
        OMElement element = mediatorSerializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("Name should be eventPublisher", "eventPublisher", element.getLocalName());
    }
}


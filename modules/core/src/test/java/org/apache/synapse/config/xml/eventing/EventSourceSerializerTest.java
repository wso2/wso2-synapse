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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.eventing.SynapseSubscription;
import org.apache.synapse.eventing.managers.DefaultInMemorySubscriptionManager;
import org.junit.Assert;
import org.junit.Test;
import org.wso2.eventing.SubscriptionManager;
import org.wso2.eventing.exceptions.EventException;

import javax.xml.stream.XMLStreamException;
import java.util.Calendar;
import java.util.Date;

/**
 * This is the test class for EventSourceSerializer class.
 */
public class EventSourceSerializerTest {

    /**
     * Event source with no name
     */
    @Test(expected = NullPointerException.class)
    public void testSerializeEvent() {
        EventSourceSerializer.serializeEventSource(null, null);
    }

    /**
     * Test serializeEventSource and assert OMElement returned is not null.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testSerializeEvent2() throws XMLStreamException {
        SynapseEventSource synapseEventSource = new SynapseEventSource("Test");
        OMElement omElement = EventSourceSerializer.serializeEventSource(null, synapseEventSource);
        Assert.assertNotNull("OMElement cannot be null.", omElement);
    }

    /**
     * Test serializeEventSource with subscriptionManager and assert OMElement returned is not null.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testSerializeEvent3() throws XMLStreamException {
        SynapseEventSource synapseEventSource = new SynapseEventSource("Test");
        SubscriptionManager subscriptionManager = new DefaultInMemorySubscriptionManager();
        synapseEventSource.setSubscriptionManager(subscriptionManager);
        OMElement omElement = EventSourceSerializer.serializeEventSource(null, synapseEventSource);
        Assert.assertNotNull("OMElement cannot be null.", omElement);
    }

    /**
     * Test SerialEvent and assert OMElement returned is not null.
     *
     * @throws XMLStreamException - XMLStreamException
     * @throws EventException     - EventException
     */
    @Test
    public void testSerializeEvent4() throws XMLStreamException, EventException {
        String inputXML =
                "      <eventSource name=\"SampleEventSource\" xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                        "            <subscriptionManager class=\"org.apache.synapse.eventing.managers." +
                        "DefaultInMemorySubscriptionManager\">\n" +
                        "                <property name=\"topicHeaderName\" value=\"Topic\"/>\n" +
                        "                <property name=\"topicHeaderNS\" value=\"http://apache.org/aip\"/>\n" +
                        "            </subscriptionManager>\n" +
                        "            <subscription id=\"mySubscription\">\n" +
                        "                 <filter source =\"synapse/event/test\" dialect=\"http://synapse.apache.org/" +
                        "eventing/dialect/topicFilter\"/>\n" +
                        "                 <endpoint><address uri=\"http://localhost:9000/services/" +
                        "SimpleStockQuoteService\"/></endpoint>\n" +
                        "            </subscription>\n" +
                        "            <subscription id=\"mySubscription2\">\n" +
                        "                 <filter source =\"synapse/event/test\" dialect=\"http://synapse.apache.org/" +
                        "eventing/dialect/topicFilter\"/>\n" +
                        "                 <endpoint><address uri=\"http://localhost:9000/services/" +
                        "SimpleStockQuoteService\"/></endpoint>\n" +
                        "                 <expires>2020-06-27T21:07:00.000-08:00</expires>\n" +
                        "            </subscription>\n" +
                        "      </eventSource>\n";
        OMElement element = AXIOMUtil.stringToOM(inputXML);

        SynapseEventSource synapseEventSource = new SynapseEventSource("Test");
        SubscriptionManager subscriptionManager = new DefaultInMemorySubscriptionManager();
        subscriptionManager.addProperty("Name", "Test");
        SynapseSubscription synapseSubscription = new SynapseSubscription();

        synapseSubscription.setStaticEntry(true);
        Date date = new Date(System.currentTimeMillis() + 3600000);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        synapseSubscription.setExpires(cal);
        subscriptionManager.subscribe(synapseSubscription);
        synapseEventSource.setSubscriptionManager(subscriptionManager);
        OMElement omElement = EventSourceSerializer.serializeEventSource(element, synapseEventSource);
        Assert.assertNotNull("OMElement cannot be null.", omElement);
    }
}

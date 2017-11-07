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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.eventing.SynapseEventSource;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

/**
 * Test class for EventSourceFactory class.
 */
public class EventSourceFactoryTest {

    /**
     * Event source with no name is passed.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test(expected = SynapseException.class)
    public void testCreateEventSource() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<name><value>Test</value></name>");
        EventSourceFactory.createEventSource(element, null);
    }

    /**
     * Having SynapseSubscription Manager not specified.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test(expected = SynapseException.class)
    public void testCreateEventSource2() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<name><value>Test</value></name>");
        OMFactory fac = OMAbstractFactory.getOMFactory();
        element.addAttribute(fac.createOMAttribute("name", null, "Test"));
        EventSourceFactory.createEventSource(element, null);
    }

    /**
     * Test to CreateEventSource and asserting EventSource is created.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testCreateEventSource3() throws XMLStreamException {
        String inputXML =
                "      <eventSource name=\"SampleEventSource\" xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                        "            <subscriptionManager class=\"org.apache.synapse.eventing.managers." +
                        "DefaultInMemorySubscriptionManager\">\n" +
                        "                <property name=\"topicHeaderName\" value=\"Topic\"/>\n" +
                        "                <property name=\"topicHeaderNS\" value=\"http://apache.org/aip\"/>\n" +
                        "            </subscriptionManager>\n" +
                        "            <subscription id=\"mySubscription\">\n" +
                        "                 <filter source =\"synapse/event/test\" dialect=\"http://synapse.apache." +
                        "org/eventing/dialect/topicFilter\"/>\n" +
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
        SynapseEventSource eventSource = EventSourceFactory.createEventSource(element, null);
        Assert.assertNotNull("SynapseEventSource is not created", eventSource);
    }

}


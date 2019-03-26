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
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

/**
 * Test class for EventPublisherMediatorFactory class.
 */
public class EventPublisherMediatorFactoryTest {

    /**
     * Event source with no name specified.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test(expected = SynapseException.class)
    public void testCreateSpecificMediator() throws XMLStreamException {
        EventPublisherMediatorFactory mediatorFactory = new EventPublisherMediatorFactory();
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
        mediatorFactory.createSpecificMediator(element, null);
    }

    /**
     * Test CreateSpecificMediator and assert the mediator is created.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testCreateSpecificMediator2() throws XMLStreamException {
        EventPublisherMediatorFactory mediatorFactory = new EventPublisherMediatorFactory();
        String inputXML =
                "<publishEvent async=\"false\">\n" +
                        "                <eventSink>String</eventSink>\n" +
                        "                <streamName>String </streamName>\n" +
                        "                <streamVersion>String</streamVersion>\n" +
                        "                <attributes>\n" +
                        "                    <meta/>\n" +
                        "                    <correlation>\n" +
                        "                        <attribute defaultValue=\"\" name=\"attribute1\" type=\"STRING\" value=\"string_val\"/>\n" +
                        "                    </correlation>\n" +
                        "                    <payload>\n" +
                        "                        <attribute defaultValue=\"\" name=\"attribute2\" type=\"STRING\" value=\"attribute_val2\"/>\n" +
                        "                    </payload>\n" +
                        "                    <arbitrary/>\n" +
                        "                </attributes>\n" +
                        "            </publishEvent>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        OMFactory fac = OMAbstractFactory.getOMFactory();
        element.addAttribute(fac.createOMAttribute("eventSourceName", null, "Test"));
        Mediator mediator = mediatorFactory.createSpecificMediator(element, null);
        Assert.assertNotNull("Mediator cannot be null", mediator);
    }
}

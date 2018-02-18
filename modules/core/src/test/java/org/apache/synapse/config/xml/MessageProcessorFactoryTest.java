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
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.message.processor.MessageProcessor;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

/**
 * This is the test class for MessageProcessorFactory class.
 */
public class MessageProcessorFactoryTest {

    /**
     * Test CreateMessageProcessor with given XML configuration and asserting it is created.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testCreateMessageProcessor() throws XMLStreamException {
        String inputXML = "<messageProcessor\n" +
                "        class=\"org.apache.synapse.message.processor.impl.forwarder." +
                "ScheduledMessageForwardingProcessor\"\n" +
                "        messageStore=\"MyStore\" name=\"ScheduledProcessor\" targetEndpoint=\"StockQuoteServiceEp\" " +
                "xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "        <parameter name=\"interval\">10000</parameter>\n" +
                "        <parameter name=\"throttle\">false</parameter>\n" +
                "        <parameter name=\"target.endpoint\">StockQuoteServiceEp</parameter>\n" +
                "        <description>  test </description>" +
                "    </messageProcessor>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        MessageProcessor messageProcessor = MessageProcessorFactory.createMessageProcessor(element);
        Assert.assertNotNull("MessageProcessor is not created", messageProcessor);
    }

}


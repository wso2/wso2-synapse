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
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.processor.MessageProcessor;
import org.apache.synapse.message.processor.impl.forwarder.ScheduledMessageForwardingProcessor;
import org.apache.synapse.message.processor.impl.sampler.SamplingProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the test class fro MessageProcessorSerializer class.
 */
public class MessageProcessorSerializerTest {

    /**
     * test serializeMessageProcessor with no Message store name.
     */
    @Test(expected = SynapseException.class)
    public void testSerializeMessageProcessor() {
        MessageProcessor messageProcessor = new SamplingProcessor();
        MessageProcessorSerializer.serializeMessageProcessor(null, messageProcessor);
    }

    /**
     * test serializeMessageProcessor and assert the OMElement is created.
     */
    @Test
    public void testSerializeMessageProcessor2() {
        MessageProcessor messageProcessor = new SamplingProcessor();
        messageProcessor.setName("testStore");
        OMElement element = MessageProcessorSerializer.serializeMessageProcessor(null, messageProcessor);
        Assert.assertNotNull("OMElement is not returned", element);
    }

    /**
     * test serializeMessageProcessor with parameters and assert the OMElement is created.
     */
    @Test
    public void testSerializeMessageProcessor3() {
        Map<String, Object> parameters = new HashMap<>();
        MessageProcessor messageProcessor = new ScheduledMessageForwardingProcessor();
        messageProcessor.setName("testStore");
        parameters.put("interval", "1000");
        messageProcessor.setParameters(parameters);
        messageProcessor.setDescription("testMessageProcessor");
        messageProcessor.setTargetEndpoint("target");
        messageProcessor.setMessageStoreName("testMessageStore");
        OMElement element = MessageProcessorSerializer.serializeMessageProcessor(null, messageProcessor);
        Assert.assertNotNull("OMElement is not returned", element);
    }

}

/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.executors.config;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.commons.executors.InternalQueue;
import org.apache.synapse.commons.executors.MultiPriorityBlockingQueue;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.PRRNextQueueAlgorithm;
import org.apache.synapse.commons.executors.queues.FixedSizeQueue;
import org.apache.synapse.commons.executors.queues.UnboundedQueue;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for PriorityExecutorSerializer
 */
public class PriorityExecutorSerializerTest extends TestCase {

    /**
     * Test serializing a PriorityExecutor created with FixedSizeQueues
     */
    public void testSerialize() {
        PriorityExecutor priorityExecutor = new PriorityExecutor();
        priorityExecutor.setName("executor1");

        List<InternalQueue> internalQueueList = new ArrayList<>();
        internalQueueList.add(new FixedSizeQueue(10, 10));
        internalQueueList.add(new FixedSizeQueue(5, 25));
        MultiPriorityBlockingQueue multiPriorityBlockingQueue = new MultiPriorityBlockingQueue(
                internalQueueList, true, new PRRNextQueueAlgorithm());
        priorityExecutor.setQueue(multiPriorityBlockingQueue);

        OMElement element = PriorityExecutorSerializer.serialize(null, priorityExecutor,
                "http://ws.apache.org/ns/synapse");

        assertEquals("Invalid serialization of PriorityExecutor",
                "<priorityExecutor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">" +
                        "<queues><queue size=\"10\" priority=\"10\" /><queue size=\"25\" priority=\"5\" /></queues>" +
                        "<threads max=\"100\" core=\"20\" keep-alive=\"5\" />" +
                        "</priorityExecutor>",
                element.toString());
    }

    /**
     * Test serializing a PriorityExecutor when parent element is defined
     * @throws XMLStreamException
     */
    public void testSerializeGivenParentElement() throws XMLStreamException {
        OMElement parentElement = AXIOMUtil.stringToOM("<definitions></definitions>");
        PriorityExecutor priorityExecutor = new PriorityExecutor();
        priorityExecutor.setName("executor1");

        List<InternalQueue> internalQueueList = new ArrayList<>();
        internalQueueList.add(new FixedSizeQueue(10, 10));
        internalQueueList.add(new FixedSizeQueue(5, 25));
        MultiPriorityBlockingQueue multiPriorityBlockingQueue = new MultiPriorityBlockingQueue(
                internalQueueList, true, new PRRNextQueueAlgorithm());
        priorityExecutor.setQueue(multiPriorityBlockingQueue);

        PriorityExecutorSerializer.serialize(parentElement, priorityExecutor,
                "http://ws.apache.org/ns/synapse");

        assertEquals("Invalid serialization of PriorityExecutor",
                "<definitions>" +
                        "<priorityExecutor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">" +
                        "<queues><queue size=\"10\" priority=\"10\" /><queue size=\"25\" priority=\"5\" /></queues>" +
                        "<threads max=\"100\" core=\"20\" keep-alive=\"5\" />" +
                        "</priorityExecutor>" +
                        "</definitions>",
                parentElement.toString());
    }

    /**
     * Test serializing a PriorityExecutor created with UnboundedQueues
     */
    public void testSerializeWithUnboundedQueues() {
        PriorityExecutor priorityExecutor = new PriorityExecutor();
        priorityExecutor.setName("executor1");

        List<InternalQueue> internalQueueList = new ArrayList<>();
        internalQueueList.add(new UnboundedQueue(10));
        internalQueueList.add(new UnboundedQueue(5));
        MultiPriorityBlockingQueue multiPriorityBlockingQueue = new MultiPriorityBlockingQueue(
                internalQueueList, false, new PRRNextQueueAlgorithm());
        priorityExecutor.setQueue(multiPriorityBlockingQueue);

        OMElement element = PriorityExecutorSerializer.serialize(null, priorityExecutor,
                "http://ws.apache.org/ns/synapse");

        assertEquals("Invalid serialization of PriorityExecutor",
                "<priorityExecutor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">" +
                        "<queues isFixedSize=\"false\"><queue priority=\"10\" /><queue priority=\"5\" /></queues>" +
                        "<threads max=\"100\" core=\"20\" keep-alive=\"5\" />" +
                        "</priorityExecutor>",
                element.toString());
    }
}

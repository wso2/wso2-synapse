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
import org.apache.axis2.AxisFault;
import org.apache.synapse.commons.executors.PriorityExecutor;

import javax.xml.stream.XMLStreamException;
import java.util.Properties;

/**
 * Test class for PriorityExecutorFactory
 */
public class PriorityExecutorFactoryTest extends TestCase {

    /**
     * Test creating PriorityExecutor from given xml configuration
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutor() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns:ns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">" +
                "<ns:queues></ns:queues>" +
                "</priority-executor>");
        PriorityExecutor executor = PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                element, false, new Properties());
        assertNotNull("Error creating PriorityExecutor", executor);
    }

    /**
     * Test creating PriorityExecutor by defining multiple queues in the xml configuration
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithQueues() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">\n" +
                "   <queues isFixedSize=\"false\">\n" +
                "      <queue priority=\"10\"/>\n" +
                "      <queue priority=\"5\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        PriorityExecutor executor = PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                element, true, new Properties());
        assertEquals("Priority executor queue count invalid", 2, executor.getQueue().getQueues().size());
    }

    /**
     * Test creating PriorityExecutor when no queues are defined in the xml configuration
     * @throws XMLStreamException
     */
    public void testCreateExecutorNoQueues() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns:ns=\"http://ws.apache.org/ns/synapse\" name=\"executor1\">" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("AxisFault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received", "Queues configuration is mandatory", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor by defining fixed sized queues in the xml configuration
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithFixedSizeQueues() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor2\">\n" +
                "   <queues isFixedSize=\"true\">\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "      <queue size=\"15\" priority=\"5\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");

        PriorityExecutor executor = PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                element, true, new Properties());
        assertEquals("Priority executor queue count invalid", 2, executor.getQueue().getQueues().size());
    }

    /**
     * Test creating PriorityExecutor when name is not defined
     * @throws XMLStreamException
     */
    public void testCreateExecutorNameRequired() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns:ns=\"http://ws.apache.org/ns/synapse\">" +
                "<ns:queues></ns:queues>" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor(null,
                    element, true, new Properties());
            fail("AxisFault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received", "name is required for a priorityExecutor", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor when queue priority is not defined
     * @throws XMLStreamException
     */
    public void testCreateExecutorPriorityNull() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor\">\n" +
                "   <queues>\n" +
                "      <queue size=\"25\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, true, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received", "Priority must be specified", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor when queue size is not defined for fixes size queues
     * @throws XMLStreamException
     */
    public void testCreateExecutorSizeNotSpecified() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" name=\"executor\">\n" +
                "   <queues isFixedSize=\"true\">\n" +
                "      <queue priority=\"10\"/>\n" +
                "      <queue priority=\"5\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received", "Queues should have a size", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor with NextQueueAlgorithm
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithNextAlgorithm() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "   <queues nextQueue=\"org.apache.synapse.commons.executors.PRRNextQueueAlgorithm\">\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        PriorityExecutor executor = PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                element, false, new Properties());
        assertNotNull("Next queue algorithm should not be null", executor.getQueue().getNextQueueAlgorithm());
    }

    /**
     * Test creating PriorityExecutor with invalid NextQueueAlgorithm
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithInvalidNextAlgoName() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "   <queues nextQueue=\"NextQueueAlgorithm1\">\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received",
                    "NextQueue algorithm class, NextQueueAlgorithm1 is not found", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor by defining a non NextQueueAlgorithm class as the "nextQueue" attribute
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithNonInstanceofNextQueueAlgorithm() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\">\n" +
                "   <queues nextQueue=\"java.lang.String\">\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received",
                    "NextQueue algorithm class, java.lang.String is not type of BeforeExecuteHandler", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor with invalid BeforeExecuteHandler
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithInvalidBeforeExecuteHandler() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "beforeExecuteHandler=\"BeforeExecuteHandler1\">\n" +
                "   <queues>\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received",
                    "Before execute handler class, BeforeExecuteHandler1 is not found", axisFault.getMessage());
        }
    }

    /**
     * Test creating PriorityExecutor with non BeforeExecuteHandler class as the "beforeExecuteHandler" attribute
     * @throws XMLStreamException
     * @throws AxisFault
     */
    public void testCreateExecutorWithNonInstanceofBeforeExecuteHandler() throws XMLStreamException, AxisFault {
        OMElement element = AXIOMUtil.stringToOM("<priority-executor xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "beforeExecuteHandler=\"java.lang.String\">\n" +
                "   <queues>\n" +
                "      <queue size=\"25\" priority=\"10\"/>\n" +
                "   </queues>\n" +
                "   <threads max=\"100\" core=\"20\" keep-alive=\"5\"/>\n" +
                "</priority-executor>");
        try {
            PriorityExecutorFactory.createExecutor("http://ws.apache.org/ns/synapse",
                    element, false, new Properties());
            fail("Axis Fault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Invalid fault message received",
                    "Before execute handler class, java.lang.String is not type of BeforeExecuteHandler", axisFault.getMessage());
        }
    }
}

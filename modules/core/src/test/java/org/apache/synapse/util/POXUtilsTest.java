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

package org.apache.synapse.util;

import junit.framework.TestCase;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import javax.xml.stream.XMLStreamException;

/**
 * Test class for POXUtils
 */
public class POXUtilsTest extends TestCase {

    /**
     * Test converting soap fault with detail to POX
     * @throws AxisFault
     */
    public void testConvertSOAPFaultToPOX() throws AxisFault, XMLStreamException {
        String expectedMessage = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body>" +
                "<Exception>Resource Not Found</Exception>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        MessageContext messageContext = new MessageContext();
        SOAPEnvelope envelope = createSoapFaultMessage("404", null, "Resource Not Found");
        assertNotNull("Error creating SOAPEnvelope", envelope);
        messageContext.setEnvelope(envelope);
        POXUtils.convertSOAPFaultToPOX(messageContext);
        assertEquals("Invalid POX fault conversion", expectedMessage, messageContext.getEnvelope().toString());
    }

    /**
     * Test converting soap fault with reason to POX
     * @throws AxisFault
     * @throws XMLStreamException
     */
    public void testConvertSOAPFaultToPOXGetExceptionFromReason() throws AxisFault, XMLStreamException {
        String expectedMessage = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body>" +
                "<Exception>SynapseException : Processing Error 1</Exception>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        MessageContext messageContext = new MessageContext();
        SOAPEnvelope envelope = createSoapFaultMessage("500", "SynapseException : Processing Error 1", null);
        assertNotNull("Error creating SOAPEnvelope", envelope);
        messageContext.setEnvelope(envelope);
        POXUtils.convertSOAPFaultToPOX(messageContext);
        assertEquals("Invalid POX fault conversion", expectedMessage, messageContext.getEnvelope().toString());
    }

    /**
     * Test converting soap fault with reason containing XML element to POX
     * @throws AxisFault
     * @throws XMLStreamException
     */
    public void testConvertSOAPFaultDetailWithElementToPOX() throws AxisFault, XMLStreamException {
        String expectedMessage = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soapenv:Body>" +
                "<Exception>" +
                "<Fault-detail>" +
                "<Exception>Synapse Exception</Exception>" +
                "<Message>Processing Error</Message>" +
                "</Fault-detail>" +
                "</Exception>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";

        MessageContext messageContext = new MessageContext();
        SOAPEnvelope envelope = createSoapFaultMessage("500", null,
                "<Fault-detail><Exception>Synapse Exception</Exception><Message>Processing Error</Message></Fault-detail>");
        assertNotNull("Error creating SOAPEnvelope", envelope);
        messageContext.setEnvelope(envelope);
        POXUtils.convertSOAPFaultToPOX(messageContext);
        assertEquals("Invalid POX fault conversion", expectedMessage, messageContext.getEnvelope().toString());
    }

    /**
     * Helper method to create soap fault message
     * @param code fault code
     * @param reason fault reason
     * @param detail fault detail
     * @return SOAPEnvelope containing soap fault
     */
    private SOAPEnvelope createSoapFaultMessage(String code, String reason, String detail) {
        SOAP11Factory factory = new SOAP11Factory();
        SOAPEnvelope envelope = factory.createSOAPEnvelope();
        SOAPBody body = factory.createSOAPBody(envelope);
        SOAPFault fault = factory.createSOAPFault(body);
        if(code != null) {
            factory.createSOAPFaultCode(fault).setText(code);
        }
        if(reason != null) {
            factory.createSOAPFaultReason(fault).setText(reason);
        }
        if(detail != null) {
            factory.createSOAPFaultDetail(fault).setText(detail);
        }
        return envelope;
    }
}

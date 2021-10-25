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

package org.apache.synapse.commons.builders;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisBindingOperation;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.util.MultipleEntryHashMap;
import org.apache.synapse.commons.json.Util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Test class for XFormURLEncodedBuilder
 */
public class XFormURLEncodedBuilderTest extends TestCase {

    /**
     * Test XFormURLEncodedBuilder
     * @throws AxisFault
     */
    public void testProcessDocument() throws AxisFault {
        String expectedSoapEnvelope = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soapenv:Body />" +
                "</soapenv:Envelope>";
        MessageContext messageContext = Util.newMessageContext();
        messageContext.setTo(new EndpointReference("http://localhost:9000/stockquote/test?symbol=WSO2&price=10"));
        XFormURLEncodedBuilder urlEncodedBuilder = new XFormURLEncodedBuilder();
        OMElement element = urlEncodedBuilder.processDocument(null, "", messageContext);
        assertEquals("Invalid SOAPEnvelope received", expectedSoapEnvelope, element.toString());
    }

    /**
     * Test XFormURLEncodedBuilder with HTTP POST method
     * @throws AxisFault
     */
    public void testProcessDocumentPostMethod() throws AxisFault {
        String expectedSoapEnvelope = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soapenv:Body>" +
                "<xformValues>" +
                "<price>10</price>" +
                "<symbol>WSO2</symbol>" +
                "</xformValues>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
        String expectedSoapEnvelope2 = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soapenv:Body>" +
                "<xformValues>" +
                "<symbol>WSO2</symbol>" +
                "<price>10</price>" +
                "</xformValues>" +
                "</soapenv:Body>" +
                "</soapenv:Envelope>";
        MessageContext messageContext = Util.newMessageContext();
        messageContext.setTo(new EndpointReference("http://localhost:9000/stockquote/test?symbol=WSO2&price=10"));
        messageContext.setProperty(HTTPConstants.HTTP_METHOD, "POST");
        XFormURLEncodedBuilder urlEncodedBuilder = new XFormURLEncodedBuilder();
        OMElement element = urlEncodedBuilder.processDocument(null, "", messageContext);
        boolean assertion = expectedSoapEnvelope.equals(element.toString()) || expectedSoapEnvelope2.equals(element.toString());
        assertTrue("Invalid SOAPEnvelope received", assertion);
    }

    /**
     * Test extracting parameter from 'whttp:location'
     * @throws AxisFault
     */
    public void testExtractMultipleParametersUsingHttpLocation2() throws AxisFault {
        AxisBindingOperation axisBindingOperation = new AxisBindingOperation();
        axisBindingOperation.setProperty("whttp:location", "books/{name}/");

        MessageContext messageContext = Util.newMessageContext();
        messageContext.setProperty("AxisBindingOperation", axisBindingOperation);
        messageContext.setTo(new EndpointReference("http://localhost:9000/store"));

        XFormURLEncodedBuilder urlEncodedBuilder = new XFormURLEncodedBuilder();
        urlEncodedBuilder.processDocument(null, "", messageContext);

        assertNotNull("Parameter 'name' not found in request parameter map",
                ((MultipleEntryHashMap)messageContext.getProperty(Constants.REQUEST_PARAMETER_MAP)).get("name"));
    }

    /**
     * Test extracting multiple parameters from 'whttp:location'
     * @throws AxisFault
     */
    public void testExtractMultipleParametersUsingHttpLocation() throws AxisFault {
        AxisBindingOperation axisBindingOperation = new AxisBindingOperation();
        axisBindingOperation.setProperty("whttp:location", "books/{category}/{name}");

        MessageContext messageContext = Util.newMessageContext();
        messageContext.setProperty("AxisBindingOperation", axisBindingOperation);
        messageContext.setTo(new EndpointReference("http://localhost:9000/store"));

        XFormURLEncodedBuilder urlEncodedBuilder = new XFormURLEncodedBuilder();
        urlEncodedBuilder.processDocument(null, "", messageContext);

        assertNotNull("Parameter 'category' not found in request parameter map",
                ((MultipleEntryHashMap)messageContext.getProperty(Constants.REQUEST_PARAMETER_MAP)).get("category"));
        assertNotNull("Parameter 'name' not found in request parameter map",
                ((MultipleEntryHashMap)messageContext.getProperty(Constants.REQUEST_PARAMETER_MAP)).get("name"));
    }

    /**
     * Test extracting parameters from input stream
     * @throws AxisFault
     */
    public void testExtractParametersFromInputStream() throws AxisFault {
        MessageContext messageContext = Util.newMessageContext();
        messageContext.setTo(new EndpointReference("http://localhost:9000/stockquote/"));
        messageContext.setProperty("CHARACTER_SET_ENCODING", "UTF-8");
        InputStream inputStream = new ByteArrayInputStream("symbol=WSO2&price=10".getBytes());
        XFormURLEncodedBuilder urlEncodedBuilder = new XFormURLEncodedBuilder();
        urlEncodedBuilder.processDocument(inputStream, "", messageContext);

        assertNotNull("Parameter 'symbol' not found in request parameter map",
                ((MultipleEntryHashMap)messageContext.getProperty(Constants.REQUEST_PARAMETER_MAP)).get("symbol"));
        assertNotNull("Parameter 'price' not found in request parameter map",
                ((MultipleEntryHashMap)messageContext.getProperty(Constants.REQUEST_PARAMETER_MAP)).get("price"));
    }
}
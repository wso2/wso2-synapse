/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.json;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;

import java.io.InputStream;

public class JsonStreamingBuilderTest extends TestCase {

    public void testCase() throws AxisFault {

        String jsonPayload = "{\n" +
                "\"account_number\":\"1234567890\",\n" +
                "\"routing_number\":\"09100001\",\n" +
                "\"image_type\":\"COMMERCIAL_DEPOSIT\"\n" +
                "}";

        String expectedXml = "<jsonObject>" +
                "<account_number>1234567890</account_number>" +
                "<routing_number>09100001</routing_number>" +
                "<image_type>COMMERCIAL_DEPOSIT</image_type>" +
                "</jsonObject>";

        MessageContext messageContext = Util.newMessageContext();
        InputStream inputStream = Util.newInputStream(jsonPayload.getBytes());
        Builder jsonBuilder = Util.newJsonStreamBuilder();
        OMElement element = jsonBuilder.processDocument(inputStream, "application/json", messageContext);
        assertEquals("Invalid content received", expectedXml, element.toString());
    }

    /**
     * Test for the RuntimeException thrown when the json payload is invalid.
     */
    public void testInvalidJson() {

        String invalidJson = "{\n" +
                "\"account_number\":\"1234567890\",\n" +
                "\"routing_number\":\"09100001\n" +
                "\"image_type\":\"COMMERCIAL_DEPOSIT\"\n" +
                "}";
        try {
            MessageContext message = Util.newMessageContext();
            InputStream inputStream = Util.newInputStream(invalidJson.getBytes());
            Builder jsonBuilder = Util.newJsonStreamBuilder();
            OMElement element = jsonBuilder.processDocument(inputStream, "application/json", message);
            message.getEnvelope().getBody().addChild(element);
        } catch (Exception e) {
            assertTrue("Not a RuntimeException instance", e instanceof RuntimeException);
        }
    }

    public void testProcessDocumentInputStreamNull() {

        try {
            MessageContext messageContext = Util.newMessageContext();
            Builder jsonBuilder = Util.newJsonStreamBuilder();
            jsonBuilder.processDocument(null, "application/json", messageContext);
            Assert.fail("AxisFault expected");
        } catch (AxisFault axisFault) {
            assertEquals("Cannot build payload without a valid EPR.", axisFault.getMessage());
        }
    }

    public void testProcessDocumentNoJsonPayload() throws AxisFault {

        String defaultEnvelope = "<?xml version='1.0' encoding='utf-8'?>" +
                "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soapenv:Body />" +
                "</soapenv:Envelope>";
        MessageContext messageContext = Util.newMessageContext();
        messageContext.setTo(new EndpointReference("http://localhost:8000"));
        Builder jsonBuilder = Util.newJsonStreamBuilder();
        OMElement element = jsonBuilder.processDocument(null, "application/json", messageContext);
        assertEquals("Default envelope expected", defaultEnvelope, element.toString());
    }
}
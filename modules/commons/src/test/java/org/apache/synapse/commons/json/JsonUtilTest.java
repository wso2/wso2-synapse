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

package org.apache.synapse.commons.json;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonUtilTest extends TestCase {

    public void testCaseContentType() throws AxisFault {
        MessageContext messageContext = Util.newMessageContext();
        JsonUtil.setContentType(messageContext);
        assertEquals("Invalid content type", "application/json", messageContext.getProperty("messageType"));
    }

    public void testJsonPayloadToString() throws AxisFault {
        MessageContext messageContext = Util.newMessageContext();
        String payloadString = JsonUtil.jsonPayloadToString(messageContext);
        assertEquals("Invalid payload received", "{}", payloadString);
    }

    public void testJsonPayloadToStringContextNull() {
        String payloadString = JsonUtil.jsonPayloadToString(null);
        assertEquals("Invalid payload received", "{}", payloadString);
    }

    public void testOMElementToJsonString() throws XMLStreamException, AxisFault {
        OMElement omElement = AXIOMUtil.stringToOM("<name>WSO2</name>");
        StringBuilder stringBuilder = JsonUtil.toJsonString(omElement);
        assertEquals("Invalid payload received", "{\"name\":\"WSO2\"}", stringBuilder.toString());
    }

    public void testNullElementToJsonString() throws AxisFault {
        StringBuilder stringBuilder = JsonUtil.toJsonString(null);
        assertEquals("Invalid content received", "{}", stringBuilder.toString());
    }

    public void testNullInputStreamToXml() throws AxisFault {
        OMElement element = JsonUtil.toXml(null, false);
        assertEquals("Null value expected", null, element);
    }

    public void testGetReaderFromNullInputStream() throws XMLStreamException {
        XMLStreamReader streamReader = JsonUtil.getReader(null);
        assertEquals("Null value expected", null, streamReader);
    }

    public void testGetReaderWithPIFromNullInputStream() throws XMLStreamException {
        XMLStreamReader streamReader = JsonUtil.getReader(null, true);
        assertEquals("Null value expected", null, streamReader);
    }

    public void testOMElementToJsonStream() throws IOException, XMLStreamException {
        OMElement omElement = AXIOMUtil.stringToOM("<name>WSO2</name>");
        InputStream stream = JsonUtil.toJsonStream(omElement);
        assertEquals("Invalid payload received", "{\"name\":\"WSO2\"}".getBytes().length, stream.available());
    }

    public void testNullElementToJsonStream() {
        InputStream stream = JsonUtil.toJsonStream(null);
        assertEquals("Null value expected", null, stream);
    }

    public void testTransformElementWithNamespaces() throws XMLStreamException {
        String xmlPayload = "<ns:stock xmlns:ns='http://services.samples'>\n" +
                "               <ns:name>WSO2</ns:name>\n" +
                "            </ns:stock>";
        OMElement omElement = AXIOMUtil.stringToOM(xmlPayload);
        JsonUtil.transformElement(omElement, false);
        assertEquals("Invalid payload received", "<stock><name>WSO2</name></stock>", omElement.toString());
    }

    public void testIsJsonPayloadElementJsonObject() throws XMLStreamException {
        OMElement omElement = AXIOMUtil.stringToOM("<jsonObject><vehicle>car</vehicle></jsonObject>");
        assertTrue("Json payload identified incorrectly", JsonUtil.isAJsonPayloadElement(omElement));
    }

    public void testIsJsonPayloadElementJsonArray() throws XMLStreamException {
        OMElement omElement = AXIOMUtil.stringToOM("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        assertTrue("Json payload identified incorrectly", JsonUtil.isAJsonPayloadElement(omElement));
    }

    public void testHasJsonPayloadElementJsonArray() throws XMLStreamException {
        OMElement omElement = AXIOMUtil.stringToOM("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        assertTrue("Json payload identified incorrectly", JsonUtil.hasAJsonPayload(omElement));
    }

    public void testHasJsonPayloadContext() throws AxisFault, XMLStreamException {
        MessageContext messageContext = Util.newMessageContext("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        assertFalse("Incorrectly identified as json payload", JsonUtil.hasAJsonPayload(messageContext));
    }

    public void testGetJsonPayload() throws IOException, XMLStreamException {
        MessageContext messageContext = Util.newMessageContext("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        OMElement element = AXIOMUtil.stringToOM("<name>WSO2</name>");
        messageContext.setProperty("org.apache.synapse.commons.json.JsonInputStream", JsonUtil.toJsonStream(element));
        InputStream inputStream = JsonUtil.getJsonPayload(messageContext);
        assertEquals("Invalid json payload received", "{\"name\":\"WSO2\"}".getBytes().length, inputStream.available());
    }

    public void testWriteAsJson() throws AxisFault, XMLStreamException {
        OMElement omElement = AXIOMUtil.stringToOM("<name>WSO2</name>");
        OutputStream outputStream = new ByteArrayOutputStream();
        JsonUtil.writeAsJson(omElement, outputStream);
        assertEquals("Invalid payload received", "{\"name\":\"WSO2\"}", outputStream.toString());
    }

    public void testCloneJsonPayload() throws AxisFault, XMLStreamException {
        MessageContext sourceContext = Util.newMessageContext("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        OMElement element = AXIOMUtil.stringToOM("<name>WSO2</name>");
        sourceContext.setProperty("org.apache.synapse.commons.json.JsonInputStream", JsonUtil.toJsonStream(element));
        MessageContext targetContext = Util.newMessageContext();
        assertTrue("Json payload identified incorrectly", JsonUtil.cloneJsonPayload(sourceContext, targetContext));
    }

    public void testJsonPayloadToByteArray() throws AxisFault, XMLStreamException {
        MessageContext messageContext = Util.newMessageContext("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        OMElement element = AXIOMUtil.stringToOM("<name>WSO2</name>");
        messageContext.setProperty("org.apache.synapse.commons.json.JsonInputStream", JsonUtil.toJsonStream(element));
        byte[] bytes = JsonUtil.jsonPayloadToByteArray(messageContext);
        assertEquals("Invalid payload received", "{\"name\":\"WSO2\"}", new String(bytes));
    }

    public void testJsonPayloadToByteArrayContextNull() {
        byte[] bytes = JsonUtil.jsonPayloadToByteArray(null);
        assertEquals("Empty array expected", 0, bytes.length);
    }

    public void testJsonPayloadToByteArrayNoJson() throws AxisFault, XMLStreamException {
        MessageContext messageContext = Util.newMessageContext("<name>WSO2</name>");
        byte[] bytes = JsonUtil.jsonPayloadToByteArray(messageContext);
        assertEquals("Empty array expected", 0, bytes.length);
    }

}

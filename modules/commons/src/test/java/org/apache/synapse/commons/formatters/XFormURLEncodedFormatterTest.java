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

package org.apache.synapse.commons.formatters;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.commons.json.Util;

import javax.xml.stream.XMLStreamException;

public class XFormURLEncodedFormatterTest extends TestCase {

    public  void testGetBytes() throws AxisFault, XMLStreamException {
        XFormURLEncodedFormatter formatter = new XFormURLEncodedFormatter();
        MessageContext messageContext = Util.newMessageContext("<test><test>123</test></test>");
        byte[] bytes = formatter.getBytes(messageContext, new OMOutputFormat());
        assertEquals("Invalid X-form URL Encoded string", "test=123", new String(bytes));
    }

    public void testGetContentType() throws AxisFault, XMLStreamException {
        XFormURLEncodedFormatter formatter = new XFormURLEncodedFormatter();
        MessageContext messageContext = Util.newMessageContext();
        String contentType = formatter.getContentType(messageContext, new OMOutputFormat(), "");
        assertEquals("Invalid content type", "application/x-www-form-urlencoded", contentType);
    }

    public void testGetContentTypeWithSoapAction() throws AxisFault, XMLStreamException {
        XFormURLEncodedFormatter formatter = new XFormURLEncodedFormatter();
        MessageContext messageContext = Util.newMessageContext();
        String contentType = formatter.getContentType(messageContext, new OMOutputFormat(), "urn:mediate");
        assertEquals("Invalid content type", "application/x-www-form-urlencoded;action=\"urn:mediate\";", contentType);
    }

    public void testGetContentTypeWithCharsetEncoding() throws AxisFault {
        OMOutputFormat format = new OMOutputFormat();
        format.setCharSetEncoding("UTF-8");
        XFormURLEncodedFormatter formatter = new XFormURLEncodedFormatter();
        MessageContext messageContext = Util.newMessageContext();
        String contentType = formatter.getContentType(messageContext, format, "");
        assertEquals("Invalid content type", "application/x-www-form-urlencoded; charset=UTF-8", contentType);
    }
}

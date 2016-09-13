/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11HeaderBlockImpl;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12HeaderBlockImpl;
import org.apache.axis2.context.MessageContext;

import javax.activation.DataHandler;

/**
 * 
 */
public class MessageHelperTest extends TestCase {

    public void testClonePartially() throws Exception {
        String key = "propKey";
        MessageContext origMc = new MessageContext();
        origMc.setProperty(key, "propValue");
        MessageContext newMc = MessageHelper.clonePartially(origMc);
        Object result = newMc.getProperty(key);
        assertEquals(result, "propValue");
    }
    
    // Regression test for SYNAPSE-309
    public void testClonePartiallyWithAttachments() throws Exception {
        MessageContext origMc = new MessageContext();
        String contentId = origMc.addAttachment(new DataHandler("test", "text/html"));
        MessageContext newMc = MessageHelper.clonePartially(origMc);
        DataHandler dh = newMc.getAttachment(contentId);
        assertNotNull(dh);
        assertEquals("test", dh.getContent());
    }

    public void testCloneSoapEnvelope() {
        SOAPFactory soapFactory;
        SOAPHeaderBlock header;
        OMFactory omFactory = OMAbstractFactory.getOMFactory();
        // Creating a namespace for the header
        OMNamespace ns = omFactory.createOMNamespace("http://ws.apache.org/axis2", "hns");
        OMElement childNode = omFactory.createOMElement("Child",ns);
        // testing SOAP 1.1
        soapFactory = OMAbstractFactory.getSOAP11Factory();
        //creating a SOAP header block
        header = new SOAP11HeaderBlockImpl("CustomHeader",ns,soapFactory);
        performTestForCloneEnvelope(soapFactory,header,childNode);
        // testing SOAP 1.1
        soapFactory = OMAbstractFactory.getSOAP12Factory();
        header = new SOAP12HeaderBlockImpl("CustomHeader",ns,soapFactory);
        performTestForCloneEnvelope(soapFactory, header, childNode);
    }

    private void performTestForCloneEnvelope(SOAPFactory soapFactory, SOAPHeaderBlock header, OMElement childNode) {

        SOAPEnvelope tempEnvelope = soapFactory.getDefaultEnvelope();
        //adding a text value
        header.setText("my custom header");
        //adding an attribute
        header.addAttribute("name", "value", null);
        tempEnvelope.getHeader().addChild(header);
        tempEnvelope.getBody().addChild(childNode);
        SOAPEnvelope clonedEnvelope= MessageHelper.cloneSOAPEnvelope(tempEnvelope);
        assertEquals(tempEnvelope.toString(),clonedEnvelope.toString());
    }
}

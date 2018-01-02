/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.passthru.util;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test cases for BinaryRelayBuilder class
 */
public class BinaryRelayBuilderTest extends TestCase {
    /**
     * This method tests the ability to read from the given input stream
     * @throws IOException
     */
    @Test
    public void testRead() throws IOException {
        InputStream in = new ByteArrayInputStream( "testString".getBytes());
        byte[] result = BinaryRelayBuilder.readAllFromInputSteam(in);
        Assert.assertNotNull("Couldn't read from the input stream!", result);
    }

    /**
     * This method tests whether it is possible to create OMElement from the input stream
     * @throws Exception
     */
    @Test
    public void testProcessDocument() throws Exception{
        InputStream in = new ByteArrayInputStream("testString".getBytes());
        String contentType = "application/xml";
        MessageContext messageContext = new MessageContext();

        BinaryRelayBuilder builder = new BinaryRelayBuilder();

        OMElement response = builder.processDocument(in, contentType, messageContext);

        String expected = "<?xml version='1.0' encoding='utf-8'?>"+
                "<soapenv:Envelope xmlns:soapenv=\"http://www.w3.org/2003/05/soap-envelope\">"+
                "<soapenv:Body><ns:binary xmlns:ns=\"http://ws.apache.org/commons/ns/payload\">dGVzdFN0cmluZw==</ns:binary>"+
                "</soapenv:Body></soapenv:Envelope>";

        Assert.assertEquals("Couldn't create OMElement!", expected, response.toString());
    }
}
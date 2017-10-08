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

package org.apache.synapse.samples.framework.tests.rest;

import org.apache.axiom.om.OMElement;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Sample10002 extends SynapseTestCase {

    private static final int[] SIZES = new int[] {
            500, 1024, 10240, 102400, 1024 * 1024
    };

    private static final String[] SIZE_STRINGS = new String[] {
            "500B", "1K", "10K", "100K", "1M"
    };

    public Sample10002() {
        super(10002);
    }

    public void testDirectMediation() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        int children = 0;
        StringBuilder xml = new StringBuilder("<foo>");

        for (int i = 0; i < SIZES.length; i++) {
            while (xml.length() < SIZES[i]) {
                xml.append("<bar>").append(UUID.randomUUID().toString()).append("</bar>");
                children++;
            }
            verifyMediationResult("DirectMediation", client, xml, children, SIZE_STRINGS[i]);
        }
    }

    public void testCBRMediation() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        int children = 1;
        StringBuilder xml = new StringBuilder("<foo><bar>uuid:1234567890</bar>");

        for (int i = 0; i < SIZES.length; i++) {
            while (xml.length() < SIZES[i]) {
                xml.append("<bar>").append(UUID.randomUUID().toString()).append("</bar>");
                children++;
            }
            verifyMediationResult("ContentBasedRouting", client, xml, children, SIZE_STRINGS[i]);
        }
    }

    public void testHeaderBasedRoutingMediation() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        int children = 0;
        StringBuilder xml = new StringBuilder("<foo>");

        Map<String,String> headers = new HashMap<String, String>();
        headers.put("CustomHeader", "TestValue");

        for (int i = 0; i < SIZES.length; i++) {
            while (xml.length() < SIZES[i]) {
                xml.append("<bar>").append(UUID.randomUUID().toString()).append("</bar>");
                children++;
            }
            verifyMediationResult("HeaderBasedRouting", client, xml, children,
                    SIZE_STRINGS[i], headers);
        }
    }

    public void testXSLTMediation() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        int children = 0;
        StringBuilder xml = new StringBuilder("<foo>");

        for (int i = 0; i < SIZES.length; i++) {
            while (xml.length() < SIZES[i]) {
                xml.append("<bar>").append(UUID.randomUUID().toString()).append("</bar>");
                children++;
            }
            verifyMediationResult("XSLT", client, xml, children, SIZE_STRINGS[i]);
        }
    }

    private void verifyMediationResult(String scenario, BasicHttpClient client,
                                       StringBuilder message, int childrenCount,
                                       String sizeStr) throws Exception {

        verifyMediationResult(scenario, client, message, childrenCount, sizeStr, null);
    }

    private void verifyMediationResult(String scenario, BasicHttpClient client,
                                       StringBuilder message, int childrenCount,
                                       String sizeStr, Map<String,String> headers) throws Exception {

        log.info(">>>>>>>>>>>>>>>> Testing " + scenario + "; Payload size: " + sizeStr);

        HttpResponse response;
        if (headers != null) {
            response = client.doPost("http://127.0.0.1:8280/services/" + scenario + "Proxy",
                    message.append("</foo>").toString().getBytes(), "application/xml", headers);
        } else {
            response = client.doPost("http://127.0.0.1:8280/services/" + scenario + "Proxy",
                    message.append("</foo>").toString().getBytes(), "application/xml");
        }

        // remove the closing tag added in the previous step
        message.setLength(message.length() - 6);

        // We must get a 200 OK
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        OMElement body = response.getBodyAsXML();
        // First element must be 'foo'
        assertEquals("foo", body.getLocalName());

        Iterator childElements = body.getChildrenWithLocalName("bar");
        int returnedChildren = 0;
        while (childElements.hasNext()) {
            returnedChildren++;
            childElements.next();
        }
        // Must return all the child 'bar' elements we sent
        assertEquals(childrenCount, returnedChildren);

        log.info(">>>>>>>>>>>>>>>> " + scenario + " (" + sizeStr + "): SUCCESS");
    }
}

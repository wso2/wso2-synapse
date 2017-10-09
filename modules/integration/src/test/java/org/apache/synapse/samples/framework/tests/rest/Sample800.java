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
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
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

public class Sample800 extends SynapseTestCase {

    public Sample800() {
        super(800);
    }

    public void testGetQuote() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/stockquote/view/IBM");
        assertEquals(response.getStatus(), HttpStatus.SC_OK);
        OMElement body = response.getBodyAsXML();
        assertEquals(body.getLocalName(), "getQuoteResponse");
    }

    public void testPlaceOrder() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        String payload = "<placeOrder xmlns=\"http://services.samples\">\n" +
                "  <order>\n" +
                "     <price>50</price>\n" +
                "     <quantity>10</quantity>\n" +
                "     <symbol>IBM</symbol>\n" +
                "  </order>\n" +
                "</placeOrder>";
        HttpResponse response = client.doPost("http://127.0.0.1:8280/stockquote/order",
                payload.getBytes(), "application/xml");
        assertEquals(response.getStatus(), HttpStatus.SC_ACCEPTED);
        Thread.sleep(2000);
        assertEquals(1, getAxis2Server().getMessageCount("SimpleStockQuoteService", "placeOrder"));
    }
}

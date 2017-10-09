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

package org.apache.synapse.samples.framework.tests.proxy;

import org.apache.axiom.om.OMElement;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample150 extends SynapseTestCase {

    public Sample150() {
        super(150);
    }

    public void testBasicProxy() {
        String addUrl = "http://localhost:8280/services/StockQuoteProxy";
        log.info("Running test: Introduction to proxy services");
        StockQuoteSampleClient client = getStockQuoteClient();
        SampleClientResult result = client.requestStandardQuote(addUrl, null, null, "IBM" ,null);
        assertTrue("Client did not get run successfully ", result.responseReceived());
    }

    public void testProxyWSDL() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://localhost:8280/services/StockQuoteProxy?wsdl");
        assertEquals(response.getStatus(), HttpStatus.SC_OK);
        OMElement element = response.getBodyAsXML();
        assertEquals(element.getLocalName(), "definitions");
    }

}

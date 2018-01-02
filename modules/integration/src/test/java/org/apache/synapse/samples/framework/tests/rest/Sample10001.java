/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.samples.framework.tests.rest;

import org.apache.axiom.om.OMElement;
import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

public class Sample10001 extends SynapseTestCase {

    public Sample10001() {
        super(10001);
    }

    public void testGetQuote() throws Exception {
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/stockquote/view/IBM");
        assertEquals(response.getStatus(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        OMElement body = response.getBodyAsXML();
        assertEquals(body.getLocalName(), "Exception");
        log.info("An exception was thrown as expected.");
    }
}

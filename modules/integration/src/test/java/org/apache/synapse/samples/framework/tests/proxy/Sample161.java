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

import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

public class Sample161 extends SynapseTestCase {

    private static final String requestXml = "<test>foo</test>";

    public Sample161() {
        super(161);
    }

    public void testRespondMediator() throws Exception {
        String url = "http://localhost:8280/services/EchoService";
        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doPost(url, requestXml.getBytes(), "application/xml");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals("Request and Echo Service Response is not equal", requestXml, response.getBodyAsString());

    }

}

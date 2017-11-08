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

import org.apache.commons.io.FilenameUtils;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample152 extends SynapseTestCase {

    public Sample152() {
        super(152);
    }


    public void testTransportAndFormatSwitching() {
        String url2 = "https://localhost:8243/services/StockQuoteProxy";
        String trustStore = FilenameUtils.normalize(System.getProperty("user.dir") +
                "/modules/integration/src/test/resources/trust.jks");
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        StockQuoteSampleClient client = getStockQuoteClient();
        log.info("Running test: Switching transports and message format from SOAP to REST/POX");

        SampleClientResult result2 = client.requestStandardQuote(null, url2, null, "IBM" ,null);
        assertTrue("Client did not a response with https ", result2.responseReceived());
    }

}

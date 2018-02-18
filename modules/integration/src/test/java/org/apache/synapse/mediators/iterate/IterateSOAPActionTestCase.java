/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.iterate;

import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

/**
 * Test SOAP action attribute of iterate mediator
 */
public class IterateSOAPActionTestCase extends MediatorTestCase {

    public IterateSOAPActionTestCase() {
        loadConfiguration("/mediators/iterateSOAPActionTestConfig.xml");
    }

    public void testSOAPAction() throws Exception {
        String addUrl = "http://localhost:8280/services/iterateWithSOAPActionTestProxy";
        StockQuoteSampleClient client = getStockQuoteClient();
        SampleClientResult result = client.requestStandardQuote(addUrl, null, null, "WSO2", null);
        assertTrue("Client didn't get response", result.responseReceived());
    }
}

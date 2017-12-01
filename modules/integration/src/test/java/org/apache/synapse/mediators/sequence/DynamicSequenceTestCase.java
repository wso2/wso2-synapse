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

package org.apache.synapse.mediators.sequence;

import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

/**
 * Test case for setting a dynamic sequence name in sequence mediator
 * https://wso2.org/jira/browse/CARBON-12746
 */
public class DynamicSequenceTestCase extends MediatorTestCase {

    public DynamicSequenceTestCase() {
        loadConfiguration("/mediators/dynamicSequenceTestConfig.xml");
    }

    public void testSequenceMediatorWithDynamicSequence() {
        StockQuoteSampleClient client = getStockQuoteClient();
        client.addHttpHeader("Sequence", "sendToStockQuoteServiceSequence");
        SampleClientResult result = client.requestStandardQuote(
                "http://localhost:8280/services/sequenceMediatorDynamicSequenceTestProxy",
                null, null, "WSO2", null);
        assertTrue("Response not received", result.responseReceived());
    }
}

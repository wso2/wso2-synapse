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

package org.apache.synapse.mediators.send;

import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

/**
 * Test the message flow when a log mediator is placed after send mediator
 * https://wso2.org/jira/browse/ESBJAVA-1629
 */
public class LogMediatorAfterSendMediatorTestCase extends MediatorTestCase {

    public LogMediatorAfterSendMediatorTestCase() {
        loadConfiguration("/mediators/logMediatorAfterSendMediatorTestConfig.xml");
    }

    public void testLogMediatorAfterSendMediator() {
        StockQuoteSampleClient client = getStockQuoteClient();
        SampleClientResult result = client.requestStandardQuote("http://localhost:8280/services/sendMediatorWithLogAfterwardsTestProxy",
                null, null, "WSO2", null);
        assertTrue("Response not received", result.responseReceived());
    }
}

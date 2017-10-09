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

package org.apache.synapse.samples.framework.tests.message;

import org.apache.axis2.AxisFault;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample5 extends SynapseTestCase {

    public Sample5() {
        super(5);
    }


    public void testCreateFaultAndChangeDirection() {
        String addUrl = "http://localhost:9000/services/SimpleStockQuoteService";
        String trpUrl = "http://localhost:8280";
        String expectedError_MSFT = "Error while connecting to the endpoint (Connection Failed)";
        String expectedError_SUN = "Error while connecting to the endpoint (Connection Failed)";

        StockQuoteSampleClient client = getStockQuoteClient();

        log.info("Running test: Creating SOAP fault messages and changing the direction of a message");
        SampleClientResult result = client.requestStandardQuote(addUrl, trpUrl, null, "MSFT" ,null);
        assertFalse("Must not get a response", result.responseReceived());
        Exception resultEx = result.getException();
        assertNotNull("Did not receive expected error", resultEx);
        log.info("Got an error as expected: " + resultEx.getMessage());
        assertTrue("Did not receive expected error", resultEx instanceof AxisFault);
        assertTrue("Did not receive expected error",
                resultEx.getMessage().indexOf(expectedError_MSFT) != -1);

        result = client.requestStandardQuote(addUrl, trpUrl, null, "SUN" ,null);
        assertFalse("Must not get a response", result.responseReceived());
        resultEx = result.getException();
        assertNotNull("Did not receive expected error", resultEx);
        log.info("Got an error as expected: " + resultEx.getMessage());
        assertTrue("Did not receive expected error", resultEx instanceof AxisFault);
        assertTrue("Did not receive expected error",
                resultEx.getMessage().indexOf(expectedError_SUN) != -1);

        result = client.requestStandardQuote(addUrl, trpUrl, null, "IBM" ,null);
        assertFalse("Must not get a response", result.responseReceived());
        resultEx = result.getException();
        assertNotNull("Did not receive expected error", resultEx);
        log.info("Got an error as expected: " + resultEx.getMessage());
        assertTrue("Did not receive expected error", resultEx instanceof AxisFault);
    }

}

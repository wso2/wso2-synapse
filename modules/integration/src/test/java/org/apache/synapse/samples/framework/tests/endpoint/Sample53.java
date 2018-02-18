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

package org.apache.synapse.samples.framework.tests.endpoint;

import org.apache.axis2.AxisFault;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample53 extends SynapseTestCase {

    public Sample53() {
        super(53);
    }

    public void testFailOver() {
        String expectedError = "COULDN'T SEND THE MESSAGE TO THE SERVER";
        String addUrl = "http://localhost:8280/services/LBService1";
        log.info("Running test: Failover sending among 3 endpoints");
        final StockQuoteSampleClient client = getStockQuoteClient();

        // Send some messages and check
        SampleClientResult result = client.sessionlessClient(addUrl, null, 10);
        assertResponseReceived(result);

        // Stop BE server 1
        getBackendServerControllers().get(0).stopProcess();
        sleep(2000);

        // Send another burst of messages and check
        result = client.sessionlessClient(addUrl, null, 10);
        assertResponseReceived(result);

        // Stop BE server 2
        getBackendServerControllers().get(1).stopProcess();
        sleep(2000);

        // Send some more messages and check
        result = client.sessionlessClient(addUrl, null, 10);
        assertResponseReceived(result);

        // Stop BE server 3
        getBackendServerControllers().get(2).stopProcess();
        sleep(2000);

        // Send another message - Should fail
        result = client.sessionlessClient(addUrl, null, 1);
        Exception resultEx = result.getException();
        assertNotNull("Did not receive expected error", resultEx);
        log.info("Got an error as expected: " + resultEx.getMessage());
        assertTrue("Did not receive expected error", resultEx instanceof AxisFault);
        assertTrue("Did not receive expected error", resultEx.getMessage().contains(expectedError));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

}

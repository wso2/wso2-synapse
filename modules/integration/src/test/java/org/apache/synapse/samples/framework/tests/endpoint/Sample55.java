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

import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

public class Sample55 extends SynapseTestCase {

    private SampleClientResult result;

    public Sample55() {
        super(55);
    }


    public void testSessionFullLBFailOver() {
        final String addUrl = "http://localhost:8280/services/LBService1";
        final StockQuoteSampleClient client = getStockQuoteClient();
        log.info("Running test: Failover sending among 3 endpoints");
        Thread t = new Thread(new Runnable() {
            public void run() {
                result = client.statefulClient(addUrl, null, 200);
            }
        });
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {

        }

        getBackendServerControllers().get(0).stopProcess();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {

        }

        assertTrue("Did not receive a response", result.responseReceived());
    }
}

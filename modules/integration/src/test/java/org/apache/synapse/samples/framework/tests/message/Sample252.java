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

package org.apache.synapse.samples.framework.tests.message;

import org.apache.synapse.samples.framework.Axis2BackEndServerController;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.JMSSampleClient;

/**
 * Test case for Sample 252: Pure Text (Binary) and POX Message Support with JMS
 */
public class Sample252 extends SynapseTestCase {

    public Sample252() {
        super(252);
        System.setProperty("java.naming.provider.url", "tcp://localhost:62626");
    }

    public void testPlaceOrderTypeText() throws Exception {
        JMSSampleClient client = new JMSSampleClient();
        Axis2BackEndServerController axis2Server = getAxis2Server();
        if (axis2Server == null) {
            fail("Failed to load the Axis2BackEndServerController");
        }
        assertEquals(0, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        try {
            client.connect("dynamicQueues/JMSTextProxy");
            client.sendTextMessage("12.33 1000 ACP");
            Thread.sleep(2000);
            assertEquals(1, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        } finally {
            client.shutdown();
        }
    }

    public void testPlaceOrderTypePOX() throws Exception {
        JMSSampleClient client = new JMSSampleClient();
        Axis2BackEndServerController axis2Server = getAxis2Server();
        if (axis2Server == null) {
            fail("Failed to load the Axis2BackEndServerController");
        }
        assertEquals(0, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        try {
            client.connect("dynamicQueues/JMSPoxProxy");
            client.sendAsPox("MSFT");
            Thread.sleep(2000);
            assertEquals(1, axis2Server.getMessageCount("SimpleStockQuoteService", "placeOrder"));
        } finally {
            client.shutdown();
        }
    }
}

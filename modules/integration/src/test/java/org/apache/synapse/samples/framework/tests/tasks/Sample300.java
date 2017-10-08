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

package org.apache.synapse.samples.framework.tests.tasks;

import org.apache.synapse.samples.framework.SynapseTestCase;

public class Sample300 extends SynapseTestCase {

    public Sample300() {
        super(300);
    }

    public void testScheduledTask() throws Exception {
        log.info("Waiting 10 seconds for the task to run...");
        Thread.sleep(10000);
        int messageCount = getAxis2Server().getMessageCount("SimpleStockQuoteService", "getQuote");
        log.info("Task sent " + messageCount + " messages.");
        assertTrue(messageCount >= 2);
    }
}

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

import org.apache.commons.io.FilenameUtils;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.MTOMSwASampleClient;

import java.io.File;

public class Sample51 extends SynapseTestCase {

    public Sample51() {
        super(51);
    }

    public void testMTOMOptimization() {
        String ep = "http://localhost:8280/services/MTOMSwASampleService";
        String currentLocation = System.getProperty("user.dir") + File.separator;
        String filename = FilenameUtils.normalize(
                currentLocation + "repository/conf/sample/resources/mtom/asf-logo.gif");
        MTOMSwASampleClient client = getMTOMSwASampleClient();
        log.info("Running test: MTOM optimization and request/response correlation ");
        SampleClientResult result = client.sendUsingMTOM(filename, ep);
        assertResponseReceived(result);
    }

    public void testSWAOptimization() {
        String ep = "http://localhost:8280/services/MTOMSwASampleService";
        String currentLocation = System.getProperty("user.dir") + File.separator;
        String filename = FilenameUtils.normalize(
                currentLocation + "repository/conf/sample/resources/mtom/asf-logo.gif");
        MTOMSwASampleClient client = getMTOMSwASampleClient();
        log.info("Running test:SwA optimization and request/response correlation ");
        SampleClientResult result = client.sendUsingSWA(filename, ep);
        assertResponseReceived(result);
    }

}

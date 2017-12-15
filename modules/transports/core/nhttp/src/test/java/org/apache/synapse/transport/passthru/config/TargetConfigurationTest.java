/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.passthru.config;

import junit.framework.Assert;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.base.threads.NativeWorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTestUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Test class for TargetConfiguration.
 */
public class TargetConfigurationTest {

    private static final String PASS_THROUGH_TRANSPORT_WORKER_POOL = "PASS_THROUGH_TRANSPORT_WORKER_POOL";
    private PassThroughConfiguration passThroughConfiguration = PassThroughTestUtils.getPassThroughConfiguration();
    private TargetConfiguration targetConfiguration = null;

    @Before
    public void setUp() throws Exception {
        ConfigurationContext configurationContext = new ConfigurationContext(new AxisConfiguration());
        WorkerPool workerPool = new NativeWorkerPool(3, 4, 5, 5, "name", "id");
        configurationContext.setProperty(PASS_THROUGH_TRANSPORT_WORKER_POOL, workerPool);
        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(true, "testScheme");
        targetConfiguration = new TargetConfiguration(configurationContext, null, workerPool, metrics, null);
        targetConfiguration.build();
    }

    @Test
    public void testBuild() throws Exception {
        Assert.assertNotNull("Building base configuration isn't successful.", targetConfiguration);
    }

    @Test
    public void testGetPreserveHttpHeaders() throws Exception {
        List<String> preserveHttpHeaders = targetConfiguration.getPreserveHttpHeaders();
        Assert.assertNotNull("Preserve Headers lis is null.", preserveHttpHeaders);
        Assert.assertTrue("No header has been preserved.", preserveHttpHeaders.size() > 0);
    }

}
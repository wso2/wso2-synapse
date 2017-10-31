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

package org.apache.synapse.metrics;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;

/**
 * Unit tests for MetricsAggregatorModule
 */
public class MetricsAggregatorModuleTest extends TestCase {

    private MetricsAggregatorModule metricsAggregatorModule = new MetricsAggregatorModule();

    /**
     * Initializing metricsAggregationModule and assert for the returned counter obj
     * @throws AxisFault
     */
    public void testInit() throws AxisFault {
        AxisConfiguration axisConfiguration = new AxisConfiguration();
        ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
        metricsAggregatorModule.init(configurationContext, null);
        Counter counter = (Counter) axisConfiguration.getParameter(MetricsConstants.GLOBAL_REQUEST_COUNTER).getValue();
        Assert.assertEquals("Counter should be 0",counter.getCount(), 0);
    }

    public void testCanSupportAssertion() {
        Assert.assertFalse(metricsAggregatorModule.canSupportAssertion(null));
    }

}

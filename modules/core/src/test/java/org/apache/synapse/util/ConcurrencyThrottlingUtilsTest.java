/*
*  Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
*  KIND, either express or implied. See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.apache.synapse.util;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessController;
import org.apache.synapse.mediators.TestUtils;
import org.junit.Assert;

/**
 * Unit tests for ConcurrencyThrottlingUtils class.
 */
public class ConcurrencyThrottlingUtilsTest extends TestCase {

    /**
     * Test decrementConcurrencyThrottleAccessController method for incremented connection count.
     * @throws Exception
     */
    public void testDecrement() throws Exception {
        MessageContext mc = TestUtils.getTestContext("<a><b>bob</b></a>", null);
        mc.setProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE, true);
        ConcurrentAccessController controller = new ConcurrentAccessController(1);
        mc.setProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_CONTROLLER, controller);
        //Set limit and counter to 5
        controller.set(5);
        ConcurrencyThrottlingUtils.decrementConcurrencyThrottleAccessController(mc);
        //getAndDecrement - decrement by 1 and return previous value
        int newValue = controller.getAndDecrement();
        Assert.assertEquals("connection count after increment should be 6", 6, newValue);
        Assert.assertFalse("flag should be cleared to avoid further executions of the method",
                (boolean) mc.getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE));
    }
}

/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.throttle.module.utils;

import junit.framework.TestCase;
import java.net.UnknownHostException;

/**
 * Test class for org.apache.synapse.commons.throttle.module.utils.Utils
 */
public class UtilsTest extends TestCase {

    /**
     * Test IP in range method
     * @throws UnknownHostException
     */
    public void testIpInRange() throws UnknownHostException {
        assertTrue("IP is in range", Utils.isIpInRange("192.168.1.1", "192.168.1.1-192.168.1.10"));
        assertTrue("IP is in range", Utils.isIpInRange("192.168.1.4", "192.168.1.1-192.168.1.10"));
        assertFalse("IP not in range", Utils.isIpInRange("192.164.1.10", "192.168.1.1-192.168.1.10"));
        assertFalse("IP not in range", Utils.isIpInRange("193.168.1.1", "192.168.1.1-192.168.1.10"));
    }

    /**
     * Test IP in range for null values
     * @throws UnknownHostException
     */
    public void testIpInRangeWithNull() throws UnknownHostException {
        assertFalse("Result should be false since IP is null", Utils.isIpInRange(null, "192.168.1.1-192.168.1.10"));
        assertFalse("Result should be false since range is null", Utils.isIpInRange("193.168.1.1", null));
    }

    /**
     * Test extracting consumer key from Auth header
     */
    public void testExtractConsumerKeyFromAuthHeader() {
        String header = "OAuth oauth_consumer_key=\"nq21LN39VlKe6OezcOndBx\",\n" +
                "oauth_signature_method=\"HMAC-SHA1\", oauth_signature=\"DZKyT75hiOIdtMGCU%2BbITArs4sU%3D\",\n" +
                "oauth_timestamp=\"1328590467\", oauth_nonce=\"7031216264696\", oauth_version=\"1.0\"";
        assertEquals("Invalid consumer key received", "nq21LN39VlKe6OezcOndBx",
                Utils.extractCustomerKeyFromAuthHeader(header));
    }
}

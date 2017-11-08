/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.endpoints.DynamicLoadbalanceEndpoint;
import org.junit.Test;

/**
 * Test class for building and then serializing dynamic loadbalance endpoint
 */
public class DynamicLoadBalanceEndpointFactoryTest {
    /**
     * Testing whether synapse exception is thrown when the membership handler cannot be found.
     * The given membership handler is a false one.
     * @throws Exception
     */
    @Test(expected = SynapseException.class)
    public void test() throws Exception {
        String inputXml = "<endpoint  xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<dynamicLoadbalance policy=\"roundRobin\" failover=\"true\">"
                + "    <membershipHandler class=\"org.apache.synapse.core.LoadBalanceMembershipHandler\">"
                + "      <property name=\"name\" value=\"value\"/>" + "    </membershipHandler>"
                + "  </dynamicLoadbalance>" + "</endpoint>";
        OMElement inputElement = AXIOMUtil.stringToOM(inputXml);
        DynamicLoadbalanceEndpoint endpoint = (DynamicLoadbalanceEndpoint) DynamicLoadbalanceEndpointFactory
                .getEndpointFromElement(inputElement, true, null);
    }
}
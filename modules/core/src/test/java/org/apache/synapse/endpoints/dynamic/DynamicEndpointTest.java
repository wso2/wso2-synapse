/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints.dynamic;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.util.xpath.SynapseXPath;

public class DynamicEndpointTest extends TestCase {

    public void testContextProperties() throws Exception {
        SynapseXPath xpath = new SynapseXPath("$ctx:timeout");
        AbstractEndpoint endpoint = new AddressEndpoint();
        EndpointDefinition definition = new EndpointDefinition();
        endpoint.setDefinition(definition);
        definition.setDynamicTimeoutExpression(xpath);
        MessageContext synCtx = new TestMessageContext();
        synCtx.setProperty("timeout", "90000");
        assertEquals(Long.valueOf((String) xpath.evaluate(synCtx)).longValue(),
                endpoint.getDefinition().evaluateDynamicEndpointTimeout(synCtx));
    }

}

/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.config.xml.inbound;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.commons.property.PropertyHolder;
import org.apache.synapse.commons.resolvers.ResolverException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.inbound.InboundEndpoint;
import org.junit.After;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests resolving the suspend attribute of an inbound endpoint through the configuration resolver.
 */
public class InboundEndpointFactorySuspendTest {

    private static final String CONFIG_KEY = "inboundSuspendTestKey";

    @After
    public void tearDown() {
        PropertyHolder.getInstance().getProperties().remove(CONFIG_KEY);
    }

    @Test
    public void testSuspendResolvedToTrue() throws XMLStreamException {
        PropertyHolder.getInstance().setProperty(CONFIG_KEY, "true");
        assertTrue(createInboundEndpoint("suspend=\"${configs." + CONFIG_KEY + "}\"").isSuspend());
    }

    @Test
    public void testSuspendResolvedToFalse() throws XMLStreamException {
        PropertyHolder.getInstance().setProperty(CONFIG_KEY, "false");
        assertFalse(createInboundEndpoint("suspend=\"${configs." + CONFIG_KEY + "}\"").isSuspend());
    }

    @Test(expected = ResolverException.class)
    public void testSuspendUndefinedConfigKey() throws XMLStreamException {
        createInboundEndpoint("suspend=\"${configs." + CONFIG_KEY + "}\"");
    }

    @Test
    public void testSuspendLiteralValues() throws XMLStreamException {
        assertTrue(createInboundEndpoint("suspend=\"true\"").isSuspend());
        assertFalse(createInboundEndpoint("suspend=\"false\"").isSuspend());
    }

    @Test
    public void testSuspendDefaultsToFalse() throws XMLStreamException {
        assertFalse(createInboundEndpoint("").isSuspend());
    }

    private InboundEndpoint createInboundEndpoint(String suspendAttribute) throws XMLStreamException {
        OMElement inboundElem = AXIOMUtil.stringToOM("<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "name=\"SuspendInboundEP\" sequence=\"TestIn\" onError=\"fault\" protocol=\"http\" " +
                suspendAttribute + "><parameters>" +
                "<parameter name=\"inbound.http.port\">8085</parameter></parameters></inboundEndpoint>");
        return InboundEndpointFactory.createInboundEndpoint(inboundElem, new SynapseConfiguration());
    }
}

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
 *
 */
package org.apache.synapse.endpoints.dynamic;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.inbound.InboundEndpointFactory;
import org.apache.synapse.config.xml.inbound.InboundEndpointSerializer;
import org.apache.synapse.inbound.InboundEndpoint;

/**
 * Testing InboundEndpoint related operations
 */
public class InboundEndpointTestCase extends TestCase {
    private InboundEndpoint ep;
    private SynapseConfiguration config = new SynapseConfiguration();
    private InboundEndpointSerializer serializer = new InboundEndpointSerializer();
    private InboundEndpointFactory factory = new InboundEndpointFactory();

    private static final String sampleEP = "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n"
            + "                name=\"HttpListenerEP1\"\n" + "                sequence=\"TestIn\"\n"
            + "                onError=\"fault\"\n" + "                protocol=\"http\"\n"
            + "                suspend=\"false\" statistics=\"enable\" trace=\"enable\">\n" + "   <parameters>\n"
            + "    <parameter name=\"inbound.http.port\">8085</parameter>\n" + "   </parameters>\n"
            + "</inboundEndpoint>";
    private static final String sampleEP2 = "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n"
            + "                name=\"HttpListenerEP2\"\n" + "                sequence=\"TestIn\"\n"
            + "                onError=\"fault\"\n" + "                protocol=\"http\"\n"
            + "                suspend=\"false\">\n" + "   <parameters>\n"
            + "    <parameter name=\"inbound.http.port\">8085</parameter>\n" + "   </parameters>\n"
            + "</inboundEndpoint>";
    private static final String sampleEP3 = "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n"
            + "                name=\"HttpListenerEP3\"\n" + "                sequence=\"TestIn\"\n"
            + "                onError=\"fault\"\n" + "                protocol=\"http\"\n"
            + "                 statistics=\"enable\" trace=\"enable\">\n" + "   <parameters>\n"
            + "    <parameter name=\"inbound.http.port\">8085</parameter>\n" + "   </parameters>\n"
            + "</inboundEndpoint>";
    private static final String sampleEP4 =
            "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n" + "name=\"\"\n"
                    + "                sequence=\"TestIn\"\n" + "onError=\"fault\"\n"
                    + "                protocol=\"http\"\n"
                    + "                 statistics=\"enable\" trace=\"enable\">\n" + "<parameters>\n"
                    + "  <parameter name=\"inbound.http.port\">8085</parameter>   </parameters>\n"
                    + "</inboundEndpoint>";
    private static final String invalidSampleEP = "<inboundEndpoint xmlns=\"http://ws.apache.org/ns/synapse\"\n"
            + "                                sequence=\"TestIn\"\n" + "onError=\"fault\"\n"
            + "                protocol=\"http\"\n" + "suspend=\"false\">\n" + "<parameters>\n"
            + "    <parameter name=\"inbound.http.port\">8085</parameter>\n" + "</parameters>\n" + "</inboundEndpoint>";
    private static final String parentElm = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"TestIn\">\n"
            + "    <send receive=\"receiveSeq\">\n" + "<endpoint>\n"
            + "            <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n"
            + "        </endpoint>\n" + "    </send>\n" + "</sequence>\n"
            + "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"receiveSeq\">\n" + "    <send/>\n"
            + "</sequence>";

    public void testCreateValidInboundEP() throws Exception {
        ep = factory.createInboundEndpoint(AXIOMUtil.stringToOM(sampleEP), config);
        Assert.assertNotNull("Inbound Endpoint is null", ep);
        Assert.assertEquals("Invalid EP name for inbound endpoint", "HttpListenerEP1", ep.getName());
    }

    public void testSerializeInboundEP() throws Exception {
        ep = factory.createInboundEndpoint(AXIOMUtil.stringToOM(sampleEP2), config);
        OMElement elm = serializer.serializeInboundEndpoint(ep);
        Assert.assertNotNull("Serialized endpoint is null", elm);
        Assert.assertEquals("Invalid EP name for serialized inbound endpoint", "HttpListenerEP2",
                ep.getName());
    }

    public void testSerializeInboundEPWithParent() throws Exception {
        ep = factory.createInboundEndpoint(AXIOMUtil.stringToOM(sampleEP3), config);
        OMElement elm2 = serializer.serializeInboundEndpoint(AXIOMUtil.stringToOM(parentElm), ep);
        Assert.assertNotNull("Serialized endpoint with parent  is null", elm2);
        Assert.assertEquals("Invalid EP name for serialized inbound endpoint", "HttpListenerEP3",
                ep.getName());
    }

    public void testCreateInboundEPWithEmptyName() throws Exception {
        ep = factory.createInboundEndpoint(AXIOMUtil.stringToOM(sampleEP4), config);
        Assert.assertNotNull("Serialized endpoint with empty name  is null", ep);
        Assert.assertEquals(ep.getName().isEmpty(), true);
    }

    public void testCreateInvalidInboundEP() throws Exception {
        InboundEndpointFactory factory = new InboundEndpointFactory();
        try {
            ep = factory.createInboundEndpoint(AXIOMUtil.stringToOM(invalidSampleEP), config);
            Assert.fail("Expected exception is not thrown for invalid inbound EP");
        } catch (SynapseException e) {
            Assert.assertEquals(e.getMessage().contains("Inbound Endpoint name cannot be null"), true);
        }
    }
}

/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.aspects.flow.statistics.util;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for TracingDataCollectionHelper class.
 */
public class TracingDataCollectionHelperTest {

    private static Axis2MessageContext messageContext;

    /**
     * Initializing message context.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void initialize() throws Exception {
        String samplePayload = "<test>value</test>";
        Map<String, Entry> properties = new HashMap<>();
        messageContext = TestUtils.getAxis2MessageContext(samplePayload, properties);
    }

    /**
     * Test CollectPayload exception condition.
     */
    @Test
    public void testCollectPayloadNone() {
        MessageContext messageContext = new TestMessageContext();
        String payload = TracingDataCollectionHelper.collectPayload(messageContext);
        Assert.assertEquals("should return NONE for invalid message context", "NONE", payload);
    }

    /**
     * Test CollectPayload for valid message context.
     *
     * @throws Exception
     */
    @Test
    public void testCollectPayload() throws Exception {
        String payload = TracingDataCollectionHelper.collectPayload(messageContext);
        Assert.assertEquals("should return envelope for a valid messageContext",
                messageContext.getEnvelope().toString(), payload);
    }

    /**
     * Test extractContextProperties method.
     *
     * @throws Exception
     */
    @Test
    public void testExtractContextProperties() throws Exception {
        messageContext.setProperty("testProp", "testValue");
        messageContext.setProperty(SynapseConstants.STATISTICS_STACK, "value2");
        messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, "value3");
        Map<String, Object> tempMap = TracingDataCollectionHelper.extractContextProperties(messageContext);
        Assert.assertNull("value should be removed from the map", tempMap.get(SynapseConstants.STATISTICS_STACK));
        Assert.assertNull("value should be removed from the map",
                tempMap.get(StatisticsConstants.STAT_COLLECTOR_PROPERTY));
        Assert.assertEquals("remaining entry should be in the map", 1, tempMap.size());
    }

    /**
     * Test extractTransportProperties method.
     */
    @Test
    public void testExtractTransportProperties() {
        String Cookie = "Cookie";
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Cookie, "testCookie");
        propertyMap.put("Random", "RandomValue");
        messageContext.getAxis2MessageContext().setProperty(org.apache
                .axis2.context.MessageContext.TRANSPORT_HEADERS, propertyMap);
        EndpointReference endpointReferenceTO = new EndpointReference();
        endpointReferenceTO.setAddress("testAddressTo");
        EndpointReference endpointReferenceFrom = new EndpointReference();
        endpointReferenceFrom.setAddress("testAddressFrom");
        EndpointReference endpointReferenceReplyTo = new EndpointReference();
        endpointReferenceReplyTo.setAddress("testAddressReplyTo");
        messageContext.setTo(endpointReferenceTO);
        messageContext.setFrom(endpointReferenceFrom);
        messageContext.setWSAAction("testWSAAction");
        messageContext.setSoapAction("testSoapAction");
        messageContext.setReplyTo(endpointReferenceReplyTo);
        messageContext.setMessageID("1234");
        Map<String, Object> tempMap = TracingDataCollectionHelper.extractTransportProperties(messageContext);
        Assert.assertNull("cookies should be removed", tempMap.get(Cookie));
        Assert.assertEquals("all the properties other than cookies should be inserted to the map",
                7, tempMap.size());
    }
}

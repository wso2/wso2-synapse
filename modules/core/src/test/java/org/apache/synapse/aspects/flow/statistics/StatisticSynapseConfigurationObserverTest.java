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
package org.apache.synapse.aspects.flow.statistics;

import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.DefaultEndpoint;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.api.API;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for StatisticSynapseConfigurationObserver class.
 */
public class StatisticSynapseConfigurationObserverTest {

    private StatisticSynapseConfigurationObserver observer = new StatisticSynapseConfigurationObserver();

    /**
     * Test sequenceAdded method by hash generation.
     */
    @Test
    public void testSequenceAdded() {
        SequenceMediator mediator = new SequenceMediator();
        observer.sequenceAdded(mediator);
        Assert.assertNotNull("New hash must be set by the method", mediator.getAspectConfiguration().getHashCode());
    }

    /**
     * Test proxyServiceAdded method by hash generation.
     */
    @Test
    public void testProxyServiceAdded() {
        ProxyService proxyService = new ProxyService("test");
        observer.proxyServiceAdded(proxyService);
        Assert.assertNotNull("New hash must be set by the method",
                proxyService.getAspectConfiguration().getHashCode());
    }

    /**
     * Test apiUpdated method by hash generation.
     */
    @Test
    public void testApiUpdated() {
        final String apiName = "testName";
        final String apiContext = "/testContext";
        API api = new API(apiName, apiContext);
        observer.apiUpdated(api);
        Assert.assertNotNull("New hash must be set by the method", api.getAspectConfiguration().getHashCode());
    }

    /**
     * Test InboundEndpointAdded by hash generation.
     */
    @Test
    public void testInboundEndpointAdded() {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        observer.inboundEndpointAdded(inboundEndpoint);
        Assert.assertNotNull("New hash must be set by the method",
                inboundEndpoint.getAspectConfiguration().getHashCode());
    }

    /**
     * Test EndpointAdded by hash generation.
     */
    @Test
    public void testEndpointAdded() {
        DefaultEndpoint endpoint = new DefaultEndpoint();
        observer.endpointAdded(endpoint);
        Assert.assertNotNull("New hash must be set by the method",
                endpoint.getDefinition().getAspectConfiguration().getHashCode());
    }
}

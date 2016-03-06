/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.synapse.Mediator;
import org.apache.synapse.Nameable;
import org.apache.synapse.Startup;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseObserver;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.eventing.SynapseEventSource;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.API;

public class StatisticSynapseConfigurationObserver implements SynapseObserver{
	@Override public void sequenceAdded(Mediator sequence) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(((Nameable) sequence).getName());
		sequence.setComponentStatisticsId();
		((SequenceMediator) sequence).getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void sequenceRemoved(Mediator sequence) {

	}

	@Override public void sequenceTemplateAdded(Mediator template) {

	}

	@Override public void sequenceTemplateRemoved(Mediator template) {

	}

	@Override public void entryAdded(Entry entry) {

	}

	@Override public void entryRemoved(Entry entry) {

	}

	@Override public void endpointAdded(Endpoint endpoint) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(endpoint.getName());
		endpoint.setComponentStatisticsId();
		((AbstractEndpoint) endpoint).getDefinition().getAspectConfiguration()
		                             .setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void endpointRemoved(Endpoint endpoint) {

	}

	@Override public void proxyServiceAdded(ProxyService proxy) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(proxy.getName());
		proxy.setComponentStatisticsId();
		proxy.getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void proxyServiceRemoved(ProxyService proxy) {

	}

	@Override public void apiAdded(API api) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(api.getName());
		api.setComponentStatisticsId();
		api.getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void apiRemoved(API api) {

	}

	@Override public void apiUpdated(API api) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(api.getName());
		api.setComponentStatisticsId();
		api.getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void inboundEndpointAdded(InboundEndpoint inboundEndpoint) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(inboundEndpoint.getName());
		inboundEndpoint.setComponentStatisticsId();
		inboundEndpoint.getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void inboundEndpointRemoved(InboundEndpoint inboundEndpoint) {

	}

	@Override public void inboundEndpointUpdated(InboundEndpoint inboundEndpoint) {
		StatisticIdentityGenerator.resetId();
		StatisticIdentityGenerator.setParent(inboundEndpoint.getName());
		inboundEndpoint.setComponentStatisticsId();
		inboundEndpoint.getAspectConfiguration().setHashCode(StatisticIdentityGenerator.getHashCode());
		StatisticIdentityGenerator.resetId();
	}

	@Override public void startupAdded(Startup startup) {

	}

	@Override public void startupRemoved(Startup startup) {

	}

	@Override public void eventSourceAdded(SynapseEventSource eventSource) {

	}

	@Override public void eventSourceRemoved(SynapseEventSource eventSource) {

	}

	@Override public void priorityExecutorAdded(PriorityExecutor exec) {

	}

	@Override public void priorityExecutorRemoved(PriorityExecutor exec) {

	}
}

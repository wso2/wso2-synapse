/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.observability;

import io.prometheus.client.hotspot.DefaultExports;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.AbstractSynapseErrorHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.API;

public class CustomObservabilitySynapseHandler extends AbstractSynapseErrorHandler {

    @Override
    public boolean handleErrorResponse(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) synCtx)
                .getAxis2MessageContext();

        String remoteAddr = (String) axis2MessageContext.getProperty(
                org.apache.axis2.context.MessageContext.REMOTE_ADDR);

        if (null != synCtx.getProperty(SynapseConstants.PROXY_SERVICE)) {

            String name = synCtx.getProperty("proxy.name").toString();
            String host = axis2MessageContext.getProperty("REMOTE_HOST").toString();

            PrometheusMetrics.ERROR_REQUESTS_RECEIVED_PROXY_SERVICE.labels(name, host).inc();
        } else if (null != synCtx.getProperty(SynapseConstants.IS_INBOUND)) {
            String inboundEndpointName = synCtx.getProperty("inbound.endpoint.name").toString();

            PrometheusMetrics.ERROR_REQUESTS_RECEIVED_INBOUND_ENDPOINT.labels(inboundEndpointName).inc();
        } else {
            String context = axis2MessageContext.getProperty("TransportInURL").toString();
            String apiInvocationUrl = axis2MessageContext.getProperty("SERVICE_PREFIX").toString() +
                    context.replaceFirst("/", "");
            String apiContext = context.substring(context.indexOf("/"), context.lastIndexOf("/"));
            String apiName = "";

            for (API api : synCtx.getEnvironment().getSynapseConfiguration().getAPIs()) {
                if (api.getContext().equals(apiContext)) {
                    apiName = api.getAPIName();
                }
            }

            PrometheusMetrics.ERROR_REQUESTS_RECEIVED_API.labels(apiName, apiInvocationUrl, remoteAddr).inc();
        }

        if ((null != synCtx.getProperty("REST_FULL_REQUEST_PATH")) && (synCtx.getProperty("REST_FULL_REQUEST_PATH").
                                                                            equals("/metric-service/metrics"))) {

        } else {
            if (null != PrometheusMetrics.proxyLatencyTimer) {
                PrometheusMetrics.proxyLatencyTimer.observeDuration();
            }
            if (null != PrometheusMetrics.apiLatencyTimer) {
                PrometheusMetrics.apiLatencyTimer.observeDuration();
            }
            if (null != PrometheusMetrics.inboundEndpointLatencyTimer) {
                PrometheusMetrics.inboundEndpointLatencyTimer.observeDuration();
            }
        }
        synCtx.setProperty("HasCountedError",true);
        return true;
    }

    private static final Log log = LogFactory.getLog(CustomObservabilitySynapseHandler.class);

    @Override
    public boolean handleRequestInFlow(MessageContext synCtx) {

        DefaultExports.initialize();

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) synCtx)
                                                               .getAxis2MessageContext();

        String remoteAddr = (String) axis2MessageContext.getProperty(
                                                                org.apache.axis2.context.MessageContext.REMOTE_ADDR);

        if (null != synCtx.getProperty(SynapseConstants.PROXY_SERVICE)) {
                                                   String proxyName = axis2MessageContext.getAxisService().getName();

            PrometheusMetrics.TOTAL_REQUESTS_RECEIVED_PROXY.labels(proxyName, remoteAddr).inc();
            PrometheusMetrics.proxyLatencyTimer = PrometheusMetrics.PROXY_LATENCY_DURATION_HISTOGRAM.labels(proxyName).
                                                                                                 startTimer();

            synCtx.setProperty("HistogramTimer", PrometheusMetrics.proxyLatencyTimer);
        } else if (null != synCtx.getProperty(SynapseConstants.IS_INBOUND)) {
            String inboundEndpointName = synCtx.getProperty("inbound.endpoint.name").toString();

            PrometheusMetrics.TOTAL_REQUESTS_RECEIVED_INBOUND_ENDPOINT.labels(inboundEndpointName).inc();
            PrometheusMetrics.inboundEndpointLatencyTimer = PrometheusMetrics.INBOUND_ENDPOINT_LATENCY_HISTOGRAM.
                    labels(inboundEndpointName).startTimer();

            synCtx.setProperty("HistogramTimer", PrometheusMetrics.inboundEndpointLatencyTimer);
        } else {
            String context = axis2MessageContext.getProperty("TransportInURL").toString();
            String apiInvocationUrl = axis2MessageContext.getProperty("SERVICE_PREFIX").toString() +
                    context.replaceFirst("/", "");
            String apiContext = context.substring(context.indexOf("/"), context.lastIndexOf("/"));
            String apiName = "";

            for (API api : synCtx.getEnvironment().getSynapseConfiguration().getAPIs()) {
                if (api.getContext().equals(apiContext)) {
                    apiName = api.getAPIName();
                }
            }

            PrometheusMetrics.TOTAL_REQUESTS_RECEIVED_API.labels(apiName, apiInvocationUrl, remoteAddr).inc();
            PrometheusMetrics.apiLatencyTimer = PrometheusMetrics.API_REQUEST_LATENCY_HISTOGRAM.
                    labels(apiName, apiInvocationUrl).startTimer();

            synCtx.setProperty("HistogramTimer", PrometheusMetrics.apiLatencyTimer);
        }

        return true;
    }

    @Override
    public boolean handleRequestOutFlow(MessageContext synCtx) {
        return true;
    }

    @Override
    public boolean handleResponseInFlow(MessageContext synCtx) {
        return true;
    }

    @Override
    public boolean handleResponseOutFlow(MessageContext synCtx) {
        if ((null != synCtx.getProperty("REST_FULL_REQUEST_PATH")) && (synCtx.getProperty("REST_FULL_REQUEST_PATH").
                                                                            equals("/metric-service/metrics"))) {
            log.info("Loading the metrics endpoint");

        } else {
            if (null != PrometheusMetrics.proxyLatencyTimer) {
                PrometheusMetrics.proxyLatencyTimer.observeDuration();
            }
            if (null != PrometheusMetrics.apiLatencyTimer) {
                PrometheusMetrics.apiLatencyTimer.observeDuration();
            }
            if (null != PrometheusMetrics.inboundEndpointLatencyTimer) {
                PrometheusMetrics.inboundEndpointLatencyTimer.observeDuration();
            }
        }
        return true;
    }
}

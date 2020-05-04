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

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

public final class PrometheusMetrics {

    /**
     * Counters for instrumenting metrics for Proxy Services.
     **/
    public static final Counter TOTAL_REQUESTS_RECEIVED_PROXY = Counter.build("total_request_count_proxy_serv", "Total number of requests. "
            + "to a proxy service").labelNames("service", "remoteAddress").register();

    public static final Counter ERROR_REQUESTS_RECEIVED_PROXY_SERVICE = Counter.build("total_error_request_count_proxy_service",
            "Total number of error requests to a proxy service").labelNames("service", "remoteAddress").register();

    /**
     * Counters for instrumenting metrics for APIs.
     **/
    public static final Counter TOTAL_REQUESTS_RECEIVED_API = Counter.build("total_request_count_api",
            "Total number of requests to an API.").labelNames("apiName", "invocationUrl", "remoteAddress").register();

    public static final Counter ERROR_REQUESTS_RECEIVED_API = Counter.build("total_error_request_count_api",
            "Total number of error requests to an api").labelNames("api_name", "invocationUrl", "remoteAddress").register();

    /**
     * Counters for instrumenting metrics for Inbound Endpoints.
     **/
    public static final Counter TOTAL_REQUESTS_RECEIVED_INBOUND_ENDPOINT = Counter.build("total_request_count_inbound_endpoint",
            "Total number of requests to an Inbound Endpoint.").labelNames("inboundEndpointName").register();

    public static final Counter ERROR_REQUESTS_RECEIVED_INBOUND_ENDPOINT = Counter.build("total_error_request_count_inbound_endpoint",
            "Total number of error requests to an inbound endpoint").labelNames("inboundEndpointName", "remoteHost").register();
    /**
     * Histograms for instrumenting metrics for Proxy Services.
     **/

    public static final Histogram PROXY_LATENCY_DURATION_HISTOGRAM = Histogram.build()
            .name("proxy_latency_seconds")
            .help("Proxy service latency in seconds")
            .labelNames("proxy_name")
            .buckets(0.19, 0.20, 0.25, 0.30, 0.35, 0.40, 0.50, 0.60, 1, 5)
            .register();

    /**
     * Histograms for instrumenting metrics for APIs.
     **/

    public static final Histogram API_REQUEST_LATENCY_HISTOGRAM = Histogram.build()
            .name("api_latency_time_seconds")
            .help("API latency time in seconds")
            .labelNames("api_name", "invocation_url")
            .buckets(0.0005, 0.0007, 0.001, 0.005, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1)
            .register();

    /**
     * Histograms for instrumenting metrics for Inbound Endpoints.
     **/
    public static final Histogram INBOUND_ENDPOINT_LATENCY_HISTOGRAM = Histogram.build()
            .name("inbound_endpoint_latency_time_seconds")
            .help("Inbound Endpoint latency time in seconds")
            .labelNames("inobund_endpoint_name")
            .buckets(0.00001,0.0001,0.0005, 0.0007, 0.001, 0.005, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1, 5, 10)
            .register();

    /**
     * Histograms timers for instrumenting metrics for Proxy Services.
     **/
    public static Histogram.Timer proxyLatencyTimer;

    /**
     * Histograms timers for instrumenting metrics for APIs.
     **/
    public static Histogram.Timer apiLatencyTimer;

    /**
     * Histograms timers for instrumenting metrics for Inbound Endpoints.
     **/
    public static Histogram.Timer inboundEndpointLatencyTimer;
}

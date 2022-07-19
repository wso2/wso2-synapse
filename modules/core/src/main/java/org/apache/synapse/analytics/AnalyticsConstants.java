/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.apache.synapse.analytics;

public class AnalyticsConstants {

    /**
     * Synapse Configurations
     */
    public static class SynapseConfiguration {
        /**
         * Schema version of the analytic.
         */
        public static final int SCHEMA_VERSION = 1;

        /**
         * Unique identifier for the publisher that can be used to filter analytics if multiple micro integrators are
         * publishing data to the same Elasticsearch server.
         */
        public static final String IDENTIFIER = "analytics.id";

        /**
         * Name of the Synapse configuration used to determine if analytics for APIs are enabled or disabled.
         */
        public static final String API_ANALYTICS_ENABLED = "analytics.api_analytics.enabled";

        /**
         * Name of the Synapse configuration used to determine if analytics for ProxyServices are enabled or disabled.
         */
        public static final String PROXY_SERVICE_ANALYTICS_ENABLED = "analytics.proxy_service_analytics.enabled";

        /**
         * Name of the Synapse configuration used to determine if analytics for Sequences are enabled or disabled.
         */
        public static final String SEQUENCE_ANALYTICS_ENABLED = "analytics.sequence_analytics.enabled";

        /**
         * Name of the Synapse configuration used to determine if analytics for Endpoints are enabled or disabled.
         */
        public static final String ENDPOINT_ANALYTICS_ENABLED = "analytics.endpoint_analytics.enabled";

        /**
         * Name of the Synapse configuration used to determine if analytics for Inbound Endpoints are enabled or disabled.
         */
        public static final String INBOUND_ENDPOINT_ANALYTICS_ENABLED = "analytics.inbound_endpoint_analytics.enabled";

        /**
         * Name of the Synapse configuration used to determine if sequence analytics are published only for NAMED sequences.
         */
        public static final String NAMED_SEQUENCES_ONLY = "analytics.sequence_analytics.named_only";

        /**
         * Name of the Synapse configuration used to determine the prefix Elasticsearch analytics are published with.
         * The purpose of this prefix is to distinguish log lines which hold analytics data from others.
         */
        public static final String ELASTICSEARCH_PREFIX = "analytics.prefix";

        /**
         * Name of the Synapse configuration used to determine if the Elasticsearch service is enabled.
         */
        public static final String ELASTICSEARCH_ENABLED = "analytics.enabled";
    }

    public static class ServerMetadataFieldDef {
        public static final String HOST_NAME = "hostname";
        public static final String SERVER_NAME = "serverName";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String PUBLISHER_ID = "id";
    }

    public static class EnvelopDef {
        public static final String TIMESTAMP = "timestamp";
        public static final String SCHEMA_VERSION = "schemaVersion";
        public static final String SERVER_INFO = "serverInfo";
        public static final String PAYLOAD = "payload";

        public static final String ENTITY_TYPE = "entityType";
        public static final String ENTITY_CLASS_NAME = "entityClassName";
        public static final String FAULT_RESPONSE = "faultResponse";
        public static final String CORRELATION_ID = "correlation_id";
        public static final String MESSAGE_ID = "messageId";
        public static final String LATENCY = "latency";
        public static final String METADATA = "metadata";

        public static final String REMOTE_HOST = "remoteHost";
        public static final String CONTENT_TYPE = "contentType";
        public static final String HTTP_METHOD = "httpMethod";

        public static final String API = "api";
        public static final String SUB_REQUEST_PATH = "subRequestPath";
        public static final String API_CONTEXT = "apiContext";
        public static final String METHOD = "method";
        public static final String TRANSPORT = "transport";
        public static final String API_DETAILS = "apiDetails";

        public static final String SEQUENCE_TYPE = "type";
        public static final String SEQUENCE_NAME = "name";
        public static final String SEQUENCE_API_CONTEXT = "apiContext";
        public static final String SEQUENCE_API = "api";
        public static final String SEQUENCE_API_SUB_REQUEST_PATH = "subRequestPath";
        public static final String SEQUENCE_API_METHOD = "method";
        public static final String SEQUENCE_PROXY_NAME = "proxyName";
        public static final String SEQUENCE_DETAILS = "sequenceDetails";

        public static final String PROXY_SERVICE_TRANSPORT = "transport";
        public static final String PROXY_SERVICE_IS_DOING_REST = "isClientDoingREST";
        public static final String PROXY_SERVICE_IS_DOING_SOAP11 = "isClientDoingSOAP11";
        public static final String PROXY_SERVICE_NAME = "name";
        public static final String PROXY_SERVICE_DETAILS = "proxyServiceDetails";

        public static final String ENDPOINT_NAME = "name";
        public static final String ENDPOINT_IS_ANONYMOUS = "isAnonymous";
        public static final String ENDPOINT_DETAILS = "endpointDetails";

        public static final String INBOUND_ENDPOINT_NAME = "name";
        public static final String INBOUND_ENDPOINT_PROTOCOL = "protocol";
        public static final String INBOUND_ENDPOINT_DETAILS = "inboundEndpointDetails";
    }
}

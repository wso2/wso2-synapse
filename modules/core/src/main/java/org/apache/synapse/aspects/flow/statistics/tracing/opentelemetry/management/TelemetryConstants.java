/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

public class TelemetryConstants {

    /**
     * Enable OpenTelemetry.
     */
    public static final String OPENTELEMETRY_ENABLE = "opentelemetry.enable";

    /**
     * Get the required OpenTelemetry endpoint URL.
     */
    public static final String OPENTELEMETRY_URL = "opentelemetry.url";

    /**
     * Fully qualified class name of the OpenTelemetry manager.
     */
    public static final String OPENTELEMETRY_CLASS = "opentelemetry.class";

    /**
     * Get the required OpenTelemetry host.
     */
    public static final String OPENTELEMETRY_HOST = "opentelemetry.host";

    /**
     * OpenTelemetry port.
     */
    public static final String OPENTELEMETRY_PORT = "opentelemetry.port";
    public static final String DEFAULT_OPENTELEMETRY_CLASS = "org.apache.synapse.aspects.flow.statistics.tracing" +
            ".opentelemetry.management.JaegerTelemetryManager";
    public static final String USER_DEFINED_NAME = System.getenv("SERVICE_NAME");
    public static final String SERVICE_NAME =
            USER_DEFINED_NAME != null && !USER_DEFINED_NAME.isEmpty() ? USER_DEFINED_NAME : "WSO2-SYNAPSE";
    public static final String OPENTELEMETRY_INSTRUMENTATION_NAME = "org.wso2.synapse.tracing.telemetry";
    public static final String DEFAULT_JAEGER_HOST = "localhost";
    public static final String DEFAULT_JAEGER_PORT = "14250";
    public static final String DEFAULT_ZIPKIN_HOST = "localhost";
    public static final String DEFAULT_ZIPKIN_PORT = "9411";
    public static final String ZIPKIN_API_CONTEXT = "/api/v2/spans";
    public static final String OPENTELEMETRY_PROPERTIES_PREFIX = "opentelemetry.properties.";
    static final String LATENCY = "Latency";
    static final String SPAN_NAME = "Span Name";
    static final String ATTRIBUTES = "Tags";
    static final String TRACE_ID = "Trace Id";
    static final String SPAN_ID = "Span Id";
    static final String TRACER = "tracer";

    //span attributes
    public static final String BEFORE_PAYLOAD_ATTRIBUTE_KEY = "beforePayload";
    public static final String AFTER_PAYLOAD_ATTRIBUTE_KEY = "afterPayload";
    public static final String BEFORE_CONTEXT_PROPERTY_MAP_ATTRIBUTE_KEY = "beforeContextPropertyMap";
    public static final String AFTER_CONTEXT_PROPERTY_MAP_ATTRIBUTE_KEY = "afterContextPropertyMap";
    public static final String BEFORE_CONTEXT_VARIABLE_MAP_ATTRIBUTE_KEY = "beforeContextVariableMap";
    public static final String AFTER_CONTEXT_VARIABLE_MAP_ATTRIBUTE_KEY = "afterContextVariableMap";
    public static final String PROPERTY_MEDIATOR_VALUE_ATTRIBUTE_KEY = "propertyMediatorValue";
    public static final String COMPONENT_NAME_ATTRIBUTE_KEY = "componentName";
    public static final String COMPONENT_TYPE_ATTRIBUTE_KEY = "componentType";
    public static final String COMPONENT_ID_ATTRIBUTE_KEY = "componentId";
    public static final String THREAD_ID_ATTRIBUTE_KEY = "threadId";
    public static final String HASHCODE_ATTRIBUTE_KEY = "hashcode";
    public static final String TRANSPORT_HEADERS_ATTRIBUTE_KEY = "Transport Headers";
    public static final String STATUS_CODE_ATTRIBUTE_KEY = "Status code";
    public static final String STATUS_DESCRIPTION_ATTRIBUTE_KEY = "Status description";
    public static final String ENDPOINT_ATTRIBUTE_KEY = "Endpoint";
    public static final String CORRELATION_ID_ATTRIBUTE_KEY = "CorrelationId";

    public static final String ERROR_CODE_ATTRIBUTE_KEY = "error.code";
    public static final String ERROR_MESSAGE_ATTRIBUTE_KEY = "error.message";

    public static final String OTEL_RESOURCE_ATTRIBUTE_KEY = "opentelemetry.properties.resource_attributes";
    public static final String OTEL_RESOURCE_ATTRIBUTES_ENVIRONMENT_VARIABLE_NAME = "OTEL_RESOURCE_ATTRIBUTES";

    public static final String OLTP_CUSTOM_SPAN_TAGS = "oltp.custom.span.header.tags";

    public static final String OLTP_FILTERED_MEDIATOR_NAMES = "oltp.filtered.mediator.names";
}

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
    public static final String OPENTRACING_ENABLE = "opentelemetry.enable";

    /**
     * Get the required URL.
     */
    public static final String TRACE_TYPE_URL = "opentelemetry.url";

    /**
     * Get the required classpath.
     */
    public static final String TRACE_TYPE_CLASS = "opentelemetry.class";

    /**
     * Get the required host.
     */
    public static final String TRACE_TYPE_HOST = "opentelemetry.host";

    /**
     * Get the required port.
     */
    public static final String TRACE_TYPE_PORT = "opentelemetry.port";
    public static final String DEFAULT_TRACE_CLASS = "org.apache.synapse.aspects.flow.statistics.tracing" +
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

}

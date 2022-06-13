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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.OpenTelemetryManager;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.SpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.concurrent.TimeUnit;

public class JaegerTelemetryManager implements OpenTelemetryManager {

    private Log logger = LogFactory.getLog(JaegerTelemetryManager.class);
    private SdkTracerProvider sdkTracerProvider;
    private OpenTelemetry openTelemetry;
    private TelemetryTracer tracer;
    private SpanHandler handler;

    @Override
    public void init() {

        String endPointURL = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.TRACE_TYPE_URL, null);
        JaegerGrpcSpanExporter jaegerExporter;
        if (endPointURL == null) {
            jaegerExporter =
                    JaegerGrpcSpanExporter.builder().setEndpoint("http://" + SynapsePropertiesLoader
                                    .getPropertyValue(TelemetryConstants.TRACE_TYPE_HOST,
                                            TelemetryConstants.DEFAULT_JAEGER_HOST)
                                    + ":" + Integer.parseInt(SynapsePropertiesLoader
                                    .getPropertyValue(TelemetryConstants.TRACE_TYPE_PORT,
                                            TelemetryConstants.DEFAULT_JAEGER_PORT))).setTimeout(30, TimeUnit.SECONDS)
                            .build();
        } else {
            jaegerExporter =
                    JaegerGrpcSpanExporter.builder().setEndpoint(endPointURL).setTimeout(30, TimeUnit.SECONDS)
                            .build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Jaeger exporter: " + jaegerExporter + " is configured");
        }

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME,
                TelemetryConstants.SERVICE_NAME));

        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .setResource(Resource.getDefault().merge(serviceNameResource))
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(JaegerPropagator.getInstance()))
                .build();

        this.tracer = new TelemetryTracer(getTelemetryTracer());
        if (logger.isDebugEnabled()) {
            logger.debug("Tracer: " + this.tracer + " is configured");
        }
        resolveHandler();
    }

    @Override
    public Tracer getTelemetryTracer() {

        return openTelemetry.getTracer(TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME);
    }

    @Override
    public void close() {

        if (sdkTracerProvider != null) {
            sdkTracerProvider.close();
        }
    }

    @Override
    public String getServiceName() {

        return TelemetryConstants.SERVICE_NAME;
    }

    @Override
    public void resolveHandler() {

        this.handler = new SpanHandler(tracer, openTelemetry, new TracingScopeManager());
    }

    @Override
    public OpenTelemetrySpanHandler getHandler() {

        return this.handler;
    }
}

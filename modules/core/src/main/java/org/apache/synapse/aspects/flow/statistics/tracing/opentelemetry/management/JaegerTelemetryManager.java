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
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.SpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
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

        String endPointURL = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_URL, null);
        String endPointHost = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_HOST,
                TelemetryConstants.DEFAULT_JAEGER_HOST);
        String endPointPort = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_PORT,
                TelemetryConstants.DEFAULT_JAEGER_PORT);
        JaegerGrpcSpanExporter jaegerExporter;
        if (endPointURL == null) {
            String jaegerExporterEndpoint = String.format("http://%s:%s", endPointHost, endPointPort);
            jaegerExporter = JaegerGrpcSpanExporter.builder().setEndpoint(jaegerExporterEndpoint).setTimeout(30,
                    TimeUnit.SECONDS).build();
        } else {
            if (endPointHost != null && endPointPort != null){
                logger.info("Disregarding " + TelemetryConstants.OPENTELEMETRY_HOST + " and " +
                        TelemetryConstants.OPENTELEMETRY_PORT + ", and using the provided " +
                        TelemetryConstants.OPENTELEMETRY_CLASS);
            }
            jaegerExporter =
                    JaegerGrpcSpanExporter.builder().setEndpoint(endPointURL).setTimeout(30, TimeUnit.SECONDS)
                            .build();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Jaeger exporter: " + jaegerExporter + " is configured");
        }

        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .setResource(Resource.getDefault().merge(TelemetryUtil.getTracerProviderResource(TelemetryConstants.SERVICE_NAME)))
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(JaegerPropagator.getInstance()))
                .build();

        this.tracer = new TelemetryTracer(getTelemetryTracer());
        if (logger.isDebugEnabled()) {
            logger.debug("Tracer: " + this.tracer + " is configured");
        }
        this.handler = new SpanHandler(tracer, openTelemetry, new TracingScopeManager());
    }

    @Override
    public Tracer getTelemetryTracer() {

        return openTelemetry.getTracer(TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME);
    }

    @Override
    public Meter getTelemetryMeter() {
        // Current implementation does not support metrics for Jaeger, This is implemented to return a No-op Meter.
        // Can be enhanced in the future to support metrics if required.
        return openTelemetry.getMeter(TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME);
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
    public OpenTelemetrySpanHandler getHandler() {

        return this.handler;
    }
}

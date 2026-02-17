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
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.time.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.SpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.Enumeration;

public class OTLPTelemetryManager implements OpenTelemetryManager {

    private Log logger = LogFactory.getLog(OTLPTelemetryManager.class);
    private SdkTracerProvider sdkTracerProvider;
    private SdkMeterProvider sdkMeterProvider;
    private OpenTelemetry openTelemetry;
    private TelemetryTracer tracer;
    private SpanHandler handler;

    @Override
    public void init() {

        // Read configuration properties
        String protocol = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_PROTOCOL,
                TelemetryConstants.GRPC_PROTOCOL);
        String endPointURL = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_URL, null);
        if (endPointURL == null) {
            String message = "Url not found in opentelemetry configurations";
            logger.error(message);
            throw new SynapseException(message);
        }

        // Determine protocol: validate protocol and select exporter
        boolean useHttp;
        if (TelemetryConstants.HTTP_PROTOCOL.equalsIgnoreCase(protocol)) {
            useHttp = true;
        } else if (TelemetryConstants.GRPC_PROTOCOL.equalsIgnoreCase(protocol)) {
            useHttp = false;
        } else {
            String message = "Invalid OpenTelemetry protocol: " + protocol
                    + ". Supported values are '" + TelemetryConstants.HTTP_PROTOCOL
                    + "' and '" + TelemetryConstants.GRPC_PROTOCOL + "'.";
            logger.error(message);
            throw new SynapseException(message);
        }

        // Get header property for authentication
        String headerProperty = getHeaderKeyProperty();
        if (headerProperty == null) {
            throw new SynapseException("No properties found starting with opentelemetry.properties");
        }
        String headerKey = headerProperty.substring(TelemetryConstants.OPENTELEMETRY_PROPERTIES_PREFIX.length());
        String headerValue = SynapsePropertiesLoader.getPropertyValue(headerProperty, null);
        if (headerValue == null) {
            String message = "Header value not found for property: " + headerKey;
            logger.error(message);
            throw new SynapseException(message);
        }

        // Create appropriate exporter based on protocol
        SpanExporter spanExporter;
        MetricExporter metricExporter;
        if (useHttp) {
            if (logger.isDebugEnabled()) {
                logger.debug("Configuring OTLP HTTP Exporters for endpoint: " + endPointURL);
            }
            spanExporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(endPointURL)
                    .setCompression("gzip")
                    .addHeader(headerKey, headerValue)
                    .build();
            metricExporter = OtlpHttpMetricExporter.builder()
                    .setEndpoint(endPointURL)
                    .addHeader(headerKey, headerValue)
                    .build();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Configuring OTLP gRPC Exporters for endpoint: " + endPointURL);
            }
            spanExporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(endPointURL)
                    .setCompression("gzip")
                    .addHeader(headerKey, headerValue)
                    .build();

            metricExporter = OtlpGrpcMetricExporter.builder()
                    .setEndpoint(endPointURL)
                    .setCompression("gzip")
                    .addHeader(headerKey, headerValue)
                    .build();
        }

        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(Resource.getDefault().merge(TelemetryUtil.getTracerProviderResource(TelemetryConstants.SERVICE_NAME)))
                .build();

        int metricIntervalSeconds = getMetricIntervalSeconds();

        sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setInterval(Duration.ofSeconds(metricIntervalSeconds))
                        .build())
                .setResource(Resource.getDefault()
                        .merge(TelemetryUtil.getTracerProviderResource(TelemetryConstants.SERVICE_NAME)))
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setMeterProvider(sdkMeterProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        this.tracer = new TelemetryTracer(getTelemetryTracer());
        if (logger.isDebugEnabled()) {
            logger.debug("Tracer for instrumentation '" + TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME + "' is configured with " +
                    (useHttp ? "HTTP" : "gRPC") + " protocol");
        }
        this.handler = new SpanHandler(tracer, openTelemetry, new TracingScopeManager());
    }

    @Override
    public Tracer getTelemetryTracer() {

        return openTelemetry.getTracer(TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME);
    }

    @Override
    public Meter getTelemetryMeter() {
        return openTelemetry.getMeter(TelemetryConstants.OPENTELEMETRY_INSTRUMENTATION_NAME);
    }

    @Override
    public void close() {

        if (sdkTracerProvider != null) {
            sdkTracerProvider.close();
        }

        if (sdkMeterProvider != null) {
            sdkMeterProvider.close();
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

    /**
     * Return the header key from synapse.properties file for specific OTLP based APM.
     *
     * @return Header key.
     */
    public String getHeaderKeyProperty() {

        Enumeration<?> synapsePropertyKeys = SynapsePropertiesLoader.loadSynapseProperties().propertyNames();
        while (synapsePropertyKeys.hasMoreElements()) {
            String property = (String) synapsePropertyKeys.nextElement();
            if (property.startsWith(TelemetryConstants.OPENTELEMETRY_PROPERTIES_PREFIX)) {
                return property;
            }
        }
        return null;
    }


    private int getMetricIntervalSeconds() {
        String metricIntervalString = SynapsePropertiesLoader.getPropertyValue(
                TelemetryConstants.OPENTELEMETRY_METRIC_PUSH_INTERVAL_SECONDS,
                TelemetryConstants.OPENTELEMETRY_METRIC_DEFAULT_PUSH_INTERVAL_SECONDS);

        int metricIntervalSeconds;
        try {
            metricIntervalSeconds = Integer.parseInt(metricIntervalString);
        } catch (NumberFormatException e) {
            String message = "Invalid OpenTelemetry metric push interval: " + metricIntervalString + ". Using default value: "
                    + TelemetryConstants.OPENTELEMETRY_METRIC_DEFAULT_PUSH_INTERVAL_SECONDS + " seconds.";
            logger.warn(message);
            metricIntervalSeconds = Integer.parseInt(
                    TelemetryConstants.OPENTELEMETRY_METRIC_DEFAULT_PUSH_INTERVAL_SECONDS);
        }
        if (metricIntervalSeconds <= 0) {
            logger.warn("OpenTelemetry metric push interval must be positive. Got: " + metricIntervalSeconds
                    + ". Using default value: " + TelemetryConstants.OPENTELEMETRY_METRIC_DEFAULT_PUSH_INTERVAL_SECONDS + " seconds.");
            metricIntervalSeconds = Integer.parseInt(
                    TelemetryConstants.OPENTELEMETRY_METRIC_DEFAULT_PUSH_INTERVAL_SECONDS);
        }
        return metricIntervalSeconds;
    }
}

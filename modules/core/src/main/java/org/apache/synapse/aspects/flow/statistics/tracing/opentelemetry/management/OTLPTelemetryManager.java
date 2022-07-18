package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.SpanHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.Enumeration;

public class OTLPTelemetryManager implements OpenTelemetryManager {

    private Log logger = LogFactory.getLog(OTLPTelemetryManager.class);
    private SdkTracerProvider sdkTracerProvider;
    private OpenTelemetry openTelemetry;
    private TelemetryTracer tracer;
    private SpanHandler handler;

    @Override
    public void init() {

        String headerProperty = getHeaderKeyProperty();
        String headerKey = headerProperty.substring(TelemetryConstants.OPENTELEMETRY_PROPERTIES.length());
        String endPointURL = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_URL, null);
        String headerValue = SynapsePropertiesLoader.getPropertyValue(headerProperty, null);
        OtlpGrpcSpanExporterBuilder otlpGrpcSpanExporterBuilder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endPointURL)
                .setCompression("gzip")
                .addHeader(headerKey, headerValue);

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME,
                TelemetryConstants.SERVICE_NAME));

        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(otlpGrpcSpanExporterBuilder.build()).build())
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
        this.handler = new SpanHandler(tracer, openTelemetry, new TracingScopeManager());
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
            if (property.startsWith(TelemetryConstants.OPENTELEMETRY_PROPERTIES)) {
                return property;
            }
        }
        return null;
    }
}

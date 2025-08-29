package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.HashMap;
import java.util.Map;

import static org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants.OTEL_RESOURCE_ATTRIBUTES_ENVIRONMENT_VARIABLE_NAME;
import static org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants.OTEL_RESOURCE_ATTRIBUTE_KEY;


public class TelemetryUtil {
    private static final Log log = LogFactory.getLog(TelemetryUtil.class);

    private TelemetryUtil() {}

    /**
     * Gets the tracer provider resource with the provided default service name.
     *
     * @param defaultServiceName    Default service name.
     * @return                      Tracer provider resource.
     */
    public static Resource getTracerProviderResource(String defaultServiceName) {
        Map<String, String> otelResourceAttributes = new HashMap<>();

        // Get resource attributes from configuration
        String otelResourceAttributeValue = SynapsePropertiesLoader.getPropertyValue(OTEL_RESOURCE_ATTRIBUTE_KEY, null);
        extractValues(otelResourceAttributeValue, otelResourceAttributes);

        /* Get resource attributes from environment variables. If a resource attribute's value has been already
        provided via configuration, the value provided via environment variable will overwrite that. */
        String environmentVariableValue = System.getenv(OTEL_RESOURCE_ATTRIBUTES_ENVIRONMENT_VARIABLE_NAME);
        extractValues(environmentVariableValue, otelResourceAttributes);

        AttributesBuilder attributesBuilder = Attributes.builder();
        for (Map.Entry<String, String> otelResourceAttribute : otelResourceAttributes.entrySet()) {
            attributesBuilder.put(otelResourceAttribute.getKey(), otelResourceAttribute.getValue());
        }
        Attributes attributes = attributesBuilder.build();

        Resource tracerProviderResource = Resource.getDefault();
        Resource serviceNameResource = Resource.create(
                Attributes.of(ResourceAttributes.SERVICE_NAME, defaultServiceName));
        tracerProviderResource = tracerProviderResource.merge(serviceNameResource);
        tracerProviderResource = tracerProviderResource.merge(Resource.create(attributes));

        return tracerProviderResource;
    }

    private static void extractValues(String valueString, Map<String, String> otelResourceAttributes) {
        if (valueString != null) {
            String[] resourceAttributes = StringUtils.split(valueString, ",");
            for (String keyValuePair : resourceAttributes) {
                String[] keyValue = StringUtils.split(keyValuePair, "=");
                if (keyValue.length != 2) {
                    log.warn(String.format("Malformed OTEL_RESOURCE_ATTRIBUTES value: '%s' This attribute will be ignored.", keyValuePair));
                } else {
                    otelResourceAttributes.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }
}

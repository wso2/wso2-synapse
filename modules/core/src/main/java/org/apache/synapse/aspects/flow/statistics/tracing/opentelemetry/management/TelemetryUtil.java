/*
 *  Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
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
        String otelResourceAttributeValue =
                SynapsePropertiesLoader.getPropertyValue(OTEL_RESOURCE_ATTRIBUTE_KEY, null);
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
                Attributes.of(ServiceAttributes.SERVICE_NAME, defaultServiceName));
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
                    log.warn(String.format("Malformed OTEL_RESOURCE_ATTRIBUTES value: '%s' " +
                            "This attribute will be ignored.", keyValuePair));
                } else {
                    otelResourceAttributes.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }

    public static boolean isAllBranchesFinished(MessageContext synCtx) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder != null) {
            return eventHolder.countHolder.getBranchCount() <= 0;
        }
        return false;

    }

    /**
     * Decrements the branch count of the flow of synCtx.
     *
     * @param synCtx Message context of the flow.
     * @deprecated Branch units are owned by MediatorWorker and the main branch is closed with
     *             {@link #closeMainBranch(MessageContext)}; calling this unbalances the count. Not called by Synapse.
     */
    @Deprecated
    public static void decrementBranchCount(MessageContext synCtx) {
        if (RuntimeStatisticCollector.shouldReportStatistic(synCtx)) {
            StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
            if (eventHolder != null) {
                eventHolder.countHolder.decrementAndGetBranchCount();
            }
        }
    }


    /**
     * Increments the branch count of the flow of synCtx.
     *
     * @param synCtx Message context of the flow.
     * @deprecated Branch units are owned by MediatorWorker and the main branch is closed with
     *             {@link #closeMainBranch(MessageContext)}; calling this unbalances the count. Not called by Synapse.
     */
    @Deprecated
    public static void incrementBranchCount(MessageContext synCtx) {
        if (RuntimeStatisticCollector.shouldReportStatistic(synCtx)) {
            StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
            if (eventHolder != null) {
                eventHolder.countHolder.incrementBranchCount();
            }
        }
    }

    /**
     * Adds a branch unit for a MediatorWorker of the flow of synCtx.
     *
     * @param synCtx Message context the worker mediates.
     * @return the holder incremented, or null if the flow's statistics are not collected or it has no event holder yet
     *         (the worker then owns no unit)
     */
    public static StatisticsReportingEventHolder startBranch(MessageContext synCtx) {
        if (synCtx == null || !RuntimeStatisticCollector.shouldReportStatistic(synCtx)) {
            return null;
        }
        Object holder = synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (!(holder instanceof StatisticsReportingEventHolder)) {
            return null;
        }
        ((StatisticsReportingEventHolder) holder).countHolder.incrementBranchCount();
        return (StatisticsReportingEventHolder) holder;
    }

    /**
     * Closes the main branch of the flow of synCtx (at most once per flow).
     *
     * @param synCtx Message context of the flow.
     */
    public static void closeMainBranch(MessageContext synCtx) {
        if (RuntimeStatisticCollector.shouldReportStatistic(synCtx)) {
            Object holder = synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
            if (holder instanceof StatisticsReportingEventHolder) {
                ((StatisticsReportingEventHolder) holder).countHolder.closeMainBranch();
            }
        }
    }

    /**
     * Ends a MediatorWorker of the flow of synCtx: runs branchEnd (release of the branch unit and the worker's statistics
     * close events) through the span handler, which may hold a lock around it, or directly when tracing is off.
     *
     * @param synCtx    Message context the worker mediated.
     * @param branchEnd End sequence of the worker.
     */
    public static void endBranch(MessageContext synCtx, Runnable branchEnd) {
        OpenTelemetrySpanHandler handler = null;
        if (synCtx != null && RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
            OpenTelemetryManager manager = OpenTelemetryManagerHolder.getOpenTelemetryManager();
            if (manager != null) {
                handler = manager.getHandler();
            }
        }
        if (handler == null) {
            branchEnd.run();
        } else {
            handler.handleBranchEnd(synCtx, branchEnd);
        }
    }

    public static boolean isOuterLayerComponent(ComponentType componentType){
        return componentType == ComponentType.TASK
                || componentType == ComponentType.MESSAGEPROCESSOR
                || componentType == ComponentType.PROXYSERVICE
                || componentType == ComponentType.API
                || componentType == ComponentType.INBOUNDENDPOINT;
    }
}

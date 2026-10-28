/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span;

import io.opentelemetry.api.OpenTelemetry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
import org.apache.synapse.config.SynapsePropertiesLoader;

/**
 * Creates the span handler selected with the {@value TelemetryConstants#OPENTELEMETRY_SPAN_HANDLER} property.
 */
public final class SpanHandlerFactory {

    private static final Log log = LogFactory.getLog(SpanHandlerFactory.class);

    private SpanHandlerFactory() {}

    /**
     * Creates the configured span handler.
     *
     * @param tracer        Tracer used to create spans.
     * @param openTelemetry OpenTelemetry instance, used for context propagation.
     * @return {@link MessageContextSpanHandler} when the property is set to
     *         {@value TelemetryConstants#SPAN_HANDLER_MESSAGE_CONTEXT}, otherwise the legacy {@link SpanHandler}.
     */
    public static OpenTelemetrySpanHandler createSpanHandler(TelemetryTracer tracer, OpenTelemetry openTelemetry) {
        String type = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER,
                TelemetryConstants.SPAN_HANDLER_LEGACY).trim();
        if (TelemetryConstants.SPAN_HANDLER_MESSAGE_CONTEXT.equalsIgnoreCase(type)) {
            // While both handlers exist, transports and connectors still call the static propagation helpers of the
            // legacy SpanHandler, so they need the OpenTelemetry instance even when the message context handler is in
            // use.
            SpanHandler.setOpenTelemetry(openTelemetry);
            return new MessageContextSpanHandler(tracer, openTelemetry);
        }
        if (!TelemetryConstants.SPAN_HANDLER_LEGACY.equalsIgnoreCase(type)) {
            log.warn("Unknown value '" + type + "' for " + TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER
                    + ". Using the " + TelemetryConstants.SPAN_HANDLER_LEGACY + " span handler");
        }
        return new SpanHandler(tracer, openTelemetry, new TracingScopeManager());
    }
}

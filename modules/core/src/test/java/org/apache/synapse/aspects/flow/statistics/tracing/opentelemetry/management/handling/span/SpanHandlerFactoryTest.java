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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import junit.framework.TestCase;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests that the {@value TelemetryConstants#OPENTELEMETRY_SPAN_HANDLER} property selects the span handler, and that
 * the static propagation helpers of both handlers work whichever handler is selected.
 */
public class SpanHandlerFactoryTest extends TestCase {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    private final OpenTelemetry openTelemetry =
            OpenTelemetry.propagating(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));

    private final TelemetryTracer tracer = new TelemetryTracer(openTelemetry.getTracer("test"));

    @Override
    protected void tearDown() throws Exception {
        cachedProperties().remove(TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER);
        SpanHandler.setOpenTelemetry(null);
        MessageContextSpanHandler.setOpenTelemetry(null);
    }

    public void testLegacyHandlerByDefault() throws Exception {
        cachedProperties().remove(TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER);
        assertTrue(SpanHandlerFactory.createSpanHandler(tracer, openTelemetry) instanceof SpanHandler);
        assertRemoteContext(SpanHandler.extract(traceparentCarrier()));
    }

    public void testMessageContextHandler() throws Exception {
        cachedProperties().setProperty(TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER, " Message_Context ");
        assertTrue(SpanHandlerFactory.createSpanHandler(tracer, openTelemetry)
                instanceof MessageContextSpanHandler);
        // Transports and connectors still call the legacy statics while both handlers exist
        assertRemoteContext(SpanHandler.extract(traceparentCarrier()));
        assertRemoteContext(MessageContextSpanHandler.extract(traceparentCarrier()));
    }

    public void testUnknownValueFallsBackToLegacyHandler() throws Exception {
        cachedProperties().setProperty(TelemetryConstants.OPENTELEMETRY_SPAN_HANDLER, "unknown");
        assertTrue(SpanHandlerFactory.createSpanHandler(tracer, openTelemetry) instanceof SpanHandler);
    }

    private static Map<String, String> traceparentCarrier() {
        Map<String, String> carrier = new HashMap<>();
        carrier.put("traceparent", "00-" + TRACE_ID + "-" + SPAN_ID + "-01");
        return carrier;
    }

    private static void assertRemoteContext(io.opentelemetry.context.Context context) {
        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        assertTrue(spanContext.isValid());
        assertTrue(spanContext.isRemote());
        assertEquals(TRACE_ID, spanContext.getTraceId());
        assertEquals(SPAN_ID, spanContext.getSpanId());
    }

    private static Properties cachedProperties() throws Exception {
        SynapsePropertiesLoader.loadSynapseProperties();
        Field field = SynapsePropertiesLoader.class.getDeclaredField("cacheProperties");
        field.setAccessible(true);
        return (Properties) field.get(null);
    }
}

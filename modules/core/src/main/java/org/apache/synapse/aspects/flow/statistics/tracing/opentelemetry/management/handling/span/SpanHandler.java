/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving.ParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScope;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScopeManager;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.ContinuationStateSequenceInfo;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.axis2.Axis2MessageContext;

/**
 * Controls Jaeger spans, with respect to various events received during Synapse message flow.
 */
public class SpanHandler implements OpenTelemetrySpanHandler {

    private Log logger = LogFactory.getLog(SpanHandler.class);
    /**
     * The tracer object, that is used to hold all the Jaeger spans.
     */
    private Tracer tracer;
    private static OpenTelemetry openTelemetry;

    /**
     * Manages tracing scopes.
     * Useful during cases like when an API is called within Proxy service.
     */
    private TracingScopeManager tracingScopeManager;

    public SpanHandler(TelemetryTracer tracer, OpenTelemetry openTelemetry, TracingScopeManager tracingScopeManager) {
        this.tracer = tracer.getTelemetryTracingTracer();
        SpanHandler.openTelemetry = openTelemetry;
        this.tracingScopeManager = tracingScopeManager;
    }

    /**
     * Inject tracer specific information to tracerSpecificCarrier.
     *
     * @param span                  Span which the span information will be injected to the tracerSpecificCarrier.
     * @param tracerSpecificCarrier Hashmap to inject the tracer and span context.
     */
    public static void inject(io.opentelemetry.api.trace.Span span, Map<String, String> tracerSpecificCarrier) {

        TextMapSetter<Map<String, String>> setter = new TextMapSetter<Map<String, String>>() {
            @Override
            public void set(Map<String, String> tracerSpecificCarrier, String key, String value) {

                if (tracerSpecificCarrier != null) {
                    tracerSpecificCarrier.put(key, value);
                }

            }
        };
        try (Scope ignored = (span).makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), tracerSpecificCarrier
                    , setter);
        }
    }

    /**
     * Extract tracer specific information from tracerSpecificCarrier and return the extracted context.
     *
     * @param tracerSpecificCarrier Hashmap to extract the tracer and span context.
     * @return extracted context.
     */
    public static Context extract(Map<String, String> tracerSpecificCarrier) {

        TextMapGetter<Map<String, String>> getter =
                new TextMapGetter<Map<String, String>>() {
                    public String get(Map<String, String> tracerSpecificCarrier, String key) {

                        if (tracerSpecificCarrier != null) {
                            return tracerSpecificCarrier.get(key);
                        }
                        return null;
                    }

                    public Iterable<String> keys(Map<String, String> tracerSpecificCarrier) {

                        return tracerSpecificCarrier.keySet();
                    }
                };

        return openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), tracerSpecificCarrier, getter);
    }

    @Override
    public void handleOpenEntryEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenEntryEvent(StatisticDataUnit statisticDataUnit,
                                     org.apache.axis2.context.MessageContext msgCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(msgCtx);
        synchronized (tracingScope.getSpanStore()) {
            startSpan(statisticDataUnit, msgCtx, tracingScope.getSpanStore());
        }
    }

    @Override
    public void handleOpenChildEntryEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowContinuableEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowSplittingEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowAggregateEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    /**
     * Starts a span during a statistic data unit collection.
     * When the statistic data unit collection is for a sequence whose state can be stacked by the
     * ContinuationStackManager, the statistic data unit will be buffered until a stack modification event occurs
     * upon which, the span can be started.
     * @param statisticDataUnit Reported statistic data unit object.
     * @param synCtx            Message context.
     */
    private void startSpanOrBufferSequenceContinuationState(StatisticDataUnit statisticDataUnit,
                                                            MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isContinuationStateApplicable(statisticDataUnit)) {
                startSpan(statisticDataUnit, synCtx, tracingScope.getSpanStore());
            } else {
                // Will begin during addSeqContinuationState
                bufferSequenceContinuationState(statisticDataUnit, tracingScope.getSpanStore());
            }
        }
    }

    /**
     * Returns whether the given statistic data unit represents a sequence,
     * where continuation state stack management is applicable.
     * This is used to buffer open events until continuation state stack insertion events occur.
     * @param statisticDataUnit Statistic unit object.
     * @return                  Whether continuation state stack is applicable for the statistic data unit.
     */
    private boolean isContinuationStateApplicable(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType() == ComponentType.SEQUENCE &&
                (SequenceType.PROXY_INSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.PROXY_OUTSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.API_INSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.API_OUTSEQ.toString().equals(statisticDataUnit.getComponentName()));
    }

    /**
     * Starts a span, and stores necessary information in the span store to retrieve them back when needed.
     * @param statisticDataUnit Statistic data unit object, which was collected during a statistic event.
     * @param synCtx            Message context.
     * @param spanStore         Span store object.
     */
    private void startSpan(StatisticDataUnit statisticDataUnit, MessageContext synCtx, SpanStore spanStore) {
        SpanWrapper parentSpanWrapper = ParentResolver.resolveParent(statisticDataUnit, spanStore, synCtx);
        Span parentSpan = null;
        Context context = null;
        if (parentSpanWrapper != null) {
            parentSpan = parentSpanWrapper.getSpan();
        }
        Span span;
        Map<String, String> tracerSpecificCarrier = new HashMap<>();

        Map headersMap;
        Object headers = ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers instanceof Map) {
            headersMap = new ConcurrentHashMap<>((Map) headers);
        } else {
            // We only need to extract span context from headers when there are trp headers available
            headersMap = new ConcurrentHashMap();
        }
        Object statusCode = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("HTTP_SC");
        Object statusDescription = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("HTTP_DESC");
        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            // Extract span context from headers
            context = extract(headersMap);
        } else if (parentSpan != null) {
            context = Context.current().with(parentSpan);
        } else {
            context = Context.current();
        }
        span = tracer.spanBuilder(statisticDataUnit.getComponentName()).setParent(context).startSpan();

        // Set tracing headers
        inject(span, tracerSpecificCarrier);
        synCtx.setProperty(SynapseConstants.JAEGER_TRACE_ID, span.getSpanContext().getTraceId());
        synCtx.setProperty(SynapseConstants.JAEGER_SPAN_ID, span.getSpanContext().getSpanId());
        if (logger.isDebugEnabled()) {
            logger.debug("Jaeger Trace ID: " + synCtx.getProperty(SynapseConstants.JAEGER_TRACE_ID) + " Jaeger Span ID: " + synCtx.getProperty(SynapseConstants.JAEGER_SPAN_ID));
        }

        // Set text map key value pairs as HTTP headers
        // Fix possible null pointer issue which can occur when following property is used
        // <property name="TRANSPORT_HEADERS" action="remove" scope="axis2"/>
        if (headersMap != null) {
            headersMap.putAll(tracerSpecificCarrier);
            statisticDataUnit.setTransportHeaderMap(headersMap);
            ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                    .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

            // Set custom span header tags
            Object prevCustomSpanTagsObj = synCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS);
            String customTagsString =
                    SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, null);
            boolean isCustomSpanTagsEnabled = customTagsString != null && !customTagsString.isEmpty();
            if (isCustomSpanTagsEnabled) {
                if (prevCustomSpanTagsObj != null) {
                    Map<String, Object> customTagsMap = (Map<String, Object>) prevCustomSpanTagsObj;
                    if (!customTagsMap.isEmpty()) {
                        statisticDataUnit.setCustomProperties(customTagsMap);
                    }
                } else {
                    String[] customTags = customTagsString.split(",");
                    Map<String, Object> customTagsMap = new HashMap<>();
                    for (String tag : customTags) {
                        if (headersMap.containsKey(tag.trim())) {
                            customTagsMap.put(tag, headersMap.get(tag));
                        }
                    }
                    if (!customTagsMap.isEmpty()) {
                        synCtx.setProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, customTagsMap);
                        statisticDataUnit.setCustomProperties(customTagsMap);
                    }
                }
            }
        }

        if (statusCode != null) {
            statisticDataUnit.setStatusCode(statusCode.toString());
        }
        if (statusDescription != null) {
            statisticDataUnit.setStatusDescription(statusDescription.toString());
        }
        if (statisticDataUnit.getComponentType() != null
                & statisticDataUnit.getComponentType() == ComponentType.ENDPOINT) {
            statisticDataUnit.setEndpoint(synCtx.getEndpoint(statisticDataUnit.getComponentName()));
        }

        String spanId = TracingUtils.extractId(statisticDataUnit);
        SpanWrapper spanWrapper = spanStore.addSpanWrapper(spanId, span, statisticDataUnit, parentSpanWrapper, synCtx);

        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }
    }

    /**
     * Starts a span, and stores necessary information in the span store to retrieve them back when needed.
     *
     * @param statisticDataUnit Statistic data unit object, which was collected during a statistic event.
     * @param msgCtx            Axis2 Message context.
     * @param spanStore         Span store object.
     */
    private void startSpan(StatisticDataUnit statisticDataUnit, org.apache.axis2.context.MessageContext msgCtx,
                           SpanStore spanStore) {
        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
        SpanWrapper parentSpanWrapper = spanStore.getSpanWrapper(parentId);
        Span parentSpan = null;
        Context context = null;
        if (parentSpanWrapper != null) {
            parentSpan = parentSpanWrapper.getSpan();
        }
        Span span;
        Map<String, String> tracerSpecificCarrier = new HashMap<>();

        Map headersMap;
        Object headers = msgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers instanceof Map) {
            headersMap = new ConcurrentHashMap<>((Map) headers);
        } else {
            // We only need to extract span context from headers when there are trp headers available
            headersMap = new ConcurrentHashMap();
        }
        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            context = extract(headersMap);
        } else if (parentSpan != null) {
            context = Context.current().with(parentSpan);
        } else {
            context = Context.current();
        }
        span = tracer.spanBuilder(statisticDataUnit.getComponentName()).setParent(context).startSpan();

        // Set tracing headers
        inject(span, tracerSpecificCarrier);
        msgCtx.setProperty(SynapseConstants.JAEGER_TRACE_ID, span.getSpanContext().getTraceId());
        msgCtx.setProperty(SynapseConstants.JAEGER_SPAN_ID, span.getSpanContext().getSpanId());
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Jaeger Trace ID: " + msgCtx.getProperty(SynapseConstants.JAEGER_TRACE_ID) + " Jaeger Span ID: " +
                            msgCtx.getProperty(SynapseConstants.JAEGER_SPAN_ID));
        }

        // Set text map key value pairs as HTTP headers
        // Fix possible null pointer issue which can occur when following property is used
        // <property name="TRANSPORT_HEADERS" action="remove" scope="axis2"/>
        if (headersMap != null) {
            headersMap.putAll(tracerSpecificCarrier);
            msgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

            Object prevCustomSpanTagsObj = msgCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS);
            String customTagsString =
                    SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, null);
            boolean isCustomSpanTagsEnabled = customTagsString != null && !customTagsString.isEmpty();
            if (isCustomSpanTagsEnabled) {
                if (prevCustomSpanTagsObj != null) {
                    Map<String, Object> customTagsMap = (Map<String, Object>) prevCustomSpanTagsObj;
                    if (!customTagsMap.isEmpty()) {
                        statisticDataUnit.setCustomProperties(customTagsMap);
                    }
                } else {
                    String[] customTags = customTagsString.split(",");
                    Map<String, Object> customTagsMap = new HashMap<>();
                    for (String tag : customTags) {
                        if (headersMap.containsKey(tag.trim())) {
                            customTagsMap.put(tag, headersMap.get(tag));
                        }
                    }
                    if (!customTagsMap.isEmpty()) {
                        msgCtx.setProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, customTagsMap);
                        statisticDataUnit.setCustomProperties(customTagsMap);
                    }
                }
            }
        }

        statisticDataUnit.setTransportHeaderMap(headersMap);

        String spanId = TracingUtils.extractId(statisticDataUnit);
        SpanWrapper spanWrapper = spanStore.addSpanWrapper(spanId, span, statisticDataUnit, parentSpanWrapper, msgCtx);

        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }
    }

    /**
     * Buffers the given statistic data unit which is reported by an open event,
     * until an appropriate continuation stack event is reported.
     * A continuation event does not have the information about statistic data units to start and stop spans,
     * and that information can only be obtained from this buffered open event.
     * @param statisticDataUnit Statistic data unit object.
     * @param spanStore         Span store object.
     */
    private void bufferSequenceContinuationState(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        ContinuationStateSequenceInfo continuationStateSequenceInfo =
                new ContinuationStateSequenceInfo(statisticDataUnit);
        spanStore.addContinuationStateSequenceInfo(continuationStateSequenceInfo);
    }

    /**
     * Returns whether the given statistic data unit belongs to a component, which represents an outer level span.
     * An outer level span is the super parent span for an entire tracing scope.
     * The provided span store should not already have an assigned outer level span, in order to check by type.
     * @param statisticDataUnit Statistic data unit object.
     * @param spanStore         Span store object.
     * @return                  Whether the given statistic data unit denotes an outer level span.
     */
    private boolean isOuterLevelSpan(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        return spanStore.getOuterLevelSpanWrapper() == null
                && (statisticDataUnit.getComponentType() == ComponentType.TASK ||
                statisticDataUnit.getComponentType() == ComponentType.PROXYSERVICE
                || statisticDataUnit.getComponentType() == ComponentType.API
                || statisticDataUnit.getComponentType() == ComponentType.INBOUNDENDPOINT
                || statisticDataUnit.isOuterLayerSpan()
                || (statisticDataUnit.getComponentType() == ComponentType.SEQUENCE
                && SynapseConstants.MAIN_SEQUENCE_KEY.equals(statisticDataUnit.getComponentName())));
    }

    @Override
    public void handleOpenFlowAsynchronousEvent(BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    @Override
    public void handleOpenContinuationEvents(BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handleCloseEvent(basicStatisticDataUnit, synCtx, false);
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit,
                                      org.apache.axis2.context.MessageContext msgCtx) {
        handleCloseEvent(basicStatisticDataUnit, msgCtx, false);
    }


    @Override
    public void handleCloseEntryWithErrorEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handleCloseEvent(basicStatisticDataUnit, synCtx, true);
    }

    @Override
    public void handleCloseEntryWithErrorEvent(BasicStatisticDataUnit basicStatisticDataUnit,
                                               org.apache.axis2.context.MessageContext msgCtx) {
        handleCloseEvent(basicStatisticDataUnit, msgCtx, true);
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        SpanStore spanStore = tracingScope.getSpanStore();
        String spanWrapperId = TracingUtils.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);

        // finish the current span
        handleCloseEvent(basicStatisticDataUnit, synCtx, false);

        // finish outer level spans since the control is not returned to the original flow to close them gracefully.
        while (spanWrapper != null && spanWrapper.getParentSpanWrapper() != null) {
            spanWrapper = spanWrapper.getParentSpanWrapper();
            spanStore.finishSpan(spanWrapper, synCtx);
        }
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit,
                                          org.apache.axis2.context.MessageContext msgCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(msgCtx);
        SpanStore spanStore = tracingScope.getSpanStore();
        String spanWrapperId = TracingUtils.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);

        // finish the current span
        handleCloseEvent(basicStatisticDataUnit, msgCtx, true);

        // finish outer level spans since the control is not returned to the original flow to close them gracefully.
        while (spanWrapper != null && spanWrapper.getParentSpanWrapper() != null) {
            spanWrapper = spanWrapper.getParentSpanWrapper();
            spanStore.finishSpan(spanWrapper, msgCtx, true);
        }
    }

    private void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx, boolean isError) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isBufferedForContinuationState(basicStatisticDataUnit, tracingScope.getSpanStore())) {
                finishSpan(basicStatisticDataUnit, synCtx, tracingScope.getSpanStore(), tracingScope, isError);
            }
            // Else: Absorb. Will end during pop from stack
        }
    }

    private void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit,
                                  org.apache.axis2.context.MessageContext msgCtx, boolean isError) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(msgCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (!isBufferedForContinuationState(basicStatisticDataUnit, tracingScope.getSpanStore())) {
                finishSpan(basicStatisticDataUnit, msgCtx, tracingScope.getSpanStore(), tracingScope, isError);
            }
        }
    }

    /**
     * Returns whether the given basic statistic data unit has been buffered to consider the continuation state.
     * In such cases, This will be used to absorb and skip the close event.
     * @param basicStatisticDataUnit    Basic statistic unit object.
     * @param spanStore                 Span store object.
     * @return                          Whether the given basic statistic data unit has been buffered to consider the
     *                                  continuation state.
     */
    private boolean isBufferedForContinuationState(BasicStatisticDataUnit basicStatisticDataUnit, SpanStore spanStore) {
        return spanStore.hasContinuationStateSequenceInfoWithId(TracingUtils.extractId(basicStatisticDataUnit));
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    /**
     * Ends a span which is related to the provided basic statistic data unit, and performs necessary updates or
     * removals in the provided span store.
     * @param basicStatisticDataUnit    Basic statistic data unit object, which was collected during a statistic event.
     * @param synCtx                    Message context.
     * @param spanStore                 Span store object.
     * @param tracingScope              The tracing scope of the appropriate span.
     */
    private void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit,
                            MessageContext synCtx,
                            SpanStore spanStore,
                            TracingScope tracingScope) {
        finishSpan(basicStatisticDataUnit, synCtx, spanStore, tracingScope, false);
    }

    private void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit,
                            MessageContext synCtx,
                            SpanStore spanStore,
                            TracingScope tracingScope, boolean isError) {
        String spanWrapperId = TracingUtils.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);
        //Set the statistic data unit of the close event into the span wrapper
        if (spanWrapper != null && (basicStatisticDataUnit instanceof StatisticDataUnit)) {
            spanWrapper.setCloseEventStatisticDataUnit((StatisticDataUnit) basicStatisticDataUnit);
        }
        if (!Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
            // A non-outer level span
            spanStore.finishSpan(spanWrapper, synCtx, isError);
        } else {
            // An outer level span
            if (tracingScope.isEventCollectionFinished(synCtx)) {
                cleanupContinuationStateSequences(spanStore, synCtx);
                spanStore.finishSpan(spanWrapper, synCtx, isError);
                tracingScopeManager.cleanupTracingScope(tracingScope.getTracingScopeId());
            }
            // Else - Absorb. Will be handled when all the callbacks are completed
        }
    }

    private void finishSpan(BasicStatisticDataUnit basicStatisticDataUnit,
                            org.apache.axis2.context.MessageContext msgCtx,
                            SpanStore spanStore,
                            TracingScope tracingScope, boolean isError) {
        String spanWrapperId = TracingUtils.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);
        //Set the statistic data unit of the close event into the span wrapper
        if (spanWrapper != null && (basicStatisticDataUnit instanceof StatisticDataUnit)) {
            spanWrapper.setCloseEventStatisticDataUnit((StatisticDataUnit) basicStatisticDataUnit);
        }
        if (!Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
            spanStore.finishSpan(spanWrapper, msgCtx, isError);
        } else {
            spanStore.finishSpan(spanWrapper, msgCtx, isError);
            tracingScopeManager.cleanupTracingScope(tracingScope.getTracingScopeId());

        }
    }

    /**
     * Cleans up remaining unfinished continuation state sequences before ending the outer level span.
     * @param spanStore Span store object.
     * @param synCtx Synapse message context
     */
    private void cleanupContinuationStateSequences(SpanStore spanStore, MessageContext synCtx) {
        if (!spanStore.getContinuationStateSequenceInfos().isEmpty()) {
            List<ContinuationStateSequenceInfo> continuationStateSequences =
                    spanStore.getContinuationStateSequenceInfos();
            for (ContinuationStateSequenceInfo continuationStateSequence : continuationStateSequences) {
                finishSpanForContinuationStateSequence(continuationStateSequence, spanStore, synCtx);
            }
            continuationStateSequences.clear();
        }
    }

    /**
     * Finishes a span, which has been added as a continuation state sequence.
     * @param continuationStateSequenceInfo Object that contains information about the continuation state sequence.
     * @param spanStore             Span store object.
     */
    private void finishSpanForContinuationStateSequence(ContinuationStateSequenceInfo continuationStateSequenceInfo,
                                                        SpanStore spanStore, MessageContext synCtx) {
        String spanWrapperId = continuationStateSequenceInfo.getSpanReferenceId();
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);
        spanStore.finishSpan(spanWrapper, synCtx);
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(messageContext);
        tracingScope.addCallback();
    }

    @Override
    public void handleCallbackCompletionEvent(MessageContext oldMessageContext, String callbackId) {
        handleCallbackFinishEvent(oldMessageContext);
    }

    @Override
    public void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId) {
        // Absorb. Callback handling completion will be reported after handling the specific message.
    }

    @Override
    public void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {
        handleCallbackFinishEvent(synapseOutMsgCtx);
    }

    private void handleCallbackFinishEvent(MessageContext messageContext) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(messageContext);
        tracingScope.removeCallback();
        // The last callback received in a scope will finish the outer level span
        if (tracingScope.isEventCollectionFinished(messageContext)) {
            synchronized (tracingScope.getSpanStore()) {
                cleanupContinuationStateSequences(tracingScope.getSpanStore(), messageContext);
                SpanWrapper outerLevelSpanWrapper = tracingScope.getSpanStore().getOuterLevelSpanWrapper();
                tracingScope.getSpanStore().finishSpan(outerLevelSpanWrapper, messageContext);
                tracingScopeManager.cleanupTracingScope(tracingScope.getTracingScopeId());
            }
        }
    }

    public void handleScatterGatherFinishEvent(MessageContext messageContext) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(messageContext);
        synchronized (tracingScope.getSpanStore()) {
            cleanupContinuationStateSequences(tracingScope.getSpanStore(), messageContext);
            cleanUpActiveSpans(tracingScope.getSpanStore(), messageContext);
            SpanWrapper outerLevelSpanWrapper = tracingScope.getSpanStore().getOuterLevelSpanWrapper();
            tracingScope.getSpanStore().finishSpan(outerLevelSpanWrapper, messageContext);
            tracingScopeManager.cleanupTracingScope(tracingScope.getTracingScopeId());
        }
    }

    private void cleanUpActiveSpans(SpanStore spanStore, MessageContext messageContext) {
        List<SpanWrapper> activeSpanWrappers = spanStore.getActiveSpanWrappers();
        for (int i = activeSpanWrappers.size() - 1; i > 0; i--) {
            SpanWrapper spanWrapper = activeSpanWrappers.get(i);
            spanStore.finishSpan(spanWrapper, messageContext);
        }
    }

    @Override
    public void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            ContinuationStateSequenceInfo continuationStateSequenceInfo =
                    findContinuationStateSequenceInfo(seqType, tracingScope.getSpanStore(),false);
            if (continuationStateSequenceInfo != null) {
                StatisticDataUnit statisticDataUnit = continuationStateSequenceInfo.getStatisticDataUnit();
                continuationStateSequenceInfo.setSpanActive(true);
                startSpan(statisticDataUnit, synCtx, tracingScope.getSpanStore());
            }
        }
    }

    @Override
    public void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            if (continuationState instanceof SeqContinuationState) { // No other type will be kept track of
                ContinuationStateSequenceInfo continuationStateSequenceInfo =
                        findContinuationStateSequenceInfo(
                                ((SeqContinuationState)continuationState).getSeqType(),
                                tracingScope.getSpanStore(),
                                true);
                if (continuationStateSequenceInfo != null) {
                    continuationStateSequenceInfo.setSpanActive(false);
                    finishSpanForContinuationStateSequence(continuationStateSequenceInfo, tracingScope.getSpanStore(),
                            synCtx);
                    tracingScope.getSpanStore().getContinuationStateSequenceInfos()
                            .remove(continuationStateSequenceInfo);
                }
            }
        }
    }

    /**
     * Finds the appropriate continuation state sequence which contains the statistic data unit information - from
     * the buffer, when a continuation state stack event has been reported.
     * This method does the correlation between a statistic data unit and a continuation state stack event, in order to
     * start or end a span.
     * @param seqType                   Type of the sequence.
     * @param spanStore                 Span store object.
     * @param desiredSpanActiveState    Whether the span related to this continuation state sequence
     *                                  should be already active or not.
     *
     *                                  False: Set during continuation state stack insertion, which denotes
     *                                  "Find the next span that has not been started yet".
     *
     *                                  True: Set during continuation state stack removal, which denotes
     *                                  "Find the next span that is currently active".
     *
     *                                  This flag is helpful in cases where multiple copies (denoting reliant states)
     *                                  of the same sequence has to be referred correctly, in scenarios like Iterate
     *                                  mediator.
     * @return                          The found continuation state sequence info. Null when not found.
     */
    private ContinuationStateSequenceInfo findContinuationStateSequenceInfo(SequenceType seqType,
                                                                            SpanStore spanStore,
                                                                            boolean desiredSpanActiveState) {
        for (ContinuationStateSequenceInfo continuationStateSequenceInfo :
                spanStore.getContinuationStateSequenceInfos()) {
            if (seqType.toString().equals(continuationStateSequenceInfo.getStatisticDataUnit().getComponentName()) &&
                    (continuationStateSequenceInfo.isSpanActive() == desiredSpanActiveState)) {
                return continuationStateSequenceInfo;
            }
        }
        return null;
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        synchronized (tracingScope.getSpanStore()) {
            List<ContinuationStateSequenceInfo> stackedSequences =
                    tracingScope.getSpanStore().getContinuationStateSequenceInfos();
            for (ContinuationStateSequenceInfo stackedSequence : stackedSequences) {
                finishSpanForContinuationStateSequence(stackedSequence, tracingScope.getSpanStore(), synCtx);
            }
            stackedSequences.clear();
        }
    }
}

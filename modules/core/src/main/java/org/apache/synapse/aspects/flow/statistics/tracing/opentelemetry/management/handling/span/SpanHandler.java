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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryUtil;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving.LatestActiveParentResolver;
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
     * Writes trace context entries into a plain map carrier.
     */
    private static final TextMapSetter<Map<String, String>> MAP_SETTER = (carrier, key, value) -> {
        if (carrier != null) {
            carrier.put(key, value);
        }
    };

    /**
     * Reads trace context entries from a plain map carrier.
     */
    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<Map<String, String>>() {
        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier != null ? carrier.get(key) : null;
        }

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier != null ? carrier.keySet() : Collections.emptySet();
        }
    };

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
    public static void inject(Span span, Map<String, String> tracerSpecificCarrier) {
        inject(span, tracerSpecificCarrier, MAP_SETTER);
    }

    /**
     * Injects the context of the given span into an arbitrary carrier, using the carrier's own setter.
     * Lets transports that do not carry HTTP style headers - such as the Solace JCSMP client - propagate
     * trace context in their native message representation.
     *
     * @param span    Span whose context is injected into the carrier.
     * @param carrier Carrier the trace context is written to.
     * @param setter  Setter that knows how to write a key value pair into the carrier.
     * @param <T>     Type of the carrier.
     */
    public static <T> void inject(Span span, T carrier, TextMapSetter<T> setter) {
        if (openTelemetry == null) {
            return;
        }
        try (Scope ignored = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), carrier, setter);
        }
    }

    /**
     * Extract tracer specific information from tracerSpecificCarrier and return the extracted context.
     *
     * @param tracerSpecificCarrier Hashmap to extract the tracer and span context.
     * @return extracted context.
     */
    public static Context extract(Map<String, String> tracerSpecificCarrier) {
        return extract(tracerSpecificCarrier, MAP_GETTER);
    }

    /**
     * Extracts trace context from an arbitrary carrier, using the carrier's own getter.
     * Counterpart of {@link #inject(Span, Object, TextMapSetter)} for transports with a native
     * message representation.
     *
     * @param carrier Carrier to extract the tracer and span context from.
     * @param getter  Getter that knows how to read a key from the carrier.
     * @param <T>     Type of the carrier.
     * @return Extracted context, or the current context when tracing is not initialized.
     */
    public static <T> Context extract(T carrier, TextMapGetter<T> getter) {
        if (openTelemetry == null) {
            return Context.current();
        }
        return openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), carrier, getter);
    }

    @Override
    public SpanStore getSpanStore(MessageContext messageContext) {
        return tracingScopeManager.getSpanStore(messageContext);
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
                if (synCtx.isContinuationEnabled()) {
                    bufferSequenceContinuationState(statisticDataUnit, tracingScope.getSpanStore());
                }else {
                    startSpan(statisticDataUnit, synCtx, tracingScope.getSpanStore());
                }
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

        Map headersMap = (Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headersMap == null) {
            headersMap = new TreeMap<String, String>(String::compareToIgnoreCase);
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
        ThreadContext.put(SynapseConstants.TRACE_ID, span.getSpanContext().getTraceId());
        ThreadContext.put(SynapseConstants.SPAN_ID, span.getSpanContext().getSpanId());

        if (logger.isDebugEnabled()) {
            logger.debug("Jaeger Trace ID: " + synCtx.getProperty(SynapseConstants.JAEGER_TRACE_ID) + " Jaeger Span ID: " + synCtx.getProperty(SynapseConstants.JAEGER_SPAN_ID));
        }

        headersMap.putAll(tracerSpecificCarrier);
        statisticDataUnit.setTransportHeaderMap(getSafeTransportHeaders(headersMap));
        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
            .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

        // Set custom span header tags
        Object prevCustomSpanTagsObj = synCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS);
        String customTagsString =
            SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS,
                null);
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

        Map headersMap = (Map) msgCtx.getProperty(
            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headersMap == null) {
            headersMap = new TreeMap<String, String>(String::compareToIgnoreCase);
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
        ThreadContext.put(SynapseConstants.TRACE_ID, span.getSpanContext().getTraceId());
        ThreadContext.put(SynapseConstants.SPAN_ID, span.getSpanContext().getSpanId());

        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Jaeger Trace ID: " + msgCtx.getProperty(SynapseConstants.JAEGER_TRACE_ID) + " Jaeger Span ID: " +
                            msgCtx.getProperty(SynapseConstants.JAEGER_SPAN_ID));
        }

        headersMap.putAll(tracerSpecificCarrier);
        msgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

        Object prevCustomSpanTagsObj = msgCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS);
        String customTagsString =
            SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS,
                null);
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

        statisticDataUnit.setTransportHeaderMap(getSafeTransportHeaders(headersMap));

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
                && (TelemetryUtil.isOuterLayerComponent(statisticDataUnit.getComponentType()) || statisticDataUnit.isOuterLayerSpan()
                || (statisticDataUnit.getComponentType() == ComponentType.SEQUENCE
                && SynapseConstants.MAIN_SEQUENCE_KEY.equals(statisticDataUnit.getComponentName())));
    }

    /**
     * Creates a copy of the given transport headers map for the statistic data unit, leaving out
     * entries with a null key or value.
     * <p>
     * A copy is needed because the map is reported when the span is finished, by which time the live
     * header map has changed further. Null entries are left out because transport headers legitimately
     * carry them, while a ConcurrentHashMap rejects them with a NullPointerException.
     *
     * @param headersMap Live transport headers map. Must not be null.
     * @return           Copy of the given map, without its null keys and values.
     */
    private static Map<Object, Object> getSafeTransportHeaders(Map<?, ?> headersMap) {
        Map<Object, Object> safeHeaders = new ConcurrentHashMap<>();
        headersMap.forEach((key, value) -> {
            if (key != null && value != null) {
                safeHeaders.put(key, value);
            }
        });
        return safeHeaders;
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
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx, boolean error) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(synCtx);
        SpanStore spanStore = tracingScope.getSpanStore();
        String spanWrapperId = TracingUtils.extractId(basicStatisticDataUnit);
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(spanWrapperId);

        if (!error) {
            if (TelemetryUtil.isAllBranchesFinished(synCtx)) {
                handleCloneFinishEvent(synCtx, spanWrapper);
            }
            // if it's not an error we shouldn't close parent span wrappers
            return;
        }
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

    private void handleCloneFinishEvent(MessageContext messageContext, SpanWrapper spanWrapper) {
        TracingScope tracingScope = tracingScopeManager.getTracingScope(messageContext);
        tracingScope.getSpanStore().finishSpan(spanWrapper, messageContext);
        if (tracingScope.isEventCollectionFinished(messageContext)) {
            synchronized (tracingScope.getSpanStore()) {
                cleanupContinuationStateSequences(tracingScope.getSpanStore(), messageContext);
                cleanUpActiveSpans(tracingScope.getSpanStore(), messageContext);
                SpanWrapper outerLevelSpanWrapper = tracingScope.getSpanStore().getOuterLevelSpanWrapper();
                if (outerLevelSpanWrapper != null && !outerLevelSpanWrapper.equals(spanWrapper)) {
                    tracingScope.getSpanStore().finishSpan(outerLevelSpanWrapper, messageContext);
                }
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

    /**
     * Extracts the trace context carried by an inbound transport message and writes it into the
     * transport headers of the Synapse message context that was created for it.
     * <p>
     * The entry span of a message flow is parented off the trace context found in
     * {@code TRANSPORT_HEADERS} (see {@link #startSpan}). Transports that carry trace context in
     * their own message representation rather than in HTTP style headers - such as Solace JCSMP -
     * therefore have to restate it there; otherwise the flow starts a brand new trace instead of
     * continuing the producer's one.
     * <p>
     * No-op when tracing is disabled, or when the message context is not backed by Axis2.
     *
     * @param message       Transport message to read the trace context from.
     * @param synCtx        Synapse message context created for that message.
     * @param textMapGetter Getter that knows how to read a key from the transport message.
     * @param <T>           Type of the transport message.
     */
    public static <T> void extractTraceContextAndInjectToMessageContext(T message, MessageContext synCtx,
                                                                        TextMapGetter<T> textMapGetter) {
        if (!RuntimeStatisticCollector.isOpenTelemetryEnabled() || openTelemetry == null
                || !(synCtx instanceof Axis2MessageContext)) {
            return;
        }
        Context extractedContext = extract(message, textMapGetter);
        Map<String, String> carrier = new HashMap<>();
        openTelemetry.getPropagators().getTextMapPropagator().inject(extractedContext, carrier, MAP_SETTER);
        if (carrier.isEmpty()) {
            // The message carried no trace context - leave the headers untouched so the flow
            // starts its own trace rather than inheriting a stale parent.
            return;
        }
        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Map headersMap = (Map) axis2MsgCtx
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headersMap == null) {
            headersMap = new TreeMap<String, String>(String::compareToIgnoreCase);
        }
        headersMap.putAll(carrier);
        axis2MsgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
    }

    /**
     * Injects the trace context of the latest active span of the given message flow into an
     * outbound transport message, so that the remote consumer can continue the same trace.
     * <p>
     * No-op when tracing is disabled or when the flow has no active span.
     *
     * @param synCtx  Synapse message context of the flow performing the send.
     * @param message Outbound transport message to write the trace context into.
     * @param setter  Setter that knows how to write a key value pair into the transport message.
     * @param <T>     Type of the transport message.
     */
    public static <T> void injectTraceContext(MessageContext synCtx, T message, TextMapSetter<T> setter) {
        if (!RuntimeStatisticCollector.isOpenTelemetryEnabled() || synCtx == null) {
            return;
        }
        SpanWrapper spanWrapper = LatestActiveParentResolver.resolveParent(synCtx);
        if (spanWrapper == null || spanWrapper.getSpan() == null) {
            return;
        }
        inject(spanWrapper.getSpan(), message, setter);
    }
}

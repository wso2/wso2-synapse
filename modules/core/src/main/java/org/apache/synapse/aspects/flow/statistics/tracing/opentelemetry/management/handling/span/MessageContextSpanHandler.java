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
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
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
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryUtil;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers.TracingUtils;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving.LatestActiveParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.parentresolving.ParentResolver;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.MessageContextTracingScope;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.ContinuationStateSequenceInfo;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.stores.SpanStore;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Span handler that keeps the tracing scope of a message flow on the message context, instead of in a map held by
 * the handler.
 * <p>
 * The scope ({@link MessageContextTracingScope}) is stored in the {@value TelemetryConstants#TRACING_SCOPE_PROPERTY}
 * property. Cloned, iterated and callback response message contexts copy that property by reference, so all of them
 * share the scope of the flow, and the scope is garbage collected together with the last of them. Because nothing
 * else refers to a scope, a flow whose outer level span never ends cannot leave its spans behind.
 * <p>
 * Spans are started, parented and tagged in the same way as in the legacy span handler, so the traces have the same
 * shape. What differs is when the outer level span ends. The legacy handler ends it when the statistics event
 * collection of the flow is finished, which never happens in some flows (for example after an aggregation, which
 * leaves {@link StatisticsConstants#CONTINUE_STATISTICS_FLOW} set). This handler ends it when the flow has no work
 * left (see {@link MessageContextTracingScope#isFlowCompleted()}), evaluated under the span store lock: the outer
 * level component has closed, the flow has no pending callback, and no MediatorWorker of the flow is running (the
 * branch count without the main branch). The counts come from the statistics event holder
 * ({@link StatisticsConstants#STAT_COLLECTOR_PROPERTY}) captured when the scope was created; the scope keeps no
 * counters of its own. A flow without an event holder ends at the close of its outer level component.
 * <p>
 * A MediatorWorker ends through {@link #handleBranchEnd(MessageContext, Runnable)}: the release of its branch unit
 * and its statistics close events run while this handler holds the span store lock, and the end of the flow is
 * evaluated in the same critical section. So no thread sees the unit released before the spans of the branch are
 * finished.
 * <p>
 * An outer level component whose flow has another statistics event holder than the current scope (a message
 * processor dispatching a stored message while the producer's flow is still open) starts its own scope; clones,
 * task branches and response contexts share the holder, so they share the scope.
 */
public class MessageContextSpanHandler implements OpenTelemetrySpanHandler {

    private static final Log logger = LogFactory.getLog(MessageContextSpanHandler.class);

    private final Tracer tracer;

    private static OpenTelemetry openTelemetry;

    public MessageContextSpanHandler(TelemetryTracer tracer, OpenTelemetry openTelemetry) {
        this.tracer = tracer.getTelemetryTracingTracer();
        MessageContextSpanHandler.openTelemetry = openTelemetry;
    }

    /**
     * Sets the OpenTelemetry instance used by the static propagation helpers of this class.
     *
     * @param openTelemetry OpenTelemetry instance.
     */
    static void setOpenTelemetry(OpenTelemetry openTelemetry) {
        MessageContextSpanHandler.openTelemetry = openTelemetry;
    }

    // ----- Context propagation (public API used by transports and connectors) -----

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

    // ----- Tracing scope on the message context -----

    private static MessageContextTracingScope asScope(Object property) {
        return property instanceof MessageContextTracingScope ? (MessageContextTracingScope) property : null;
    }

    private static StatisticsReportingEventHolder asEventHolder(Object property) {
        return property instanceof StatisticsReportingEventHolder ? (StatisticsReportingEventHolder) property : null;
    }

    /**
     * Whether a span of the given component needs a new tracing scope: the message context has none, or the
     * component starts a new flow (an outer level component) and the current scope has already finished or belongs to
     * a flow with another statistics event holder (for example a message processor that dispatches a stored message
     * while the producer's flow is still open).
     */
    private static boolean needsNewScope(MessageContextTracingScope scope, StatisticDataUnit statisticDataUnit,
                                         StatisticsReportingEventHolder eventHolder) {
        return scope == null || (isOuterLevelComponent(statisticDataUnit)
                && (scope.isFinished() || (scope.getEventHolder() != null && eventHolder != null
                && scope.getEventHolder() != eventHolder)));
    }

    /**
     * Returns the tracing scope to start a span of the given component in, creating it when needed (see
     * {@link #needsNewScope}). The scope captures the statistics event holder of the flow.
     */
    private static MessageContextTracingScope getOrCreateTracingScope(StatisticDataUnit statisticDataUnit,
                                                                      MessageContext synCtx) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        StatisticsReportingEventHolder eventHolder =
                asEventHolder(synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY));
        if (needsNewScope(scope, statisticDataUnit, eventHolder)) {
            scope = new MessageContextTracingScope(
                    (String) synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID), eventHolder);
            synCtx.setProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY, scope);
        }
        return scope;
    }

    /**
     * Axis2 message context variant (data services), whose flows have no statistics event holder.
     */
    private static MessageContextTracingScope getOrCreateTracingScope(StatisticDataUnit statisticDataUnit,
                                                                      org.apache.axis2.context.MessageContext msgCtx) {
        MessageContextTracingScope scope = asScope(msgCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (needsNewScope(scope, statisticDataUnit, null)) {
            scope = new MessageContextTracingScope(
                    (String) msgCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID), null);
            msgCtx.setProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY, scope);
        }
        return scope;
    }

    @Override
    public SpanStore getSpanStore(MessageContext messageContext) {
        MessageContextTracingScope scope = asScope(messageContext.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        // A flow without a scope has no spans. Do not attach an empty scope to it.
        return scope != null ? scope.getSpanStore() : new SpanStore();
    }

    // ----- Open events -----

    @Override
    public void handleOpenEntryEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        startSpanOrBufferSequenceContinuationState(statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenEntryEvent(StatisticDataUnit statisticDataUnit,
                                     org.apache.axis2.context.MessageContext msgCtx) {
        MessageContextTracingScope scope = getOrCreateTracingScope(statisticDataUnit, msgCtx);
        synchronized (scope.getSpanStore()) {
            startSpan(statisticDataUnit, msgCtx, scope.getSpanStore());
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

    @Override
    public void handleOpenFlowAsynchronousEvent(BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    @Override
    public void handleOpenContinuationEvents(BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    /**
     * Starts a span, or buffers the open event of a proxy or API in/out sequence until it is pushed to the
     * continuation stack (as the legacy handler does).
     */
    private void startSpanOrBufferSequenceContinuationState(StatisticDataUnit statisticDataUnit,
                                                            MessageContext synCtx) {
        MessageContextTracingScope scope = getOrCreateTracingScope(statisticDataUnit, synCtx);
        SpanStore spanStore = scope.getSpanStore();
        synchronized (spanStore) {
            if (isContinuationStateApplicable(statisticDataUnit) && synCtx.isContinuationEnabled()) {
                // Will begin during addSeqContinuationState
                spanStore.addContinuationStateSequenceInfo(new ContinuationStateSequenceInfo(statisticDataUnit));
            } else {
                startSpan(statisticDataUnit, synCtx, spanStore);
            }
        }
        if (logger.isDebugEnabled()) {
            debug("open " + statisticDataUnit.getComponentName() + " #" + statisticDataUnit.getCurrentIndex(), scope);
        }
    }

    private boolean isContinuationStateApplicable(StatisticDataUnit statisticDataUnit) {
        return statisticDataUnit.getComponentType() == ComponentType.SEQUENCE &&
                (SequenceType.PROXY_INSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.PROXY_OUTSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.API_INSEQ.toString().equals(statisticDataUnit.getComponentName()) ||
                        SequenceType.API_OUTSEQ.toString().equals(statisticDataUnit.getComponentName()));
    }

    /**
     * Starts a span and stores it in the span store. Same as the legacy handler.
     */
    private void startSpan(StatisticDataUnit statisticDataUnit, MessageContext synCtx, SpanStore spanStore) {
        SpanWrapper parentSpanWrapper = ParentResolver.resolveParent(statisticDataUnit, spanStore, synCtx);
        Span parentSpan = null;
        Context context;
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

        headersMap.putAll(tracerSpecificCarrier);
        statisticDataUnit.setTransportHeaderMap(getSafeTransportHeaders(headersMap));
        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

        // Set custom span header tags
        Map<String, Object> customTagsMap =
                getCustomSpanTags(synCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS), headersMap);
        if (customTagsMap != null) {
            if (synCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS) == null) {
                synCtx.setProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, customTagsMap);
            }
            statisticDataUnit.setCustomProperties(customTagsMap);
        }

        if (statusCode != null) {
            statisticDataUnit.setStatusCode(statusCode.toString());
        }
        if (statusDescription != null) {
            statisticDataUnit.setStatusDescription(statusDescription.toString());
        }
        if (statisticDataUnit.getComponentType() == ComponentType.ENDPOINT) {
            statisticDataUnit.setEndpoint(synCtx.getEndpoint(statisticDataUnit.getComponentName()));
        }

        String spanId = TracingUtils.extractId(statisticDataUnit);
        SpanWrapper spanWrapper = spanStore.addSpanWrapper(spanId, span, statisticDataUnit, parentSpanWrapper, synCtx);

        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }
    }

    /**
     * Starts a span for an Axis2 message context and stores it in the span store. Same as the legacy handler.
     */
    private void startSpan(StatisticDataUnit statisticDataUnit, org.apache.axis2.context.MessageContext msgCtx,
                           SpanStore spanStore) {
        SpanWrapper parentSpanWrapper = spanStore.getSpanWrapper(String.valueOf(statisticDataUnit.getParentIndex()));
        Span parentSpan = null;
        Context context;
        if (parentSpanWrapper != null) {
            parentSpan = parentSpanWrapper.getSpan();
        }
        Map<String, String> tracerSpecificCarrier = new HashMap<>();

        Map headersMap = (Map) msgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
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
        Span span = tracer.spanBuilder(statisticDataUnit.getComponentName()).setParent(context).startSpan();

        // Set tracing headers
        inject(span, tracerSpecificCarrier);
        msgCtx.setProperty(SynapseConstants.JAEGER_TRACE_ID, span.getSpanContext().getTraceId());
        msgCtx.setProperty(SynapseConstants.JAEGER_SPAN_ID, span.getSpanContext().getSpanId());
        ThreadContext.put(SynapseConstants.TRACE_ID, span.getSpanContext().getTraceId());
        ThreadContext.put(SynapseConstants.SPAN_ID, span.getSpanContext().getSpanId());

        headersMap.putAll(tracerSpecificCarrier);
        msgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);

        Map<String, Object> customTagsMap =
                getCustomSpanTags(msgCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS), headersMap);
        if (customTagsMap != null) {
            if (msgCtx.getProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS) == null) {
                msgCtx.setProperty(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS, customTagsMap);
            }
            statisticDataUnit.setCustomProperties(customTagsMap);
        }

        statisticDataUnit.setTransportHeaderMap(getSafeTransportHeaders(headersMap));

        String spanId = TracingUtils.extractId(statisticDataUnit);
        SpanWrapper spanWrapper = spanStore.addSpanWrapper(spanId, span, statisticDataUnit, parentSpanWrapper, msgCtx);

        if (isOuterLevelSpan(statisticDataUnit, spanStore)) {
            spanStore.assignOuterLevelSpan(spanWrapper);
        }
    }

    /**
     * Returns the configured custom span tags (transport headers copied as span attributes) for a span. Values found
     * by an earlier span of the flow are reused, so all spans carry the values of the entry request.
     *
     * @param previousTags Tags stored by an earlier span of the flow, or null.
     * @param headersMap   Transport headers of the message.
     * @return Tags to set on the span, or null when there are none.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getCustomSpanTags(Object previousTags, Map headersMap) {
        String customTagsString = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.OLTP_CUSTOM_SPAN_TAGS,
                null);
        if (customTagsString == null || customTagsString.isEmpty()) {
            return null;
        }
        if (previousTags != null) {
            Map<String, Object> customTagsMap = (Map<String, Object>) previousTags;
            return customTagsMap.isEmpty() ? null : customTagsMap;
        }
        Map<String, Object> customTagsMap = new HashMap<>();
        for (String tag : customTagsString.split(",")) {
            if (headersMap.containsKey(tag.trim())) {
                customTagsMap.put(tag, headersMap.get(tag));
            }
        }
        return customTagsMap.isEmpty() ? null : customTagsMap;
    }

    private static boolean isOuterLevelComponent(StatisticDataUnit statisticDataUnit) {
        return TelemetryUtil.isOuterLayerComponent(statisticDataUnit.getComponentType())
                || statisticDataUnit.isOuterLayerSpan()
                || (statisticDataUnit.getComponentType() == ComponentType.SEQUENCE
                && SynapseConstants.MAIN_SEQUENCE_KEY.equals(statisticDataUnit.getComponentName()));
    }

    private static boolean isOuterLevelSpan(StatisticDataUnit statisticDataUnit, SpanStore spanStore) {
        return spanStore.getOuterLevelSpanWrapper() == null && isOuterLevelComponent(statisticDataUnit);
    }

    private static Map<Object, Object> getSafeTransportHeaders(Map<?, ?> headersMap) {
        Map<Object, Object> safeHeaders = new ConcurrentHashMap<>();
        headersMap.forEach((key, value) -> {
            if (key != null && value != null) {
                safeHeaders.put(key, value);
            }
        });
        return safeHeaders;
    }

    // ----- Close events -----

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
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        // Absorb
    }

    private void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx,
                                  boolean isError) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            // No span has been started in this flow. Never create a scope here: the legacy handler did (its
            // getTracingScope creates on a miss), and a close that arrived after the root had ended and the scope had
            // been removed re-created an empty scope that nothing removed again (aggregating ForEach, Scatter-Gather).
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        synchronized (spanStore) {
            // Proxy and API in/out sequences are finished when they are popped from the continuation stack
            if (!spanStore.hasContinuationStateSequenceInfoWithId(TracingUtils.extractId(basicStatisticDataUnit))) {
                SpanWrapper spanWrapper = spanStore.getSpanWrapper(TracingUtils.extractId(basicStatisticDataUnit));
                if (spanWrapper != null && basicStatisticDataUnit instanceof StatisticDataUnit) {
                    spanWrapper.setCloseEventStatisticDataUnit((StatisticDataUnit) basicStatisticDataUnit);
                }
                if (spanWrapper == null || !Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
                    spanStore.finishSpan(spanWrapper, synCtx, isError);
                } else {
                    // The outer level span ends when the flow has no work left: record that the root's own work is
                    // done, and end it now only if no worker or callback is pending. Otherwise the last of them ends
                    // it (handleBranchEnd, handleCallbackFinishEvent).
                    scope.setOuterLevelCloseReceived();
                    tryFinishOuterLevelSpan(scope, synCtx, isError);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            debug("close #" + basicStatisticDataUnit.getCurrentIndex(), scope);
        }
    }

    private void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit,
                                  org.apache.axis2.context.MessageContext msgCtx, boolean isError) {
        MessageContextTracingScope scope = asScope(msgCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        synchronized (spanStore) {
            if (!spanStore.hasContinuationStateSequenceInfoWithId(TracingUtils.extractId(basicStatisticDataUnit))) {
                SpanWrapper spanWrapper = spanStore.getSpanWrapper(TracingUtils.extractId(basicStatisticDataUnit));
                if (spanWrapper != null && basicStatisticDataUnit instanceof StatisticDataUnit) {
                    spanWrapper.setCloseEventStatisticDataUnit((StatisticDataUnit) basicStatisticDataUnit);
                }
                if (spanWrapper != null && Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
                    // Flows reported with an Axis2 message context (data services) have no asynchronous parts
                    finishAxis2OuterLevelSpan(scope, spanStore, spanWrapper, msgCtx, isError);
                } else {
                    spanStore.finishSpan(spanWrapper, msgCtx, isError);
                }
            }
        }
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx,
                                          boolean error) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(TracingUtils.extractId(basicStatisticDataUnit));

        if (!error) {
            // A MediatorWorker ended. Runs inside handleBranchEnd's lock; the worker's unit is already released, and
            // handleBranchEnd decides whether the outer level span can end. Here, only the span the branch stopped at
            // is finished, as in the legacy handler. isAllBranchesFinished is the raw branchCount, so where the main
            // branch is never closed (main-sequence root, S4, sequential task, uncollected inbound) endActiveSpans is
            // not set: same as the legacy handler.
            if (TelemetryUtil.isAllBranchesFinished(synCtx)) {
                synchronized (spanStore) {
                    if (spanWrapper != null && !Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
                        spanStore.finishSpan(spanWrapper, synCtx);
                    }
                    scope.setEndActiveSpansWithOuterLevelSpan();
                }
            }
            if (logger.isDebugEnabled()) {
                debug("branch close #" + basicStatisticDataUnit.getCurrentIndex(), scope);
            }
            return;
        }

        // A fault sequence handled an error. The control does not return to the components that were running, so
        // their spans, up to the outer level span, are finished now.
        handleCloseEvent(basicStatisticDataUnit, synCtx, false);
        synchronized (spanStore) {
            while (spanWrapper != null && spanWrapper.getParentSpanWrapper() != null) {
                spanWrapper = spanWrapper.getParentSpanWrapper();
                if (Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
                    finishOuterLevelSpan(scope, synCtx, false, false);
                } else {
                    spanStore.finishSpan(spanWrapper, synCtx);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            debug("error close #" + basicStatisticDataUnit.getCurrentIndex(), scope);
        }
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit,
                                          org.apache.axis2.context.MessageContext msgCtx) {
        MessageContextTracingScope scope = asScope(msgCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(TracingUtils.extractId(basicStatisticDataUnit));

        // finish the current span
        handleCloseEvent(basicStatisticDataUnit, msgCtx, true);

        // finish outer level spans since the control is not returned to the original flow to close them gracefully.
        synchronized (spanStore) {
            while (spanWrapper != null && spanWrapper.getParentSpanWrapper() != null) {
                spanWrapper = spanWrapper.getParentSpanWrapper();
                if (Objects.equals(spanWrapper, spanStore.getOuterLevelSpanWrapper())) {
                    finishAxis2OuterLevelSpan(scope, spanStore, spanWrapper, msgCtx, true);
                } else {
                    spanStore.finishSpan(spanWrapper, msgCtx, true);
                }
            }
        }
    }

    /**
     * Ends the outer level span of a flow reported with an Axis2 message context, once. Must be called while holding
     * the span store lock.
     */
    private static void finishAxis2OuterLevelSpan(MessageContextTracingScope scope, SpanStore spanStore,
                                                  SpanWrapper spanWrapper,
                                                  org.apache.axis2.context.MessageContext msgCtx, boolean isError) {
        if (!scope.isFinished()) {
            spanStore.finishSpan(spanWrapper, msgCtx, isError);
            scope.setFinished();
            ThreadContext.remove(SynapseConstants.TRACE_ID);
        }
    }

    /**
     * Handles the end of an aggregation (ForEach or Scatter-Gather). The legacy handler ends the outer level span
     * here. This handler ends it when the flow has no work left, which can be later: the outer level component may
     * not have closed yet, and branches or callbacks started after the aggregation may still be running.
     * Example: Scatter-Gather followed by a non-blocking call. The legacy handler ends the root at the aggregation, and
     * the spans of the call and of the response path land in separate traces.
     * <p>
     * The flag set here keeps what the legacy handler did at this point (its cleanUpActiveSpans): spans that are still
     * active when the root ends are ended together with the root instead of being left open. Only the moment moves,
     * from the aggregation to the end of the flow.
     */
    @Override
    public void handleScatterGatherFinishEvent(MessageContext synCtx) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        synchronized (scope.getSpanStore()) {
            scope.setEndActiveSpansWithOuterLevelSpan();
            tryFinishOuterLevelSpan(scope, synCtx, false);
        }
        if (logger.isDebugEnabled()) {
            debug("aggregation finished", scope);
        }
    }

    // ----- Branches and callbacks -----

    /**
     * Ends a MediatorWorker of the flow. The release of the worker's branch unit and its close events (branchEnd) are
     * applied under the span store lock, followed by the check whether the flow has no work left.
     */
    @Override
    public void handleBranchEnd(MessageContext synCtx, Runnable branchEnd) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            branchEnd.run();
            return;
        }
        // The release of the worker's branch unit and its close events are applied under the span store lock, so no
        // root end decision (which is only taken under this lock) sees the unit released before the branch's spans are
        // finished.
        // The worker must release its unit before its close events (the statistics finish check in those events needs
        // to see it released). Without the lock that order allows this race (clone with continueParent and a slow main
        // path): the worker releases its unit; the main thread closes the proxy, sees no pending branch and ends the
        // root; only then does the worker report the close of its last mediator, so that span ends after its root.
        // With the lock, the main thread's close waits until the worker's close events are done.
        synchronized (scope.getSpanStore()) {
            try {
                branchEnd.run();
            } finally {
                tryFinishOuterLevelSpan(scope, synCtx, false);
            }
        }
        if (logger.isDebugEnabled()) {
            debug("branch end", scope);
        }
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {
        MessageContextTracingScope scope = asScope(messageContext.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        // The callback is counted by the statistics event holder before this call
        if (scope != null) {
            if (logger.isDebugEnabled()) {
                debug("callback added " + callbackId, scope);
            }
        }
    }

    @Override
    public void handleCallbackCompletionEvent(MessageContext oldMessageContext, String callbackId) {
        handleCallbackFinishEvent(oldMessageContext, callbackId);
    }

    @Override
    public void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId) {
        // Absorb. Callback handling completion will be reported after handling the specific message.
    }

    @Override
    public void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {
        handleCallbackFinishEvent(synapseOutMsgCtx, callbackId);
    }

    private void handleCallbackFinishEvent(MessageContext messageContext, String callbackId) {
        MessageContextTracingScope scope = asScope(messageContext.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        // The statistics event holder has already decremented its callback count (CallbackStatisticCollector updates
        // it before calling the handler); this only re-checks whether that was the last pending work.
        synchronized (scope.getSpanStore()) {
            tryFinishOuterLevelSpan(scope, messageContext, false);
        }
        if (logger.isDebugEnabled()) {
            debug("callback completed " + callbackId, scope);
        }
    }

    // ----- Ending the outer level span -----

    /**
     * Ends the outer level span if the flow has no work left. Must be called while holding the span store lock.
     */
    private void tryFinishOuterLevelSpan(MessageContextTracingScope scope, MessageContext synCtx, boolean isError) {
        if (!scope.isFinished() && scope.isFlowCompleted()) {
            finishOuterLevelSpan(scope, synCtx, isError, scope.isEndActiveSpansWithOuterLevelSpan());
        }
    }

    /**
     * Ends the outer level span, together with the buffered in/out sequences and, if requested, the spans that are
     * still active. Must be called while holding the span store lock.
     */
    private void finishOuterLevelSpan(MessageContextTracingScope scope, MessageContext synCtx, boolean isError,
                                      boolean endActiveSpans) {
        if (scope.isFinished()) {
            return;
        }
        scope.setFinished();
        SpanStore spanStore = scope.getSpanStore();
        SpanWrapper outerLevelSpanWrapper = spanStore.getOuterLevelSpanWrapper();
        finishContinuationStateSequences(spanStore, synCtx);
        if (endActiveSpans) {
            List<SpanWrapper> activeSpanWrappers = new ArrayList<>(spanStore.getActiveSpanWrappers());
            for (int i = activeSpanWrappers.size() - 1; i >= 0; i--) {
                SpanWrapper spanWrapper = activeSpanWrappers.get(i);
                if (!Objects.equals(spanWrapper, outerLevelSpanWrapper)) {
                    spanStore.finishSpan(spanWrapper, synCtx);
                }
            }
        }
        spanStore.finishSpan(outerLevelSpanWrapper, synCtx, isError);
        ThreadContext.remove(SynapseConstants.TRACE_ID);
        if (logger.isDebugEnabled()) {
            debug("outer level span finished, active spans left: "
                    + (spanStore.getActiveSpanWrappers().size()), scope);
        }
    }

    // ----- Continuation stack -----

    @Override
    public void handleStateStackInsertion(MessageContext synCtx, String seqName, SequenceType seqType) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        synchronized (spanStore) {
            ContinuationStateSequenceInfo continuationStateSequenceInfo =
                    findContinuationStateSequenceInfo(seqType, spanStore, false);
            if (continuationStateSequenceInfo != null) {
                continuationStateSequenceInfo.setSpanActive(true);
                startSpan(continuationStateSequenceInfo.getStatisticDataUnit(), synCtx, spanStore);
            }
        }
    }

    @Override
    public void handleStateStackRemoval(ContinuationState continuationState, MessageContext synCtx) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null || !(continuationState instanceof SeqContinuationState)) {
            // No other type will be kept track of
            return;
        }
        SpanStore spanStore = scope.getSpanStore();
        synchronized (spanStore) {
            ContinuationStateSequenceInfo continuationStateSequenceInfo = findContinuationStateSequenceInfo(
                    ((SeqContinuationState) continuationState).getSeqType(), spanStore, true);
            if (continuationStateSequenceInfo != null) {
                continuationStateSequenceInfo.setSpanActive(false);
                finishSpanForContinuationStateSequence(continuationStateSequenceInfo, spanStore, synCtx);
                spanStore.getContinuationStateSequenceInfos().remove(continuationStateSequenceInfo);
            }
        }
    }

    @Override
    public void handleStateStackClearance(MessageContext synCtx) {
        MessageContextTracingScope scope = asScope(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
        if (scope == null) {
            return;
        }
        synchronized (scope.getSpanStore()) {
            finishContinuationStateSequences(scope.getSpanStore(), synCtx);
        }
    }

    /**
     * Finds the buffered in/out sequence of the given type to start (not active yet) or to finish (active), as the
     * legacy handler does.
     */
    private ContinuationStateSequenceInfo findContinuationStateSequenceInfo(SequenceType seqType, SpanStore spanStore,
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

    private void finishContinuationStateSequences(SpanStore spanStore, MessageContext synCtx) {
        List<ContinuationStateSequenceInfo> continuationStateSequences = spanStore.getContinuationStateSequenceInfos();
        for (ContinuationStateSequenceInfo continuationStateSequence : continuationStateSequences) {
            finishSpanForContinuationStateSequence(continuationStateSequence, spanStore, synCtx);
        }
        continuationStateSequences.clear();
    }

    private void finishSpanForContinuationStateSequence(ContinuationStateSequenceInfo continuationStateSequenceInfo,
                                                        SpanStore spanStore, MessageContext synCtx) {
        SpanWrapper spanWrapper = spanStore.getSpanWrapper(continuationStateSequenceInfo.getSpanReferenceId());
        spanStore.finishSpan(spanWrapper, synCtx);
    }

    private static void debug(String event, MessageContextTracingScope scope) {
        logger.debug("[" + Thread.currentThread().getName() + "] " + event + " : " + scope);
    }
}

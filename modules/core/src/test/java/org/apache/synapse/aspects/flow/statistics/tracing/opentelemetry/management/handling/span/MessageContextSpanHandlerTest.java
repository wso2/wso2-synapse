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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingCountHolder;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryTracer;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.MessageContextTracingScope;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.scoping.TracingScope;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.mediators.TestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Tests for {@link MessageContextSpanHandler}: where the tracing scope lives, and when the outer level span ends.
 * <p>
 * The handler reads the branch and callback counts of the flow's statistics event holder; the collectors and
 * MediatorWorker change them. These tests change the counts directly, the way those classes do: a worker adds a branch
 * unit when it is created and releases it in the Runnable it passes to handleBranchEnd, a callback is counted before
 * handleAddCallback and uncounted before its completion is reported, and the close of the outer level component closes
 * the main branch before the handler sees the close event.
 */
public class MessageContextSpanHandlerTest extends TestCase {

    private final List<SpanData> exported = Collections.synchronizedList(new ArrayList<>());
    private SdkTracerProvider tracerProvider;
    private MessageContextSpanHandler handler;

    @Override
    protected void setUp() {
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(new CollectingExporter())).build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
        handler = new MessageContextSpanHandler(new TelemetryTracer(openTelemetry.getTracer("test")),
                openTelemetry);
    }

    @Override
    protected void tearDown() {
        tracerProvider.close();
    }

    public void testScopeIsKeptOnMessageContextAndEndsWithFlow() throws Exception {
        MessageContext synCtx = newMessageContext("flow-1");
        withEventHolder(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        handler.handleOpenChildEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), synCtx);

        MessageContextTracingScope scope = scopeOf(synCtx);
        assertNotNull("Scope must be stored on the message context", scope);
        assertEquals("flow-1", scope.getTracingScopeId());
        assertSame(scope.getSpanStore(), handler.getSpanStore(synCtx));

        handler.handleCloseEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), synCtx);
        countsOf(synCtx).closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);

        assertTrue(scope.isFinished());
        assertEquals(names("LogMediator", "LeakKitProxy"), exportedNames());
        assertParentChild("LeakKitProxy", "LogMediator");
    }

    public void testScopeWithoutEventHolderEndsAtRootClose() throws Exception {
        // Flows without a statistics event holder have no counts: the root ends at its close
        MessageContext synCtx = newMessageContext("flow-1b");
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertNull(scopeOf(synCtx).getEventHolder());
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("LeakKitProxy"), exportedNames());
    }

    public void testOuterLevelSpanWaitsForBranches() throws Exception {
        // S2/S3/S7: the outer level component closes while MediatorWorkers still run
        MessageContext synCtx = newMessageContext("flow-2");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitTask", ComponentType.TASK, 0, -1), synCtx);
        MessageContext branch = shareScope(synCtx);
        counts.incrementBranchCount();
        counts.incrementBranchCount();

        counts.closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitTask", ComponentType.TASK, 0, -1), synCtx);
        assertFalse("Outer level span must wait for the branches", scopeOf(synCtx).isFinished());
        assertTrue(exportedNames().isEmpty());

        handler.handleOpenChildEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), branch);
        handler.handleCloseEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), branch);
        handler.handleBranchEnd(branch, counts::decrementAndGetBranchCount);
        assertFalse(scopeOf(synCtx).isFinished());

        handler.handleBranchEnd(branch, counts::decrementAndGetBranchCount);
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("LogMediator", "LeakKitTask"), exportedNames());
        assertParentChild("LeakKitTask", "LogMediator");
    }

    public void testMainSequenceRootWithOpenMainBranch() throws Exception {
        // The close of the main sequence does not close the main branch: the root still ends at its close
        MessageContext synCtx = newMessageContext("flow-2b");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("main", ComponentType.SEQUENCE, 0, -1), synCtx);
        handler.handleCloseEntryEvent(unit("main", ComponentType.SEQUENCE, 0, -1), synCtx);
        assertTrue(counts.isMainBranchOpen());
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("main"), exportedNames());
    }

    public void testOuterLevelSpanWaitsForCallbacks() throws Exception {
        MessageContext synCtx = newMessageContext("flow-3");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        counts.incrementCallBackCount();
        handler.handleAddCallback(synCtx, "cb-1");
        counts.closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertFalse(scopeOf(synCtx).isFinished());

        MessageContext response = shareScope(synCtx);
        counts.decrementAndGetCallbackCount();
        handler.handleReportCallbackHandlingCompletion(response, "cb-1");
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("LeakKitProxy"), exportedNames());
    }

    public void testCallbackTimeoutAlsoCompletesCallback() throws Exception {
        MessageContext synCtx = newMessageContext("flow-4");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        counts.incrementCallBackCount();
        handler.handleAddCallback(synCtx, "cb-1");
        counts.closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertFalse(scopeOf(synCtx).isFinished());
        counts.decrementAndGetCallbackCount();
        handler.handleCallbackCompletionEvent(synCtx, "cb-1");
        assertTrue(scopeOf(synCtx).isFinished());
    }

    public void testCallbacksCountedAfterCollectionFinished() throws Exception {
        // An error closed the statistics collection, but a callback of the flow is still pending: the error walk did
        // not reach the root (the current index has no span), so the root waits for the callback
        MessageContext synCtx = newMessageContext("flow-4b");
        StatisticsReportingEventHolder eventHolder = withEventHolder(synCtx);
        StatisticsReportingCountHolder counts = eventHolder.countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        counts.incrementCallBackCount();
        handler.handleAddCallback(synCtx, "cb-1");
        eventHolder.setEvenCollectionFinished(true);
        handler.handleCloseFlowForcefully(unit(null, null, 99, 98), synCtx, true);
        assertFalse(scopeOf(synCtx).isFinished());

        counts.closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertFalse("The pending callback is still counted", scopeOf(synCtx).isFinished());

        counts.decrementAndGetCallbackCount();
        handler.handleReportCallbackHandlingCompletion(shareScope(synCtx), "cb-1");
        assertTrue(scopeOf(synCtx).isFinished());
    }

    public void testAggregationBeforeOuterLevelCloseEndsActiveSpansAtTheEnd() throws Exception {
        // S5/S6: the aggregation completes before the proxy closes, and leaves a span without a close event
        MessageContext synCtx = newMessageContext("flow-5");
        withEventHolder(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        handler.handleOpenFlowContinuableEvent(unit("foreachmediator", ComponentType.MEDIATOR, 1, 0), synCtx);

        handler.handleScatterGatherFinishEvent(synCtx);
        assertFalse("The proxy has not closed yet", scopeOf(synCtx).isFinished());

        countsOf(synCtx).closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("foreachmediator", "LeakKitProxy"), exportedNames());

        // Late close events find the finished scope, and neither fail nor start a new scope
        MessageContextTracingScope scope = scopeOf(synCtx);
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertSame(scope, scopeOf(synCtx));
        assertEquals(2, exportedNames().size());
    }

    public void testAggregationThenParallelBranches() throws Exception {
        // S7: the aggregation completes, then branches started after it are still running
        MessageContext synCtx = newMessageContext("flow-6");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        handler.handleScatterGatherFinishEvent(synCtx);
        MessageContext branch = shareScope(synCtx);
        counts.incrementBranchCount();
        counts.closeMainBranch();
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertFalse(scopeOf(synCtx).isFinished());

        handler.handleOpenChildEntryEvent(unit("CallMediator", ComponentType.MEDIATOR, 1, 0), branch);
        handler.handleCloseEntryEvent(unit("CallMediator", ComponentType.MEDIATOR, 1, 0), branch);
        handler.handleBranchEnd(branch, counts::decrementAndGetBranchCount);
        assertTrue(scopeOf(synCtx).isFinished());
        assertParentChild("LeakKitProxy", "CallMediator");
    }

    public void testErrorEndsOuterLevelSpanWithoutItsCloseEvent() throws Exception {
        // S8: an API whose resource fails never reports its own close event
        MessageContext synCtx = newMessageContext("flow-7");
        withEventHolder(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitApi", ComponentType.API, 0, -1), synCtx);
        handler.handleOpenChildEntryEvent(unit("ThrowErrorMediator", ComponentType.MEDIATOR, 1, 0), synCtx);

        handler.handleCloseFlowForcefully(unit(null, null, 1, 0), synCtx, true);
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("ThrowErrorMediator", "LeakKitApi"), exportedNames());
    }

    public void testErrorInBranchEndsRootWithPendingBranch() throws Exception {
        // A fault in a clone branch ends the spans up to the root at once, although the branch has not ended yet
        MessageContext synCtx = newMessageContext("flow-7b");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        handler.handleOpenFlowSplittingEvent(unit("CloneMediator", ComponentType.MEDIATOR, 1, 0), synCtx);
        counts.incrementBranchCount();
        MessageContext branch = shareScope(synCtx);
        handler.handleOpenChildEntryEvent(unit("ThrowErrorMediator", ComponentType.MEDIATOR, 2, 1), branch);

        counts.closeMainBranch();
        handler.handleCloseFlowForcefully(unit(null, null, 2, 1), branch, true);
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("ThrowErrorMediator", "CloneMediator", "LeakKitProxy"), exportedNames());

        handler.handleBranchEnd(branch, counts::decrementAndGetBranchCount);
        assertEquals("The root is exported once", 3, exportedNames().size());
    }

    public void testNewOuterLevelComponentOnFinishedScopeStartsNewScope() throws Exception {
        // A stored message dispatched by a message processor carries the finished scope of the original flow
        MessageContext synCtx = newMessageContext("flow-8");
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        MessageContextTracingScope first = scopeOf(synCtx);
        assertTrue(first.isFinished());

        handler.handleOpenEntryEvent(unit("LeakKitProcessor", ComponentType.MESSAGEPROCESSOR, 0, -1), synCtx);
        MessageContextTracingScope second = scopeOf(synCtx);
        assertNotSame(first, second);
        handler.handleCloseEntryEvent(unit("LeakKitProcessor", ComponentType.MESSAGEPROCESSOR, 0, -1), synCtx);
        assertTrue(second.isFinished());
        assertEquals(names("LeakKitProxy", "LeakKitProcessor"), exportedNames());
    }

    public void testScopeKeepsEventHolderOfItsFlow() throws Exception {
        // A context of the flow whose statistic properties are replaced mid-flow does not change the counts the scope
        // reads
        MessageContext synCtx = newMessageContext("flow-8b");
        StatisticsReportingEventHolder captured = withEventHolder(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertSame(captured, scopeOf(synCtx).getEventHolder());

        StatisticsReportingEventHolder other = new StatisticsReportingEventHolder();
        other.countHolder.incrementBranchCount();
        synCtx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, other);
        handler.handleCloseEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        assertTrue("The captured holder has no pending work", scopeOf(synCtx).isFinished());
    }

    public void testMessageProcessorFlowGetsOwnScope() throws Exception {
        // In-memory store: the message processor runs the stored message while the producer's root is still open, with
        // the statistics event holder that MessageProcessorUtils created for it
        MessageContext synCtx = newMessageContext("flow-8c");
        withEventHolder(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        MessageContextTracingScope producer = scopeOf(synCtx);

        // A task branch of the same flow (same holder) opening an outer level component shares the scope
        MessageContext sameFlow = shareScope(synCtx);
        handler.handleOpenEntryEvent(unit("LeakKitTask", ComponentType.TASK, 1, 0), sameFlow);
        assertSame(producer, scopeOf(sameFlow));

        MessageContext stored = shareScope(synCtx);
        StatisticsReportingEventHolder processorHolder = withEventHolder(stored);
        handler.handleOpenEntryEvent(unit("LeakKitProcessor", ComponentType.MESSAGEPROCESSOR, 0, -1), stored);
        MessageContextTracingScope processor = scopeOf(stored);
        assertNotSame(producer, processor);
        assertSame(processorHolder, processor.getEventHolder());
        assertFalse(producer.isFinished());

        handler.handleCloseEntryEvent(unit("LeakKitProcessor", ComponentType.MESSAGEPROCESSOR, 0, -1), stored);
        assertTrue(processor.isFinished());
        assertFalse(producer.isFinished());
    }

    public void testBranchEndRunsUnderSpanStoreLock() throws Exception {
        MessageContext synCtx = newMessageContext("flow-10");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        MessageContextTracingScope scope = scopeOf(synCtx);
        counts.incrementBranchCount();
        AtomicInteger runs = new AtomicInteger();
        handler.handleBranchEnd(shareScope(synCtx), () -> {
            assertTrue(Thread.holdsLock(scope.getSpanStore()));
            counts.decrementAndGetBranchCount();
            runs.incrementAndGet();
        });
        assertEquals(1, runs.get());
    }

    public void testRootCloseWaitsForBranchCloseEvents() throws Exception {
        // The root close that arrives while a worker ends cannot see the worker's unit released before the worker's
        // close events have finished its spans
        MessageContext synCtx = newMessageContext("flow-11");
        StatisticsReportingCountHolder counts = withEventHolder(synCtx).countHolder;
        handler.handleOpenEntryEvent(unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx);
        counts.closeMainBranch();
        counts.incrementBranchCount();
        MessageContext branch = shareScope(synCtx);
        handler.handleOpenChildEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), branch);

        Thread[] rootCloser = new Thread[1];
        handler.handleBranchEnd(branch, () -> {
            counts.decrementAndGetBranchCount();
            rootCloser[0] = new Thread(() -> handler.handleCloseEntryEvent(
                    unit("LeakKitProxy", ComponentType.PROXYSERVICE, 0, -1), synCtx));
            rootCloser[0].start();
            try {
                Thread.sleep(200);
                long deadline = System.currentTimeMillis() + 5000;
                while (rootCloser[0].getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals(Thread.State.BLOCKED, rootCloser[0].getState());
            handler.handleCloseEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 1, 0), branch);
        });
        rootCloser[0].join(5000);
        assertFalse(rootCloser[0].isAlive());
        assertTrue(scopeOf(synCtx).isFinished());
        assertEquals(names("LogMediator", "LeakKitProxy"), exportedNames());
    }

    public void testEventsWithoutScopeDoNotCreateOne() throws Exception {
        MessageContext synCtx = newMessageContext("flow-9");
        handler.handleCloseEntryEvent(unit("LogMediator", ComponentType.MEDIATOR, 3, 2), synCtx);
        handler.handleCloseFlowForcefully(unit(null, null, 3, 2), synCtx, false);
        handler.handleScatterGatherFinishEvent(synCtx);
        handler.handleCallbackCompletionEvent(synCtx, "cb");
        AtomicInteger runs = new AtomicInteger();
        handler.handleBranchEnd(synCtx, runs::incrementAndGet);
        assertEquals("The branch end still runs once", 1, runs.get());
        assertNotNull(handler.getSpanStore(synCtx));
        assertNull(synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY));
    }

    public void testScopeIsATracingScope() {
        // run.sh and heap analysis look for TracingScope instances
        assertTrue(new MessageContextTracingScope("id", null) instanceof TracingScope);
    }

    public void testPropagationHelpers() {
        MessageContextSpanHandler.setOpenTelemetry(OpenTelemetry.propagating(
                ContextPropagators.create(W3CTraceContextPropagator.getInstance())));
        Span span = tracerProvider.get("test").spanBuilder("span").startSpan();
        try {
            Map<String, String> carrier = new HashMap<>();
            MessageContextSpanHandler.inject(span, carrier);
            assertTrue(carrier.containsKey("traceparent"));

            Context context = MessageContextSpanHandler.extract(carrier);
            SpanContext extracted = Span.fromContext(context).getSpanContext();
            assertTrue(extracted.isValid());
            assertTrue(extracted.isRemote());
            assertEquals(span.getSpanContext().getTraceId(), extracted.getTraceId());
            assertEquals(span.getSpanContext().getSpanId(), extracted.getSpanId());

            // OpenTelemetry is not enabled in the statistics configuration of the test: no-op
            Map<String, String> outbound = new HashMap<>();
            MessageContextSpanHandler.injectTraceContext(newMessageContext("flow-12"), outbound, Map::put);
            assertTrue(outbound.isEmpty());
        } catch (Exception e) {
            throw new AssertionError(e);
        } finally {
            span.end();
        }
    }

    // ----- helpers -----

    private static MessageContext newMessageContext(String flowId) throws Exception {
        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, flowId);
        return synCtx;
    }

    /**
     * Sets a new statistics event holder on the message context, as the first statistics event of a flow does. Must
     * be called before the first open event, so that the scope captures it.
     */
    private static StatisticsReportingEventHolder withEventHolder(MessageContext synCtx) {
        StatisticsReportingEventHolder eventHolder = new StatisticsReportingEventHolder();
        synCtx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        return eventHolder;
    }

    private static StatisticsReportingCountHolder countsOf(MessageContext synCtx) {
        return ((StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY))
                .countHolder;
    }

    /**
     * Creates a message context of the same flow, the way clones and callback response contexts are made: by
     * copying the properties, which shares the scope and the statistics event holder by reference.
     */
    private static MessageContext shareScope(MessageContext synCtx) throws Exception {
        MessageContext other = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        for (Object key : synCtx.getPropertyKeySet()) {
            other.setProperty((String) key, synCtx.getProperty((String) key));
        }
        return other;
    }

    private static MessageContextTracingScope scopeOf(MessageContext synCtx) {
        return (MessageContextTracingScope) synCtx.getProperty(TelemetryConstants.TRACING_SCOPE_PROPERTY);
    }

    private static StatisticDataUnit unit(String name, ComponentType type, int index, int parentIndex) {
        StatisticDataUnit unit = new StatisticDataUnit();
        unit.setComponentName(name);
        unit.setComponentType(type);
        unit.setCurrentIndex(index);
        unit.setParentIndex(parentIndex);
        unit.setTime(System.currentTimeMillis());
        return unit;
    }

    private static List<String> names(String... names) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, names);
        return list;
    }

    private List<String> exportedNames() {
        synchronized (exported) {
            return exported.stream().map(SpanData::getName).collect(Collectors.toList());
        }
    }

    private void assertParentChild(String parent, String child) {
        SpanData parentSpan = find(parent);
        SpanData childSpan = find(child);
        assertEquals(parentSpan.getTraceId(), childSpan.getTraceId());
        assertEquals(parentSpan.getSpanId(), childSpan.getParentSpanId());
    }

    private SpanData find(String name) {
        synchronized (exported) {
            return exported.stream().filter(s -> s.getName().equals(name)).findFirst()
                    .orElseThrow(() -> new AssertionError("Span not exported: " + name));
        }
    }

    private class CollectingExporter implements SpanExporter {

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            exported.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}

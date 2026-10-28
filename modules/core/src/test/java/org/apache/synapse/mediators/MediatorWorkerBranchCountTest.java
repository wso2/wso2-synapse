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

package org.apache.synapse.mediators;

import junit.framework.TestCase;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SequenceType;
import org.apache.synapse.aspects.flow.statistics.StatisticsCloseEventListener;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingCountHolder;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.store.MessageDataStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.OpenTelemetryManager;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.span.OpenTelemetrySpanHandler;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.inbound.InboundEndpointStatisticsCloseEventListener;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.EnqueueMediator;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Tests that a {@link MediatorWorker} owns one unit of the branch count of its flow's statistics event holder: added
 * when it is created, released exactly once when it ends (normally, with an error, or when it cannot be started), and
 * released before its statistics close listener runs.
 */
public class MediatorWorkerBranchCountTest extends TestCase {

    private final Object[] savedFlags = new Object[4];
    private Object savedManager;

    private SynapseConfiguration configuration;
    private MessageContext synCtx;
    private MessageDataStore messageDataStore;

    @Override
    protected void setUp() throws Exception {
        savedFlags[0] = getStatic(RuntimeStatisticCollector.class, "isStatisticsEnabled");
        savedFlags[1] = getStatic(RuntimeStatisticCollector.class, "isMediationFlowStatisticsEnabled");
        savedFlags[2] = getStatic(RuntimeStatisticCollector.class, "isOpenTelemetryEnabled");
        savedFlags[3] = getStatic(RuntimeStatisticCollector.class, "isCollectingAllStatistics");
        savedManager = getStatic(OpenTelemetryManagerHolder.class, "openTelemetryManager");
        setStatic(RuntimeStatisticCollector.class, "isStatisticsEnabled", true);
        setStatic(RuntimeStatisticCollector.class, "isMediationFlowStatisticsEnabled", true);
        setStatic(RuntimeStatisticCollector.class, "isOpenTelemetryEnabled", false);

        configuration = new SynapseConfiguration();
        synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>", configuration);
        messageDataStore = Mockito.mock(MessageDataStore.class);
        setField(Axis2SynapseEnvironment.class, synCtx.getEnvironment(), "messageDataStore", messageDataStore);
    }

    @Override
    protected void tearDown() throws Exception {
        setStatic(RuntimeStatisticCollector.class, "isStatisticsEnabled", savedFlags[0]);
        setStatic(RuntimeStatisticCollector.class, "isMediationFlowStatisticsEnabled", savedFlags[1]);
        setStatic(RuntimeStatisticCollector.class, "isOpenTelemetryEnabled", savedFlags[2]);
        setStatic(RuntimeStatisticCollector.class, "isCollectingAllStatistics", savedFlags[3]);
        setStatic(OpenTelemetryManagerHolder.class, "openTelemetryManager", savedManager);
    }

    // ----- Units -----

    public void testConstructionAddsUnitOnlyForCollectedHolder() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        new MediatorWorker(sequence(ctx -> { }), synCtx);
        assertEquals(2, counts.getBranchCount());
        assertEquals(1, counts.getPendingBranchCount());

        MessageContext notCollected = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        StatisticsReportingCountHolder notCollectedCounts = collected(notCollected).countHolder;
        notCollected.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
        new MediatorWorker(sequence(ctx -> { }), notCollected);
        assertEquals(1, notCollectedCounts.getBranchCount());

        MessageContext withoutHolder = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        withoutHolder.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, "flow-x");
        withoutHolder.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
        new MediatorWorker(sequence(ctx -> { }), withoutHolder);
        assertNull(withoutHolder.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY));
    }

    public void testRunReleasesUnitBeforeCloseListener() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> { }), synCtx);
        List<Integer> seenByListener = new ArrayList<>();
        worker.setStatisticsCloseEventListener(new StatisticsCloseEventListener() {
            @Override
            public void invokeCloseEventEntry(MessageContext synCtx) {
                seenByListener.add(counts.getBranchCount());
                super.invokeCloseEventEntry(synCtx);
            }
        });
        worker.run();
        assertEquals("Released before the listener", 1, (int) seenByListener.get(0));
        assertEquals(1, seenByListener.size());
        assertEquals(1, counts.getBranchCount());
    }

    public void testSynapseExceptionWithFaultHandlerReleasesOnce() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        AtomicInteger faults = new AtomicInteger();
        synCtx.pushFaultHandler(new RecordingFaultHandler(faults, false));
        new MediatorWorker(sequence(ctx -> { throw new SynapseException("fail"); }), synCtx).run();
        assertEquals(1, faults.get());
        assertEquals(1, counts.getBranchCount());
    }

    public void testSynapseExceptionWithoutFaultHandlerReleasesOnce() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        new MediatorWorker(sequence(ctx -> { throw new SynapseException("fail"); }), synCtx).run();
        assertEquals(1, counts.getBranchCount());
    }

    public void testRuntimeExceptionReleasesOnce() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        new MediatorWorker(sequence(ctx -> { throw new IllegalStateException("fail"); }), synCtx).run();
        assertEquals(1, counts.getBranchCount());
    }

    public void testErrorReleasesOnce() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        new MediatorWorker(sequence(ctx -> { throw new AssertionError("fail"); }), synCtx).run();
        assertEquals(1, counts.getBranchCount());
    }

    public void testFaultHandlerThatThrowsReleasesOnce() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        AtomicInteger faults = new AtomicInteger();
        synCtx.pushFaultHandler(new RecordingFaultHandler(faults, true));
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> { throw new SynapseException("fail"); }), synCtx);
        try {
            worker.run();
            fail("The exception of the fault handler propagates");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, faults.get());
        assertEquals(1, counts.getBranchCount());
    }

    public void testReleaseBranchIsIdempotent() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> { }), synCtx);
        worker.releaseBranch();
        worker.releaseBranch();
        assertEquals(1, counts.getBranchCount());

        MediatorWorker second = new MediatorWorker(sequence(ctx -> { }), synCtx);
        assertEquals(2, counts.getBranchCount());
        second.releaseBranch();
        second.run();
        assertEquals(1, counts.getBranchCount());
    }

    // ----- Statistics publishing -----

    public void testRootWorkerClosesMainBranchAndPublishes() throws Exception {
        // Template task / fresh context: the flow's event holder is created by the worker's own sequence
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, "flow-root");
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> {
            ctx.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
            ctx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, new StatisticsReportingEventHolder());
        }), synCtx);
        worker.run();
        StatisticsReportingEventHolder eventHolder = holderOf(synCtx);
        assertEquals(0, eventHolder.countHolder.getBranchCount());
        assertFalse(eventHolder.countHolder.isMainBranchOpen());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testRootWorkerWithOuterLevelListenerDoesNotCloseMainBranch() throws Exception {
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, "flow-inbound");
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> {
            ctx.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
            ctx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, new StatisticsReportingEventHolder());
        }), synCtx);
        List<Boolean> mainOpenInListener = new ArrayList<>();
        worker.setStatisticsCloseEventListener(new InboundEndpointStatisticsCloseEventListener("inbound", null) {
            @Override
            public void invokeCloseEventEntry(MessageContext synCtx) {
                mainOpenInListener.add(holderOf(synCtx).countHolder.isMainBranchOpen());
                super.invokeCloseEventEntry(synCtx);
            }
        });
        worker.run();
        assertEquals("The worker leaves the main branch to the INBOUND close", Boolean.TRUE,
                mainOpenInListener.get(0));
        assertFalse("The INBOUND close closed it", holderOf(synCtx).countHolder.isMainBranchOpen());
    }

    public void testScatterWorkerPublishesWithoutListener() throws Exception {
        StatisticsReportingEventHolder eventHolder = collected(synCtx);
        eventHolder.countHolder.closeMainBranch();
        synCtx.setProperty(SynapseConstants.SCATTER_MESSAGES, true);
        synCtx.pushContinuationState(new SeqContinuationState(SequenceType.NAMED, "scatterTarget"));
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> { }, false), synCtx);
        AtomicInteger listenerCalls = new AtomicInteger();
        worker.setStatisticsCloseEventListener(new StatisticsCloseEventListener() {
            @Override
            public void invokeCloseEventEntry(MessageContext synCtx) {
                listenerCalls.incrementAndGet();
            }
        });
        assertEquals(1, eventHolder.countHolder.getBranchCount());
        worker.run();
        assertEquals(0, listenerCalls.get());
        assertEquals(0, eventHolder.countHolder.getBranchCount());
        assertTrue(eventHolder.isEvenCollectionFinished());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testScatterWorkerWithoutContinuationStateReleases() throws Exception {
        StatisticsReportingEventHolder eventHolder = collected(synCtx);
        eventHolder.countHolder.closeMainBranch();
        synCtx.setProperty(SynapseConstants.SCATTER_MESSAGES, true);
        AtomicInteger mediated = new AtomicInteger();
        new MediatorWorker(sequence(ctx -> mediated.incrementAndGet()), synCtx).run();
        assertEquals("Returns before mediating", 0, mediated.get());
        assertEquals(0, eventHolder.countHolder.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testLastWorkerPublishesAfterError() throws Exception {
        StatisticsReportingEventHolder eventHolder = collected(synCtx);
        MediatorWorker worker = new MediatorWorker(
                sequence(ctx -> CloseEventCollector.closeFlowForcefully(ctx, true)), synCtx);
        worker.run();
        assertTrue(eventHolder.isMessageFlowError());
        assertEquals(0, eventHolder.countHolder.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);

        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, true);
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testWorkerDoesNotPublishAfterErrorWhileAnotherWorkerRuns() throws Exception {
        StatisticsReportingEventHolder eventHolder = collected(synCtx);
        MediatorWorker other = new MediatorWorker(sequence(ctx -> { }), synCtx);
        MediatorWorker worker = new MediatorWorker(
                sequence(ctx -> CloseEventCollector.closeFlowForcefully(ctx, true)), synCtx);
        worker.run();
        assertEquals(1, eventHolder.countHolder.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);

        other.run();
        assertEquals(0, eventHolder.countHolder.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    // ----- Span handler hook -----

    public void testEndSequenceRunsInsideSpanHandlerOnce() throws Exception {
        setStatic(RuntimeStatisticCollector.class, "isOpenTelemetryEnabled", true);
        OpenTelemetrySpanHandler handler = Mockito.mock(OpenTelemetrySpanHandler.class);
        AtomicInteger branchEnds = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
            branchEnds.incrementAndGet();
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(handler).handleBranchEnd(Mockito.any(MessageContext.class), Mockito.any(Runnable.class));
        OpenTelemetryManager manager = Mockito.mock(OpenTelemetryManager.class);
        Mockito.when(manager.getHandler()).thenReturn(handler);
        setStatic(OpenTelemetryManagerHolder.class, "openTelemetryManager", manager);

        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        MediatorWorker worker = new MediatorWorker(sequence(ctx -> { }), synCtx);
        AtomicInteger listenerCalls = new AtomicInteger();
        worker.setStatisticsCloseEventListener(new StatisticsCloseEventListener() {
            @Override
            public void invokeCloseEventEntry(MessageContext synCtx) {
                listenerCalls.incrementAndGet();
                super.invokeCloseEventEntry(synCtx);
            }
        });
        worker.run();
        assertEquals(1, branchEnds.get());
        assertEquals(1, listenerCalls.get());
        assertEquals(1, counts.getBranchCount());
    }

    public void testUnitReleasedWhenSpanHandlerFails() throws Exception {
        setStatic(RuntimeStatisticCollector.class, "isOpenTelemetryEnabled", true);
        OpenTelemetrySpanHandler handler = Mockito.mock(OpenTelemetrySpanHandler.class);
        Mockito.doThrow(new IllegalStateException("handler bug")).when(handler)
                .handleBranchEnd(Mockito.any(MessageContext.class), Mockito.any(Runnable.class));
        OpenTelemetryManager manager = Mockito.mock(OpenTelemetryManager.class);
        Mockito.when(manager.getHandler()).thenReturn(handler);
        setStatic(OpenTelemetryManagerHolder.class, "openTelemetryManager", manager);

        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        try {
            new MediatorWorker(sequence(ctx -> { }), synCtx).run();
            fail("The handler failure propagates");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, counts.getBranchCount());
    }

    // ----- Start failures at the creation sites -----

    public void testInjectAsyncStartFailureReleasesUnit() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        Axis2SynapseEnvironment environment = (Axis2SynapseEnvironment) synCtx.getEnvironment();
        for (RuntimeException failure : new RuntimeException[]{
                new RejectedExecutionException("full"), new IllegalStateException("broken")}) {
            setField(Axis2SynapseEnvironment.class, environment, "executorService", throwingExecutor(failure));
            try {
                environment.injectAsync(synCtx, new SequenceMediator());
                fail("The start failure propagates");
            } catch (RuntimeException e) {
                assertSame(failure, e);
            }
            assertEquals(1, counts.getBranchCount());
        }
    }

    public void testEnqueueStartFailureReleasesUnit() throws Exception {
        StatisticsReportingCountHolder counts = collected(synCtx).countHolder;
        configuration.addSequence("enqueued", new SequenceMediator());
        EnqueueMediator enqueue = new EnqueueMediator();
        enqueue.setSequenceName("enqueued");
        enqueue.setPriority(1);

        // Not initialised
        configuration.addPriorityExecutor("uninitialised", new PriorityExecutor());
        enqueue.setExecutorName("uninitialised");
        try {
            enqueue.mediate(synCtx);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, counts.getBranchCount());

        // No queue for the priority
        PriorityExecutor noQueue = Mockito.mock(PriorityExecutor.class);
        Mockito.doThrow(new IllegalArgumentException("no queue")).when(noQueue)
                .execute(Mockito.any(Runnable.class), Mockito.anyInt());
        configuration.addPriorityExecutor("noQueue", noQueue);
        enqueue.setExecutorName("noQueue");
        try {
            enqueue.mediate(synCtx);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertEquals(1, counts.getBranchCount());

        // Rejected: jumps to the fault handler
        PriorityExecutor full = Mockito.mock(PriorityExecutor.class);
        Mockito.doThrow(new RejectedExecutionException("full")).when(full)
                .execute(Mockito.any(Runnable.class), Mockito.anyInt());
        configuration.addPriorityExecutor("full", full);
        enqueue.setExecutorName("full");
        try {
            enqueue.mediate(synCtx);
            fail("SynapseException expected");
        } catch (SynapseException expected) {
            // expected
        }
        assertEquals(1, counts.getBranchCount());
    }

    public void testInjectInboundStartFailureReleasesUnit() throws Exception {
        setStatic(RuntimeStatisticCollector.class, "isCollectingAllStatistics", true);
        // The entry event of the inbound endpoint takes the statistics id of the flow from the message id
        synCtx.setMessageID("urn:uuid:inbound-1");
        Axis2SynapseEnvironment environment = (Axis2SynapseEnvironment) synCtx.getEnvironment();
        setField(Axis2SynapseEnvironment.class, environment, "executorServiceInbound",
                throwingExecutor(new RejectedExecutionException("full")));
        AtomicInteger mediated = new AtomicInteger();
        assertTrue(environment.injectInbound(synCtx, sequence(ctx -> mediated.incrementAndGet()), false));
        assertEquals("The fallback mediates in the same thread", 1, mediated.get());
        StatisticsReportingCountHolder counts = holderOf(synCtx).countHolder;
        assertEquals("Worker unit released, main branch closed by the INBOUND close", 0, counts.getBranchCount());
        assertEquals(0, counts.getPendingBranchCount());

        MessageContext other = TestUtils.createLightweightSynapseMessageContext("<empty/>", configuration);
        other.setMessageID("urn:uuid:inbound-2");
        setField(Axis2SynapseEnvironment.class, other.getEnvironment(), "messageDataStore", messageDataStore);
        Axis2SynapseEnvironment otherEnvironment = (Axis2SynapseEnvironment) other.getEnvironment();
        IllegalStateException failure = new IllegalStateException("broken");
        setField(Axis2SynapseEnvironment.class, otherEnvironment, "executorServiceInbound", throwingExecutor(failure));
        try {
            otherEnvironment.injectInbound(other, sequence(ctx -> { }), false);
            fail("The start failure propagates");
        } catch (IllegalStateException e) {
            assertSame(failure, e);
        }
        StatisticsReportingCountHolder otherCounts = holderOf(other).countHolder;
        assertEquals("Only the main branch is left", 1, otherCounts.getBranchCount());
        assertEquals(0, otherCounts.getPendingBranchCount());
    }

    // ----- helpers -----

    private static StatisticsReportingEventHolder collected(MessageContext synCtx) {
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, "flow-" + System.nanoTime());
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
        StatisticsReportingEventHolder eventHolder = new StatisticsReportingEventHolder();
        synCtx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        return eventHolder;
    }

    private static StatisticsReportingEventHolder holderOf(MessageContext synCtx) {
        return (StatisticsReportingEventHolder) synCtx.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
    }

    private static SequenceMediator sequence(Consumer<MessageContext> action) {
        return sequence(action, true);
    }

    private static SequenceMediator sequence(Consumer<MessageContext> action, boolean result) {
        return new SequenceMediator() {
            @Override
            public boolean mediate(MessageContext synCtx) {
                action.accept(synCtx);
                return result;
            }
        };
    }

    private static ExecutorService throwingExecutor(RuntimeException failure) {
        ExecutorService executor = Mockito.mock(ExecutorService.class);
        Mockito.doThrow(failure).when(executor).execute(Mockito.any(Runnable.class));
        return executor;
    }

    private static Object getStatic(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStatic(Class<?> type, String name, Object value) throws Exception {
        setField(type, null, name, value);
    }

    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class RecordingFaultHandler extends FaultHandler {

        private final AtomicInteger faults;
        private final boolean fail;

        RecordingFaultHandler(AtomicInteger faults, boolean fail) {
            this.faults = faults;
            this.fail = fail;
        }

        @Override
        public void handleFault(MessageContext synCtx, Exception e) {
            onFault(synCtx);
        }

        @Override
        public void onFault(MessageContext synCtx) {
            faults.incrementAndGet();
            if (fail) {
                throw new IllegalStateException("fault handler failed");
            }
        }
    }
}

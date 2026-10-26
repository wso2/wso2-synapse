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

package org.apache.synapse.aspects.flow.statistics.collectors;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingCountHolder;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndFlowEvent;
import org.apache.synapse.aspects.flow.statistics.store.MessageDataStore;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryUtil;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.TestUtils;
import org.mockito.Mockito;

import java.lang.reflect.Field;

/**
 * Tests when the statistics event holder of a flow is published: an error closes the main branch once and defers the
 * publish to the last MediatorWorker, and callback counts stay live after the collection has finished.
 */
public class EventCollectionFinishTest extends TestCase {

    private boolean statisticsEnabled;
    private boolean mediationFlowStatisticsEnabled;
    private boolean openTelemetryEnabled;

    private MessageContext synCtx;
    private StatisticsReportingEventHolder eventHolder;
    private StatisticsReportingCountHolder counts;
    private MessageDataStore messageDataStore;

    @Override
    protected void setUp() throws Exception {
        statisticsEnabled = (Boolean) getStatic("isStatisticsEnabled");
        mediationFlowStatisticsEnabled = (Boolean) getStatic("isMediationFlowStatisticsEnabled");
        openTelemetryEnabled = (Boolean) getStatic("isOpenTelemetryEnabled");
        setStatic("isStatisticsEnabled", true);
        setStatic("isMediationFlowStatisticsEnabled", true);
        setStatic("isOpenTelemetryEnabled", false);

        synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, "flow-1");
        synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
        eventHolder = new StatisticsReportingEventHolder();
        counts = eventHolder.countHolder;
        synCtx.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        messageDataStore = Mockito.mock(MessageDataStore.class);
        Field field = Axis2SynapseEnvironment.class.getDeclaredField("messageDataStore");
        field.setAccessible(true);
        field.set(synCtx.getEnvironment(), messageDataStore);
    }

    @Override
    protected void tearDown() throws Exception {
        setStatic("isStatisticsEnabled", statisticsEnabled);
        setStatic("isMediationFlowStatisticsEnabled", mediationFlowStatisticsEnabled);
        setStatic("isOpenTelemetryEnabled", openTelemetryEnabled);
    }

    public void testErrorWithPendingWorkerDefersPublishToLastWorker() {
        counts.incrementBranchCount();

        CloseEventCollector.closeFlowForcefully(synCtx, true);
        assertTrue(eventHolder.isEvenCollectionFinished());
        assertTrue(eventHolder.isMessageFlowError());
        assertFalse(counts.isMainBranchOpen());
        assertEquals(1, counts.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);

        // The worker ends: releases its unit, then runs the finish check
        counts.decrementAndGetBranchCount();
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);

        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, true);
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testErrorWithAnotherWorkerPendingDoesNotPublish() {
        counts.incrementBranchCount();
        counts.incrementBranchCount();
        CloseEventCollector.closeFlowForcefully(synCtx, true);

        counts.decrementAndGetBranchCount();
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);

        counts.decrementAndGetBranchCount();
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testErrorThenOuterCloseClosesMainBranchOnce() {
        // A proxy fault: the fault sequence closes the flow with an error, then the proxy's finally closes the outer
        // level component, which closes the main branch again
        CloseEventCollector.closeFlowForcefully(synCtx, true);
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
        TelemetryUtil.closeMainBranch(synCtx);
        assertEquals("Not -1", 0, counts.getBranchCount());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testScatterCompletionPublishesWithoutCloseEvent() {
        counts.closeMainBranch();
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, false);
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);
        assertFalse(eventHolder.isEvenCollectionFinished());

        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, true);
        assertTrue(eventHolder.isEvenCollectionFinished());
        Mockito.verify(messageDataStore, Mockito.times(1)).enqueue(eventHolder);
    }

    public void testScatterCompletionWaitsForContinueStatisticsFlow() {
        counts.closeMainBranch();
        synCtx.setProperty(StatisticsConstants.CONTINUE_STATISTICS_FLOW, true);
        RuntimeStatisticCollector.tryFinishEventCollection(synCtx, eventHolder, true);
        assertFalse(eventHolder.isEvenCollectionFinished());
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);
    }

    public void testCallbackCountsAfterCollectionFinished() {
        eventHolder.setEvenCollectionFinished(true);

        RuntimeStatisticCollector.addEventAndIncrementCallbackCount(synCtx, endFlowEvent());
        assertEquals(1, counts.getCallBackCount());
        assertEquals("No event is added after the collection finished", 0, eventHolder.getQueueSize());

        RuntimeStatisticCollector.addEventAndDecrementCallbackCount(synCtx, endFlowEvent());
        assertEquals(0, counts.getCallBackCount());
        Mockito.verify(messageDataStore, Mockito.never()).enqueue(eventHolder);
    }

    private EndFlowEvent endFlowEvent() {
        BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
        dataUnit.setStatisticId("flow-1");
        dataUnit.setCurrentIndex(1);
        return new EndFlowEvent(dataUnit);
    }

    private static Object getStatic(String name) throws Exception {
        Field field = RuntimeStatisticCollector.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStatic(String name, Object value) throws Exception {
        Field field = RuntimeStatisticCollector.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}

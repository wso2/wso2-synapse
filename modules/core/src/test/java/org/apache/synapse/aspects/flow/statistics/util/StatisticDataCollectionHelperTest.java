/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.aspects.flow.statistics.util;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for StatisticDataCollectionHelper class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(RuntimeStatisticCollector.class)
public class StatisticDataCollectionHelperTest {

    private static Axis2MessageContext messageContext;
    private static Axis2MessageContext messageContextOld;
    private static Axis2MessageContext messageContextNew;

    /**
     * Initializing message context.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void initialize() throws Exception {
        String samplePayload = "<test>value</test>";
        Map<String, Entry> properties = new HashMap<>();
        messageContext = TestUtils.getAxis2MessageContext(samplePayload, properties);
        messageContextOld = TestUtils.getAxis2MessageContext(samplePayload, properties);
        messageContextNew = TestUtils.getAxis2MessageContext(samplePayload, properties);
    }

    /**
     * Test getStatisticTraceId method by validating returned string.
     */
    @Test
    public void testGetStatisticTraceId() {
        //inserting text value here to distinguish from numeric values later appended
        String id = "testId";
        messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, id);
        String traceId = StatisticDataCollectionHelper.getStatisticTraceId(messageContext);
        Assert.assertTrue("should return concatenation of id and timestamp",traceId.contains(id));
    }

    /**
     * Test getFlowPosition method for start and increment.
     */
    @Test
    public void testGetFlowPosition() {
        int currentIndex = StatisticDataCollectionHelper.getFlowPosition(messageContext);
        Assert.assertEquals("index should start from 0", 0, currentIndex);
        Assert.assertNotNull("indexing object must be set by the method", messageContext.getProperty
                (StatisticsConstants.MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT));
        currentIndex = StatisticDataCollectionHelper.getFlowPosition(messageContext);
        Assert.assertEquals("index should be incremented to 1", 1, currentIndex);
    }

    /**
     * Test getParentFlowPosition method for default and inserted parent ids.
     */
    @Test
    public void testGetParentFlowPosition() {
        int position = StatisticDataCollectionHelper.getParentFlowPosition(messageContext, 0);
        Assert.assertNotNull("should set a new parent index property",
                messageContext.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX));
        Assert.assertEquals("should return the default parent index",
                StatisticsConstants.DEFAULT_PARENT_INDEX, position);
        messageContext.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX, 1);
        Assert.assertEquals("should return the inserted value to the parent index property",
                1, StatisticDataCollectionHelper.getParentFlowPosition(messageContext, 0));
    }

    /**
     * Test getParentList method by validating parent id list.
     */
    @Test
    public void testGetParentList() {
        Assert.assertNull("should return null since no list added",
                StatisticDataCollectionHelper.getParentList(messageContext));
        List<Integer> parentList = Arrays.asList(1, 2, 3);
        messageContext.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST, parentList);
        Assert.assertEquals("should return the added parentList",
                parentList, StatisticDataCollectionHelper.getParentList(messageContext));
    }

    /**
     * Test isOutOnlyFlow method for true and false cases.
     */
    @Test
    public void testIsOutOnlyFlow() {
        messageContext.setProperty(SynapseConstants.OUT_ONLY, "true");
        Assert.assertTrue("should return true", StatisticDataCollectionHelper.isOutOnlyFlow(messageContext));
        messageContext.setProperty(SynapseConstants.OUT_ONLY, "false");
        Assert.assertFalse("should return false", StatisticDataCollectionHelper.isOutOnlyFlow(messageContext));
    }

    /**
     * Test collectAggregatedParents method by validating aggregated parent IDs.
     */
    @Test
    public void testCollectAggregatedParents() {
        PowerMockito.mockStatic(RuntimeStatisticCollector.class);
        PowerMockito.when(RuntimeStatisticCollector.isStatisticsEnabled()).thenReturn(true);
        PowerMockito.when(RuntimeStatisticCollector.shouldReportStatistic(any(MessageContext.class))).thenReturn(true);
        messageContextOld.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX, 1);
        messageContextNew.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX, 2);
        List<MessageContext> messageList = new ArrayList<>();
        messageList.add(messageContextOld);
        StatisticDataCollectionHelper.collectAggregatedParents(messageList, messageContextNew);
        List<Integer> parentList = Arrays.asList(1);
        Assert.assertEquals("should return all the parent indices in message list", parentList, messageContextNew
                .getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST));
    }

    /**
     * Test collectData method by validating dataUnit.
     */
    @Test
    public void testCollectData() {
        PowerMockito.mockStatic(RuntimeStatisticCollector.class);
        PowerMockito.when(RuntimeStatisticCollector.isCollectingPayloads()).thenReturn(true);
        PowerMockito.when(RuntimeStatisticCollector.isCollectingProperties()).thenReturn(true);
        StatisticDataUnit dataUnit = new StatisticDataUnit();
        StatisticDataCollectionHelper.collectData(messageContext, true, true, dataUnit);
        Assert.assertNotNull("context properties should be inserted", dataUnit.getContextPropertyMap());
        Assert.assertNotNull("transport properties should be inserted", dataUnit.getTransportPropertyMap());
        Assert.assertNotNull("payload should be inserted", dataUnit.getPayload());
    }
}

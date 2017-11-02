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

package org.apache.synapse.aspects.flow.statistics.publishing;

import junit.framework.Assert;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for PublishingEvent class.
 */
public class PublishingEventTest {

    private static PublishingEvent publishingEvent;
    private static final String COMPONENT_NAME = "testComponentName";
    private static final String FLOW_ID = "testFlowId";
    private static final String COMPONENT_ID = "testComponentId";
    private static final Integer COMPONENT_INDEX = 1;
    private static final Long START_TIME = 5L;
    private static final Long END_TIME = 10L;
    private static final Long DURATION = 5L;
    private static final String BEFORE_PAYLOAD = "testBeforePayload";
    private static final String AFTER_PAYLOAD = "testAfterPayload";
    private static final Integer[] CHILDREN = {1, 2, 3};
    private static final String ENTRY_POINT = "testEntryPoint";
    private static final Integer ENTRY_POINT_HASH_CODE = 1234;
    private static final Integer FAULT_COUNT = 1;
    private static final Integer HASHCODE = 12345;
    private static final Map<String, Object> PROPERTY_MAP = new HashMap<>();

    /**
     * Initializing PublishingEvent before tests.
     */
    @BeforeClass
    public static void init() {
        StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
        statisticDataUnit.setTime(START_TIME);
        PROPERTY_MAP.put("1", new Integer(1));
        statisticDataUnit.setContextPropertyMap(PROPERTY_MAP);
        StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit);
        statisticsLog.setComponentType(ComponentType.ENDPOINT);
        statisticsLog.setComponentName(COMPONENT_NAME);
        statisticsLog.setComponentId(COMPONENT_ID);
        statisticsLog.setEndTime(END_TIME);
        statisticsLog.setChildren(Arrays.asList(CHILDREN));
        statisticsLog.incrementNoOfFaults();
        statisticsLog.setHashCode(HASHCODE);
        publishingEvent = new PublishingEvent(FLOW_ID, COMPONENT_INDEX, statisticsLog, ENTRY_POINT,
                ENTRY_POINT_HASH_CODE);
        publishingEvent.setBeforePayload(BEFORE_PAYLOAD);
        publishingEvent.setAfterPayload(AFTER_PAYLOAD);
    }

    /**
     * Testing the overridden toSting method.
     */
    @Test
    public void testToString() {
        String expected = "Component Type " + StatisticsConstants.FLOW_STATISTICS_ENDPOINT
                + " , Component Name " + COMPONENT_NAME;
        Assert.assertEquals("toString should return expected string", publishingEvent.toString(), expected);
    }

    /**
     * Testing getObjectList() (order of resulting array objects should be asserted).
     */
    @Test
    public void testGetObjectList() {
        ArrayList<Object> objectList = publishingEvent.getObjectAsList();
        int index = 0;
        Assert.assertEquals("comparing stored values", objectList.get(index++), FLOW_ID);
        Assert.assertEquals("comparing stored values", objectList.get(index++),
                StatisticsConstants.FLOW_STATISTICS_ENDPOINT);
        Assert.assertEquals("comparing stored values", objectList.get(index++), COMPONENT_NAME);
        Assert.assertEquals("comparing stored values", objectList.get(index++), COMPONENT_INDEX);
        Assert.assertEquals("comparing stored values", objectList.get(index++), COMPONENT_ID);
        Assert.assertEquals("comparing stored values", objectList.get(index++), START_TIME);
        Assert.assertEquals("comparing stored values", objectList.get(index++), END_TIME);
        Assert.assertEquals("comparing stored values", objectList.get(index++), DURATION);
        Assert.assertEquals("comparing stored values", objectList.get(index++), BEFORE_PAYLOAD);
        Assert.assertEquals("comparing stored values", objectList.get(index++), AFTER_PAYLOAD);
        Assert.assertEquals("null since not initialized", objectList.get(index++), PROPERTY_MAP.toString());
        Assert.assertNull("null since not initialized", objectList.get(index++));
        Assert.assertEquals("comparing stored values", objectList.get(index++), Arrays.toString(CHILDREN));
        Assert.assertEquals("comparing stored values", objectList.get(index++), ENTRY_POINT);
        Assert.assertEquals("comparing stored values", objectList.get(index++),
                String.valueOf(ENTRY_POINT_HASH_CODE));
        Assert.assertEquals("comparing stored values", objectList.get(index++), FAULT_COUNT);
        Assert.assertEquals("comparing stored values", objectList.get(index), String.valueOf(HASHCODE));
    }

}

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

import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Map;

/**
 * Unit tests for PublishFlow class
 */
public class PublishingFlowTest {

    private PublishingFlow publishingFlow = new PublishingFlow();

    /**
     * initialize publishingFlow and test getObjectAsMap method
     */
    @Test
    public void testGetObjectAsMap() {
        String MESSAGE_FLOW_ID = "messageFlowId";
        String EVENTS = "events";
        String PAYLOADS = "payloads";

        publishingFlow.setMessageFlowId(MESSAGE_FLOW_ID);
        final PublishingEvent publishingEvent = new PublishingEvent();
        publishingFlow.addEvent(publishingEvent);
        ArrayList<PublishingPayload> payloadCollection = new ArrayList<>();
        final PublishingPayload publishingPayload = new PublishingPayload();
        payloadCollection.add(publishingPayload);
        publishingFlow.setPayloads(payloadCollection);
        Map<String, Object> objectMap = publishingFlow.getObjectAsMap();

        Assert.assertEquals("flow id should be equal to initialized value", objectMap.get(MESSAGE_FLOW_ID),
                MESSAGE_FLOW_ID);
        Assert.assertEquals("events should be equal to initialized value", objectMap.get(EVENTS),
                new ArrayList<Object>() {{
                    add(publishingEvent.getObjectAsList());
                }});
        Assert.assertEquals("payloads should be equal to initialized value", objectMap.get(PAYLOADS),
                payloadCollection);
    }
}

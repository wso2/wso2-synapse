/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PublishingFlow {

    private String messageFlowId;

    private ArrayList<PublishingEvent> events = new ArrayList<>();

    private ArrayList<Map<String, Object>> eventsMap = new ArrayList<>();

    private ArrayList<PublishingPayload> payloads;


    public PublishingEvent getEvent(int index) {
        return events.get(index);
    }

    public boolean addEvent(PublishingEvent publishingEvent) {

        return events.add(publishingEvent) && eventsMap.add(publishingEvent.getObjectAsMap());
    }

    public String getMessageFlowId() {
        return messageFlowId;
    }

    public void setMessageFlowId(String messageFlowId) {
        this.messageFlowId = messageFlowId;
    }

    public ArrayList<PublishingEvent> getEvents() {
        return events;
    }

    public void setEvents(
            ArrayList<PublishingEvent> events) {
        this.events = events;
    }

    public ArrayList<PublishingPayload> getPayloads() {
        return payloads;
    }


    public void setPayloads(Collection<PublishingPayload> values) {
        payloads = new ArrayList<>(values);
    }

    public Map<String, Object> getObjectAsMap() {
        String MESSAGE_FLOW_ID = "messageFlowId";
        String EVENTS = "events";
        String PAYLOADS = "payloads";

        Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put(MESSAGE_FLOW_ID, this.messageFlowId);
        objectMap.put(EVENTS, eventsMap);
        objectMap.put(PAYLOADS, payloads);

        return objectMap;
    }
}

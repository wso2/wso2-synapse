/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.publishing;

import java.util.ArrayList;

public class PublishingPayload {

    private String payload;

    private ArrayList<PublishingPayloadEvent> events = new ArrayList<>();

    public PublishingPayloadEvent getEvent(int index) {
        return events.get(index);
    }

    public boolean addEvent(PublishingPayloadEvent publishingPayloadEvent) {
        return events.add(publishingPayloadEvent);
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ArrayList<PublishingPayloadEvent> getEvents() {
        return events;
    }

    public void setEvents(
            ArrayList<PublishingPayloadEvent> events) {
        this.events = events;
    }
}

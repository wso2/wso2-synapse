/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.aspects.flow.statistics.log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by rajith on 7/15/16.
 */
public class StatisticsReportingEventHolder {
    private Queue<StatisticsReportingEvent> eventQueue;
//    private List<StatisticsReportingEvent> eventList;
    public StatisticsReportingCountHolder countHolder;

    public StatisticsReportingEventHolder() {
        eventQueue = new ConcurrentLinkedQueue<StatisticsReportingEvent>();
//        eventList = new LinkedList<StatisticsReportingEvent>();
        countHolder = new StatisticsReportingCountHolder();
    }

    public void addEvent(StatisticsReportingEvent event) {
        this.eventQueue.add(event);
//        this.eventList.add(event);
    }

//    public List<StatisticsReportingEvent> getEventList() {
//        return this.eventList;
//    }

    public StatisticsReportingEvent deQueueEvent() {
        return eventQueue.poll();
    }

    public List<StatisticsReportingEvent> getEventList() {
        return new ArrayList<>(eventQueue);
    }



    public int getQueueSize() {
        return this.eventQueue.size();
    }
}

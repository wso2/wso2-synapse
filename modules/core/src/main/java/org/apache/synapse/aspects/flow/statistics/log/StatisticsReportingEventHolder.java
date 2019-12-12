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
 * This class is to hold events and stat count information before starting processing events.
 */
public class StatisticsReportingEventHolder {
    /**
     * Queue to accumulate events, before processing happens.
     */
    private Queue<StatisticsReportingEvent> eventQueue;
    /**
     * Count holder which keeps stat count and callback counts.
     */
    public StatisticsReportingCountHolder countHolder;

    private boolean evenCollectionFinished = false;

    private boolean messageFlowError = false;

    /**
     * Denotes whether to publish data for Mediation Flow Statistics or not.
     */
    private boolean publishMediationFlowStatistics;

    public StatisticsReportingEventHolder() {
        eventQueue = new ConcurrentLinkedQueue<StatisticsReportingEvent>();
        countHolder = new StatisticsReportingCountHolder();
    }

    public void addEvent(StatisticsReportingEvent event) {
        this.eventQueue.add(event);
    }

    public StatisticsReportingEvent deQueueEvent() {
        return eventQueue.poll();
    }

    public List<StatisticsReportingEvent> getEventList() {
        return new ArrayList<>(eventQueue);
    }

    public int getQueueSize() {
        return this.eventQueue.size();
    }

    public boolean isEvenCollectionFinished() {
        return evenCollectionFinished;
    }

    public void setEvenCollectionFinished(boolean evenCollectionFinished) {
        this.evenCollectionFinished = evenCollectionFinished;
    }

    public boolean isMessageFlowError() {
        return messageFlowError;
    }

    public void setMessageFlowError(boolean messageFlowError) {
        this.messageFlowError = messageFlowError;
    }

    public void setPublishMediationFlowStatistics(boolean publishMediationFlowStatistics) {
        this.publishMediationFlowStatistics = publishMediationFlowStatistics;
    }

    public boolean isPublishMediationFlowStatistics() {
        return publishMediationFlowStatistics;
    }
}

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

import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;

/**
 * Created by rajith on 7/19/16.
 */
public class StatisticsProcessWorker implements Runnable {

    private StatisticsReportingEventHolder eventHolder;
    private StatisticEventProcessor2 eventProcessor;

    public StatisticsProcessWorker(StatisticsReportingEventHolder eventHolder) {
        this.eventHolder = eventHolder;
    }

    @Override
    public void run() {
        eventProcessor = new StatisticEventProcessor2();
        for (StatisticsReportingEvent event : eventHolder.getEventList()) {
            event.processEvents(eventProcessor);
        }
    }
}

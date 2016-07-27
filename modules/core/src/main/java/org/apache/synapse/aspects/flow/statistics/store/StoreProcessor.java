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
package org.apache.synapse.aspects.flow.statistics.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.log.StatisticEventProcessor;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;

/**
 * This is the processor which will process statistic events before publishing to Analytic server
 */
public class StoreProcessor implements Runnable {
    private static Log log = LogFactory.getLog(StoreProcessor.class);

    private MessageDataStore store;

    private String threadName;

    public StoreProcessor(MessageDataStore store, String threadName) {
        this.store = store;
        this.threadName = threadName;
    }

    @Override
    public void run() {
//        StatisticsReportingEventHolder statisticsReportingEventHolder;
//        while (!store.isStopped()) {
//            try {
//                statisticsReportingEventHolder = store.dequeue();
//                if (statisticsReportingEventHolder != null) {
//                    StatisticEventProcessor eventProcessor2 = new StatisticEventProcessor();
//                    for (StatisticsReportingEvent event : statisticsReportingEventHolder.getEventList()) {
//                        event.processEvents(eventProcessor2);
//                    }
//                } else {
//                    Thread.sleep(1);
//                }
//            } catch (Exception exception) {
//                log.error("Error in mediation flow statistic data consumer while consuming data", exception);
//            }
//        }
    }
}

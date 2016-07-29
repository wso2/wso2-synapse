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
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MessageDataCollector contains the non-blocking queue and utility methods to store and retrieve elements from the
 * queue.
 */
public class MessageDataStore {

    private static Log log = LogFactory.getLog(MessageDataStore.class);
    /**
     * Queue which holds event holder objects with collected events.
     */
    private Queue<StatisticsReportingEventHolder> queue;

    public MessageDataStore() {
        queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Add StatisticsReportingEventHolder instance to the queue
     *
     * @param statisticsReportingEventHolder StatisticReportingLog to be stored in the queue
     */
    public void enqueue(StatisticsReportingEventHolder statisticsReportingEventHolder) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Adding eventHolder: " + statisticsReportingEventHolder);
            }
            queue.add(statisticsReportingEventHolder);
        } catch (Exception e) {
            log.error("Error adding statistic event holder to the Queue. Dropping statistics events.");
        }
    }

    /**
     * Removes and return StatisticReportingLog from the queue
     *
     * @return StatisticReportingLog instance
     * @throws Exception
     */
    public StatisticsReportingEventHolder dequeue() throws Exception {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Polling statistics event holder object from the Queue");
            }
            return queue.poll();
        } catch (Exception e) {
            log.error("Error polling statistics event holder objects from Queue");
            return null;
        }
    }

}

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
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MessageDataCollector contains the non-blocking queue and utility methods to store and retrieve elements from the
 * queue.
 */
public class MessageDataStore2 implements Runnable {

    private static Log log = LogFactory.getLog(MessageDataStore2.class);

    private Queue<StatisticsReportingEventHolder> queue;

    public MessageDataStore2(int queueSize) {
        queue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Add StatisticReportingLog instance to the queue
     *
     * @param statisticsReportingEventHolder StatisticReportingLog to be stored in the queue
     */
    public void enqueue(StatisticsReportingEventHolder statisticsReportingEventHolder) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Adding event: " + statisticsReportingEventHolder.getEventList());
            }
            queue.add(statisticsReportingEventHolder);
        } catch (Exception e) {
            log.error("Statistics queue became full. Dropping statistics events.");
        }
    }

    /**
     * Removes and return StatisticReportingLog from the queue
     *
     * @return StatisticReportingLog instance
     * @throws Exception
     */
    public StatisticsReportingEventHolder dequeue() throws Exception {
//		try {
        return queue.poll();
//		} catch (InterruptedException exception) {
//			String errorMsg = "Error consuming statistic data queue";
//			throw new Exception(errorMsg, exception);
//		}
    }

    /**
     * Checks whether the queue is empty
     *
     * @return Tru if empty/false otherwise
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private boolean isStopped = false;

    public void run() {
        StatisticsReportingEventHolder statisticsReportingEventHolder;
        while (!isStopped && !isEmpty()) {
            try {
                statisticsReportingEventHolder = dequeue();
                if (statisticsReportingEventHolder != null) {
                    for (StatisticsReportingEvent event : statisticsReportingEventHolder.getEventList()) {
                        event.process();
                    }
                } else {
                    Thread.sleep(1);
                }
            } catch (Exception exception) {
                log.error("Error in mediation flow statistic data consumer while consuming data", exception);
            }
        }
    }

    public void setStopped() {
        this.isStopped = true;
    }
}

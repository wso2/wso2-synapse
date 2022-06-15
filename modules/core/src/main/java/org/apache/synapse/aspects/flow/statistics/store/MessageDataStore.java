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
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapseConfiguration;

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

    private String queueEvictionPolicy;
    private long maxStaticsReportingQueueSize;

    public MessageDataStore(SynapseConfiguration synCfg) {
        queue = new ConcurrentLinkedQueue<>();
        queueEvictionPolicy = synCfg.getProperty(StatisticsConstants.STATISTIC_REPORTING_QUEUE_EVICTION_POLICY,
                                                 StatisticsConstants.QUEUE_EVICTION_POLICY_NEW_MESSAGES);
        maxStaticsReportingQueueSize = synCfg.getProperty(StatisticsConstants.STATISTIC_REPORTING_QUEUE_SIZE,
                                                          StatisticsConstants.MAX_STATISTIC_REPORTING_QUEUE_SIZE);
    }

    /**
     * Add StatisticsReportingEventHolder instance to the queue
     *
     * @param statisticsReportingEventHolder StatisticReportingLog to be stored in the queue
     */
    public void enqueue(StatisticsReportingEventHolder statisticsReportingEventHolder) {
        if (queue.size() > maxStaticsReportingQueueSize) {
            // This will does not add anymore
            if (queueEvictionPolicy.equals(StatisticsConstants.QUEUE_EVICTION_POLICY_NEW_MESSAGES)) {
                log.warn("Dropping new statistic messages since the queue is full");
                return;
            } else if (queueEvictionPolicy.equals(StatisticsConstants.QUEUE_EVICTION_POLICY_OLD_MESSAGES)) {
                // This will dequeue old messages and enqueue new messages
                log.warn("Dropping old statistic messages since the queue is full");
                queue.poll();
            }
        }
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

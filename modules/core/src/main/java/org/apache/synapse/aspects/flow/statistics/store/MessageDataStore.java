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

    public static MessageDataStore messageDataStore;

    private Queue<StatisticsReportingEventHolder> queue;

    public MessageDataStore() {
        queue = new ConcurrentLinkedQueue<>();
    }

//    public static MessageDataStore getInstance() {
//        if (messageDataStore == null) {
//            createMessageDataStore();
//        }
//        return messageDataStore;
//    }
//
//    private synchronized static void createMessageDataStore() {
//        if (messageDataStore == null) {
//            messageDataStore = new MessageDataStore();
//        }
//    }

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
        return queue.poll();
    }

}

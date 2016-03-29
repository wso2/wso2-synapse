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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * MessageDataCollector contains the non-blocking queue and utility methods to store and retrieve elements from the
 * queue.
 */
public class MessageDataStore implements Runnable {

	private static Log log = LogFactory.getLog(MessageDataStore.class);

	private BlockingQueue<StatisticsReportingEvent> queue;

	public MessageDataStore(int queueSize) {
		queue = new ArrayBlockingQueue<>(queueSize);
	}

	/**
	 * Add StatisticReportingLog instance to the queue
	 *
	 * @param statisticsReportingEvent StatisticReportingLog to be stored in the queue
	 */
	public void enqueue(StatisticsReportingEvent statisticsReportingEvent) {
		try {
			queue.add(statisticsReportingEvent);
		}catch (Exception e){
			log.error("Statistics queue became full. Dropping statistics events.");
		}
	}

	/**
	 * Removes and return StatisticReportingLog from the queue
	 *
	 * @return StatisticReportingLog instance
	 * @throws Exception
	 */
	public StatisticsReportingEvent dequeue() throws Exception {
		try {
			return queue.take();
		} catch (InterruptedException exception) {
			String errorMsg = "Error consuming statistic data queue";
			throw new Exception(errorMsg, exception);
		}
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
		StatisticsReportingEvent statisticsReportingEvent;
		while (!isStopped && !isEmpty()) {
			try {
				statisticsReportingEvent = dequeue();
				statisticsReportingEvent.process();
			} catch (Exception exception) {
				log.error("Error in mediation flow statistic data consumer while consuming data", exception);
			}
		}
	}

	public void setStopped() {
		this.isStopped = true;
	}
}

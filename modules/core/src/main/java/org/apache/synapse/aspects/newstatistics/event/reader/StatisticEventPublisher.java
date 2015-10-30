/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.newstatistics.event.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.newstatistics.log.templates.StatisticReportingLog;

import java.util.concurrent.BlockingQueue;

public class StatisticEventPublisher implements Runnable {
	private static final Log log = LogFactory.getLog(StatisticEventPublisher.class);
	BlockingQueue<StatisticReportingLog> queue;
	public StatisticEventPublisher(BlockingQueue<StatisticReportingLog> queue) {
		this.queue = queue;
	}

	public void run() {
		while (true) {
			try{
				StatisticReportingLog statisticReportingLog = queue.take();
				statisticReportingLog.process();
			}catch(InterruptedException e){
				log.error("Statistic event worker was interrupted.");
			}
		}
	}
}

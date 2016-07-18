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

package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticEventProcessor;
import org.apache.synapse.aspects.flow.statistics.log.templates.ParentReopenEvent;
import org.apache.synapse.aspects.flow.statistics.store.MessageDataStore;
import org.apache.synapse.aspects.flow.statistics.util.MediationFlowController;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RuntimeStatisticCollector receives statistic events and responsible for handling each of these events.
 */
public abstract class RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(RuntimeStatisticCollector.class);

	/**
	 * Is statistics collection enabled in synapse.properties file.
	 */
	private static boolean isStatisticsEnabled;

	/**
	 * Is payload collection enabled in synapse.properties file.
	 */
	private static boolean isCollectingPayloads;

	/**
	 * Is message context property collection enabled in synapse.properties file.
	 */
	private static boolean isCollectingProperties;

	/**
	 * Is collecting statistics of all artifacts
	 */
	private static boolean  isCollectingAllStatistics;

	/**
	 * Statistic event queue to hold statistic events.
	 */
	protected static MessageDataStore statisticEventQueue;

	public static long eventExpireTime;

	/**
	 * Initialize statistics collection when ESB starts.
	 */
	public static void init() {
		isStatisticsEnabled = Boolean.parseBoolean(
				SynapsePropertiesLoader.getPropertyValue(StatisticsConstants.STATISTICS_ENABLE, String.valueOf(false)));
		if (isStatisticsEnabled) {
			if (log.isDebugEnabled()) {
				log.debug("Mediation statistics collection is enabled.");
			}
			int queueSize = Integer.parseInt(SynapsePropertiesLoader
					                                 .getPropertyValue(StatisticsConstants.FLOW_STATISTICS_QUEUE_SIZE,
					                                                   StatisticsConstants.FLOW_STATISTICS_DEFAULT_QUEUE_SIZE));
			Long eventConsumerTime = Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
					StatisticsConstants.FLOW_STATISTICS_EVENT_CONSUME_TIME,
					StatisticsConstants.FLOW_STATISTICS_DEFAULT_EVENT_CONSUME_INTERVAL));

			isCollectingPayloads = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
					StatisticsConstants.COLLECT_MESSAGE_PAYLOADS, String.valueOf(false)));

			if (!isCollectingPayloads && log.isDebugEnabled()) {
				log.debug("Payload collecting is not enabled in \'synapse.properties\' file.");
			}

			isCollectingProperties = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
					StatisticsConstants.COLLECT_MESSAGE_PROPERTIES, String.valueOf(false)));

			if (!isCollectingProperties && log.isDebugEnabled()) {
				log.debug("Property collecting is not enabled in \'synapse.properties\' file.");
			}

			isCollectingAllStatistics = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
					StatisticsConstants.COLLECT_ALL_STATISTICS, String.valueOf(false)));

			statisticEventQueue = new MessageDataStore(queueSize);
			//Thread to consume queue and update data structures for publishing
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Mediation Statistic Data consumer Task");
					return t;
				}
			});
			executor.scheduleAtFixedRate(statisticEventQueue, 0, eventConsumerTime, TimeUnit.MILLISECONDS);
			eventExpireTime =
					SynapseConfigUtils.getGlobalTimeoutInterval() + SynapseConfigUtils.getTimeoutHandlerInterval() +
					eventConsumerTime;
			log.info("Statistics Entry Expiration time set to " + eventExpireTime + " milliseconds");

			StatisticEventProcessor.initializeCleaningThread();
			new MediationFlowController();
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Statistics is not enabled in \'synapse.properties\' file.");
			}
		}
	}

	/**
	 * Add event in to the event queue. This event will inform statistic collection to put all the flow continuable
	 * mediators before the index specified by current Index to open state.
	 *
	 * @param synCtx synapse message context.
	 */
	public static void openContinuationEvents(MessageContext synCtx) {
		if (shouldReportStatistic(synCtx)) {
			BasicStatisticDataUnit basicStatisticDataUnit = new BasicStatisticDataUnit();

			basicStatisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(synCtx, null));
			basicStatisticDataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(synCtx));

			ParentReopenEvent parentReopenEvent = new ParentReopenEvent(basicStatisticDataUnit);
			statisticEventQueue.enqueue(parentReopenEvent);
		}
	}

	/**
	 * Set message Id of the message context as statistic trace Id at the beginning of the statistic flow.
	 *
	 * @param msgCtx synapse message context.
	 */
	protected static void setStatisticsTraceId(MessageContext msgCtx) {
		if (msgCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null && msgCtx.getMessageID() != null) {
			msgCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, msgCtx.getMessageID().replace(':', '_'));
		} else if (msgCtx.getMessageID() == null) {
			log.error("Message ID is null");
		}
	}

	/**
	 * Returns true if statistics is collected in this message flow path.
	 *
	 * @param messageContext synapse message context.
	 * @return true if statistics is collected in the message flow.
	 */
	public static boolean shouldReportStatistic(MessageContext messageContext) {
		if (!isStatisticsEnabled)
			return false;

		Boolean isStatCollected =
				(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);
		Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		return (statID != null && isStatCollected != null && isStatCollected);
	}

	/**
	 * Returns whether statistics collection is enabled globally for the esb as specified in the
	 * synapse.properties file.
	 *
	 * @return true if statistics collection is enabled.
	 */

	public static boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	/**
	 * Return whether collecting payloads is enabled.
	 *
	 * @return true if need to collect payloads.
	 */
	public static boolean isCollectingPayloads() {
		return isStatisticsEnabled && isCollectingPayloads;
	}

	/**
	 * Return whether collecting message-properties is enabled.
	 *
	 * @return true if need to collect message-properties.
	 */
	public static boolean isCollectingProperties() {
		return isStatisticsEnabled && isCollectingProperties;
	}

	/**
	 * Return whether collecting statistics for all artifacts is enabled (this also needs isStatisticsEnabled)
	 *
	 * @return true if need to collect statistics for all artifacts
     */
	public static boolean isCollectingAllStatistics() {
		return isStatisticsEnabled && isCollectingAllStatistics;
	}

	/**
	 * Allow external to alter state of collecting statistics for all artifacts, during runtime
	 */
	public static void setCollectingAllStatistics(boolean state) {
		isCollectingAllStatistics = state;
		log.info("Collecting statistics for all artifacts state changed to: " + state);
	}

	/**
	 * Stops the MessageDataStore execution.
	 */
	public static void stopConsumer() {
		if (isStatisticsEnabled) {
			statisticEventQueue.setStopped();
		}
	}
}

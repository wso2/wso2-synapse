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
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.ParentReopenEvent;
import org.apache.synapse.aspects.flow.statistics.store.MessageDataStore;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RuntimeStatisticCollector receives statistic events and responsible for handling each of these events.
 */
public abstract class RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(RuntimeStatisticCollector.class);
	protected static Map<String, StatisticsEntry> runtimeStatistics = new HashMap<>();
	private static boolean isStatisticsEnabled;
	private static boolean isCollectingPayloads;
	private static boolean isCollectingProperties;
	protected static MessageDataStore messageDataStore;

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

			messageDataStore = new MessageDataStore(queueSize);
			//Thread to consume queue and update data structures for publishing
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Mediation Statistic Data consumer Task");
					return t;
				}
			});
			executor.scheduleAtFixedRate(messageDataStore, 0, 1000, TimeUnit.MILLISECONDS);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Statistics is not enabled in \'synapse.properties\' file.");
			}
		}
	}

	/**
	 * Opens statistic log for the reporting component.
	 *
	 * @param statisticDataUnit raw statistic data object.
	 */
	public static void recordStatisticsOpenEvent(StatisticDataUnit statisticDataUnit) {
		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(statisticDataUnit.getStatisticId()).createLog(statisticDataUnit);
		} else if (!(statisticDataUnit.getComponentType() == ComponentType.MEDIATOR ||
		             statisticDataUnit.getComponentType() == ComponentType.RESOURCE)) {
			//Mediators and resources can't start statistic collection for a flow.
			StatisticsEntry statisticsEntry = new StatisticsEntry(statisticDataUnit);
			runtimeStatistics.put(statisticDataUnit.getStatisticId(), statisticsEntry);
			if (log.isDebugEnabled()) {
				log.debug("Creating New Entry in Running Statistics: Current size :" + runtimeStatistics.size());
			}
		} else {
			log.error("Wrong element tried to open statistics: " + statisticDataUnit.getComponentId());
		}
	}

	/**
	 * Closes statistic collection log after finishing statistic collection for that component.
	 *
	 * @param dataUnit raw data unit containing id relevant for closing
	 * @param mode     Mode of closing GRACEFULLY_CLOSE, ATTEMPT_TO_CLOSE or FORCEFULLY_CLOSE
	 */
	public static void closeStatisticEntry(BasicStatisticDataUnit dataUnit, int mode) {

		if (runtimeStatistics.containsKey(dataUnit.getStatisticId())) {
			StatisticsEntry statisticsEntry = runtimeStatistics.get(dataUnit.getStatisticId());

			if (StatisticsConstants.GRACEFULLY_CLOSE == mode) {
				/**
				 * Ends statistics collection log for the reported statistics component.
				 */

				boolean finished = statisticsEntry.closeLog((StatisticDataUnit) dataUnit);
				if (finished) {
					endMessageFlow(dataUnit, statisticsEntry, false);
				}

			} else if (StatisticsConstants.ATTEMPT_TO_CLOSE == mode) {
				/**
				 * Check whether Statistics entry present for the message flow and if there is an entry try
				 * to finish ending statistics collection for that entry.
				 */
				endMessageFlow(dataUnit, statisticsEntry, false);

			} else if (StatisticsConstants.FORCEFULLY_CLOSE == mode) {
				/**
				 * Close the statistic log after finishing the message flow forcefully. When we try to use this method to end
				 * statistic collection for a message flow it will not consider any thing and close all the remaining logs and
				 * will send the completed statistic entry for collection.
				 */
				endMessageFlow(dataUnit, statisticsEntry, true);

			} else {
				log.error("Invalid mode for closing statistic entry");
			}
		}
	}

	/**
	 * Opens Flow Continuable mediators after callback is received for continuation call to the backend.
	 *
	 * @param basicStatisticDataUnit data unit which holds raw data
	 */
	public static void openParents(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (runtimeStatistics.containsKey(basicStatisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(basicStatisticDataUnit.getStatisticId()).openFlowContinuableMediators(
					basicStatisticDataUnit);
		}
	}

	/**
	 * End the statistics collection for the message flow. If entry is successfully completed ending
	 * its statistics collection statistics store is updated with new statistics data. Then entry
	 * is removed from the running statistic map.
	 *
	 * @param dataUnit        Statistics raw data object.
	 * @param statisticsEntry Statistic entry for the message flow.
	 * @param closeForceFully Whether to close statistics forcefully.
	 */
	private synchronized static void endMessageFlow(BasicStatisticDataUnit dataUnit, StatisticsEntry statisticsEntry,
	                                                boolean closeForceFully) {
		boolean isMessageFlowEnded = statisticsEntry.endAll(dataUnit, closeForceFully);
		if (isMessageFlowEnded) {
			if (log.isDebugEnabled()) {
				log.debug("Statistic collection is ended for the message flow with statistic " +
				          "trace Id :" + dataUnit.getStatisticId());
			}
			dataUnit.getSynapseEnvironment().getCompletedStatisticStore()
			        .putCompletedStatisticEntry(statisticsEntry.getMessageFlowLogs());
			runtimeStatistics.remove(dataUnit.getStatisticId());
		}
	}

	/**
	 * Add event in to the event queue. This event will inform statistic collection to put all the flow continuable
	 * mediators before the index specified by current Index to open state.
	 *
	 * @param synCtx synapse message context.
	 */
	public static void openContinuationEvents(MessageContext synCtx) {
		BasicStatisticDataUnit basicStatisticDataUnit = new BasicStatisticDataUnit();

		basicStatisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(synCtx, null));
		basicStatisticDataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(synCtx));

		ParentReopenEvent parentReopenEvent = new ParentReopenEvent(basicStatisticDataUnit);
		messageDataStore.enqueue(parentReopenEvent);
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
	protected static boolean shouldReportStatistic(MessageContext messageContext) {
		Boolean isStatCollected =
				(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);
		Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		return (statID != null && isStatCollected != null && isStatCollected && isStatisticsEnabled);
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
		return isStatisticsEnabled & isCollectingPayloads;
	}

	/**
	 * Return whether collecting message-properties is enabled.
	 *
	 * @return true if need to collect message-properties.
	 */
	public static boolean isCollectingProperties() {
		return isStatisticsEnabled & isCollectingProperties;
	}

	/**
	 * Stops the MessageDataStore execution.
	 */
	public static void stopConsumer() {
		if (isStatisticsEnabled) {
			messageDataStore.setStopped();
		}
	}
}

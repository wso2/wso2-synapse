/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.util.StatisticCleaningThread;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class is to process the events and build runtime-statistics map for each message flow
 */
public class StatisticEventProcessor {

	private static final Log log = LogFactory.getLog(StatisticEventProcessor.class);

	/**
	 * Map to hold statistic of current message flows in esb.
	 */
	private static Map<String, StatisticsEntry> runtimeStatistics = new HashMap<>();

	public static void initializeCleaningThread() {
		//Thread to consume queue and update data structures for publishing
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("Mediation Statistic Stale Entry Cleaning Task");
				return t;
			}
		});
		Long eventCleanTime = Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
				StatisticsConstants.FLOW_STATISTICS_EVENT_CLEAN_TIME,
				StatisticsConstants.FLOW_STATISTICS_DEFAULT_EVENT_CLEAN__INTERVAL));
		StatisticCleaningThread statisticCleaningThread = new StatisticCleaningThread(runtimeStatistics);
		executor.scheduleAtFixedRate(statisticCleaningThread, 0, eventCleanTime, TimeUnit.MILLISECONDS);
	}

	/**
	 * Opens statistic log for the reporting component.
	 *
	 * @param statisticDataUnit raw statistic data object.
	 */
	public static void openStatisticEntry(StatisticDataUnit statisticDataUnit) {
		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(statisticDataUnit.getStatisticId()).createLog(statisticDataUnit);
		} else if (!(statisticDataUnit.getComponentType() == ComponentType.MEDIATOR ||
		             statisticDataUnit.getComponentType() == ComponentType.RESOURCE)) {
			//Mediators and resources can't start statistic collection for a flow.
			if (statisticDataUnit.isIndividualStatisticCollected() && statisticDataUnit.getCurrentIndex() == 0) {
				StatisticsEntry statisticsEntry = new StatisticsEntry(statisticDataUnit);
				runtimeStatistics.put(statisticDataUnit.getStatisticId(), statisticsEntry);
				if (log.isDebugEnabled()) {
					log.debug("Creating New Entry in Running Statistics: Current size :" + runtimeStatistics.size());
				}
			} else if (statisticDataUnit.getCurrentIndex() > 0 && !statisticDataUnit.isIndividualStatisticCollected()) {
				log.error("Component: " + statisticDataUnit.getComponentId() + "Is in a middle of the statistics " +
				          "collection. But collection cannot be found collection seem to be broken.");
			} else {
				log.error("Component: " + statisticDataUnit.getComponentId() + "tried to open statistics, but its " +
				          "individual collection was not enabled");
			}
		} else {
			log.error("Wrong element tried to open statistics: " + statisticDataUnit.getComponentName());
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
	 * Opens Flow Continuable mediators after callback is received for continuation call to the backend.
	 *
	 * @param basicStatisticDataUnit data unit which holds raw data
	 */
	public static void openParents(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (runtimeStatistics.containsKey(basicStatisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(basicStatisticDataUnit.getStatisticId())
			                 .openFlowContinuableMediators(basicStatisticDataUnit);
		}
	}

	/**
	 * Registers callback information for the message flow on the corresponding StatisticsEntry.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void addCallbacks(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).addCallback(callbackDataUnit);
		}
	}

	/**
	 * Removes specified callback info from StatisticsEntry for the message flow after all the processing for that
	 * callback is ended.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void removeCallback(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).removeCallback(callbackDataUnit);
		}
	}

	/**
	 * Updates end time of the statistics logs in the StatisticsEntry after corresponding callback is removed from
	 * SynapseCallbackReceiver.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void updateForReceivedCallback(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).updateCallbackReceived(callbackDataUnit);
		}
	}

	/**
	 * Report to the StatisticsEntry that message flow encountered a Fault During Mediation.
	 *
	 * @param basicStatisticDataUnit raw statistic unit carrying statistic data
	 */
	public static void reportFault(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (runtimeStatistics.containsKey(basicStatisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(basicStatisticDataUnit.getStatisticId()).reportFault(basicStatisticDataUnit);
		}
	}

	/**
	 * Report to the StatisticsEntry that message flow encountered Asynchronous flow.
	 *
	 * @param basicStatisticDataUnit raw statistic unit carrying statistic data
	 */
	public static void reportAsynchronousExecution(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (runtimeStatistics.containsKey(basicStatisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(basicStatisticDataUnit.getStatisticId()).addAsynchronousFlow(basicStatisticDataUnit);
		}
	}
}

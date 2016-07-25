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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
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
public class StatisticEventProcessor2 {

	private static final Log log = LogFactory.getLog(StatisticEventProcessor2.class);

    private StatisticsEntry entry;

	/**
	 * Map to hold statistic of current message flows in esb.
	 */
//	private Map<String, StatisticsEntry> runtimeStatistics = new HashMap<>();

	public void initializeCleaningThread() {
		//Thread to consume queue and update data structures for publishing
//		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
//			public Thread newThread(Runnable r) {
//				Thread t = new Thread(r);
//				t.setName("Mediation Statistic Stale Entry Cleaning Task");
//				return t;
//			}
//		});
//		Long eventCleanTime = Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
//				StatisticsConstants.FLOW_STATISTICS_EVENT_CLEAN_TIME,
//				StatisticsConstants.FLOW_STATISTICS_DEFAULT_EVENT_CLEAN_INTERVAL));
//		StatisticCleaningThread statisticCleaningThread = new StatisticCleaningThread(runtimeStatistics);
//		executor.scheduleAtFixedRate(statisticCleaningThread, 0, eventCleanTime, TimeUnit.MILLISECONDS);
	}

	/**
	 * Opens statistic log for the reporting component.
	 *
	 * @param statisticDataUnit raw statistic data object.
	 */
	public void openStatisticEntry(StatisticDataUnit statisticDataUnit) {
		if (entry != null) {
			if (log.isDebugEnabled()) {
				log.debug("Reported open statistics for component: " + statisticDataUnit.getComponentName() + " for " +
				          "message flow :" + statisticDataUnit.getStatisticId());
			}
            entry.createLog(statisticDataUnit);
		} else if (!(statisticDataUnit.getComponentType() == ComponentType.MEDIATOR ||
		             statisticDataUnit.getComponentType() == ComponentType.RESOURCE)) {
			//Mediators and resources can't start statistic collection for a flow.
			if (statisticDataUnit.isIndividualStatisticCollected() && statisticDataUnit.getCurrentIndex() == 0) {
                entry = new StatisticsEntry(statisticDataUnit);
//				if (log.isDebugEnabled()) {
//					log.debug("Creating New Entry in Running Statistics: Current size :" + runtimeStatistics.size() +
//					          "|Statistic Id : " + statisticDataUnit.getStatisticId());
//				}
			} else if (statisticDataUnit.getCurrentIndex() > 0 && !statisticDataUnit.isIndividualStatisticCollected()) {
				if (log.isDebugEnabled()) {
					log.debug("Component: " + statisticDataUnit.getComponentName() + " is in a middle of the statistics " +
							"collection. But collection cannot be found and it seems to be broken. |Statistic Id : " +
							statisticDataUnit.getStatisticId());
				}
			} else {
				log.error("Component: " + statisticDataUnit.getComponentName() + " is tried to open statistics, but " +
				          "its individual collection was not enabled. |Statistic Id : " +
				          statisticDataUnit.getStatisticId());
			}
		} else {
			log.error("Component: " + statisticDataUnit.getComponentName() + " is tried to open statistics, but " +
			          "its not a statistic entry component. |Statistic Id : " + statisticDataUnit.getStatisticId());
		}
	}

	/**
	 * Closes statistic collection log after finishing statistic collection for that component.
	 *
	 * @param dataUnit raw data unit containing id relevant for closing
	 * @param mode     Mode of closing GRACEFULLY_CLOSE, ATTEMPT_TO_CLOSE or FORCEFULLY_CLOSE
	 */
	public void closeStatisticEntry(BasicStatisticDataUnit dataUnit, int mode) {
		if (log.isDebugEnabled()) {
			log.debug("Closing statistic event received for "+  dataUnit.getStatisticId());
		}
		if (entry != null) {
//			StatisticsEntry statisticsEntry = runtimeStatistics.get(dataUnit.getStatisticId());

			if (StatisticsConstants.GRACEFULLY_CLOSE == mode) {
				/**
				 * Ends statistics collection log for the reported statistics component.
				 */
				if (log.isDebugEnabled()) {
					log.debug("Closing statistic event received for component :" +
					          ((StatisticDataUnit) dataUnit).getComponentName() + " for the message flow : " +
					          dataUnit.getStatisticId());
				}
				boolean finished = entry.closeLog((StatisticDataUnit) dataUnit);
				if (finished) {
					endMessageFlow(dataUnit, entry, false);
				}

			} else if (StatisticsConstants.ATTEMPT_TO_CLOSE == mode) {
				/**
				 * Check whether Statistics entry present for the message flow and if there is an entry try
				 * to finish ending statistics collection for that entry.
				 */
				if (log.isDebugEnabled()) {
					log.debug("Trying to close statistic for " + dataUnit.getStatisticId());
				}
				endMessageFlow(dataUnit, entry, false);

			} else if (StatisticsConstants.FORCEFULLY_CLOSE == mode) {
				/**
				 * Close the statistic log after finishing the message flow forcefully. When we try to use this method to end
				 * statistic collection for a message flow it will not consider any thing and close all the remaining logs and
				 * will send the completed statistic entry for collection.
				 */
				if (log.isDebugEnabled()) {
					log.debug("Forcefully close statistic event received for " + dataUnit.getStatisticId());
				}
				endMessageFlow(dataUnit, entry, true);

			} else {
				log.error("Invalid mode for closing statistic entry |Statistic Id : " + dataUnit.getStatisticId());
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + dataUnit.getStatisticId());
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
	private synchronized void endMessageFlow(BasicStatisticDataUnit dataUnit, StatisticsEntry statisticsEntry,
	                                                boolean closeForceFully) {
		if (log.isDebugEnabled()) {
			log.debug("Checking whether message flow is ended for " + dataUnit.getStatisticId());
		}
		boolean isMessageFlowEnded = statisticsEntry.endAll(dataUnit, closeForceFully);
		if (isMessageFlowEnded) {
			if (log.isDebugEnabled()) {
				log.debug("Statistic collection is ended for the message flow with statistic " +
				          "trace Id :" + dataUnit.getStatisticId());
			}
			dataUnit.getSynapseEnvironment().getCompletedStatisticStore()
			        .putCompletedStatisticEntry(statisticsEntry.getMessageFlowLogs());
//            logEvent(statisticsEntry.getMessageFlowLogs());
//            dataUnit.getSynapseEnvironment().getStatisticsObservable().submitToMediationLayer(statisticsEntry.getMessageFlowLogs());
            entry = null;
		}
	}


    //todo this is only for test, please remove this before committing - rajith
    private static void logEvent(PublishingFlow publishingFlow) {
        Map<String, Object> mapping = publishingFlow.getObjectAsMap();
        mapping.put("host", "localhost"); // Adding host
        mapping.put("tenantId", "1234");

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(mapping);
        } catch (JsonProcessingException e) {
            log.error("Unable to convert", e);
        }
        log.info("Uncompressed data -------------------------- :" + jsonString);
    }

	/**
	 * Opens Flow Continuable mediators after callback is received for continuation call to the backend.
	 *
	 * @param basicStatisticDataUnit data unit which holds raw data
	 */
	public void openParents(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Open parent event is reported for " + basicStatisticDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.openFlowContinuableMediators(basicStatisticDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + basicStatisticDataUnit.getStatisticId());
		}
	}

	/**
	 * Registers callback information for the message flow on the corresponding StatisticsEntry.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public void addCallbacks(CallbackDataUnit callbackDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Callback registering event is reported for " + callbackDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.addCallback(callbackDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + callbackDataUnit.getStatisticId());
		}
	}

	/**
	 * Removes specified callback info from StatisticsEntry for the message flow after all the processing for that
	 * callback is ended.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public void removeCallback(CallbackDataUnit callbackDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Callback remove event is reported for " + callbackDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.removeCallback(callbackDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + callbackDataUnit.getStatisticId());
		}
	}

	/**
	 * Updates end time of the statistics logs in the StatisticsEntry after corresponding callback is removed from
	 * SynapseCallbackReceiver.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public void updateForReceivedCallback(CallbackDataUnit callbackDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Callback received event is reported for " + callbackDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.updateCallbackReceived(callbackDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + callbackDataUnit.getStatisticId());
		}
	}

	/**
	 * Report to the StatisticsEntry that message flow encountered a Fault During Mediation.
	 *
	 * @param basicStatisticDataUnit raw statistic unit carrying statistic data
	 */
	public void reportFault(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Fault Occurring is reported for " + basicStatisticDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.reportFault(basicStatisticDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + basicStatisticDataUnit.getStatisticId());
		}
	}

	/**
	 * Report to the StatisticsEntry that message flow encountered Asynchronous flow.
	 *
	 * @param basicStatisticDataUnit raw statistic unit carrying statistic data
	 */
	public void reportAsynchronousExecution(BasicStatisticDataUnit basicStatisticDataUnit) {
		if (log.isDebugEnabled()) {
			log.debug("Asynchronous execution reported for " + basicStatisticDataUnit.getStatisticId());
		}
		if (entry != null) {
            entry.addAsynchronousFlow(basicStatisticDataUnit);
		} else if (log.isDebugEnabled()) {
			log.debug(StatisticsConstants.STATISTIC_NOT_FOUND_ERROR + basicStatisticDataUnit.getStatisticId());
		}
	}
}

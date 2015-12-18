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
 *
 */

package org.apache.synapse.aspects.newstatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.newstatistics.event.reader.StatisticEventReceiver;
import org.apache.synapse.aspects.newstatistics.util.ClusterInformationProvider;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.SynapseEnvironment;

import java.util.*;

/**
 * RuntimeStatisticCollector receives statistic events and responsible for handling each of these
 * events. It holds statistic store which contains the in memory statistics for the message
 * mediation happened in the ESB.
 */
public class RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(RuntimeStatisticCollector.class);

	private static Map<String, StatisticsEntry> runningStatistics = new HashMap<String, StatisticsEntry>();

	private static boolean isStatisticsEnable = false;

	private final static String STATISTICS_ENABLE = "new.statistics.enable";

	private static String localMemberPort = null;

	private static String localMemberHost = null;

	/**
	 * Create statistic log for the the reporting component.
	 */
	public static void recordStatisticCreateEntry(StatisticDataUnit statisticDataUnit) {

		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			runningStatistics.get(statisticDataUnit.getStatisticId()).createLog(statisticDataUnit);
		} else {
			StatisticsEntry statisticsEntry = new StatisticsEntry(statisticDataUnit, localMemberHost, localMemberPort);
			runningStatistics.put(statisticDataUnit.getStatisticId(), statisticsEntry);
			if (log.isDebugEnabled()) {
				log.debug("Creating New Entry in Running Statistics: Current size :" + runningStatistics.size());
			}
		}
	}

	/**
	 * Create fault log at the start of the fault sequence.
	 */
	public static void recordStatisticCreateFaultLog(StatisticDataUnit statisticDataUnit) {
		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			runningStatistics.get(statisticDataUnit.getStatisticId()).createFaultLog(statisticDataUnit);
		}
	}

	/**
	 * Ends statistics collection log for the reported statistics component Id.
	 */
	public static void recordStatisticCloseLog(StatisticDataUnit statisticDataUnit) {

		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry statisticsEntry = runningStatistics.get(statisticDataUnit.getStatisticId());
			boolean finished = statisticsEntry.closeLog(statisticDataUnit);
			if (finished) {
				endMessageFlow(statisticDataUnit, statisticsEntry, false);
			}
		}

	}

	/**
	 * Registers callback information for the message flow on the corresponding statistics entry.
	 *
	 * @param statisticsTraceId statistic Trace ID
	 * @param callbackId        callback identification number
	 */
	public static void addCallbacks(String statisticsTraceId, String callbackId, int msgId) {

		if (statisticsTraceId != null) {
			if (runningStatistics.containsKey(statisticsTraceId)) {
				runningStatistics.get(statisticsTraceId).addCallback(callbackId, msgId);
			}
		}
	}

	/**
	 * Updates end time of the statistics logs after corresponding callback is removed from
	 * SynapseCallbackReceiver.
	 *
	 * @param statisticsTraceId message context
	 * @param callbackId        callback identification number
	 * @param endTime           callback removal time at SynapseCallbackReceiver
	 */
	public static void updateForReceivedCallback(String statisticsTraceId, String callbackId, Long endTime) {
		if (statisticsTraceId != null) {
			if (runningStatistics.containsKey(statisticsTraceId)) {
				runningStatistics.get(statisticsTraceId).updateCallbackReceived(callbackId, endTime);
			}
		}
	}

	/**
	 * Removes specified callback info for a message flow after all the processing for that
	 * callback is ended.
	 *
	 * @param statisticsTraceId message context
	 * @param callbackId        callback identification number
	 */
	public static void removeCallback(String statisticsTraceId, String callbackId) {
		if (statisticsTraceId != null) {
			if (runningStatistics.containsKey(statisticsTraceId)) {
				runningStatistics.get(statisticsTraceId).removeCallback(callbackId);
				if (log.isDebugEnabled()) {
					log.debug("Removed callback from statistic entry");
				}
			}
		}
	}

	/**
	 * Check whether Statistics entry present for the message flow and if there is an entry try
	 * to finish ending statistics collection for that entry.
	 */
	public static void finalizeEntry(StatisticDataUnit statisticDataUnit) {
		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry entry = runningStatistics.get(statisticDataUnit.getStatisticId());
			endMessageFlow(statisticDataUnit, entry, false);
		}
	}

	/**
	 * Close the statistic log after finishing the message flow forcefully. When we try to use this method to end
	 * statistic collection for a message flow it will not consider any thing and close all the remaining logs and
	 * will send the completed statistic entry for collection.
	 * <p/>
	 * * @param endTime end time of the message flow
	 */
	public static void closeStatisticEntryForcefully(StatisticDataUnit statisticDataUnit) {
		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry entry = runningStatistics.get(statisticDataUnit.getStatisticId());
			endMessageFlow(statisticDataUnit, entry, true);
		}
	}

	/**
	 * End the statistics collection for the message flow. If entry is successfully completed ending
	 * its statistics collection statistics store is updated with new statistics data. Then entry
	 * is removed from the running statistic map.
	 */
	private synchronized static void endMessageFlow(StatisticDataUnit statisticDataUnit,
	                                                StatisticsEntry statisticsEntry, boolean closeForceFully) {
		boolean isMessageFlowEnded = statisticsEntry.endAll(statisticDataUnit.getTime(), closeForceFully);
		if (isMessageFlowEnded) {
			if (log.isDebugEnabled()) {
				log.debug("Statistic collection is ended for the message flow with statistic " +
				          "trace Id :" + statisticDataUnit.getStatisticId());
			}
			//statisticsStore.update(statisticsEntry.getMessageFlowLogs());
			statisticDataUnit.getSynapseEnvironment().getCompletedStatisticStore()
			                 .putCompletedStatisticEntry(statisticsEntry.getMessageFlowLogs());
			runningStatistics.remove(statisticDataUnit.getStatisticId());
		}
	}

	//	/**
	//	 * Set statistics trace Id for statistic collection.
	//	 *
	//	 * @param msgCtx Message context
	//	 */
	public static void setStatisticsTraceId(MessageContext msgCtx) {
		if (msgCtx.getProperty(SynapseConstants.NEW_STATISTICS_ID) == null) {
			msgCtx.setProperty(SynapseConstants.NEW_STATISTICS_ID, msgCtx.getMessageID());
		}
	}
	//
	//	/**
	//	 * Returns statistics trace id corresponding to the message context.
	//	 *
	//	 * @param msgCtx Message context
	//	 * @return statistics trace id
	//	 */
	//	private static String getStatisticsTraceId(MessageContext msgCtx) {
	//		return (String) msgCtx.getProperty(SynapseConstants.NEW_STATISTICS_ID);
	//	}
	//
	//	/**
	//	 * Returns cloned message identification number for the specified  message context. If message
	//	 * context is not a cloned one default value of -1 is sent.
	//	 *
	//	 * @param msgCtx message context
	//	 * @return cloned message identification number
	//	 */
	//	private static int getMsgId(MessageContext msgCtx) {
	//		if (msgCtx.getProperty(SynapseConstants.NEW_STATISTICS_MESSAGE_ID) != null) {
	//			return (Integer) msgCtx.getProperty(SynapseConstants.NEW_STATISTICS_MESSAGE_ID);
	//		} else {
	//			return -1;
	//		}
	//	}
	//

	/**
	 * Returns clone message identification number for the next cloning message in the message flow.
	 *
	 * @param statisticTraceId statisticTraceID
	 * @return next clone message identification number
	 */
	public static int getClonedMsgNumber(String statisticTraceId) {
		//TODO remove this and collect it without contacting RuntimeStatCollector
		if (statisticTraceId != null) {
			if (runningStatistics.containsKey(statisticTraceId)) {
				return runningStatistics.get(statisticTraceId).incrementAndGetClonedMsgCount();
			}
		}
		return -1;
	}

	/**
	 * Initialize statistics collection when ESB starts. If statistic cleaning is enabled in
	 * synapse.properties file this method will schedule a timer event to clean statistics at
	 * that specified time interval.
	 *
	 * @param synapseTimer timer object for the Synapse Configuration
	 */
	public static void init(Timer synapseTimer) {
		isStatisticsEnable = Boolean.parseBoolean(
				SynapsePropertiesLoader.getPropertyValue(STATISTICS_ENABLE, String.valueOf(false)));
		if (isStatisticsEnable) {
			if (log.isDebugEnabled()) {
				log.debug("Statistics is enabled.");
			}
			ClusterInformationProvider clusterInformationProvider = new ClusterInformationProvider();
			if (clusterInformationProvider.isClusteringEnabled()) {
				localMemberHost = clusterInformationProvider.getLocalMemberHostName();
				localMemberPort = clusterInformationProvider.getLocalMemberPort();
			}
			StatisticEventReceiver.Init();
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Statistics is not enabled in \'synapse.properties\' file.");
			}
		}

	}

	/**
	 * Returns whether statistics collection is enabled globally for the esb as specified in the
	 * synapse.properties file.
	 *
	 * @return true if statistics collection is enabled
	 */
	public static boolean isStatisticsEnable() {
		return isStatisticsEnable;
	}

	public static boolean shouldReportStatistic(MessageContext messageContext) {
		Boolean isStatCollected = (Boolean) messageContext.getProperty(SynapseConstants.NEW_STATISTICS_IS_COLLECTED);
		Object statID = messageContext.getProperty(SynapseConstants.NEW_STATISTICS_ID);
		return (statID != null && isStatCollected != null && isStatCollected && isStatisticsEnable);
	}

	public static boolean isStatisticsTraced(MessageContext messageContext) {
		Object statID = messageContext.getProperty(SynapseConstants.NEW_STATISTICS_ID);
		return (statID != null && isStatisticsEnable);
	}

	public static void informCloneOperation(String statisticId) {
		if (runningStatistics.containsKey(statisticId)) {
			StatisticsEntry entry = runningStatistics.get(statisticId);
			entry.setCloneLog(true);
		}
	}

	public static void informAggregateFinishOperation(StatisticDataUnit statisticDataUnit) {
		if (runningStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry entry = runningStatistics.get(statisticDataUnit.getStatisticId());
			entry.endHaveAggregateLogs();
			RuntimeStatisticCollector.finalizeEntry(statisticDataUnit);
		}
	}
}

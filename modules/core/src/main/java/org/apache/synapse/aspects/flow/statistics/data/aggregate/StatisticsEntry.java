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

package org.apache.synapse.aspects.flow.statistics.data.aggregate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingPayload;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.aspects.flow.statistics.util.TracingDataCollectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StatisticsEntry collects all the statistics logs related to a message flow. It is responsible
 * for collecting statistics logs in correct hierarchy so that these logs can be directly fed
 * into the statistic store as inputs.
 */
public class StatisticsEntry {

	private static final Log log = LogFactory.getLog(StatisticsEntry.class);

	/**
	 * List to hold all the statistic logs related to the message flow
	 */
	private final List<StatisticsLog> messageFlowLogs = new ArrayList<>();

	/**
	 * Map to hold all the remaining callbacks related to the message flow
	 */
	private final Set<String> callbacks = new HashSet<>();

	/**
	 * Map to hold all the opened statistic logs related to the message flow
	 */
	private final Set<Integer> openLogs = new HashSet<>();

	/**
	 * Expected number of faults in this message flow. This can increase if message flow split in to several flow by
	 * cloning or iteration.
	 */
	private int expectedFaults = 0;

	/**
	 * Whether this message flow has encountered a fault.
	 */
	private boolean hasFault;

	/**
	 * Index of first StatisticLog for the message flow
	 */
	private static final int ROOT_LEVEL = 0;

	/**
	 *Data structure using to serialize thr statistic data while publishing.
	 */
	private PublishingFlow publishingFlow = new PublishingFlow();

	/**
	 * payload map which contains all the payloads of the message flow.
	 */
	private Map<String, PublishingPayload> payloadMap = new HashMap<>();

	/**
	 * Is aspect configuration is enabled for tracing
	 */
	private boolean aspectConfigTraceEnabled = true;

	/**
	 * This overloaded constructor will create the root statistic log og the Statistic Entry
	 * according to given parameters. Statistic Event for creating statistic entry can be either
	 * PROXY, API, INBOUND_ENDPOINT, ENDPOINT or SEQUENCE
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 */
	public StatisticsEntry(StatisticDataUnit statisticDataUnit) {

		if (statisticDataUnit.isIndividualStatisticCollected()) {
			StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit);
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
			openLogs.add(statisticDataUnit.getCurrentIndex());
			if (log.isDebugEnabled()) {
				log.debug("Created statistic Entry for [ElementId|" + statisticDataUnit.getComponentName());
			}
		} else {
			//create imaginary root
			StatisticsLog statisticsLog =
					new StatisticsLog(ComponentType.IMAGINARY, StatisticsConstants.IMAGINARY_COMPONENT_ID,
					                  StatisticsConstants.DEFAULT_PARENT_INDEX);
			if (log.isDebugEnabled()) {
				log.debug("Created imaginary root for [ElementId|" + statisticDataUnit.getComponentName());
			}
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
			openLogs.add(statisticDataUnit.getCurrentIndex());
			createLog(statisticDataUnit);
		}
	}

	/**
	 * Create statistics log for a statistic reporting element.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 */
	public synchronized void createLog(StatisticDataUnit statisticDataUnit) {

		if (openLogs.contains(ROOT_LEVEL) && openLogs.size() == 1) {
			if (messageFlowLogs.get(0).getComponentType() == ComponentType.IMAGINARY) {
				if (!statisticDataUnit.isIndividualStatisticCollected()) {
					return;//because if imaginary root is there it means that this is not whole collection
				}
			}
		}

		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit);

		if (hasFault) {
			hasFault = false;
		}

		//Filling the gaps
		if (statisticDataUnit.getCurrentIndex() > messageFlowLogs.size()) {
			for (int i = messageFlowLogs.size(); i < statisticDataUnit.getCurrentIndex(); i++) {
				messageFlowLogs.add(null);
			}
		}

		if (messageFlowLogs.get(statisticDataUnit.getParentIndex()).isFlowSplittingMediator()) {
			statisticsLog.setParentIndex(statisticDataUnit.getParentIndex());
			expectedFaults += 1;
		} else {
			statisticsLog.setParentIndex(getParent(statisticDataUnit.getParentIndex()));
		}

		if (statisticDataUnit.getCurrentIndex() < messageFlowLogs.size()) {
			messageFlowLogs.set(statisticDataUnit.getCurrentIndex(), statisticsLog);
		} else {
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
		}

		if (statisticDataUnit.getParentList() != null && !statisticDataUnit.getParentList().isEmpty()) {
			for (int parent : statisticDataUnit.getParentList()) {
				messageFlowLogs.get(parent).setChildren(statisticDataUnit.getCurrentIndex());
			}
		} else {
			messageFlowLogs.get(statisticDataUnit.getParentIndex()).setChildren(statisticDataUnit.getCurrentIndex());
		}

		openLogs.add(statisticDataUnit.getCurrentIndex());
	}

	public synchronized void reportFault(BasicStatisticDataUnit basicStatisticDataUnit) {
		hasFault = true;
		addFaultsToParents(basicStatisticDataUnit.getCurrentIndex());
	}

	/**
	 * Close a opened statistics log after all the statistics collection relating to that statistics
	 * component is ended.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 * @return true if there are no open message logs in openLogs List
	 */
	public synchronized boolean closeLog(StatisticDataUnit statisticDataUnit) {
		if (statisticDataUnit.getCurrentIndex() == 0) {
			return true;
		} else {
			int currentIndex = statisticDataUnit.getCurrentIndex();
			if (statisticDataUnit.isShouldTrackParent()) {
				Integer index =
						getFirstOpenParent(statisticDataUnit.getCurrentIndex(), statisticDataUnit.getComponentName());
				if (index != null) {
					currentIndex = index;
				}
			}
			closeStatisticLog(currentIndex, statisticDataUnit.getTime(), statisticDataUnit.getPayload());
			if (messageFlowLogs.get(currentIndex).isFlowAggregateMediator()) {
				expectedFaults -= 1;
			}
			openLogs.remove(currentIndex);
		}
		return openLogs.isEmpty();
	}

	/**
	 * Close the remaining statistic logs after finishing all the message contexts of requests and
	 * responses belonging to a message flow.
	 *
	 * @param basicStatisticDataUnit raw statistic data unit
	 * @param closeForcefully        should we finish the statistics forcefully without considering anything
	 * @return true if message flow correctly ended
	 */
	public synchronized boolean endAll(BasicStatisticDataUnit basicStatisticDataUnit, boolean closeForcefully) {
		if (closeForcefully) {
			expectedFaults -= 1;
		}
		long endTime;
		if (basicStatisticDataUnit.isOutOnlyFlow()) {
			endTime = messageFlowLogs.get(messageFlowLogs.size() - 1).getEndTime();
		} else {
			endTime = basicStatisticDataUnit.getTime();
		}
		if ((callbacks.isEmpty() && (openLogs.size() <= 1)) && (expectedFaults <= 0) ||
		    (closeForcefully && (expectedFaults <= 0))) {
			if (openLogs.isEmpty()) {
				messageFlowLogs.get(ROOT_LEVEL).setEndTime(endTime);
			} else {
				for (Integer index : openLogs) {
					messageFlowLogs.get(index).setEndTime(endTime);
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("Closed all logs after message flow ended.");
			}
			return true;
		}
		return false;
	}

	/**
	 * Add a callback reference to this message flow.
	 *
	 * @param callbackDataUnit raw statistic data unit carrying callback data
	 */
	public void addCallback(CallbackDataUnit callbackDataUnit) {
		callbacks.add(callbackDataUnit.getCallbackId());
	}

	/**
	 * Removes the callback entry from the callback map belonging to this entry message flow.
	 *
	 * @param callbackDataUnit raw statistic data unit carrying callback data
	 */
	public void removeCallback(CallbackDataUnit callbackDataUnit) {
		if (callbacks.remove(callbackDataUnit.getCallbackId())) {
			if (log.isDebugEnabled()) {
				log.debug("Callback removed for the received Id:" + callbackDataUnit.getCallbackId());
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No callback entry found for the callback id.");
			}
		}
	}

	/**
	 * Updates the StatisticLogs after an response for that callback is received.
	 *
	 * @param callbackDataUnit raw statistic data unit carrying callback data.
	 */
	public synchronized void updateCallbackReceived(CallbackDataUnit callbackDataUnit) {
		if (callbacks.contains(callbackDataUnit.getCallbackId())) {
			if (!callbackDataUnit.isOutOnlyFlow()) {
				updateParentLogs(callbackDataUnit.getCurrentIndex(), callbackDataUnit.getTime());
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No stored callback information found in statistic trace.");
			}
		}
	}

	/**
	 * Set flow continuable mediators in parent path to open state. This is used when there is a continuation call.
	 *
	 * @param basicStatisticDataUnit raw statistic data unit
	 */
	public void openFlowContinuableMediators(BasicStatisticDataUnit basicStatisticDataUnit) {
		StatisticsLog statisticsLog = messageFlowLogs.get(basicStatisticDataUnit.getCurrentIndex());
		while (statisticsLog.getCurrentIndex() > 0) {
			if (statisticsLog.isFlowContinuable()) {
				statisticsLog.incrementOpenTimes();
				openLogs.add(statisticsLog.getCurrentIndex()); //We will not open continuation logs as it might lead
			}
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentIndex());
		}
	}

	/**
	 * Returns collected message flows after message flow is ended.
	 *
	 * @return Message flow logs of the message flow
	 */
	public PublishingFlow getMessageFlowLogs() {

		return TracingDataCollectionHelper.createPublishingFlow(this.messageFlowLogs);
	}

	/**
	 * Get parent for this reporting element. This is needed as reporting elements actual parent is not its actual
	 * parent.
	 *
	 * @param parentIndex index of the previous statistic reporting element.
	 * @return parent for the reporting element.
	 */
	private int getParent(int parentIndex) {
		StatisticsLog statisticsLog = messageFlowLogs.get(parentIndex);
		while (!statisticsLog.isOpenLog()) {
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentIndex());
		}
		return statisticsLog.getCurrentIndex();
	}

	/**
	 * After receiving a fault increment fault count of the statistics logs from its parent
	 * to the root log to maintain correct fault hierarchy.
	 *
	 * @param parentIndexOfFault parent Index of the fault log
	 */
	private void addFaultsToParents(int parentIndexOfFault) {
		while (parentIndexOfFault > StatisticsConstants.DEFAULT_PARENT_INDEX) {
			StatisticsLog updatingLog = messageFlowLogs.get(parentIndexOfFault);
			updatingLog.incrementNoOfFaults();
			parentIndexOfFault = updatingLog.getParentIndex();
		}
	}

	/**
	 * Updates parent logs from the specified element after an notification is received. It updates
	 * all the ended parent logs from specified index.
	 *
	 * @param closedIndex child index in the messageFlowLogs Array List
	 * @param endTime     end time of the child
	 */
	private void updateParentLogs(int closedIndex, Long endTime) {
		if (closedIndex > -1) {
			do {
				StatisticsLog updatingLog = messageFlowLogs.get(closedIndex);
				updatingLog.setEndTime(endTime);
				closedIndex = updatingLog.getParentIndex();
			} while (closedIndex > StatisticsConstants.DEFAULT_PARENT_INDEX);

			if (log.isDebugEnabled()) {
				log.debug("Statistic Log updating finished.");
			}
		}
	}

	/**
	 * Closes opened statistics log specified by the componentIndex.
	 *
	 * @param componentIndex index of the closing statistic log in messageFlowLogs
	 * @param endTime        endTime of the closing statistics log
	 * @param payload        payload of the messageContext
	 */

	private void closeStatisticLog(int componentIndex, Long endTime, String payload) {
		StatisticsLog currentLog = messageFlowLogs.get(componentIndex);
		currentLog.decrementOpenTimes();
		if (log.isDebugEnabled()) {
			log.debug("Closed statistic log of [ElementId" + currentLog.getComponentName());
		}
		currentLog.setEndTime(endTime);
		// TODO: add after payload
		currentLog.setAfterPayload(payload);
		updateParentLogs(currentLog.getParentIndex(), endTime);
	}

	/**
	 * Returns index of the StatisticLog which in the open state which matches with the componentId specified.
	 *
	 * @param currentIndex current position in the message flow
	 * @param componentId  name of the statistic reporting component
	 * @return index of the matching StatisticLog
	 */
	private Integer getFirstOpenParent(int currentIndex, String componentId) {
		StatisticsLog statisticsLog = messageFlowLogs.get(messageFlowLogs.get(currentIndex).getParentIndex());
		while (statisticsLog.getCurrentIndex() > 0) {
			if (statisticsLog.isOpenLog() && statisticsLog.getComponentName().equals(componentId)) {
				return statisticsLog.getCurrentIndex();
			}
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentIndex());
		}
		return null;
	}
}
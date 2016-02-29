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
import org.apache.synapse.aspects.flow.statistics.util.ContinuationStateHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

import java.util.*;

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
	private final Map<String, Integer> callbacks = new HashMap<>();

	/**
	 * Map to hold continuation call details of the message flow
	 */
	private final Map<String, ContinuationStateHolder> continuationStateMap = new HashMap<>();

	/**
	 * Map to hold all the opened statistic logs related to the message flow
	 */
	private final HashSet<Integer> openLogs = new HashSet<>();

	private int expectedFaults = 0;

	private boolean hasFault;

	private int offset = 0; //To use when imaginary present

	private static final int PARENT_LEVEL_OF_ROOT = -1;

	private static final int ROOT_LEVEL = 0;

	private PublishingFlow publishingFlow = new PublishingFlow();

	private Map<String, PublishingPayload> payloadMap = new HashMap<>();

	private boolean aspectConfigTraceEnabled = true; // set this accordingly

	/**
	 * This overloaded constructor will create the root statistic log og the Statistic Entry
	 * according to given parameters. Statistic Event for creating statistic entry can be either
	 * PROXY, API or SEQUENCE.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 */
	public StatisticsEntry(StatisticDataUnit statisticDataUnit) {

		if (statisticDataUnit.isIndividualStatisticCollected()) {
			StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit);
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
			openLogs.add(statisticDataUnit.getCurrentIndex());
			if (log.isDebugEnabled()) {
				log.debug(
						"Created statistic Entry [Start|RootElement]|[ElementId|" + statisticDataUnit.getComponentId() +
						"]|[MsgId|" + statisticDataUnit.getFlowId() + "].");
			}
		} else {
			//create imaginary root
			StatisticsLog statisticsLog =
					new StatisticsLog(ComponentType.IMAGINARY, StatisticsConstants.IMAGINARY_COMPONENT_ID,
					                  StatisticsConstants.DEFAULT_MSG_ID, PARENT_LEVEL_OF_ROOT);
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
			openLogs.add(statisticDataUnit.getCurrentIndex());
			createLog(statisticDataUnit);

		}
	}

	/**
	 * Create statistics log at the start of a statistic reporting element.
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

		if (hasFault) {
			hasFault = false;
		}

		//Filling the gaps
		if (statisticDataUnit.getCurrentIndex() > messageFlowLogs.size()) {
			for (int i = messageFlowLogs.size(); i < statisticDataUnit.getCurrentIndex(); i++) {
				messageFlowLogs.add(null);
			}
		}
		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit);
		if (messageFlowLogs.get(statisticDataUnit.getParentIndex()).isFlowSplittingMediator()) {
			statisticsLog.setParentLevel(statisticDataUnit.getParentIndex());
			expectedFaults += 1;
		} else {
			statisticsLog.setParentLevel(getParent(statisticDataUnit.getParentIndex()));
		}
		if (statisticDataUnit.getCurrentIndex() < messageFlowLogs.size()) {
			messageFlowLogs.set(statisticDataUnit.getCurrentIndex(), statisticsLog);
		} else {
			messageFlowLogs.add(statisticDataUnit.getCurrentIndex(), statisticsLog);
		}
		openLogs.add(statisticDataUnit.getCurrentIndex());

		if (statisticDataUnit.getParentList() != null && !statisticDataUnit.getParentList().isEmpty()) {
			for (int parent : statisticDataUnit.getParentList()) {
				messageFlowLogs.get(parent).setChildren(statisticDataUnit.getCurrentIndex());
			}
		} else {
			messageFlowLogs.get(statisticDataUnit.getParentIndex()).setChildren(statisticDataUnit.getCurrentIndex());
		}
		//			expectedFaults += 1; when there is aggregates
	}

	private int getParent(int parentIndex) {
		StatisticsLog statisticsLog = messageFlowLogs.get(parentIndex);
		while (!statisticsLog.isOpenLog()) {
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentLevel());
		}
		return statisticsLog.getCurrentIndex();
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
			if (statisticDataUnit.isShouldBackpackParent()) {
				Integer index =
						getFirstOpenParent(statisticDataUnit.getCurrentIndex(), statisticDataUnit.getComponentId());
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

	private Integer getFirstOpenParent(int currentIndex, String componentId) {
		StatisticsLog statisticsLog = messageFlowLogs.get(messageFlowLogs.get(currentIndex).getParentLevel());
		while (statisticsLog.getCurrentIndex() > 0) {
			if (statisticsLog.isOpenLog() && statisticsLog.getComponentId().equals(componentId)) {
				return statisticsLog.getCurrentIndex();
			}
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentLevel());
		}
		return null;
	}

	/**
	 * Closes opened statistics log specified by the componentLevel.
	 *
	 * @param componentLevel index of the closing statistic log in messageFlowLogs
	 * @param endTime        endTime of the closing statistics log
	 */

	private void closeStatisticLog(int componentLevel, Long endTime, String payload) {
		StatisticsLog currentLog = messageFlowLogs.get(componentLevel);
		currentLog.decrementOpenTimes();
		if (log.isDebugEnabled()) {
			log.debug("Closed statistic log of [ElementId" + currentLog.getComponentId() +
			          "][MsgId" + currentLog.getParentMsgId());
		}
		currentLog.setEndTime(endTime);
		// TODO: add after payload
		currentLog.setAfterPayload(payload);
		updateParentLogs(currentLog.getParentLevel(), endTime);
	}

	/**
	 * Close the remaining statistic logs after finishing all the message contexts of requests and
	 * responses belonging to a message flow.
	 *
	 * @param endTime         endTime of the message flow
	 * @param closeForcefully should we finish the statistics forcefully without considering anything
	 * @return true if message flow correctly ended
	 */
	public synchronized boolean endAll(long endTime, boolean closeForcefully) {
		if (closeForcefully) {
			expectedFaults -= 1;
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
	 * Adds a callback entry for this message flow.
	 */
	public void addCallback(CallbackDataUnit callbackDataUnit) {
		callbacks.put(callbackDataUnit.getCallbackId(), callbackDataUnit.getCurrentIndex());
	}

	/**
	 * Removes the callback entry from the callback map belonging to this entry message flow.
	 *
	 * @param callbackId callback id
	 */
	public void removeCallback(String callbackId) {
		if (callbacks.containsKey(callbackId)) {
			callbacks.remove(callbackId);
			if (log.isDebugEnabled()) {
				log.debug("Callback removed for the received Id:" + callbackId);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No callback entry found for the callback id.");
			}
		}
	}

	/**
	 * Updates the ArrayList after an response for that callback is received.
	 */
	public synchronized void updateCallbackReceived(CallbackDataUnit callbackDataUnit) {
		if (callbacks.containsKey(callbackDataUnit.getCallbackId())) {
			int closedIndex = callbacks.get(callbackDataUnit.getCallbackId());
			if (!callbackDataUnit.isOutOnlyFlow()) {
				updateParentLogs(closedIndex, callbackDataUnit.getTime());
			}
			//			if (callbackDataUnit.getIsContinuationCall()) {
			//				//Open all the flow continuable things
			//				openFlowContinuable(closedIndex);
			//			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No stored callback information found in statistic trace.");
			}
		}
	}

	public void openFlowContinuable(BasicStatisticDataUnit basicStatisticDataUnit) {
		StatisticsLog statisticsLog = messageFlowLogs.get(basicStatisticDataUnit.getCurrentIndex());
		while (statisticsLog.getCurrentIndex() > 0) {
			if (statisticsLog.isFlowContinuable()) {
				statisticsLog.incrementOpenTimes();
				openLogs.add(statisticsLog.getCurrentIndex()); //We will not open continuation logs as it might lead
			}
			statisticsLog = messageFlowLogs.get(statisticsLog.getParentLevel());
		}
	}

	/**
	 * Removes the message Id entry from the continuation map belonging to this message flow.
	 *
	 * @param messageId message Id
	 */
	public void removeContinuationEntry(String messageId) {
		if (continuationStateMap.containsKey(messageId)) {
			continuationStateMap.remove(messageId);
			if (log.isDebugEnabled()) {
				log.debug("Continuation state removed for the received Id:" + messageId);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No Continuation state entry found for the Id:" + messageId);
			}
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
			//if log is closed end time will be different than -1
			do {
				StatisticsLog updatingLog = messageFlowLogs.get(closedIndex);
				updatingLog.setEndTime(endTime);
				closedIndex = updatingLog.getParentLevel();
			} while (closedIndex > PARENT_LEVEL_OF_ROOT);

			if (log.isDebugEnabled()) {
				log.debug("Log updating finished.");
			}
		}
	}

	/**
	 * After receiving a fault increment fault count of the statistics logs from its parent
	 * to the root log to maintain correct fault hierarchy.
	 *
	 * @param parentIndexOfFault parent Index of the fault log
	 */
	private void addFaultsToParents(int parentIndexOfFault) {
		while (parentIndexOfFault > PARENT_LEVEL_OF_ROOT) {
			StatisticsLog updatingLog = messageFlowLogs.get(parentIndexOfFault);
			updatingLog.incrementNoOfFaults();
			parentIndexOfFault = updatingLog.getParentLevel();
		}
	}

	/**
	 * Returns collected message flows after message flow is ended.
	 *
	 * @return Message flow logs of the message flow
	 */
	public PublishingFlow getMessageFlowLogs() {

		//		if (messageFlowLogs.get(0).getComponentType() == ComponentType.IMAGINARY) {
		//			StatisticsLog statisticsLog = messageFlowLogs.remove(0);
		//			messageFlowLogs.get(0).setMessageFlowId(statisticsLog.getMessageFlowId());
		//			for (StatisticsLog log : messageFlowLogs) {
		//				log.decrementParentLevel();
		//				log.decrementChildren();
		//			}
		//		}
		//
		//		String entryPoint = messageFlowLogs.get(0).getComponentId();
		//		String flowId = messageFlowLogs.get(0).getMessageFlowId();
		//
		//		for (int index = 0; index < messageFlowLogs.size(); index++) {
		//			StatisticsLog currentStatLog = messageFlowLogs.get(index);
		//
		//			// Add each event to Publishing Flow
		//			this.publishingFlow.addEvent(new PublishingEvent(currentStatLog, entryPoint));
		//
		//			// Skip the rest of things, if message tracing is disabled
		//			if (!RuntimeStatisticCollector.isCollectingPayloads() || !aspectConfigTraceEnabled) {
		//				continue;
		//			}
		//
		//			if (currentStatLog.getBeforePayload() != null && currentStatLog.getAfterPayload() == null) {
		//				currentStatLog.setAfterPayload(currentStatLog.getBeforePayload());
		//			}
		//
		//			if (currentStatLog.getBeforePayload() == null) {
		//				int parentIndex = currentStatLog.getParentLevel();
		//				StatisticsLog parentStatLog = messageFlowLogs.get(parentIndex);
		//
		//				if (parentStatLog.getAfterPayload().startsWith("#REFER:")) {
		//					// Parent also referring to after-payload
		//					currentStatLog.setBeforePayload(parentStatLog.getAfterPayload());
		//					currentStatLog.setAfterPayload(parentStatLog.getAfterPayload());
		//
		//					String referringIndex = parentStatLog.getAfterPayload().split(":")[1];
		//
		//					this.payloadMap.get("after-" + referringIndex)
		//					               .addEvent(new PublishingPayloadEvent(index, "beforePayload"));
		//					this.payloadMap.get("after-" + referringIndex)
		//					               .addEvent(new PublishingPayloadEvent(index, "afterPayload"));
		//
		//				} else {
		//					// Create a new after-payload reference
		//					currentStatLog.setBeforePayload("#REFER:" + parentIndex);
		//					currentStatLog.setAfterPayload("#REFER:" + parentIndex);
		//
		//					this.payloadMap.get("after-" + parentIndex)
		//					               .addEvent(new PublishingPayloadEvent(index, "beforePayload"));
		//					this.payloadMap.get("after-" + parentIndex)
		//					               .addEvent(new PublishingPayloadEvent(index, "afterPayload"));
		//				}
		//
		//			} else {
		//
		//				// For content altering components
		//				PublishingPayload publishingPayloadBefore = new PublishingPayload();
		//				publishingPayloadBefore.setPayload(currentStatLog.getBeforePayload());
		//				publishingPayloadBefore.addEvent(new PublishingPayloadEvent(index, "beforePayload"));
		//				this.payloadMap.put("before-" + index, publishingPayloadBefore);
		//
		//				PublishingPayload publishingPayloadAfter = new PublishingPayload();
		//				publishingPayloadAfter.setPayload(currentStatLog.getAfterPayload());
		//				publishingPayloadAfter.addEvent(new PublishingPayloadEvent(index, "afterPayload"));
		//				this.payloadMap.put("after-" + index, publishingPayloadAfter);
		//
		//			}
		//
		//		}
		//
		//		this.publishingFlow.setMessageFlowId(flowId);
		//		// Move all payloads to publishingFlow object
		//		this.publishingFlow.setPayloads(this.payloadMap.values());

		return this.publishingFlow;
	}

	/**
	 * Returns collected message flows after message flow is ended.
	 *
	 * @return Message flow logs of the message flow
	 */
	public List<StatisticsLog> getMessageFlowLogsForStatisticTesting() {
		if (messageFlowLogs.get(0).getComponentType() == ComponentType.IMAGINARY) {
			StatisticsLog statisticsLog = messageFlowLogs.remove(0);
			messageFlowLogs.get(0).setMessageFlowId(statisticsLog.getMessageFlowId());
			for (StatisticsLog log : messageFlowLogs) {
				log.decrementParentLevel();
				log.decrementChildren();
			}
		}
		return messageFlowLogs;
	}
}
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
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;

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
	 * LocalMemberHost to use if this is a clustered environment
	 */
	private String localMemberHost = null;

	/**
	 * LocalMemberPort if this is a clustered environment
	 */
	private String localMemberPort = null;

	/**
	 * Map to hold all the remaining callbacks related to the message flow
	 */
	private final Map<String, Integer> callbacks = new HashMap<>();

	/**
	 * Map to hold all the opened statistic logs related to the message flow
	 */
	private final LinkedList<Integer> openLogs = new LinkedList<>();

	private boolean haveAggregateLogs;

	private boolean hasFault;

	/**
	 * This overloaded constructor will create the root statistic log og the Statistic Entry
	 * according to given parameters. Statistic Event for creating statistic entry can be either
	 * PROXY, API or SEQUENCE.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 * @param localMemberHost   localMemberHost in the cluster
	 * @param localMemberPort   localMemberPort in the cluster
	 */
	public StatisticsEntry(StatisticDataUnit statisticDataUnit, String localMemberHost, String localMemberPort) {
		this.localMemberHost = localMemberHost;
		this.localMemberPort = localMemberPort;
		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, 0, -1);
		messageFlowLogs.add(statisticsLog);
		openLogs.addFirst(messageFlowLogs.size() - 1);
		if (log.isDebugEnabled()) {
			log.debug("Created statistic Entry [Start|RootElement]|[ElementId|" + statisticDataUnit.getComponentId() +
			          "]|[MsgId|" + statisticDataUnit.getCloneId() + "].");
		}
	}

	/**
	 * Create statistics log at the start of a statistic reporting element.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 */
	public synchronized void createLog(StatisticDataUnit statisticDataUnit) {
		if (openLogs.isEmpty()) {
			statisticDataUnit.setParentId(null);
			StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, -1, -1);
			messageFlowLogs.add(statisticsLog);
			openLogs.addFirst(messageFlowLogs.size() - 1);
			if (log.isDebugEnabled()) {
				log.debug("Starting statistic log at root level [ElementId|" + statisticDataUnit.getComponentId() +
				          "]|[MsgId|" + statisticDataUnit.getCloneId() + "].");
			}
		} else {
			if (openLogs.getFirst() == 0) {
				if (messageFlowLogs.get(0).getComponentId().equals(statisticDataUnit.getComponentId())) {
					if (log.isDebugEnabled()) {
						log.debug("Statistics event is ignored as it is a duplicate of root element.");
					}
					return;
				}
			}
			if (!haveAggregateLogs && statisticDataUnit.isAggregatePoint()) {
				haveAggregateLogs = true;
			}

			int parentIndex;
			if (isCloneFlow(statisticDataUnit.getCloneId())) {
				parentIndex = getImmediateCloneIndex();
				createNewCloneLog(statisticDataUnit, parentIndex);
			} else if (haveAggregateLogs) {
				if (statisticDataUnit.isAggregatePoint()) {
					parentIndex = getParentForAggregateOperation(statisticDataUnit.getCloneId());
					Integer aggregateIndex = getImmediateAggregateIndex();
					if (aggregateIndex == null) {
						createNewLog(statisticDataUnit, parentIndex);
					} else {
						messageFlowLogs.get(parentIndex).setImmediateChild(aggregateIndex);
					}
				} else {
					parentIndex = getParentForNormalOperation(statisticDataUnit.getCloneId());
					createNewLog(statisticDataUnit, parentIndex);
				}
			} else {
				parentIndex = getParentForNormalOperation(statisticDataUnit.getCloneId());
				createNewLog(statisticDataUnit, parentIndex);
			}
		}
	}

	public synchronized void reportFault(int cloneId) {
		if (!hasFault) {
			hasFault = true;
			int parentIndex = getParentFormOpenLogs(cloneId);
			addFaultsToParents(parentIndex);
		}
	}

	/**
	 * Close a opened statistics log after all the statistics collection relating to that statistics
	 * component is ended.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 * @return true if there are no open message logs in openLogs List
	 */
	public synchronized boolean closeLog(StatisticDataUnit statisticDataUnit) {
		int componentLevel;

		if (haveAggregateLogs && statisticDataUnit.isAggregatePoint()) {
			haveAggregateLogs = false;
		}
		if (statisticDataUnit.getParentId() == null) {
			componentLevel =
					deleteAndGetComponentIndex(statisticDataUnit.getComponentId(), statisticDataUnit.getCloneId());
		} else {
			componentLevel =
					deleteAndGetComponentIndex(statisticDataUnit.getComponentId(), statisticDataUnit.getParentId(),
					                           statisticDataUnit.getCloneId());
		}
		//not closing the root statistic log as it will be closed be endAll method
		if (componentLevel > 0) {
			closeStatisticLog(componentLevel, statisticDataUnit.getTime());
		}
		return openLogs.isEmpty();
	}

	/**
	 * Closes opened statistics log specified by the componentLevel.
	 *
	 * @param componentLevel index of the closing statistic log in messageFlowLogs
	 * @param endTime        endTime of the closing statistics log
	 */

	private void closeStatisticLog(int componentLevel, Long endTime) {
		StatisticsLog currentLog = messageFlowLogs.get(componentLevel);
		if (log.isDebugEnabled()) {
			log.debug("Closed statistic log of [ElementId" + currentLog.getComponentId() +
			          "][MsgId" + currentLog.getParentMsgId());
		}
		currentLog.setEndTime(endTime);
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
		/*
	  Number of faults waiting to be handled by a fault sequence
	 */
		if ((callbacks.isEmpty() && (openLogs.size() <= 1)) && !haveAggregateLogs || closeForcefully) {
			if (openLogs.isEmpty()) {
				messageFlowLogs.get(0).setEndTime(endTime);
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
	 * Create a new statistics log for the reported statistic event for given parameters.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 * @param parentIndex       parentIndex of the statistic log
	 */
	private void createNewLog(StatisticDataUnit statisticDataUnit, int parentIndex) {
		StatisticsLog parentLog = messageFlowLogs.get(parentIndex);

		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, parentLog.getMsgId(), parentIndex);

		Integer immediateParentFormMessageLogs = getImmediateParentFormMessageLogs(statisticsLog.getMsgId());

		if (immediateParentFormMessageLogs == null) {
			immediateParentFormMessageLogs = parentIndex;
		}

		StatisticsLog possibleParent = messageFlowLogs.get(immediateParentFormMessageLogs);
		Integer lastAggregateIndex = getImmediateAggregateIndex();
		StatisticsLog lastAggregateLog = null;
		if (lastAggregateIndex != null) {
			lastAggregateLog = messageFlowLogs.get(getImmediateAggregateIndex());
		}

		if (possibleParent.getImmediateChild() == null) {
			if (possibleParent.getChildren().size() == 0) {
				possibleParent.setImmediateChild(messageFlowLogs.size());
			} else {
				if (lastAggregateLog != null && lastAggregateLog.getImmediateChild() == null) {
					lastAggregateLog.setImmediateChild(messageFlowLogs.size());
					lastAggregateLog.setMsgId(statisticsLog.getMsgId());
				} else {
					log.error("Trying to set branching tree for non clone ComponentId:" +
					          statisticDataUnit.getComponentId());
					possibleParent.setChildren(messageFlowLogs.size());
				}
			}
		} else {
			if (lastAggregateLog != null && lastAggregateLog.getImmediateChild() == null) {
				lastAggregateLog.setImmediateChild(messageFlowLogs.size());
				lastAggregateLog.setMsgId(statisticsLog.getMsgId());
			} else {
				if (possibleParent.getChildren().size() == 0) {
					possibleParent.setChildren(possibleParent.getImmediateChild());
					possibleParent.setImmediateChild(null);
					possibleParent.setChildren(messageFlowLogs.size());
					log.error("Setting immediate child of the component:" + possibleParent.getComponentId() +
					          " as branching child");
				} else {
					log.error("Unexpected unrecoverable error happened during statistics collection");
				}
			}
		}

		messageFlowLogs.add(statisticsLog);
		openLogs.addFirst(messageFlowLogs.size() - 1);
		if (log.isDebugEnabled()) {
			log.debug("Created statistic log for [ElementId|" + statisticDataUnit.getComponentId() + "]|[MsgId|" +
			          statisticDataUnit.getCloneId() + "]");
		}
	}

	private void createNewCloneLog(StatisticDataUnit statisticDataUnit, int parentIndex) {
		StatisticsLog parentLog = messageFlowLogs.get(parentIndex);
		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, parentLog.getMsgId(), parentIndex);
		messageFlowLogs.add(statisticsLog);
		parentLog.setChildren(messageFlowLogs.size() - 1);
		openLogs.addFirst(messageFlowLogs.size() - 1);
		if (log.isDebugEnabled()) {
			log.debug("Created statistic log for [ElementId|" + statisticDataUnit.getComponentId() + "]|[MsgId|" +
			          statisticDataUnit.getCloneId() + "]");
		}
	}

	/**
	 * Adds a callback entry for this message flow.
	 *
	 * @param callbackId callback id
	 * @param msgId      message id of the message context belonging to this callback
	 */
	public void addCallback(String callbackId, int msgId) {
		//id simple
		int callbackIndex = getFirstLogWithMsgId(msgId, false);
		callbacks.put(callbackId, callbackIndex);
		if (log.isDebugEnabled()) {
			log.debug("Callback stored for this message flow [CallbackId|" + callbackId + "]");
		}
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
	 *
	 * @param callbackId callback id
	 * @param endTime    response received time
	 */
	public synchronized void updateCallbackReceived(String callbackId, Long endTime) {
		if (callbacks.containsKey(callbackId)) {
			int closedIndex = callbacks.get(callbackId);
			updateParentLogs(closedIndex, endTime);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No stored callback information found in statistic trace.");
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
			StatisticsLog updatingLog = messageFlowLogs.get(closedIndex);
			//if log is closed end time will be different than -1
			while (!(updatingLog.getEndTime() == -1)) {
				updatingLog.setEndTime(endTime);
				if (updatingLog.getParentLevel() == -1) {
					break;
				}
				updatingLog = messageFlowLogs.get(updatingLog.getParentLevel());
			}
			if (log.isDebugEnabled()) {
				log.debug("Log updating finished.");
			}
		}
	}

	private Integer getImmediateAggregateIndex() {
		Integer immediateIndex = null;
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.isAggregateLog()) {
				immediateIndex = i;
				break;
			}
		}
		return immediateIndex;
	}

	private Integer getImmediateCloneIndex() {

		Integer immediateIndex = null;
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.isCloneLog()) {
				immediateIndex = i;
				break;
			}
		}
		return immediateIndex;
	}

	private boolean isCloneFlow(int msgId) {
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.getMsgId() == msgId) {
				return false;
			}
		}
		return true;
	}

	private Integer getFirstLogWithMsgIdFromOpenLogs(int msgId) {

		Integer immediateIndex = null;
		Integer immediateCloneIndex = getImmediateCloneIndex();

		if (immediateCloneIndex != null) {
			for (Integer index : openLogs) {
				StatisticsLog statisticsLog = messageFlowLogs.get(index);

				if (immediateCloneIndex >= index) {
					break;
				}

				if (statisticsLog.getMsgId() == msgId) {
					immediateIndex = index;
					break;
				}
			}
		}
		return immediateIndex;
	}

	private int getParentForNormalOperation(int msgId) {
		Integer parentIndex = getParentFormOpenLogs(msgId);
		if (parentIndex == null) {
			parentIndex = getParentFormMessageLogs(msgId);
			if (parentIndex == null) {
				if (openLogs.isEmpty()) {
					parentIndex = 0;
				} else {
					parentIndex = openLogs.getFirst();
				}
			}
		}
		return parentIndex;
	}

	private int getParentForAggregateOperation(int msgId) {
		Integer immediateAggregateIndex = getImmediateAggregateIndex();
		Integer immediateCloneIndex = getImmediateCloneIndex();
		Integer parentIndex = getParentFormOpenLogs(msgId, immediateCloneIndex);
		if (parentIndex == null) {
			parentIndex = getParentFormMessageLogs(msgId, immediateCloneIndex);
			if (parentIndex == null) {
				if (immediateAggregateIndex != null) {
					parentIndex = immediateAggregateIndex;
				} else {
					parentIndex = immediateCloneIndex;
				}
			}
		}
		return parentIndex;
	}

	private Integer getParentFormOpenLogs(int msgId) {
		Integer immediateIndex = null;
		for (Integer index : openLogs) {
			StatisticsLog statisticsLog = messageFlowLogs.get(index);

			if (statisticsLog.getMsgId() == msgId) {
				immediateIndex = index;
				break;
			}
		}
		return immediateIndex;
	}

	private Integer getParentFormOpenLogs(int msgId, int limit) {
		Integer immediateIndex = null;
		for (Integer index : openLogs) {
			if (limit >= index) {
				break;
			}
			StatisticsLog statisticsLog = messageFlowLogs.get(index);

			if (statisticsLog.getMsgId() == msgId) {
				immediateIndex = index;
				break;
			}
		}
		return immediateIndex;
	}

	private Integer getParentFormMessageLogs(int msgId) {
		Integer immediateIndex = null;
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.getMsgId() == msgId) {
				immediateIndex = i;
				break;
			}
		}
		return immediateIndex;
	}

	private Integer getImmediateParentFormMessageLogs(int msgId) {
		Integer immediateIndex = null;
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.getMsgId() == msgId) {
				immediateIndex = i;
				break;
			}
		}
		return immediateIndex;
	}

	private Integer getParentFormMessageLogs(int msgId, int limit) {
		Integer immediateIndex = null;
		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.getMsgId() == msgId) {
				immediateIndex = i;
				break;
			}
		}
		return immediateIndex;
	}

	private int getFirstLogWithMsgId(int msgId, boolean cloneAware) {

		Integer immediateParent = getFirstLogWithMsgIdFromOpenLogs(msgId);
		if (immediateParent != null) {
			return immediateParent;
		}
		Integer immediateAggregateIndex = null;
		Integer immediateCloneIndex = null;

		for (int i = messageFlowLogs.size() - 1; i >= 0; i--) {
			StatisticsLog statisticsLog = messageFlowLogs.get(i);

			if (statisticsLog.isCloneLog()) {
				immediateCloneIndex = i;
				break;
			}
			if (statisticsLog.getMsgId() == msgId) {
				return i;
			}
			if (statisticsLog.isAggregateLog() && (immediateAggregateIndex == null)) {
				immediateAggregateIndex = i;
			}
		}
		if (immediateAggregateIndex != null) {
			return immediateAggregateIndex;
		} else if (immediateCloneIndex != null) {
			return immediateCloneIndex;
		}

		return 0;
	}

	/**
	 * Get first occurrence of the statistic log related to componentId, msgId and parentId, if it is
	 * present in the openLogs list and delete it from the openLogs list.
	 *
	 * @param componentId componentId of the statistic log
	 * @param parentId    parentId of the statistic log
	 * @param msgId       msgId of the statistic log
	 * @return index of the statistic log
	 */
	private int deleteAndGetComponentIndex(String componentId, String parentId, int msgId) {
		int parentIndex = -1;
		Iterator<Integer> StatLog = openLogs.listIterator(); // set Iterator at specified index
		while (StatLog.hasNext()) {
			int index = StatLog.next();
			if (componentId.equals(messageFlowLogs.get(index).getComponentId()) &&
			    parentId.equals(messageFlowLogs.get(index).getParent()) &&
			    (msgId == messageFlowLogs.get(index).getMsgId())) {
				parentIndex = index;
				StatLog.remove();   //if it is not root element remove
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * Get first occurrence of the statistic log related to componentId and msgId, if it s present
	 * in the openLogs list and delete it from the openLogs list.
	 *
	 * @param componentId componentId of the statistic log
	 * @param msgId       msgId of the statistic log
	 * @return index of the statistic log
	 */
	private int deleteAndGetComponentIndex(String componentId, int msgId) {
		int parentIndex = -1;
		Iterator<Integer> StatLog = openLogs.listIterator(); // set Iterator at specified index
		while (StatLog.hasNext()) {
			int index = StatLog.next();
			if (componentId.equals(messageFlowLogs.get(index).getComponentId()) &&
			    (msgId == messageFlowLogs.get(index).getMsgId())) {
				parentIndex = index;
				StatLog.remove(); //if it is not root element remove
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * After receiving a fault increment fault count of the statistics logs from its parent
	 * to the root log to maintain correct fault hierarchy.
	 *
	 * @param parentIndexOfFault parent Index of the fault log
	 */
	private void addFaultsToParents(int parentIndexOfFault) {
		if (parentIndexOfFault > -1) {
			while (parentIndexOfFault > -1) {
				StatisticsLog updatingLog = messageFlowLogs.get(parentIndexOfFault);
				updatingLog.incrementNoOfFaults();
				parentIndexOfFault = updatingLog.getParentLevel();
			}
		}
	}

	/**
	 * Returns collected message flows after message flow is ended.
	 *
	 * @return Message flow logs of the message flow
	 */
	public List<StatisticsLog> getMessageFlowLogs() {
		return messageFlowLogs;
	}
}
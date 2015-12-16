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
import org.apache.synapse.aspects.ComponentType;

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

	/**
	 * Number of faults waiting to be handled by a fault sequence
	 */
	private int openFaults = 0;

	/**
	 * Number of cloned messages relating to the message flow
	 */
	private int clonedMsgCount = -1;

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
		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, -1, -1);
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
			if (statisticDataUnit.getParentId() == null) {
				int parentIndex = getFirstLogWithMsgId(statisticDataUnit.getCloneId());
				createNewLog(statisticDataUnit, parentIndex);
			} else {
				int parentIndex = getComponentIndex(statisticDataUnit.getParentId(), statisticDataUnit.getCloneId());
				if (parentIndex > -1) {
					createNewLog(statisticDataUnit, parentIndex);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Invalid stating element");
					}
				}
			}
		}
	}

	/**
	 * Create statistics log at the start of a fault sequence.
	 *
	 * @param statisticDataUnit statistic data unit with raw data
	 */
	public synchronized void createFaultLog(StatisticDataUnit statisticDataUnit) {
		int parentIndex = getParentForFault(statisticDataUnit.getParentId(), statisticDataUnit.getCloneId());
		createNewLog(statisticDataUnit, parentIndex);
		addFaultsToParents(parentIndex);
		openFaults += 1;
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

//	/**
//	 * Close fault sequence after ending the fault sequence.
//	 *
//	 * @param componentId componentId of the fault sequence
//	 * @param msgId       message id of the message context
//	 * @param endTime     endTime of the fault sequence
//	 * @return true if message flow is ended
//	 */
//	public synchronized boolean closeFaultLog(String componentId, int msgId, Long endTime) {
//		int componentLevel = deleteAndGetComponentIndex(componentId, msgId);
//		if (componentLevel > -1) {
//			closeStatisticLog(componentLevel, endTime);
//			openFaults -= 1; // decrement number of faults
//			if (callbacks.isEmpty() && (openFaults == 0)) {
//				return true;
//			}
//		}
//		return false;
//	}

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
		if ((callbacks.isEmpty() && (openFaults == 0) && (openLogs.size() <= 1)) || closeForcefully) {
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
		parentLog.incrementNoOfChildren();
		parentLog.setHasChildren(true);
		statisticDataUnit.setParentId(parentLog.getComponentId());
		StatisticsLog statisticsLog = new StatisticsLog(statisticDataUnit, parentLog.getMsgId(), parentIndex);
		messageFlowLogs.add(statisticsLog);
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
		int callbackIndex = getFirstLogWithMsgId(msgId);
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

	/**
	 * Get index of the first statistic log occurrence with the specified msgId in openLogs List.
	 *
	 * @param msgId msgId of the statistics log to be searched
	 * @return index of the statistics log
	 */
	private int getFirstLogWithMsgId(int msgId) {
		for (Integer index : openLogs) {
			if (messageFlowLogs.get(index).getMsgId() == msgId) {
				return index;
			}
		}
		//No Log entry found for the msgID. So look for log IDs with default msgID
		if (msgId != -1) {
			msgId = -1;
			for (Integer index : openLogs) {
				if (messageFlowLogs.get(index).getMsgId() == msgId) {
					return index;
				}
			}
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
	 * Get first occurrence of the statistic log related to componentId and msgId if it s present
	 * in the openLogs list.
	 *
	 * @param componentId componentId of the statistic log
	 * @param msgId       msgId of the statistic log
	 * @return index of the statistic log
	 */
	private int getComponentIndex(String componentId, int msgId) {
		int parentIndex = -1;
		for (Integer index : openLogs) {
			if (componentId.equals(messageFlowLogs.get(index).getComponentId()) &&
			    (msgId == messageFlowLogs.get(index).getMsgId())) {
				parentIndex = index;
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
	 * Returns the index of first occurrence of the statistic log for specified parentId and msgId
	 * index  of the statistic log. If no statistic log is found it will return index of the root
	 * log.
	 *
	 * @param parentId parentId of the statistics log
	 * @param msgId    msgId of the statistics log
	 * @return the index of the parent if it is present
	 */
	private int getParentForFault(String parentId, int msgId) {
		int parentIndex = 0;
		if (parentId == null) {
			return getFirstLogWithMsgId(msgId);
		}
		for (int index = messageFlowLogs.size() - 1; index >= 0; index--) {
			if (parentId.equals(messageFlowLogs.get(index).getComponentId()) &&
			    (msgId == messageFlowLogs.get(index).getMsgId())) {
				parentIndex = index;
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * Returns collected message flows after message flow is ended.
	 *
	 * @return Message flow logs of the message flow
	 */
	public List<StatisticsLog> getMessageFlowLogs() {
		return messageFlowLogs;
	}

	/**
	 * This method can be used to get the number of cloned messages in the message flow.
	 *
	 * @return Number of cloned messages in the message flow
	 */
	public int incrementAndGetClonedMsgCount() {
		clonedMsgCount += 1;
		return clonedMsgCount;
	}

	public String getLocalMemberHost() {
		return localMemberHost;
	}

	public String getLocalMemberPort() {
		return localMemberPort;
	}
}
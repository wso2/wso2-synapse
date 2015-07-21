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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * StatisticsEntry collects all the statistics logs related to a message flow. It is responsible
 * for collecting statistics logs in correct hierarchy so that these logs can be directly fed
 * into the statistic store as inputs
 */
public class StatisticsEntry {

	private static final Log log = LogFactory.getLog(StatisticsEntry.class);

	/**
	 * ArrayList to hold all the statistic logs related to the message flow
	 */
	private final ArrayList<StatisticsLog> messageFlowLogs = new ArrayList<StatisticsLog>();

	/**
	 * HashMap to hold all the remaining callbacks related to the message flow
	 */
	private final HashMap<String, Integer> callbacks = new HashMap<String, Integer>();

	/**
	 * HashMap to hold all the opened statistic logs related to the message flow
	 */
	private final LinkedList<Integer> openLogs = new LinkedList<Integer>();

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
	 * @param componentId   componentId of the statistic reporting element
	 * @param componentType component Type of the statistic reporting element
	 * @param msgId         msgId of the statistics reporting message context
	 * @param parentId      parentId of the reporting statistic event
	 * @param startTime     starting time of the statistics reporting event
	 * @param isResponse    is message context belong to an response
	 */
	public StatisticsEntry(String componentId, ComponentType componentType, int msgId,
	                       String parentId, long startTime, boolean isResponse) {
		StatisticsLog statisticsLog =
				new StatisticsLog(componentId, componentType, msgId, -1, -1, parentId, startTime);
		statisticsLog.setIsResponse(isResponse);
		messageFlowLogs.add(statisticsLog);
		openLogs.addFirst(messageFlowLogs.size() - 1);
		if (log.isDebugEnabled()) {
			log.debug("Created statistic Entry [Start|RootElement]|[ElementId|" + componentId +
			          "]|[MsgId|" + msgId + "]");
		}
	}

	/**
	 * Create statistics log at the start of a statistic reporting element
	 *
	 * @param componentId   componentId of the statistic reporting element
	 * @param componentType component Type of the statistic reporting element
	 * @param msgId         msgId of the statistics reporting message context
	 * @param parentId      parentId of the reporting statistic event
	 * @param startTime     starting time of the statistics reporting event
	 * @param isResponse    is message context belong to an response
	 */
	public synchronized void createLog(String componentId, ComponentType componentType, int msgId,
	                                   String parentId, long startTime, boolean isResponse) {

		if (openLogs.isEmpty()) {
			StatisticsLog statisticsLog =
					new StatisticsLog(componentId, componentType, msgId, -1, -1, "", startTime);
			messageFlowLogs.add(statisticsLog);
			openLogs.addFirst(messageFlowLogs.size() - 1);
			if (log.isDebugEnabled()) {
				log.debug("Starting statistic log at root level [ElementId|" + componentId +
				          "]|[MsgId|" + msgId + "]");
			}
		} else {
			if (openLogs.getFirst() == 0) {
				if (messageFlowLogs.get(0).getComponentId().equals(componentId)) {
					if (log.isDebugEnabled()) {
						log.debug("Statistics event is ignored as it is a duplicate of root " +
						          "element");
					}
					return;
				}
			}
			if (parentId.equals("")) {
				int parentIndex = getFirstLogWithMsgId(msgId);
				if (parentIndex == -1) {
					/* if msg Id not present this statistic event is related to a new cloned
					message therefore get immediate root level open statistic log as the
					parent of this log */
					parentIndex = getFirstLogWithMsgId(-1);
				}
				createNewLog(componentId, componentType, msgId, parentIndex, startTime, isResponse);
			} else {
				int parentIndex = getComponentIndex(parentId, msgId);
				/* if msg Id not present this statistic event is related to a new cloned
					message therefore get immediate root level open statistic log as the
					parent of this log */
				if (parentIndex == -1) {
					parentIndex = getFirstLogWithMsgId(-1);
				}
				if (parentIndex > -1) {
					createNewLog(componentId, componentType, msgId, parentIndex, startTime,
					             isResponse);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Invalid stating element");
					}
				}
			}
		}
	}

	/**
	 * Create statistics log at the start of a fault sequence
	 *
	 * @param componentId   componentId of the statistic reporting element
	 * @param componentType component Type of the statistic reporting element
	 * @param msgId         msgId of the statistics reporting message context
	 * @param parentId      parentId of the reporting statistic event
	 * @param startTime     starting time of the statistics reporting event
	 * @param isResponse    is message context belong to an response
	 */
	public synchronized void createFaultLog(String componentId, ComponentType componentType,
	                                        int msgId, String parentId, Long startTime,
	                                        boolean isResponse) {
		int parentIndex = getParentForFault(parentId, msgId);
		createNewLog(componentId, componentType, msgId, parentIndex, startTime, isResponse);
		addFaultsToParents(parentIndex);
		openFaults += 1;
	}

	/**
	 * close a opened statistics log after all the statistics collection relating to that statistics
	 * component is ended
	 *
	 * @param componentId componentId of the statistic event sender
	 * @param msgId       message id of the message context
	 * @param parentId    parent of the statistic event sender
	 * @param endTime     endTime of the statistics event
	 * @return true if there are no open message logs in openLogs List
	 */
	public synchronized boolean closeLog(String componentId, int msgId, String parentId,
	                                     long endTime) {
		int componentLevel;
		if (parentId.equals("")) {
			componentLevel = deleteAndGetComponentIndex(componentId, msgId);
		} else {
			componentLevel = deleteAndGetComponentIndex(componentId, parentId, msgId);
		}
		//not closing the root statistic log as it will be closed be endAll method
		if (componentLevel > 0) {
			closeStatisticLog(componentLevel, endTime);
			if (openLogs.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * close fault sequence after ending the fault sequence.
	 *
	 * @param componentId componentId of the fault sequence
	 * @param msgId       message id of the message context
	 * @param endTime     endTime of the fault sequence
	 * @return true if message flow is ended
	 */
	public synchronized boolean closeFaultLog(String componentId, int msgId, Long endTime) {
		int componentLevel = deleteAndGetComponentIndex(componentId, msgId);
		if (componentLevel > -1) {
			closeStatisticLog(componentLevel, endTime);
			openFaults -= 1; // decrement number of faults
			if (callbacks.isEmpty() && (openFaults == 0)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Closes opened statistics log specified by the componentLevel
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
		currentLog.setEndTime(endTime);
		updateParentLogs(currentLog.getParentLevel(), endTime);
	}

	/**
	 * Close the remaining statistic logs after finishing all the message contexts of requests and
	 * responses belonging to a message flow
	 *
	 * @param endTime endTime of the message flow
	 * @return true if message flow correctly ended
	 */
	public synchronized boolean endAll(long endTime) {
		if (callbacks.isEmpty() && (openFaults == 0) && (openLogs.size() <= 1)) {
			for (Integer index : openLogs) {
				messageFlowLogs.get(index).setEndTime(endTime);
			}
			if (log.isDebugEnabled()) {
				log.debug("Closed all logs after message flow ended");
			}
			return true;
		}
		return false;
	}

	/**
	 * Create a new statistics log for the reported statistic event for given parameters
	 *
	 * @param componentId   componentId of the statistic log
	 * @param componentType component Type of the statistic log
	 * @param msgId         msgId of the statistic log
	 * @param parentIndex   parentIndex of the statistic log
	 * @param startTime     starting time of the statistic log
	 * @param isResponse    is message log related to a response
	 */
	private void createNewLog(String componentId, ComponentType componentType, int msgId,
	                          int parentIndex, Long startTime, boolean isResponse) {
		StatisticsLog parentLog = messageFlowLogs.get(parentIndex);
		parentLog.incrementNoOfChildren();
		parentLog.setHasChildren(true);
		StatisticsLog statisticsLog =
				new StatisticsLog(componentId, componentType, msgId, parentIndex,
				                  parentLog.getMsgId(), parentLog.getComponentId(), startTime);
		statisticsLog.setIsResponse(isResponse);
		messageFlowLogs.add(statisticsLog);
		openLogs.addFirst(messageFlowLogs.size() - 1);
		if (log.isDebugEnabled()) {
			log.debug("Created statistic log for [ElementId|" + componentId + "]|[MsgId|" +
			          msgId + "]");
		}
	}

	/**
	 * Adds a callback entry for this message flow
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
	 * Removes the callback entry from the callback map belonging to this entry message flow
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
				log.debug("No callback entry found for the callback id");
			}
		}
	}

	/**
	 * Updates the ArrayList after an response for that callback is received
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
				log.debug("No stored callback information found in statistic trace");
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
				log.debug("Logs updating finished");
			}
		}
	}

	/**
	 * Get index of the first statistic log occurrence with the specified msgId in openLogs List
	 *
	 * @param msgId msgId of the statistics log to be searched
	 * @return index of the statistics log
	 */
	private int getFirstLogWithMsgId(int msgId) {
		int parentIndex = -1;
		for (Integer index : openLogs) {
			if (messageFlowLogs.get(index).getMsgId() == msgId) {
				parentIndex = index;
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * get first occurrence of the statistic log related to componentId, msgId and parentId if it is
	 * present in the openLogs list and delete it from the openLogs list
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
				if ((index != 0)) {
					StatLog.remove();   //if it is not root element remove
				}
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * get first occurrence of the statistic log related to componentId and msgId if it s present
	 * in the openLogs list and delete it from the openLogs list
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
				if (index != 0) {
					StatLog.remove(); //if it is not root element remove
				}
				break;
			}
		}
		return parentIndex;
	}

	/**
	 * get first occurrence of the statistic log related to componentId and msgId if it s present
	 * in the openLogs list
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
	 * to the root log to maintain correct fault hierarchy
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
	 * returns the index of first occurrence of the statistic log for specified parentId and msgId
	 * index  of the statistic log. If no statistic log is found it will return index of the root
	 * log.
	 *
	 * @param parentId parentId of the statistics log
	 * @param msgId    msgId of the statistics log
	 * @return the index of the parent if it is present
	 */
	private int getParentForFault(String parentId, int msgId) {
		int parentIndex = 0;
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
	 * Returns collected message flows after message flow is ended
	 *
	 * @return message flow logs of the message flow
	 */
	public ArrayList<StatisticsLog> getMessageFlowLogs() {
		return messageFlowLogs;
	}

	/**
	 * This method can be used to get the number of cloned messages in the message flow
	 *
	 * @return number of cloned messages in the message flow
	 */
	public int incrementAndGetClonedMsgCount() {
		clonedMsgCount += 1;
		return clonedMsgCount;
	}
}
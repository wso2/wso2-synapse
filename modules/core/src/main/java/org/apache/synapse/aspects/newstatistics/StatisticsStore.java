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
import org.apache.synapse.commons.jmx.MBeanRegistrar;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * StatisticsStore holds collected statistics in the memory. It stores these statistics in a tree
 * data structure where root of each of these trees belong to the trigger points of the ESB i.e.
 * PROXY, API or SEQUENCES
 */
public class StatisticsStore {

	private static final Log log = LogFactory.getLog(StatisticsStore.class);

	private final HashMap<String, StatisticsTree> statistics;

	public StatisticsStore() {

		statistics = new HashMap<String, StatisticsTree>();

		MBeanRegistrar registrar = MBeanRegistrar.getInstance();
		synchronized (registrar) {
			registrar.registerMBean(new StatisticCollectionView(statistics), "StatisticsCollectionView",
			                        "StatisticsCollectionView");
		}
	}

	/**
	 * Updates the statistics tree corresponding to the received StatisticsLog ArrayList. If
	 * statistics tree is not  present creates the root node of the statistics tree
	 *
	 * @param statisticsLogs Collected statistics logs for a message flow
	 */
	public void update(ArrayList<StatisticsLog> statisticsLogs) {
		if (!statisticsLogs.isEmpty()) {
			StatisticsTree tree;
			if (statistics.containsKey(statisticsLogs.get(0).getComponentId())) {
				tree = statistics.get(statisticsLogs.get(0).getComponentId());
				tree.getRoot().update(statisticsLogs.get(0).getNoOfFaults(),
				                      statisticsLogs.get(0).getEndTime() - statisticsLogs.get(0).getStartTime());
			} else {
				tree = new StatisticsTree(statisticsLogs.get(0));
				statistics.put(statisticsLogs.get(0).getComponentId(), tree);
				if (log.isDebugEnabled()) {
					log.debug("Created New statistics tree in the store for Root: " +
					          statisticsLogs.get(0).getComponentId());
				}
			}
			if (log.isDebugEnabled()) {
				for (StatisticsLog statisticsLog : statisticsLogs) {
					log.debug(statisticsLog.getComponentId() + "::NoOfChildren: " +
					          statisticsLog.getNoOfChildren() + "::Msg ID: " +
					          statisticsLog.getMsgId() + ": Parent MSG ID: " +
					          statisticsLog.getParentMsgId() + "::Parent id: " +
					          statisticsLog.getParent());
				}
			}
			buildTree(tree, statisticsLogs); //build tree with these statistic logs
		}
	}

	/**
	 * Creates a new statistic log or updates the existing statistics log for the given statistics
	 * logs
	 *
	 * @param tree           statistics tree
	 * @param statisticsLogs statistics logs relating to a message flow
	 */
	private void buildTree(StatisticsTree tree, ArrayList<StatisticsLog> statisticsLogs) {

		IndividualStatistics currentStat = tree.getRoot();
		//send root node of the tree as parent to next element and recursively find children
		for (int i = 0; i < statisticsLogs.size(); i++) {
			if (statisticsLogs.get(i) != null) {
				currentStat = buildTree(statisticsLogs, currentStat, statisticsLogs.get(i).getNoOfChildren(),
				                        currentStat.getComponentId(), statisticsLogs.get(i).getParentMsgId(), i + 1);
			}
		}
		if (log.isDebugEnabled()) { //this will be removed in actual implementation for testing only
			StringBuilder sb = new StringBuilder();
			sb.append("\nStatistics Tree\n");
			print(tree.getRoot(), sb);
			log.debug(sb.toString());
		}
	}

	/**
	 * This method recursively build and update the statistics tree
	 *
	 * @param statisticsLogs statistic logs corresponding to the message flow
	 * @param currentStat    current node of the statistics tree
	 * @param noOfChildren   no of children to be search for this parent
	 * @param parentId       parents name
	 * @param parentMsgId    parent's msg Id
	 * @param offset         from where to start searching for children
	 * @return reference to the child
	 */
	private IndividualStatistics buildTree(ArrayList<StatisticsLog> statisticsLogs, IndividualStatistics currentStat,
	                                       int noOfChildren, String parentId, int parentMsgId, int offset) {
		int count = 0;
		for (int index = offset; index < statisticsLogs.size(); index++) {
			if (!(statisticsLogs.get(index) == null)) {
				if (statisticsLogs.get(index).getParent().equals(parentId) &&
				    (statisticsLogs.get(index).getParentMsgId() == parentMsgId)) {
					count++;
					//if parent is equal to current stat log get child corresponding to this log
					currentStat = getChild(currentStat.getChildren(), statisticsLogs.get(index));

					//if that children have children find them recursively
					if (statisticsLogs.get(index).isHasChildren()) {
						currentStat =
								buildTree(statisticsLogs, currentStat, statisticsLogs.get(index).getNoOfChildren(),
								          currentStat.getComponentId(), statisticsLogs.get(index).getMsgId(),
								          index + 1);
					}
					statisticsLogs.set(index, null);
					if (count == noOfChildren) {
						break;
					}
				}
			}
		}
		return currentStat; //as next sb child will be placed under above child
	}

	/**
	 * find the child in the tree node child list which matched with the statistics log componentId
	 * . If no child matches with the log create a new child in the tree.
	 *
	 * @param childList     child list of the corresponding tree node
	 * @param statisticsLog current statistic log
	 * @return refrence to the child element
	 */
	private IndividualStatistics getChild(ArrayList<IndividualStatistics> childList, StatisticsLog statisticsLog) {
		//if child present get it otherwise create a child
		for (IndividualStatistics statisticsNode : childList) {
			if (statisticsLog.getParent().equals(statisticsNode.getParentId()) &&
			    statisticsLog.getComponentId().equals(statisticsNode.getComponentId()) &&
			    (statisticsNode.getMsgId() == statisticsLog.getMsgId()) &&
			    (statisticsNode.getParentMsgId() == statisticsLog.getParentMsgId())) {
				statisticsNode.update(statisticsLog.getNoOfFaults(),
				                      statisticsLog.getEndTime() - statisticsLog.getStartTime());
				return statisticsNode;
			}
		}
		IndividualStatistics statisticsNode = createNewNodeForLog(statisticsLog);
		childList.add(statisticsNode);
		return statisticsNode;
	}

	/**
	 * Create a new tree node for the statistic log
	 *
	 * @param statisticsLog current statistics log
	 * @return tree node for the log
	 */
	private IndividualStatistics createNewNodeForLog(StatisticsLog statisticsLog) {
		IndividualStatistics statisticsNode =
				new IndividualStatistics(statisticsLog.getComponentId(), statisticsLog.getComponentType(),
				                         statisticsLog.getMsgId(), statisticsLog.getParent(),
				                         statisticsLog.getParentMsgId(),
				                         statisticsLog.getEndTime() - statisticsLog.getStartTime(),
				                         statisticsLog.getNoOfFaults());
		statisticsNode.setIsResponse(statisticsLog.isResponse());
		return statisticsNode;
	}

	/**
	 * Temporary method to print statistics tree recursively
	 *
	 * @param treeNode treeNode
	 * @param sb       StringBuilder object
	 */
	private void print(IndividualStatistics treeNode, StringBuilder sb) {
		printNode(sb, treeNode);
		for (IndividualStatistics individualStatistics : treeNode.getChildren()) {
			if (treeNode.getChildren().size() >= 2) {
				sb.append("\n----------Printing a new Branch From ").append(treeNode.getComponentId())
				  .append("---------------\n");
			}
			print(individualStatistics, sb);
		}
	}

	/**
	 * Temporary method to print statistics node details
	 *
	 * @param sb             StringBuilder object
	 * @param statisticsNode tree node
	 */
	private void printNode(StringBuilder sb, IndividualStatistics statisticsNode) {
		sb.append(statisticsNode.getComponentId()).append("[Count : ").append(statisticsNode.getCount()).append("]\n");

		sb.append("\t\t Response Path: ").append(statisticsNode.isResponse()).append("\n");
		sb.append("\t\t Component Id: ").append(statisticsNode.getComponentId()).append("\n");
		sb.append("\t\t Component Type: ").append(statisticsNode.getComponentType()).append("\n");
		sb.append("\t\t Parent: ").append(statisticsNode.getParentId()).append("\n");
		sb.append("\t\t Parent MsgID: ").append(statisticsNode.getParentId()).append("\n");
		sb.append("\t\t Message Id(if > -1 cloned): ").append(statisticsNode.getMsgId()).append("\n");
		sb.append("\t\t Minimum Response Time: ").append(statisticsNode.getMinProcessingTime()).append("\n");
		sb.append("\t\t Maximum Response Time: ").append(statisticsNode.getMaxProcessingTime()).append("\n");
		sb.append("\t\t Average Response Time: ").append(statisticsNode.getAvgProcessingTime()).append("\n");
		sb.append("\t\t Number of Faults: ").append(statisticsNode.getFaultCount()).append("\n");
	}

	/**
	 * Clean statistics collected
	 */
	public void cleanStatistics() {
		statistics.clear();
	}
}
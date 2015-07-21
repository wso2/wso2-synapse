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
 */

package org.apache.synapse.aspects.newstatistics;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class exposes the collected statistic through JMX
 */
public class StatisticCollectionView implements StatisticCollectionViewMXBean {

	private final HashMap<String, StatisticsTree> statistics;

	public StatisticCollectionView(HashMap<String, StatisticsTree> statistics) {
		this.statistics = statistics;
	}

	@Override public int getNumberOfStatisticTrees() {
		synchronized (statistics) {
			return statistics.size();
		}
	}

	@Override public void resetStatistics() {

		synchronized (statistics) {
			statistics.clear();
		}
	}

	@Override public StatisticsCompositeObject getProxyServiceStatistics(String proxyName) {

		if (statistics.containsKey(proxyName)) {
			IndividualStatistics proxyStatistics = statistics.get(proxyName).getRoot();

			return getCompositeDataObject(proxyStatistics);

		} else {
			return null;
		}

	}

	@Override public StatisticsCompositeObject getSequenceStatistics(String sequenceName) {
		if (statistics.containsKey(sequenceName)) {
			IndividualStatistics sequenceStatistics = statistics.get(sequenceName).getRoot();

			return getCompositeDataObject(sequenceStatistics);

		} else {
			return null;
		}
	}

	@Override public StatisticsCompositeObject getAPIStatistics(String APIName) {
		if (statistics.containsKey(APIName)) {
			IndividualStatistics apiStatistics = statistics.get(APIName).getRoot();

			return getCompositeDataObject(apiStatistics);

		} else {
			return null;
		}
	}

	@Override public StatisticsCompositeObject[] getProxyServiceStatisticsTree(String proxyName) {

		if (statistics.containsKey(proxyName)) {
			IndividualStatistics proxyStatistics = statistics.get(proxyName).getRoot();

			ArrayList<StatisticsCompositeObject> statisticSamples =
					new ArrayList<StatisticsCompositeObject>();

			getStatisticTreeAsArrayList(proxyStatistics, statisticSamples);

			StatisticsCompositeObject[] statisticSampleArray =
					new StatisticsCompositeObject[statisticSamples.size()];
			return statisticSamples.toArray(statisticSampleArray);

		} else {
			return null;
		}
	}

	@Override public StatisticsCompositeObject[] getSequenceStatisticsTree(String sequenceName) {
		if (statistics.containsKey(sequenceName)) {
			IndividualStatistics sequenceStatistics = statistics.get(sequenceName).getRoot();

			ArrayList<StatisticsCompositeObject> statisticSamples =
					new ArrayList<StatisticsCompositeObject>();

			getStatisticTreeAsArrayList(sequenceStatistics, statisticSamples);

			StatisticsCompositeObject[] statisticSampleArray =
					new StatisticsCompositeObject[statisticSamples.size()];
			return statisticSamples.toArray(statisticSampleArray);

		} else {
			return null;
		}
	}

	@Override public StatisticsCompositeObject[] getAPIStatisticsTree(String APIName) {
		if (statistics.containsKey(APIName)) {
			IndividualStatistics apiStatistics = statistics.get(APIName).getRoot();

			ArrayList<StatisticsCompositeObject> statisticSamples =
					new ArrayList<StatisticsCompositeObject>();

			getStatisticTreeAsArrayList(apiStatistics, statisticSamples);

			StatisticsCompositeObject[] statisticSampleArray =
					new StatisticsCompositeObject[statisticSamples.size()];
			return statisticSamples.toArray(statisticSampleArray);

		} else {
			return null;
		}
	}

	/**
	 * Returns Composite Data Object relating to a in individualStatistics Object
	 *
	 * @param individualStatistics individualStatistics Object
	 * @return Composite Data Object that contain statistics of individualStatistics Object
	 */
	public StatisticsCompositeObject getCompositeDataObject(
			IndividualStatistics individualStatistics) {
		if (individualStatistics != null) {
			return new StatisticsCompositeObject(individualStatistics.getComponentId(),
			                                     individualStatistics.getComponentType().toString(),
			                                     individualStatistics.getParentId(),
			                                     individualStatistics.getParentMsgId(),
			                                     individualStatistics.getMsgId(),
			                                     individualStatistics.getMaxProcessingTime(),
			                                     individualStatistics.getMinProcessingTime(),
			                                     individualStatistics.getAvgProcessingTime(),
			                                     individualStatistics.isResponse(),
			                                     individualStatistics.getCount(),
			                                     individualStatistics.getFaultCount());
		}
		return null;
	}

	/**
	 * Put all the individualStatistics Objects in a statistic tree to a ArrayList
	 *
	 * @param treeNode         treeNode
	 * @param statisticSamples ArrayList That Contains All Tree Nodes
	 */
	private void getStatisticTreeAsArrayList(IndividualStatistics treeNode,
	                                         ArrayList<StatisticsCompositeObject> statisticSamples) {
		if (treeNode != null) {
			statisticSamples.add(getCompositeDataObject(treeNode));
			for (IndividualStatistics individualStatistics : treeNode.getChildren()) {
				getStatisticTreeAsArrayList(individualStatistics, statisticSamples);
			}
		} else {
			return;
		}
	}

}

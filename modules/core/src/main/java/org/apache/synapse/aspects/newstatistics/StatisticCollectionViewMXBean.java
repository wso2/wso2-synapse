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

/**
 * MBean interface to expose collected statistic data using JMX
 */
public interface StatisticCollectionViewMXBean {

	/**
	 * Returns number of statistic tree in StatisticStore
	 *
	 * @return number of statistic trees
	 */
	int getNumberOfStatisticTrees();

	/**
	 * reset in memory statistic collection in StatisticStore
	 */
	void resetStatistics();

	/**
	 * Returns statistics related to a Proxy Service
	 *
	 * @param proxyName name of the proxy service
	 * @return Composite Data Object that contains Proxy Statistics
	 */
	StatisticsCompositeObject getProxyServiceStatistics(String proxyName);

	/**
	 * Returns statistics related to a Sequence
	 *
	 * @param sequenceName name of the Sequence
	 * @return Composite Data Object that contains Sequence Statistics
	 */
	StatisticsCompositeObject getSequenceStatistics(String sequenceName);

	/**
	 * Returns statistics related to a API
	 *
	 * @param APIName name of the API
	 * @return Composite Data Object that contains API Statistics
	 */
	StatisticsCompositeObject getAPIStatistics(String APIName);

	/**
	 * Returns statistics tree related to a Proxy Service
	 *
	 * @param proxyName name of the proxy service
	 * @return Composite Data Data Array that contains API Statistics Tree
	 */
	StatisticsCompositeObject[] getProxyServiceStatisticsTree(String proxyName);

	/**
	 * Returns statistics tree related to a Sequence
	 *
	 * @param sequenceName name of the Sequence
	 * @return Composite Data Data Array that contains API Statistics Tree
	 */
	StatisticsCompositeObject[] getSequenceStatisticsTree(String sequenceName);

	/**
	 * Returns statistics tree related to a API
	 *
	 * @param APIName name of the API
	 * @return Composite Data Array that contains API Statistics Tree
	 */
	StatisticsCompositeObject[] getAPIStatisticsTree(String APIName);
}


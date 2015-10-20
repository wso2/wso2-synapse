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

/**
 * StatisticsTree holds statistics tree node for each trigger point in the ESB.
 */
public class StatisticsTree {

	private IndividualStatistics root = null;

	public StatisticsTree(StatisticsLog log) {
		root = new IndividualStatistics(log.getComponentId(), log.getComponentType(), log.getMsgId(), "",
		                                log.getParentMsgId(), log.getEndTime() - log.getStartTime(),
		                                log.getNoOfFaults());
	}

	public IndividualStatistics getRoot() {
		return root;
	}
}

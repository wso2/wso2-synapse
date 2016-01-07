/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.util;

/**
 * This class holds constants related to  mediation flow statistics collection.
 */
public class StatisticsConstants {

	/**
	 * Statistics enabled globally.
	 */
	public final static String STATISTICS_ENABLE = "mediation.flow.statistics.enable";

	/**
	 * Flow statistic queue size
	 */
	public static final String FLOW_STATISTICS_QUEUE_SIZE = "synapse.flow.statistics.queue.size";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_ID = "synapse.synapse.flow.statistics.statistic.id";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_MESSAGE_ID = "synapse.flow.statistics.msgID";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_PARENT_MESSAGE_ID = "synapse.flow.statistics.parent.msg.id";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_MSG_COUNT_HOLDER = "synapse.flow.statistics.count.holder";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_IS_FAULT_REPORTED = "synapse.flow.statistics.fault.reported";

	/**
	 * Key to specify whether statistics should be reported
	 */
	public static final String FLOW_STATISTICS_IS_COLLECTED = "synapse.flow.statistics.collected";

	/**
	 * Flow statistic default queue size
	 */
	public static final String FLOW_STATISTICS_DEFAULT_QUEUE_SIZE = "10000";
}

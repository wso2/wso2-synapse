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
	 * Enable collecting message payloads
	 */
	public final static String COLLECT_MESSAGE_PAYLOADS = "mediation.flow.statistics.tracer.collect.payloads";

	/**
	 * Enable collecting transport and message-context properties
	 */
	public final static String COLLECT_MESSAGE_PROPERTIES = "mediation.flow.statistics.tracer.collect.properties";

	/**
	 * Flow statistic queue size
	 */
	public static final String FLOW_STATISTICS_QUEUE_SIZE = "mediation.flow.statistics.queue.size";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_ID = "mediation.flow.statistics.statistic.id";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_MESSAGE_ID = "mediation.flow.statistics.msgID";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_PARENT_MESSAGE_ID = "mediation.flow.statistics.parent.msg.id";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_MSG_COUNT_HOLDER = "mediation.flow.statistics.count.holder";

	/**
	 * Key to message context statistic entry
	 */
	public static final String FLOW_STATISTICS_IS_FAULT_REPORTED = "mediation.flow.statistics.fault.reported";

	public static final String MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT = "mediation.flow.statistics.index.object";

	public static final String MEDIATION_FLOW_STATISTICS_PARENT_LIST = "mediation.flow.statistics.parent.list";
	public static final String MEDIATION_FLOW_STATISTICS_PARENT_INDEX = "mediation.flow.statistics.parent.index";

	/**
	 * Key to specify whether statistics should be reported
	 */
	public static final String FLOW_STATISTICS_IS_COLLECTED = "mediation.flow.statistics.collected";
	public static final String FLOW_TRACE_IS_COLLECTED = "mediation.flow.trace.collected";

	/**
	 * Flow statistic default queue size
	 */
	public static final String FLOW_STATISTICS_DEFAULT_QUEUE_SIZE = "10000";

	public static final String FLOW_STATISTICS_PROXYSERVICE = "Proxy Service";

	public static final String FLOW_STATISTICS_ENDPOINT = "Endpoint";

	public static final String FLOW_STATISTICS_INBOUNDENDPOINT = "Inbound EndPoint";

	public static final String FLOW_STATISTICS_SEQUENCE = "Sequence";

	public static final String FLOW_STATISTICS_MEDIATOR = "Mediator";

	public static final String FLOW_STATISTICS_API = "API";

	public static final String FLOW_STATISTICS_RESOURCE = "API Resource";

	public static final String FLOW_STATISTICS_ANY = "Other Type";

	public static final int DEFAULT_MSG_ID = 0;
	public static final int DEFAULT_PARENT_INDEX = -1;

	public static final String IMAGINARY_COMPONENT_ID = "ImaginaryName";

	/**
	 * Modes of closing statistic entry
	 */
	public static final int GRACEFULLY_CLOSE = 0;
	public static final int ATTEMPT_TO_CLOSE = 1;
	public static final int FORECEFULLY_CLOSE = 2;
}

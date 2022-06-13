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

	public static final String EI_ANALYTICS_ENABLE = "ei.analytics.enable";

	/**
	 * Enable collecting message payloads.
	 */
	public final static String COLLECT_MESSAGE_PAYLOADS = "mediation.flow.statistics.tracer.collect.payloads";

	/**
	 * Enable collecting transport and message-context properties.
	 */
	public final static String COLLECT_MESSAGE_PROPERTIES = "mediation.flow.statistics.tracer.collect.properties";

	/**
	 * Enable statistics collecting for all artifacts
	 */
	public final static String COLLECT_ALL_STATISTICS = "mediation.flow.statistics.collect.all";

	/**
	 * Manager Host of Jaeger Sampler.
	 */
	public static final String JAEGER_SAMPLER_MANAGER_HOST = "jaeger.sampler.manager.host";

	/**
	 * Manager Port of Jaeger Sampler.
	 */
	public static final String JAEGER_SAMPLER_MANAGER_PORT = "jaeger.sampler.manager.port";

	/**
	 * Agent Host of Jaeger Sender.
	 */
	public static final String JAEGER_SENDER_AGENT_HOST = "jaeger.sender.agent.host";

	/**
	 * Agent Port of Jaeger Sender.
	 */
	public static final String JAEGER_SENDER_AGENT_PORT = "jaeger.sender.agent.port";

	/**
	 * Log spans in Jaeger Reporter.
	 */
	public static final String JAEGER_REPORTER_LOG_SPANS = "jaeger.reporter.log.spans";

	/**
	 * Max queue size of Jaeger Reporter.
	 */
	public static final String JAEGER_REPORTER_MAX_QUEUE_SIZE = "jaeger.reporter.max.queue.size";

	/**
	 * Flush interval of Jaeger Reporter.
	 */
	public static final String JAEGER_REPORTER_FLUSH_INTERVAL = "jaeger.reporter.flush.interval";

	/**
	 * Enable Zipkin client.
	 */
	public static final String ENABLE_ZIPKIN = "opentelemetry.zipkin.enable";

	/**
	 * Agent URL for Zipkin backend.
	 */
	public static final String ZIPKIN_BACKEND_URL = "opentelemetry.zipkin.backend.url";

	/**
	 * Flow statistic queue size.
	 */
	public static final String FLOW_STATISTICS_QUEUE_SIZE = "mediation.flow.statistics.queue.size";

	/**
	 * Flow statistic event consuming time.
	 */
	public static final String FLOW_STATISTICS_EVENT_CONSUME_TIME = "mediation.flow.statistics.event.consume.interval";

	/**
	 * Flow statistic event consuming time.
	 */
	public static final String FLOW_STATISTICS_EVENT_CLEAN_TIME = "mediation.flow.statistics.event.clean.interval";

	/**
	 * Key to message context statistic entry.
	 */
	public static final String FLOW_STATISTICS_ID = "mediation.flow.statistics.statistic.id";

	/**
	 * Key to message context statistic entry.
	 */
	public static final String FLOW_STATISTICS_IS_FAULT_REPORTED = "mediation.flow.statistics.fault.reported";

	/**
	 * Default parent index in statistic collection.
	 */
	public static final int DEFAULT_PARENT_INDEX = -1;

	/**
	 * Key to specify object that provides indexes for each component.
	 */
	public static final String MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT = "mediation.flow.statistics.index.object";

	/**
	 * Key to specify parent list for a component.
	 */
	public static final String MEDIATION_FLOW_STATISTICS_PARENT_LIST = "mediation.flow.statistics.parent.list";

	/**
	 * Key to specify the immediate parent of the component.
	 */
	public static final String MEDIATION_FLOW_STATISTICS_PARENT_INDEX = "mediation.flow.statistics.parent.index";

	/**
	 * Key to specify whether statistics should be reported.
	 */
	public static final String FLOW_STATISTICS_IS_COLLECTED = "mediation.flow.statistics.collected";

	/**
	 * Key to specify whether tracing should be reported.
	 */
	public static final String FLOW_TRACE_IS_COLLECTED = "mediation.flow.trace.collected";

	/**
	 * Freemarker template base path.
	 */
	public static final String FREEMARKER_TEMPLATE_BASE_PATH = "freemarker.template.base.path";

	/**
	 * Flow statistic default queue size.
	 */
	public static final String FLOW_STATISTICS_DEFAULT_QUEUE_SIZE = "10000";

	/**
	 * Flow statistic default event consumer time.
	 */
	public static final String FLOW_STATISTICS_DEFAULT_EVENT_CONSUME_INTERVAL = "1000";

	/**
	 * Flow statistic cleaning Time.
	 */
	public static final String FLOW_STATISTICS_DEFAULT_EVENT_CLEAN_INTERVAL = "15000";

	/**
	 * Modes of closing statistic entry
	 */
	public static final int GRACEFULLY_CLOSE = 0;
	public static final int ATTEMPT_TO_CLOSE = 1;
	public static final int FORCEFULLY_CLOSE = 2;

	/**
	 * Name to represent component types.
	 */
	public static final String FLOW_STATISTICS_PROXYSERVICE = "Proxy Service";

	public static final String FLOW_STATISTICS_ENDPOINT = "Endpoint";

	public static final String FLOW_STATISTICS_INBOUNDENDPOINT = "Inbound EndPoint";

	public static final String FLOW_STATISTICS_SEQUENCE = "Sequence";

	public static final String FLOW_STATISTICS_MEDIATOR = "Mediator";

	public static final String FLOW_STATISTICS_API = "API";

	public static final String FLOW_STATISTICS_RESOURCE = "API Resource";

	public static final String FLOW_STATISTICS_ANY = "Other Type";

	public static final String IMAGINARY_COMPONENT_ID = "ImaginaryName";

	public static final String STATISTIC_NOT_FOUND_ERROR = "Reported statistics event cannot find a statistics entry " +
	                                                       "for the statistic ID : ";


	public static final String HASH_CODE_NULL_COMPONENT = "HashCodeNullComponent";

    public static final String STAT_COLLECTOR_PROPERTY = "STATISTIC_COLLECTOR";

    public static final String CONTINUE_STATISTICS_FLOW = "CONTINUE_STATISTICS_FLOW";

}

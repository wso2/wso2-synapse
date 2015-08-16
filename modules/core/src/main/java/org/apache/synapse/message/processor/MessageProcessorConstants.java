/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.message.processor;

public final class MessageProcessorConstants {

    public static final String PARAMETERS = "parameters";

    /**
     * Scheduled Message Processor parameters
     */
    public static final String INTERVAL = "interval";
    public static final String CRON_EXPRESSION = "cronExpression";

	public static final String MEMBER_COUNT = "member.count";

	/**
	 * Threshould interval value is 1000.
	 */
	public static final long THRESHOULD_INTERVAL = 1000;

    /**
     * Message processor parameters
     */
    public static final String MAX_DELIVER_ATTEMPTS = "max.delivery.attempts";

    /**
     * This is used to control the retry rate when the front end client is not reachable.
     */
    public static final String RETRY_INTERVAL = "client.retry.interval";

    public static final String IS_ACTIVATED = "is.active";

    public static final String SCHEDULED_MESSAGE_PROCESSOR_GROUP =
            "synapse.message.processor.quartz";
    public static final String PROCESSOR_INSTANCE = "processor.instance";
    public static final String PINNED_SERVER = "pinnedServers";

    /** Deprecated message processor implementation class names**/
    public static final String DEPRECATED_SAMPLING_PROCESSOR_CLASS =
            "org.apache.synapse.message.processors.sampler.SamplingProcessor";
    public static final String DEPRECATED_FORWARDING_PROCESSOR_CLASS =
            "org.apache.synapse.message.processors.forward.ScheduledMessageForwardingProcessor";

    /**
     * Initial delay to start message processor if it is deactivated (in milliseconds)
     */
    public static final int INITIAL_EXECUTION_DELAY = 2000;

}

/**
 *  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.message.processor.impl.failover;

/**
 * class <code>FailoverForwardingProcessorConstants</code> holds the constants that are
 * used in Forwarding processors
 */
public final class FailoverForwardingProcessorConstants {

    /**
     * used for forward in case of Error
     */
    public static final String FAULT_SEQUENCE = "message.processor.fault.sequence";

    /**
     * Indicates if the message processor is running in throttle mode
     */
    public static final String THROTTLE = "throttle";

    /**
     * Used th throttle forwarding-processor when it is used with cron expressions
     */
    public static final String THROTTLE_INTERVAL = "throttle.interval";

    /**
     * If false, the MessageProcessor will process every single message in the queue regardless of its origin
     * If true, it will only process messages that were processed by a MessageStore running on the same server
     */
    public static final String BIND_PROCESSOR_TO_SERVER = "bind.processor.server";
    /**
     * Message will be dropped after maximum delivery
     */
    public static final String MAX_DELIVERY_DROP = "max.delivery.drop";

    public static final String CRON_EXPRESSION = "cron.expression";

    /**
     * Message store which need to send the messages
     */
    public static final String TARGET_MESSAGE_STORE = "message.target.store.name";

    /**
     * Message store which need to send the messages
     */
    public static final String DEACTIVATE_SEQUENCE = "message.processor.deactivate.sequence";


}

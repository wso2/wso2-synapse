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
package org.apache.synapse.message.processor.impl.forwarder;

import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processor.impl.ScheduledMessageProcessor;
import org.apache.synapse.task.Task;

/**
 * Redelivery processor is the Message processor which implements the Dead letter channel EIP
 * It will Time to time Redeliver the Messages to a given target.
 */
public class ScheduledMessageForwardingProcessor extends ScheduledMessageProcessor {
    private MessageForwardingProcessorView view;

	@Override
	public void init(SynapseEnvironment se) {
		parameters.put(ForwardingProcessorConstants.THROTTLE,
		               String.valueOf((isThrottling(interval))));
		if (isThrottling(cronExpression)) {
			parameters.put(ForwardingProcessorConstants.THROTTLE_INTERVAL, String.valueOf(interval));
			parameters.put(ForwardingProcessorConstants.CRON_EXPRESSION, cronExpression);
		}

		if (nonRetryStatusCodes != null) {
			parameters.put(ForwardingProcessorConstants.NON_RETRY_STATUS_CODES, nonRetryStatusCodes);
		}

		// Setting the end-point here. If target endpoint is not defined at the MP,
		// target.endpoint property is used to fetch the endpoint
        if (targetEndpoint != null) {
            parameters.put(ForwardingProcessorConstants.TARGET_ENDPOINT, targetEndpoint);
        }

		super.init(se);

		try {
			view = new MessageForwardingProcessorView(this);
		} catch (Exception e) {
			throw new SynapseException(e);
		}

		// register MBean
		org.apache.synapse.commons.jmx.MBeanRegistrar.getInstance()
		                                             .registerMBean(view,
		                                                            "Message Forwarding Processor view",
		                                                            getName());
	}

    /**
     * This method is used by back end of the message processor
     * @return The associated MBean.
     */
    public MessageForwardingProcessorView getView() {
        return view;
    }
    
	@Override
	protected Task getTask() {
		return new ForwardingService(this, sender, synapseEnvironment, interval, isProcessorStartAsDeactivated());
	}
}

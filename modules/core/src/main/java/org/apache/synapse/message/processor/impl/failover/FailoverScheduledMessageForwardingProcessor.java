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

import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processor.impl.ScheduledMessageProcessor;
import org.apache.synapse.task.Task;

/**
 * Redelivery processor will Time to time Redeliver the Messages to a given message store.
 */
public class FailoverScheduledMessageForwardingProcessor extends ScheduledMessageProcessor {

	private FailoverMessageForwardingProcessorView view;

	@Override
	public void init(SynapseEnvironment se) {
		parameters.put(FailoverForwardingProcessorConstants.THROTTLE,
		               String.valueOf((isThrottling(interval))));
		if (isThrottling(cronExpression)) {
			parameters.put(FailoverForwardingProcessorConstants.THROTTLE_INTERVAL, String.valueOf(interval));
			parameters.put(FailoverForwardingProcessorConstants.CRON_EXPRESSION, cronExpression);
		}

		super.init(se);

		try {
			view = new FailoverMessageForwardingProcessorView(this);
		} catch (Exception e) {
			throw new SynapseException(e);
		}

		// register MBean
		org.apache.synapse.commons.jmx.MBeanRegistrar.getInstance()
		                                             .registerMBean(view,
		                                                            "Message Failover Forwarding Processor view",
		                                                            getName());
	}

    /**
     * This method is used by back end of the message processor
     * @return The associated MBean.
     */
    public FailoverMessageForwardingProcessorView getView() {
        return view;
    }
    
	@Override
	protected Task getTask() {
		return new FailoverForwardingService(this, synapseEnvironment, interval, isProcessorStartAsDeactivated());
	}
}

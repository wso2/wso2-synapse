/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.processor.impl.sampler;

/**
 * JMS MBean interface of <code>SamplingMessageProcessor</code>
 * This will expose the JMX operations for SamplingProcessor
 */
public interface SamplingProcessorViewMBean {
    /**
     * Activate the Sampling processor instance
     */
    public void activate();

    /**
     * De activate the sampling processor instance
     */
    public void deactivate();

    /**
     * Get the active status of Sampling processor
     * @return
     */
    public boolean isActive();

    /**
     * Checks if the task location linked to this Message Processor is known
     * @return true if the task location linked to this Message Processor is known
     */
    public boolean isTaskLocationKnown();
}

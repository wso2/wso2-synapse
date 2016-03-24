/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.data.raw;

import org.apache.synapse.core.SynapseEnvironment;

/**
 * This is the basic raw statistics data carrier to StatisticEntry.
 */
public class BasicStatisticDataUnit {

	/**
	 * Time statistic event is reported
	 */
	private Long time;

	/**
	 * Current position in the message flow.
	 */
	private int currentIndex;

	/**
	 * Statistic Tracing Id for this message flow
	 */
	private String statisticId;

	/**
	 * Is tracing enabled for a component
	 */
	private boolean isTracingEnabled = false;

	/**
	 * Is this message flow is a out_only flow
	 */
	private boolean isOutOnlyFlow;

	/**
	 * Synapse environment of the message context.
	 */
	private SynapseEnvironment synapseEnvironment;

	public String getStatisticId() {
		return statisticId;
	}

	public void setStatisticId(String statisticId) {
		this.statisticId = statisticId;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public SynapseEnvironment getSynapseEnvironment() {
		return synapseEnvironment;
	}

	public void setSynapseEnvironment(SynapseEnvironment synapseEnvironment) {
		this.synapseEnvironment = synapseEnvironment;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}

	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}

	public boolean isTracingEnabled() {
		return isTracingEnabled;
	}

	public void setTracingEnabled(boolean tracingEnabled) {
		isTracingEnabled = tracingEnabled;
	}

	public boolean isOutOnlyFlow() {
		return isOutOnlyFlow;
	}

	public void setIsOutOnlyFlow(boolean isOutOnlyFlow) {
		this.isOutOnlyFlow = isOutOnlyFlow;
	}
}

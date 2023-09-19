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
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.commons.throttle.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ThrottleProperties implements Serializable {

	private String throttleFrequency = "3600000";
	private String throttleContextDistributedCleanupTaskFrequency = "3600000";
	private String throttleContextDistributedExpiredInstanceTime = "3600000";
	private String throttleDistributedCleanupPoolSize = "1";
	private String throttleDistributedCleanupAmount = "25000";
	private String maxNonAssociatedCounterCleanupAmount = "25000";
	private String throttleDistributedCleanupTaskEnable = "true";
	private String throttlingPoolSize = "1";
	private String throttlingReplicationFrequency = "50";
	private String throttlingKeysToReplicates = "25000";
	private Map<String, String> distributedCounterConfigurations = new HashMap<>();
	private String distributedCounterType = ThrottleConstants.HAZELCAST;
	private boolean throttleSyncAsyncHybridModeEnabled = false;
	private String distributedThrottleProcessorType = "hybrid";
	private String hybridThrottleProcessorWindowType = "start_time_based";
	private String localQuotaBufferPercentage = "20";

	public String getWindowReplicatorPoolSize() {
		return windowReplicatorPoolSize;
	}

	public void setWindowReplicatorPoolSize(String windowReplicatorPoolSize) {
		this.windowReplicatorPoolSize = windowReplicatorPoolSize;
	}

	public String getWindowReplicatorFrequency() {
		return windowReplicatorFrequency;
	}

	public void setWindowReplicatorFrequency(String windowReplicatorFrequency) {
		this.windowReplicatorFrequency = windowReplicatorFrequency;
	}

	private String windowReplicatorPoolSize = "1";
	private String windowReplicatorFrequency = "50";

	public String getThrottleDistributedCleanupTaskEnable() {
		return throttleDistributedCleanupTaskEnable;
	}

	public void setThrottleDistributedCleanupTaskEnable(String throttleDistributedCleanupTaskEnable) {
		this.throttleDistributedCleanupTaskEnable = throttleDistributedCleanupTaskEnable;
	}

	public String getMaxNonAssociatedCounterCleanupAmount() {
		return maxNonAssociatedCounterCleanupAmount;
	}

	public void setMaxNonAssociatedCounterCleanupAmount(String maxNonAssociatedCounterCleanupAmount) {
		this.maxNonAssociatedCounterCleanupAmount = maxNonAssociatedCounterCleanupAmount;
	}

	public String getThrottleDistributedCleanupAmount() {
		return throttleDistributedCleanupAmount;
	}

	public void setThrottleDistributedCleanupAmount(String throttleDistributedCleanupAmount) {
		this.throttleDistributedCleanupAmount = throttleDistributedCleanupAmount;
	}

	public String getThrottleDistributedCleanupPoolSize() {
		return throttleDistributedCleanupPoolSize;
	}

	public void setThrottleDistributedCleanupPoolSize(String throttleDistributedCleanupPoolSize) {
		this.throttleDistributedCleanupPoolSize = throttleDistributedCleanupPoolSize;
	}

	public String getThrottleContextDistributedExpiredInstanceTime() {
		return throttleContextDistributedExpiredInstanceTime;
	}

	public void setThrottleContextDistributedExpiredInstanceTime(String
			                                                             throttleContextDistributedExpiredInstanceTime) {
		this.throttleContextDistributedExpiredInstanceTime = throttleContextDistributedExpiredInstanceTime;
	}

	public String getThrottleContextDistributedCleanupTaskFrequency() {
		return throttleContextDistributedCleanupTaskFrequency;
	}

	public void setThrottleContextDistributedCleanupTaskFrequency(
			String throttleContextDistributedCleanupTaskFrequency) {
		this.throttleContextDistributedCleanupTaskFrequency = throttleContextDistributedCleanupTaskFrequency;
	}

	public String getThrottleFrequency() {
		return throttleFrequency;
	}

	public void setThrottleFrequency(String throttleFrequency) {
		this.throttleFrequency = throttleFrequency;
	}

	public String getThrottlingPoolSize() {
		return throttlingPoolSize;
	}

	public void setThrottlingPoolSize(String throttlingPoolSize) {
		this.throttlingPoolSize = throttlingPoolSize;
	}

	public String getThrottlingReplicationFrequency() {
		return throttlingReplicationFrequency;
	}

	public void setThrottlingReplicationFrequency(String throttlingReplicationFrequency) {
		this.throttlingReplicationFrequency = throttlingReplicationFrequency;
	}

	public String getThrottlingKeysToReplicates() {
		return throttlingKeysToReplicates;
	}

	public void setThrottlingKeysToReplicates(String throttlingKeysToReplicates) {
		this.throttlingKeysToReplicates = throttlingKeysToReplicates;
	}

	public Map<String, String> getDistributedCounterConfigurations() {

		return distributedCounterConfigurations;
	}

	public void setDistributedCounterConfigurations(Map<String, String> distributedCounterConfigurations) {

		this.distributedCounterConfigurations = distributedCounterConfigurations;
	}

	public String getDistributedCounterType() {

		return distributedCounterType;
	}

	public void setDistributedCounterType(String distributedCounterType) {

		this.distributedCounterType = distributedCounterType;
	}

	public String getDistributedThrottleProcessorType() {
		return distributedThrottleProcessorType;
	}

	public void setDistributedThrottleProcessorType(String distributedThrottleProcessorType) {
		this.distributedThrottleProcessorType = distributedThrottleProcessorType;
	}

	public void setThrottleSyncAsyncHybridModeEnabled(boolean throttleSyncAsyncHybridModeEnabled) {
		this.throttleSyncAsyncHybridModeEnabled = throttleSyncAsyncHybridModeEnabled;
	}

	public boolean isThrottleSyncAsyncHybridModeEnabled() {
		return throttleSyncAsyncHybridModeEnabled;
	}

	public void setHybridThrottleProcessorWindowType(
			String hybridThrottleProcessorWindowType) {
		this.hybridThrottleProcessorWindowType = hybridThrottleProcessorWindowType;
	}

	public String getHybridThrottleProcessorWindowType() {
		return hybridThrottleProcessorWindowType;
	}

	public void setLocalQuotaBufferPercentage(String localQuotaBufferPercentage) {
		this.localQuotaBufferPercentage = localQuotaBufferPercentage;
	}

	public String getLocalQuotaBufferPercentage() {
		return localQuotaBufferPercentage;
	}
}

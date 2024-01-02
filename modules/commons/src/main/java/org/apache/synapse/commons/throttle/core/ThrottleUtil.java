/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ThrottleUtil {
    private static Log log = LogFactory.getLog(ThrottleUtil.class.getName());

    public static final String THROTTLING_CACHE_MANAGER = "throttling.cache.manager";

	private static final String CONF_LOCATION = "conf.location";

    public static final String THROTTLING_CACHE = "throttling.cache";

	/**
	 * This method used to set throttle properties loaded from throttle.properties file in configuration folder
	 * @return properties of throttle properties
	 */
	public static ThrottleProperties loadThrottlePropertiesFromConfigurations() {
		ThrottleProperties throttleProperties = new ThrottleProperties();
		Properties properties = new Properties();
		String throttlePropertiesFileLocation = null;
		if (System.getProperty(CONF_LOCATION) != null) {
			throttlePropertiesFileLocation = System.getProperty(CONF_LOCATION) + File.separator +"throttle.properties";
			try {
				properties.load(new FileInputStream(throttlePropertiesFileLocation));
				String throttleFrequency =
						properties.getProperty(ThrottleConstants.THROTTLE_CONTEXT_CLEANUP_TASK_FREQUENCY);
				if (!StringUtils.isEmpty(throttleFrequency)) {
					throttleProperties.setThrottleFrequency(throttleFrequency);
				} else {
					throttleProperties.setThrottleFrequency("3600000");
				}

				String throttleContextDistributedCleanupTaskFrequency =
						properties.getProperty(ThrottleConstants.THROTTLE_CONTEXT_DISTRIBUTED_CLEANUP_TASK_FREQUENCY);
				if (throttleContextDistributedCleanupTaskFrequency != null &&
				    throttleContextDistributedCleanupTaskFrequency != "") {
					throttleProperties.setThrottleContextDistributedCleanupTaskFrequency(
							throttleContextDistributedCleanupTaskFrequency);
				} else {
					throttleProperties.setThrottleContextDistributedCleanupTaskFrequency("3600000");
				}

				String throttleContextDistributedExpiredInstanceTime =
						properties.getProperty(ThrottleConstants.THROTTLE_CONTEXT_DISTRIBUTED_EXPIRED_INSTANCE_TIME);
				if (throttleContextDistributedExpiredInstanceTime != null &&
				    throttleContextDistributedExpiredInstanceTime != "") {
					throttleProperties.setThrottleContextDistributedExpiredInstanceTime(
							throttleContextDistributedExpiredInstanceTime);
				} else {
					throttleProperties.setThrottleContextDistributedExpiredInstanceTime("3600000");
				}
				String throttleDistributedCleanupPoolSize =
						properties.getProperty(ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_POOL_SIZE);
				if (throttleDistributedCleanupPoolSize != null && throttleDistributedCleanupPoolSize != "") {
					throttleProperties.setThrottleDistributedCleanupPoolSize(
							throttleDistributedCleanupPoolSize);
				} else {
					throttleProperties.setThrottleDistributedCleanupPoolSize("1");
				}

				String throttleDistributedCleanupAmount =
						properties.getProperty(ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_AMOUNT);
				if (throttleDistributedCleanupAmount != null && throttleDistributedCleanupAmount != "") {
					throttleProperties.setThrottleDistributedCleanupAmount(
							throttleDistributedCleanupAmount);
				} else {
					throttleProperties.setThrottleDistributedCleanupAmount("25000");
				}

				String throttleDistributedCleanupTaskEnable =
						properties.getProperty(ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_TASK_ENABLE);
				if (throttleDistributedCleanupTaskEnable != null && throttleDistributedCleanupTaskEnable != "") {
					throttleProperties.setThrottleDistributedCleanupTaskEnable(
							throttleDistributedCleanupTaskEnable);
				} else {
					throttleProperties.setThrottleDistributedCleanupTaskEnable("true");
				}

				String maxNonAssociatedCounterCleanupAmount =
						properties.getProperty(ThrottleConstants.MAX_NON_ASSOCIATED_COUNTER_CLEANUP_AMOUNT);
				if (maxNonAssociatedCounterCleanupAmount != null && maxNonAssociatedCounterCleanupAmount != "") {
					throttleProperties.setMaxNonAssociatedCounterCleanupAmount(
							maxNonAssociatedCounterCleanupAmount);
				} else {
					throttleProperties.setMaxNonAssociatedCounterCleanupAmount("25000");
				}

				String throttlingPoolSize =
						properties.getProperty(ThrottleConstants.THROTTLING_POOL_SIZE);
				if (throttlingPoolSize != null && throttlingPoolSize != "") {
					throttleProperties.setThrottlingPoolSize(
							throttlingPoolSize);
				} else {
					throttleProperties.setThrottlingPoolSize("1");
				}
				String throttlingReplicationFrequency =
						properties.getProperty(ThrottleConstants.THROTTLING_REPLICATION_FREQUENCY);
				if (throttlingReplicationFrequency != null && throttlingReplicationFrequency != "" ) {
					throttleProperties.setThrottlingReplicationFrequency(
							throttlingReplicationFrequency);
				} else {
					throttleProperties.setThrottlingReplicationFrequency("50");
				}

				String throttlingKeysToReplicate =
						properties.getProperty(ThrottleConstants.THROTTLING_KEYS_TO_REPLICATE);
				if (throttlingKeysToReplicate != null && throttlingKeysToReplicate != "") {
					throttleProperties.setThrottlingKeysToReplicates(
							throttlingKeysToReplicate);
				} else {
					throttleProperties.setThrottlingKeysToReplicates("25000");
				}

				String windowReplicatorPoolSize =
						properties.getProperty(ThrottleConstants.WINDOW_REPLICATOR_POOL_SIZE);
				if (windowReplicatorPoolSize != null && windowReplicatorPoolSize != "") {
					throttleProperties.setWindowReplicatorPoolSize(
							windowReplicatorPoolSize);
				} else {
					throttleProperties.setWindowReplicatorPoolSize("1");
				}

				String windowReplicatorFrequency =
						properties.getProperty(ThrottleConstants.WINDOW_REPLICATOR_FREQUENCY);
				if (windowReplicatorFrequency != null && windowReplicatorFrequency != "") {
					throttleProperties.setWindowReplicatorFrequency(
							windowReplicatorFrequency);
				} else {
					throttleProperties.setWindowReplicatorFrequency("50");
				}

			} catch (IOException e) {
				log.debug("Setting the Default Throttle Properties");
				throttleProperties.setThrottleFrequency("3600000");
				throttleProperties.setThrottleContextDistributedCleanupTaskFrequency("3600000");
				throttleProperties.setThrottleContextDistributedExpiredInstanceTime("3600000");
				throttleProperties.setThrottleDistributedCleanupPoolSize("1");
				throttleProperties.setThrottleDistributedCleanupAmount("25000");
				throttleProperties.setThrottleDistributedCleanupTaskEnable("true");
				throttleProperties.setMaxNonAssociatedCounterCleanupAmount("25000");
				throttleProperties.setThrottlingPoolSize("1");
				throttleProperties.setThrottlingReplicationFrequency("50");
				throttleProperties.setThrottlingKeysToReplicates("25000");
				throttleProperties.setWindowReplicatorPoolSize("1");
				throttleProperties.setWindowReplicatorFrequency("50");
			}
		}else{
			log.debug("Setting the Default Throttle Properties");
			throttleProperties.setThrottleFrequency("3600000");
			throttleProperties.setThrottleContextDistributedCleanupTaskFrequency("3600000");
			throttleProperties.setThrottleContextDistributedExpiredInstanceTime("3600000");
			throttleProperties.setThrottleDistributedCleanupPoolSize("1");
			throttleProperties.setThrottleDistributedCleanupAmount("25000");
			throttleProperties.setThrottleDistributedCleanupTaskEnable("true");
			throttleProperties.setMaxNonAssociatedCounterCleanupAmount("25000");
			throttleProperties.setThrottlingPoolSize("1");
			throttleProperties.setThrottlingReplicationFrequency("50");
			throttleProperties.setThrottlingKeysToReplicates("25000");
			throttleProperties.setWindowReplicatorPoolSize("1");
			throttleProperties.setWindowReplicatorFrequency("50");
		}

		return throttleProperties;

	}
    public static Cache<String, CallerContext> getThrottleCache() {
        // acquiring  cache manager.
        Cache<String, CallerContext> cache;
        CacheManager cacheManager = Caching.getCacheManagerFactory().
                getCacheManager(THROTTLING_CACHE_MANAGER);
        if (cacheManager != null) {
            cache = cacheManager.getCache(THROTTLING_CACHE);
        } else {
            cache = Caching.getCacheManager().getCache(THROTTLING_CACHE);
        }
        if (log.isDebugEnabled()) {
            log.debug("created throttling cache : " + cache);
        }
        return cache;
    }
}

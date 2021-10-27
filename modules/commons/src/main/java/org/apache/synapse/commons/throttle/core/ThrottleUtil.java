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
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.commons.MiscellaneousUtil;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ThrottleUtil {

    private static Log log = LogFactory.getLog(ThrottleUtil.class.getName());

    public static final String THROTTLING_CACHE_MANAGER = "throttling.cache.manager";

    private static final String CONF_LOCATION = "conf.location";

    public static final String THROTTLING_CACHE = "throttling.cache";

    /**
     * This method used to set throttle properties loaded from throttle.properties file in configuration folder
     *
     * @return properties of throttle properties
     */
	public static ThrottleProperties loadThrottlePropertiesFromConfigurations() {

		ThrottleProperties throttleProperties = new ThrottleProperties();
		Properties properties = new Properties();
		String throttlePropertiesFileLocation;
		if (System.getProperty(CONF_LOCATION) != null) {
			throttlePropertiesFileLocation = System.getProperty(CONF_LOCATION) + File.separator + "throttle.properties";
			try {
				properties.load(new FileInputStream(throttlePropertiesFileLocation));
				SecretResolver secretResolver = SecretResolverFactory.create(properties);
				Set<String> keys = properties.stringPropertyNames();
				for (String key : keys) {
					if (ThrottleConstants.THROTTLE_CONTEXT_CLEANUP_TASK_FREQUENCY.equals(key)) {
						String throttleFrequency = properties.getProperty(key);
						if (!StringUtils.isEmpty(throttleFrequency)) {
							throttleProperties.setThrottleFrequency(throttleFrequency);
						}
					}
					if (ThrottleConstants.THROTTLE_CONTEXT_DISTRIBUTED_CLEANUP_TASK_FREQUENCY.equals(key)) {
						String throttleContextDistributedCleanupTaskFrequency = properties.getProperty(key);
						if (throttleContextDistributedCleanupTaskFrequency != null &&
								!throttleContextDistributedCleanupTaskFrequency.equals("")) {
							throttleProperties.setThrottleContextDistributedCleanupTaskFrequency(
									throttleContextDistributedCleanupTaskFrequency);
						}
					}
					if (ThrottleConstants.THROTTLE_CONTEXT_DISTRIBUTED_EXPIRED_INSTANCE_TIME.equals(key)) {
						String throttleContextDistributedExpiredInstanceTime = properties.getProperty(key);
						if (throttleContextDistributedExpiredInstanceTime != null &&
								!throttleContextDistributedExpiredInstanceTime.equals("")) {
							throttleProperties.setThrottleContextDistributedExpiredInstanceTime(
									throttleContextDistributedExpiredInstanceTime);
						}
					}
					if (ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_POOL_SIZE.equals(key)) {
						String throttleDistributedCleanupPoolSize = properties.getProperty(key);
						if (throttleDistributedCleanupPoolSize != null && throttleDistributedCleanupPoolSize != "") {
							throttleProperties.setThrottleDistributedCleanupPoolSize(throttleDistributedCleanupPoolSize);
						}
					}
					if (ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_AMOUNT.equals(key)) {
						String throttleDistributedCleanupAmount = properties.getProperty(key);
						if (throttleDistributedCleanupAmount != null && throttleDistributedCleanupAmount != "") {
							throttleProperties.setThrottleDistributedCleanupAmount(
									throttleDistributedCleanupAmount);

						}
					}
					if (ThrottleConstants.THROTTLE_DISTRIBUTED_CLEANUP_TASK_ENABLE.equals(key)) {
						String throttleDistributedCleanupTaskEnable = properties.getProperty(key);
						if (throttleDistributedCleanupTaskEnable != null && throttleDistributedCleanupTaskEnable !=
								"") {
							throttleProperties.setThrottleDistributedCleanupTaskEnable(
									throttleDistributedCleanupTaskEnable);
						}
					}
					if (ThrottleConstants.MAX_NON_ASSOCIATED_COUNTER_CLEANUP_AMOUNT.equals(key)) {
						String maxNonAssociatedCounterCleanupAmount = properties.getProperty(key);
						if (maxNonAssociatedCounterCleanupAmount != null && maxNonAssociatedCounterCleanupAmount !=
								"") {
							throttleProperties.setMaxNonAssociatedCounterCleanupAmount(
									maxNonAssociatedCounterCleanupAmount);
						}
					}
					if (ThrottleConstants.THROTTLING_POOL_SIZE.equals(key)) {
						String throttlingPoolSize = properties.getProperty(key);
						if (throttlingPoolSize != null && throttlingPoolSize != "") {
							throttleProperties.setThrottlingPoolSize(throttlingPoolSize);
						}
					}
					if (ThrottleConstants.THROTTLING_REPLICATION_FREQUENCY.equals(key)) {
						String throttlingReplicationFrequency = properties.getProperty(key);
						if (throttlingReplicationFrequency != null && throttlingReplicationFrequency != "") {
							throttleProperties.setThrottlingReplicationFrequency(
									throttlingReplicationFrequency);
						}
					}
					if (ThrottleConstants.THROTTLING_KEYS_TO_REPLICATE.equals(key)) {
						String throttlingKeysToReplicate = properties.getProperty(key);
						if (throttlingKeysToReplicate != null && throttlingKeysToReplicate != "") {
							throttleProperties.setThrottlingKeysToReplicates(throttlingKeysToReplicate);
						}
					}
					if (ThrottleConstants.WINDOW_REPLICATOR_POOL_SIZE.equals(key)) {
						String windowReplicatorPoolSize = properties.getProperty(key);
						if (windowReplicatorPoolSize != null && windowReplicatorPoolSize != "") {
							throttleProperties.setWindowReplicatorPoolSize(windowReplicatorPoolSize);
						}
					}
					if (ThrottleConstants.WINDOW_REPLICATOR_FREQUENCY.equals(key)) {
						String windowReplicatorFrequency = properties.getProperty(key);
						if (windowReplicatorFrequency != null && windowReplicatorFrequency != "") {
							throttleProperties.setWindowReplicatorFrequency(
									windowReplicatorFrequency);
						}
					}
					if (ThrottleConstants.DISTRIBUTED_COUNTER_TYPE.equals(key)) {
						String distributedCounterType =
								properties.getProperty(key);
						if (distributedCounterType != null && !distributedCounterType.equals("")) {
							throttleProperties.setDistributedCounterType(distributedCounterType);
						}
					}
					if (key.contains(ThrottleConstants.DISTRIBUTED_COUNTER_CONFIGURATIONS)) {
						String distributedConfiguration = properties.getProperty(key);
						String configuration = key.split(ThrottleConstants.DISTRIBUTED_COUNTER_CONFIGURATIONS)[1];
						if (StringUtils.isNotEmpty(distributedConfiguration)) {
							throttleProperties.getDistributedCounterConfigurations().put(configuration,
									getResolvedValue(secretResolver, distributedConfiguration));
						}
					}
				}
			} catch (IOException e) {
				log.debug("Setting the Default Throttle Properties");
			}
		} else {
			log.debug("Setting the Default Throttle Properties");
		}

		return throttleProperties;

	}

	private static String getResolvedValue(SecretResolver secretResolver, String value) {

		if (secretResolver.isInitialized()) {
			return MiscellaneousUtil.resolve(value, secretResolver);
		}
		return value;
	}
        public static Cache<String, CallerContext> getThrottleCache () {
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

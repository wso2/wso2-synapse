/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.newstatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapsePropertiesLoader;

/**
 * Clean the statistics stored in the StatisticsStore based on a configuration specified at the
 * synapse.properties file in ${ESB:root}/repository/conf directory
 * new.statistics.clean.interval and new.statistics.clean.enable entries can be set in this file
 */
public class StatisticsStoreCleaner {

	private static final Log log = LogFactory.getLog(StatisticsStoreCleaner.class);

	private final static String CLEAN_INTERVAL = "new.statistics.clean.interval";
	private final static String CLEAN_ENABLE = "new.statistics.clean.enable";

	private StatisticsStore statisticsStore;
	private long cleanInterval;
	private boolean isCleanEnable;

	/**
	 * Constructor takes statistic store as the parameter and sets clean interval that is needed
	 * for the statistics cleaning
	 *
	 * @param statisticsStore store that will be cleared by the cleaner
	 */
	public StatisticsStoreCleaner(StatisticsStore statisticsStore) {
		this.statisticsStore = statisticsStore;
		long defaultCleanInterval = 1000 * 60 * 5; //300s default cleaning time
		this.cleanInterval = Long.parseLong(SynapsePropertiesLoader.getPropertyValue(CLEAN_INTERVAL,
		                                                                             String.valueOf(
				                                                                             defaultCleanInterval)));
		this.isCleanEnable = Boolean.parseBoolean(
				SynapsePropertiesLoader.getPropertyValue(CLEAN_ENABLE, String.valueOf(true)));
		if (isCleanEnable) {
			if (log.isDebugEnabled()) {
				log.debug("Statistics cleaning is will be occurred with interval : " +
				          cleanInterval / 1000 + " s.");
			}
		}
	}

	/**
	 * Clean the expired statistics from the statistics Store
	 */
	public void clean() {
		if (!isCleanEnable) {
			if (log.isDebugEnabled()) {
				log.debug("Statistics cleaning is disabled");
			}
			return;
		}
		if (statisticsStore == null) {
			if (log.isDebugEnabled()) {
				log.debug("There are no statistics to be cleaned");
			}
			return;
		}
		statisticsStore.cleanStatistics();
		if (log.isDebugEnabled()) {
			log.debug("Existing Statistics Cleaned");
		}
	}

	/**
	 * This method returns whether statistics cleaning is enable or disabled in synapse.properties
	 *
	 * @return true if statistic cleaning enabled
	 */
	public boolean isCleanEnable() {
		return isCleanEnable;
	}

	/**
	 * This method returns the statistics clean interval
	 *
	 * @return statistics cleaning interval
	 */
	public long getCleanInterval() {
		return cleanInterval;
	}
}


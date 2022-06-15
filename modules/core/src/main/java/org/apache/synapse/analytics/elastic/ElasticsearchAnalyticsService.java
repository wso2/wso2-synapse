/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.analytics.elastic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.analytics.AnalyticsConstants;
import org.apache.synapse.analytics.AnalyticsService;
import org.apache.synapse.analytics.schema.AnalyticsDataSchema;
import org.apache.synapse.config.SynapsePropertiesLoader;

public final class ElasticsearchAnalyticsService implements AnalyticsService {

    private static final Log log = LogFactory.getLog(ElasticsearchAnalyticsService.class);
    private static ElasticsearchAnalyticsService instance = null;
    private boolean enabled = false;
    private String analyticsDataPrefix;

    private ElasticsearchAnalyticsService() {
        loadConfigurations();
    }

    public static synchronized ElasticsearchAnalyticsService getInstance() {
        if (instance == null) {
            instance = new ElasticsearchAnalyticsService();
        }
        return instance;
    }

    private void loadConfigurations() {
        this.enabled = SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.ELASTICSEARCH_ENABLED, false);
        this.analyticsDataPrefix = SynapsePropertiesLoader.getPropertyValue(
                AnalyticsConstants.SynapseConfiguration.ELASTICSEARCH_PREFIX, "SYNAPSE_ANALYTICS_DATA");
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void publish(AnalyticsDataSchema analytic) {
        if (!isEnabled()) {
            return;
        }

        String logOutput = this.analyticsDataPrefix + " " + analytic.getJsonString();
        log.info(logOutput);
    }
}

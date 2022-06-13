/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry;

import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.OpenTelemetryManager;
import org.apache.synapse.config.SynapsePropertiesLoader;

/**
 * Holds the OpenTelemetry Manager, and configurations related to it.
 */
public class OpenTelemetryManagerHolder {
    private static boolean isCollectingPayloads;
    private static boolean isCollectingProperties;
    private static OpenTelemetryManager openTelemetryManager;

    /**
     * Prevents Instantiation.
     */
    private OpenTelemetryManagerHolder() {}

    /**
     * Loads Tracing configurations by creating an instance of the given type required for the OpenTelemetryManager.
     */
    public static void loadTracerConfigurations() {

        String classpath = SynapsePropertiesLoader.getPropertyValue(TelemetryConstants.TRACE_TYPE_CLASS,
                TelemetryConstants.DEFAULT_TRACE_CLASS);
        try {
            openTelemetryManager = (OpenTelemetryManager) Class.forName(classpath).newInstance();
            openTelemetryManager.init();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException exception) {
            System.out.println(exception);
        }
    }

    /**
     * Sets flags that denote whether to collect payloads and properties.
     *
     * @param collectPayloads   Whether to collect payloads
     * @param collectProperties Whether to collect properties
     */
    public static void setCollectingFlags(boolean collectPayloads, boolean collectProperties) {
        isCollectingPayloads = collectPayloads;
        isCollectingProperties = collectProperties;
    }

    public static boolean isCollectingPayloads() {
        return isCollectingPayloads;
    }

    public static boolean isCollectingProperties() {
        return isCollectingProperties;
    }

    public static OpenTelemetryManager getOpenTelemetryManager() {
        return openTelemetryManager;
    }
}

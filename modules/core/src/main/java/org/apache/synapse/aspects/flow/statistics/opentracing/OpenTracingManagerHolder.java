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

package org.apache.synapse.aspects.flow.statistics.opentracing;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.samplers.ConstSampler;
import org.apache.synapse.aspects.flow.statistics.opentracing.management.JaegerTracingManager;
import org.apache.synapse.aspects.flow.statistics.opentracing.management.OpenTracingManager;

/**
 * Holds the OpenTracing Manager, and configurations related to it.
 */
public class OpenTracingManagerHolder {
    private static boolean isCollectingPayloads;
    private static boolean isCollectingProperties;
    private static OpenTracingManager openTracingManager;

    /**
     * Prevents Instantiation.
     */
    private OpenTracingManagerHolder() {}

    /**
     * Loads Jaeger configurations required for the OpenTracingManager.
     *
     * @param samplerManagerHostPort    Jaeger sampler host and port.
     * @param senderAgentHost           Jaeger sender agent host.
     * @param senderAgentPort           Jaeger sender agent port.
     * @param logSpans                  Log spans in Jaeger reporter or not.
     * @param reporterMaxQueueSize      Max queue size of the Jaeger reporter.
     * @param reporterFlushInterval     Flush interval of the Jaeger reporter.
     */
    public static void loadJaegerConfigurations(String samplerManagerHostPort,
                                                String senderAgentHost,
                                                int senderAgentPort,
                                                boolean logSpans,
                                                int reporterMaxQueueSize,
                                                int reporterFlushInterval) {
        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1)
                .withManagerHostPort(samplerManagerHostPort);
        Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
                .withAgentHost(senderAgentHost)
                .withAgentPort(senderAgentPort);
        Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration()
                .withLogSpans(logSpans)
                .withSender(sender)
                .withMaxQueueSize(reporterMaxQueueSize)
                .withFlushInterval(reporterFlushInterval);
        openTracingManager = new JaegerTracingManager(sampler, reporter);
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

    public static OpenTracingManager getOpenTracingManager() {
        return openTracingManager;
    }
}

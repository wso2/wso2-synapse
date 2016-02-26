/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class InboundEPStatisticCollector extends RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(InboundEPStatisticCollector.class);

    /**
     * Reports statistics for Inbound.
     *
     * @param messageContext      Current MessageContext of the flow.
     * @param inboundName         Inbound name.
     * @param aspectConfiguration Aspect Configuration for the inbound EP.
     * @param createStatisticLog  It is statistic flow start or end.
     */
    public static void reportStatisticsForInbound(MessageContext messageContext, String inboundName,
                                                  AspectConfiguration aspectConfiguration, boolean createStatisticLog) {
        if (isStatisticsEnabled()) {
            boolean isCollectingStatistics = (inboundName != null && aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());
            boolean isCollectingTracing = (inboundName != null && aspectConfiguration != null && aspectConfiguration.isTracingEnabled());

            messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, isCollectingStatistics);
            messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, isCollectingTracing);

            if (isCollectingStatistics) {
                if (createStatisticLog) {
                    setStatisticsTraceId(messageContext);
                    createLogForMessageCheckpoint(messageContext, inboundName, ComponentType.INBOUNDENDPOINT, null,
                                                  true, false, false, true);
                } else {
                    if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) {
                        createLogForFinalize(messageContext);
                    } else {
                        log.error("Trying close statistic entry without Statistic ID");
                    }
                }
            }
        }
    }
}

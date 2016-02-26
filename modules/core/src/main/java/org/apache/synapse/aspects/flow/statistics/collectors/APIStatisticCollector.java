/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
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

public class APIStatisticCollector extends RuntimeStatisticCollector{

    private static final Log log = LogFactory.getLog(APIStatisticCollector.class);

    /**
     * Reports statistics for API.
     *
     * @param messageContext      Current MessageContext of the flow.
     * @param apiName             API name.
     * @param aspectConfiguration Aspect Configuration for the API.
     */
    public static void reportApiStatistics(MessageContext messageContext, String apiName,
                                           AspectConfiguration aspectConfiguration) {
        if (isStatisticsEnabled()) {
            boolean isCollectingStatistics = (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());
            boolean isCollectingTracing = (aspectConfiguration != null && aspectConfiguration.isTracingEnabled());

            messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, isCollectingStatistics);
            messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, isCollectingTracing);

            if (isCollectingStatistics) {
                setStatisticsTraceId(messageContext);
                createLogForMessageCheckpoint(messageContext, apiName, ComponentType.API, null, true, false, false, true);
            }
        }
    }
}

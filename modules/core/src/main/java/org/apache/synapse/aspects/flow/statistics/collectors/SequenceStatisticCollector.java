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

public class SequenceStatisticCollector extends RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(SequenceStatisticCollector.class);
    /**
     * Reports statistics for the Sequence.
     *
     * @param messageContext      Current MessageContext of the flow.
     * @param sequenceName        Sequence name.
     * @param parentName          Parent component name.
     * @param aspectConfiguration Aspect Configuration for the Sequence.
     * @param isCreateLog         It is statistic flow start or end.
     */
    public static void reportStatisticForSequence(MessageContext messageContext, String sequenceName, String parentName,
                                                  AspectConfiguration aspectConfiguration, boolean isCreateLog) {
        if (isStatisticsEnabled()) {
            Boolean isStatCollected =
                    (Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);
            if (isStatCollected == null) {
                if (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable()) {
                    if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null && !isCreateLog) {
                        log.error("Trying close statistic entry without Statistic ID");
                        return;
                    }
                    setStatisticsTraceId(messageContext);
                    createStatisticForSequence(messageContext, sequenceName, isCreateLog);
                    messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
                } else {
                    messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
                }
            } else {
                if ((messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) && isStatCollected) {
                    createStatisticForSequence(messageContext, sequenceName, isCreateLog);
                }
            }
        }
    }

    private static void createStatisticForSequence(MessageContext messageContext, String sequenceName,
                                                     boolean isCreateLog) {
        if (isCreateLog) {
            createLogForMessageCheckpoint(messageContext, sequenceName, ComponentType.SEQUENCE, null, true, false,
                                          false, true);
        } else {
            createLogForMessageCheckpoint(messageContext, sequenceName, ComponentType.SEQUENCE, null, false, false,
                                          false, true);
        }
    }
}

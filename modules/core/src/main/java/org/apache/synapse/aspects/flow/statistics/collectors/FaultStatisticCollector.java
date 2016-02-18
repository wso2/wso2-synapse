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
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndFlowEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.FaultEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

public class FaultStatisticCollector extends RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(FaultStatisticCollector.class);

    /**
     * Reports that message flow encountered a Fault During Mediation.
     *
     * @param statisticId Statistic ID of the message flow.
     * @param cloneId     Message flow branching number.
     */
    public static void reportFault(String statisticId, int cloneId) {
        if (runtimeStatistics.containsKey(statisticId)) {
            StatisticsEntry statisticsEntry = runtimeStatistics.get(statisticId);
            statisticsEntry.reportFault(cloneId);
        }
    }

    /**
     * Reports fault in message flow.
     *
     * @param messageContext Current MessageContext of the flow.
     */
    public static boolean reportFault(MessageContext messageContext) {
        if (shouldReportStatistic(messageContext)) {
            boolean isFaultCreated = isFaultAlreadyReported(messageContext);
            if (isFaultCreated) {
                EndFlowEvent endFlowEvent =
                        new EndFlowEvent(messageContext, System.currentTimeMillis());
                messageDataStore.enqueue(endFlowEvent);
            } else {
                FaultEvent faultEvent = new FaultEvent(messageContext);
                messageDataStore.enqueue(faultEvent);
            }
            messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_FAULT_REPORTED, !isFaultCreated);
            return true;
        }
        return false;
    }

    private static boolean isFaultAlreadyReported(MessageContext synCtx) {
        Object object = synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_FAULT_REPORTED);
        if (object == null) {
            return false;
        } else if ((Boolean) object) {
            return true;
        }
        return false;
    }

}

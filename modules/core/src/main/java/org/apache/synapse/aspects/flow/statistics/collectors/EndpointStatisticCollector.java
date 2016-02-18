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
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.EndpointStatisticEntry;
import org.apache.synapse.aspects.flow.statistics.data.raw.EndpointStatisticLog;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndpointEvent;
import org.apache.synapse.core.SynapseEnvironment;

public class EndpointStatisticCollector extends RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(EndpointStatisticCollector.class);

    /**
     * Reports Endpoint Statistics.
     *
     * @param messageContext               Current MessageContext of the flow.
     * @param endpointId                   Endpoint identification number.
     * @param endpointName                 Endpoint name.
     * @param individualStatisticCollected Whether individual statistic of this endpoint is collected.
     * @param isCreateLog                  It is statistic flow start or end.
     */
    public static void reportStatisticForEndpoint(MessageContext messageContext, String endpointId, String endpointName,
                                                  boolean individualStatisticCollected, boolean isCreateLog) {
        if (shouldReportStatistic(messageContext)) {
            createLogForMessageCheckpoint(messageContext, endpointName, ComponentType.ENDPOINT, null, isCreateLog,
                                          false, false, false);
        }
        if (individualStatisticCollected) {
            StatisticsReportingEvent statisticsReportingEvent;
            if (isCreateLog) {
                setStatisticsTraceId(messageContext);
            }
            statisticsReportingEvent = new EndpointEvent(messageContext, endpointId, endpointName, isCreateLog);
            messageDataStore.enqueue(statisticsReportingEvent);
        }
    }

    /**
     * Creates Endpoint Statistics for the endpoint.
     *
     * @param statisticId        Statistic ID of the message flow.
     * @param timestamp          Time of the statistic event.
     * @param endpointId         Endpoint unique identification Number.
     * @param endpointName       Endpoint name.
     * @param synapseEnvironment Synapse environment of the message flow.
     * @param time               Time of the Stat reporting.
     * @param isCreateLog        Is this a creation of a log.
     */
    public static void createEndpointStatistics(String statisticId, String timestamp, String endpointId, String endpointName,
                                                SynapseEnvironment synapseEnvironment, long time, boolean isCreateLog) {
        if (isCreateLog) {
            EndpointStatisticEntry endpointStatisticEntry;
            if (endpointStatistics.containsKey(statisticId)) {
                endpointStatisticEntry = endpointStatistics.get(statisticId);
            } else {
                endpointStatisticEntry = new EndpointStatisticEntry();
                endpointStatistics.put(statisticId, endpointStatisticEntry);
            }
            endpointStatisticEntry.createEndpointLog(endpointId, statisticId, timestamp, endpointName, time);
        } else {
            EndpointStatisticEntry endpointStatisticEntry;
            if (endpointStatistics.containsKey(statisticId)) {
                endpointStatisticEntry = endpointStatistics.get(statisticId);
                EndpointStatisticLog endpointStatisticLog =
                        endpointStatisticEntry.closeEndpointLog(endpointId, endpointName, time);
                if (endpointStatisticLog != null) {
                    synapseEnvironment.getCompletedStatisticStore()
                            .putCompletedEndpointStatisticEntry(endpointStatisticLog);
                    if (endpointStatisticEntry.getSize() == 0) {
                        endpointStatistics.remove(statisticId);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Endpoint statistic collected for Endpoint:" + endpointStatisticLog.getComponentId());
                    }
                }
            }
        }
    }

}

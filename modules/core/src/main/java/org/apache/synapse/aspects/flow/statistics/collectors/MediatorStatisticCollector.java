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

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.ContinuationEndEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.ParentReopenEvent;

public class MediatorStatisticCollector extends RuntimeStatisticCollector {

    /**
     * Report statistics for the Component
     *
     * @param messageContext Current MessageContext of the flow.
     * @param componentName  Component name.
     * @param componentType  Component type of the component.
     * @param parentName     Parent of the component.
     * @param isCreateLog    It is statistic flow start or end.
     * @param isCloneLog     is this a clone incident
     * @param isAggregateLog is this a Aggregate incident
     */
    public static void reportStatisticForMessageComponent(MessageContext messageContext, String componentName,
                                                          ComponentType componentType, String parentName,
                                                          boolean isCreateLog, boolean isCloneLog,
                                                          boolean isAggregateLog, boolean isAlteringContent) {
        if (shouldReportStatistic(messageContext)) {
            createLogForMessageCheckpoint(messageContext, componentName, componentType, parentName, isCreateLog,
                                          isCloneLog, isAggregateLog, isAlteringContent);
        }
    }

    public static void openLogForContinuation(MessageContext messageContext, String componentId) {
        if (shouldReportStatistic(messageContext)) {
            StatisticsReportingEvent statisticsReportingEvent;
            statisticsReportingEvent = new ParentReopenEvent(messageContext, componentId);
            messageDataStore.enqueue(statisticsReportingEvent);
        }
    }

    /**
     * Asynchronously remove continuation state from the message flow.
     *
     * @param messageContext message context
     */
    public static void removeContinuationState(MessageContext messageContext) {
        if (shouldReportStatistic(messageContext)) {
            StatisticsReportingEvent statisticsReportingEvent = new ContinuationEndEvent(messageContext);
            messageDataStore.enqueue(statisticsReportingEvent);
        }
    }

}

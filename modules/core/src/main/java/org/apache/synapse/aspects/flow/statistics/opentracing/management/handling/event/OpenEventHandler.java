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

package org.apache.synapse.aspects.flow.statistics.opentracing.management.handling.event;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * The interface for handling open events reported by the OpenEventCollector.
 */
public interface OpenEventHandler {

    /**
     * Handles an open entry event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenEntryEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    /**
     * Handles an open child entry event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenChildEntryEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    /**
     * Handles a flow continuable open event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenFlowContinuableEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    /**
     * Handles a flow splitting open event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenFlowSplittingEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    /**
     * Handles a flow aggregate open event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenFlowAggregateEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx);

    /**
     * Handles a flow asynchronous open event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenFlowAsynchronousEvent(BasicStatisticDataUnit statisticDataUnit,
                                         MessageContext synCtx);

    /**
     * Handles an open continuation event.
     * @param statisticDataUnit Statistic data unit object.
     * @param synCtx            Message context.
     */
    void handleOpenContinuationEvents(BasicStatisticDataUnit statisticDataUnit,
                                      MessageContext synCtx);
}

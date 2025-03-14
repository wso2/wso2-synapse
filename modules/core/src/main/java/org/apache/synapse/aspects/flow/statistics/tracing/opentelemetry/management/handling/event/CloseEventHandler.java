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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.handling.event;

import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;

/**
 * The interface for handling close events reported by the CloseEventCollector.
 */
public interface CloseEventHandler {
    /**
     * Handles a close entry event.
     * @param basicStatisticDataUnit    Basic statistic data unit object.
     * @param synCtx                    Message context.
     */
    void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    /**
     * Handles a forceful close event.
     * @param basicStatisticDataUnit    Basic statistic data unit object.
     * @param synCtx                    Message context.
     */
    void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    /**
     * Handles a try end flow event.
     * @param basicStatisticDataUnit    Basic statistic data unit object.
     * @param synCtx                    Message context.
     */
    void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);

    /**
     * Handles a close flow event.
     *
     * @param synCtx Message context.
     */
    void handleScatterGatherFinishEvent(MessageContext synCtx);

    /**
     * Handles a close entry with error event.
     * @param basicStatisticDataUnit    Basic statistic data unit object.
     * @param synCtx                    Message context.
     */
    void handleCloseEntryWithErrorEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx);
}

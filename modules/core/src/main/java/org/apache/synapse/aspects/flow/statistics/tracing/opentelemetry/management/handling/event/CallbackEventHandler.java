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

/**
 * The interface for handling callback events reported by the CallbackStatisticCollector.
 */
public interface CallbackEventHandler {

    /**
     * Handles callback addition.
     * @param messageContext    Message context.
     * @param callbackId        Callback id.
     */
    void handleAddCallback(MessageContext messageContext, String callbackId);

    /**
     * Handles callback completion.
     * @param oldMessageContext Old message context.
     * @param callbackId        Callback id.
     */
    void handleCallbackCompletionEvent(MessageContext oldMessageContext, String callbackId);

    /**
     * Handles parents update for a callback.
     * @param oldMessageContext Old message context.
     * @param callbackId        Callback id.
     */
    void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId);

    /**
     * Handles callback completion report.
     * @param synapseOutMsgCtx  Synapse out message context.
     * @param callbackId        Callback id.
     */
    void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId);
}

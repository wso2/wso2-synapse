/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackCompletionEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackHandledEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackReceivedEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackSentEvent;
import org.apache.synapse.aspects.flow.statistics.tracing.holder.TracingManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.SpanExtendingCounter;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;

public class CallbackStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(CallbackStatisticCollector.class);

	/**
	 * Enqueue CallbackSentEvent event to the event queue. This corresponds to the registering of callback for
	 * the message flow by the SynapseCallbackReceiver.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 * @param callbackId     Callback Id.
	 */
	public static void addCallback(MessageContext messageContext, String callbackId) {
		if (shouldReportStatistic(messageContext)) {
			CallbackDataUnit dataUnit = new CallbackDataUnit();
			dataUnit.setCallbackId(callbackId);
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(messageContext, null));

			CallbackSentEvent callbackSentEvent = new CallbackSentEvent(dataUnit);
            addEventAndIncrementCallbackCount(messageContext, callbackSentEvent);

			System.out.println("MsgCtx: " + Util.getObjectReference(messageContext) + "[ADD_CALLBACK] Counter - " + SpanExtendingCounter.incrementAndGetValue() + " CallbackId - " + callbackId);
			TracingManagerHolder.getOpenTracingManager().getHandler()
					.handleAddCallback(messageContext, callbackId);
		}
	}

	/**
	 * Enqueue CallbackCompletionEvent event to the event queue. This event inform that callback handling finished or
	 * callback is removed by the TimeoutHandler.
	 *
	 * @param oldMessageContext Current MessageContext of the flow.
	 * @param callbackId        Callback Id.
	 */
	public static void callbackCompletionEvent(MessageContext oldMessageContext, String callbackId) {
		if (shouldReportStatistic(oldMessageContext)) {
			CallbackDataUnit dataUnit = new CallbackDataUnit();
			dataUnit.setCallbackId(callbackId);
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(oldMessageContext.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(oldMessageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(oldMessageContext, null));
			dataUnit.setIsOutOnlyFlow(StatisticDataCollectionHelper.isOutOnlyFlow(oldMessageContext));

			CallbackCompletionEvent callbackCompletionEvent = new CallbackCompletionEvent(dataUnit);
            addEventAndDecrementCallbackCount(oldMessageContext, callbackCompletionEvent);

			System.out.println("MsgCtx: " + Util.getObjectReference(oldMessageContext) + "[CALLBACK_COMPLETION_EVENT] Counter - " + SpanExtendingCounter.decrementAndGetValue() + " CallbackId - " + callbackId);
			TracingManagerHolder.getOpenTracingManager().getHandler()
					.handleCallbackCompletionEvent(oldMessageContext, callbackId);
		}
	}

	/**
	 * Enqueue CallbackReceivedEvent event to the event queue. This informs that callback has received its response.
	 *
	 * @param oldMessageContext Current MessageContext of the flow.
	 * @param callbackId        Callback Id.
	 */
	public static void updateParentsForCallback(MessageContext oldMessageContext, String callbackId) {
		if (shouldReportStatistic(oldMessageContext)) {
			CallbackDataUnit dataUnit = new CallbackDataUnit();
			dataUnit.setCallbackId(callbackId);
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(oldMessageContext.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(oldMessageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(oldMessageContext, null));
			dataUnit.setIsOutOnlyFlow(StatisticDataCollectionHelper.isOutOnlyFlow(oldMessageContext));

			CallbackReceivedEvent callbackReceivedEvent = new CallbackReceivedEvent(dataUnit);
            addEvent(oldMessageContext, callbackReceivedEvent);

			System.out.println("MsgCtx: " + Util.getObjectReference(oldMessageContext) + "[UPDATE_PARENTS_FOR_CALLBACK] CallbackId - " + callbackId);
			TracingManagerHolder.getOpenTracingManager().getHandler()
					.handleUpdateParentsForCallback(oldMessageContext, callbackId);
		}
	}

	/**
	 * Enqueue CallbackHandledEvent event to the event queue. This informs that callback handling is finished after
	 * receiving the callback.
	 *
	 * @param synapseOutMsgCtx Old MessageContext of the flow.
	 * @param callbackId       Callback Id.
	 */
	public static void reportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {
		if (shouldReportStatistic(synapseOutMsgCtx)) {
			CallbackDataUnit dataUnit = new CallbackDataUnit();
			dataUnit.setCallbackId(callbackId);
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(synapseOutMsgCtx.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(synapseOutMsgCtx));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(synapseOutMsgCtx, null));

			CallbackHandledEvent callbackHandledEvent = new CallbackHandledEvent(dataUnit);
            addEventAndDecrementCallbackCount(synapseOutMsgCtx, callbackHandledEvent);

			SpanExtendingCounter.decrementAndGetValue();

			System.out.println("MsgCtx: " + Util.getObjectReference(synapseOutMsgCtx) + "[REPORT_CALLBACK_HANDLING_COMPLETION] Counter - " + SpanExtendingCounter.getValue() + " CallbackId - " + callbackId);
			TracingManagerHolder.getOpenTracingManager().getHandler()
					.handleReportCallbackHandlingCompletion(synapseOutMsgCtx, callbackId);
		}
	}
}

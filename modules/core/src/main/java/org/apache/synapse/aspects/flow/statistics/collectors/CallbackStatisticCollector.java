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
			statisticEventQueue.enqueue(callbackSentEvent);
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
			statisticEventQueue.enqueue(callbackCompletionEvent);
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
			statisticEventQueue.enqueue(callbackReceivedEvent);
		}
	}

	/**
	 * Enqueue CallbackHandledEvent event to the event queue. This informs that callback handling is finished after
	 * receiving the callback.
	 *
	 * @param synapseOutMsgCtx Old MessageContext of the flow.
	 * @param synNewCtx        New MessageContext of the flow.
	 * @param callbackId       Callback Id.
	 */
	public static void reportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, MessageContext synNewCtx,
	                                                    String callbackId) {
		if (shouldReportStatistic(synapseOutMsgCtx)) {
			CallbackDataUnit dataUnit = new CallbackDataUnit();
			dataUnit.setCallbackId(callbackId);
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(synapseOutMsgCtx.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(synapseOutMsgCtx));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(synapseOutMsgCtx, null));

			CallbackHandledEvent callbackHandledEvent = new CallbackHandledEvent(dataUnit);
			statisticEventQueue.enqueue(callbackHandledEvent);
		}
	}

	/**
	 * Registers callback information for the message flow on the corresponding StatisticsEntry.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void addCallbacks(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).addCallback(callbackDataUnit);
		}
	}

	/**
	 * Updates end time of the statistics logs in the StatisticsEntry after corresponding callback is removed from
	 * SynapseCallbackReceiver.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void updateForReceivedCallback(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).updateCallbackReceived(callbackDataUnit);
		}
	}

	/**
	 * Removes specified callback info from StatisticsEntry for the message flow after all the processing for that
	 * callback is ended.
	 *
	 * @param callbackDataUnit raw statistic data unit
	 */
	public static void removeCallback(CallbackDataUnit callbackDataUnit) {
		if (runtimeStatistics.containsKey(callbackDataUnit.getStatisticId())) {
			runtimeStatistics.get(callbackDataUnit.getStatisticId()).removeCallback(callbackDataUnit);
		}
	}
}

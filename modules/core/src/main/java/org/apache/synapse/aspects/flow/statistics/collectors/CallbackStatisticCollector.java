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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackHandledEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackReceivedEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.CallbackSentEvent;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.rest.RESTConstants;

public class CallbackStatisticCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(ResourceStatisticCollector.class);

	/**
	 * Report callback received for message flow.
	 *
	 * @param oldMessageContext Current MessageContext of the flow.
	 * @param callbackId        Callback Id.
	 */
	public static void reportCallbackReceived(MessageContext oldMessageContext, String callbackId) {
		if (isStatisticsTraced(oldMessageContext)) {
			createLogForCallbackReceived(oldMessageContext, callbackId);
			createLogForRemoveCallback(oldMessageContext, callbackId);
			createLogForFinalize(oldMessageContext);
		}
	}

	/**
	 * Register callback for message flow.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 * @param callbackId     Callback Id.
	 */
	public static void addCallbackEntryForStatistics(MessageContext messageContext, String callbackId) {
		if (isStatisticsTraced(messageContext)) {
			createLogForCallbackRegister(messageContext, callbackId);
		}
	}

	/**
	 * Updates parents after callback received for message flow.
	 *
	 * @param oldMessageContext Current MessageContext of the flow.
	 * @param callbackId        Callback Id.
	 */
	public static void updateStatisticLogsForReceivedCallbackLog(MessageContext oldMessageContext, String callbackId) {
		if (isStatisticsTraced(oldMessageContext)) {
			createLogForCallbackReceived(oldMessageContext, callbackId);
		}
	}

	/**
	 * Updates properties after callback received for message flow.
	 *
	 * @param synapseOutMsgCtx Old MessageContext of the flow.
	 * @param synNewCtx        New MessageContext of the flow.
	 * @param callbackId       Callback Id.
	 */
	public static void reportFinishingHandlingCallback(MessageContext synapseOutMsgCtx, MessageContext synNewCtx,
	                                                   String callbackId) {
		if (isStatisticsTraced(synapseOutMsgCtx)) {
			createLogForRemoveCallback(synapseOutMsgCtx, callbackId);
			if (shouldReportStatistic(synapseOutMsgCtx)) {
				Boolean isContinuationCall = (Boolean) synNewCtx.getProperty(SynapseConstants.CONTINUATION_CALL);
				Object synapseRestApi = synapseOutMsgCtx.getProperty(RESTConstants.REST_API_CONTEXT);
				Object restUrlPattern = synapseOutMsgCtx.getProperty(RESTConstants.REST_URL_PATTERN);
				Object synapseResource = synapseOutMsgCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
				if (synapseRestApi != null) {
					String textualStringName;
					if (restUrlPattern != null) {
						textualStringName = (String) synapseRestApi + restUrlPattern;
					} else {
						textualStringName = (String) synapseRestApi;
					}
					createLogForMessageCheckpoint(synapseOutMsgCtx, textualStringName, ComponentType.RESOURCE, null,
					                              false, false, false, false);
				} else if (synapseResource != null) {
					createLogForMessageCheckpoint(synapseOutMsgCtx, (String) synapseResource, ComponentType.RESOURCE,
					                              null, false, false, false, false);
				}
			}
			createLogForFinalize(synapseOutMsgCtx);
		}
	}

	/**
	 * Registers callback information for the message flow on the corresponding statistics entry.
	 *
	 * @param statisticsTraceId Statistic Id for the message flow.
	 * @param callbackId        Callback identification number.
	 */
	public static void addCallbacks(String statisticsTraceId, String callbackId, int msgId) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				runtimeStatistics.get(statisticsTraceId).addCallback(callbackId, msgId);
			}
		}
	}

	/**
	 * Updates end time of the statistics logs after corresponding callback is removed from
	 * SynapseCallbackReceiver.
	 *
	 * @param statisticsTraceId  Statistic Id for the message flow
	 * @param callbackId         callback identification number
	 * @param endTime            callback removal time at SynapseCallbackReceiver
	 * @param isContinuation     whether this callback entry was a continuation call
	 * @param synapseEnvironment Synapse environment of the message flow
	 */
	public static void updateForReceivedCallback(String statisticsTraceId, String callbackId, Long endTime,
	                                             Boolean isContinuation, SynapseEnvironment synapseEnvironment,
	                                             boolean isOutOnlyFlow) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				runtimeStatistics.get(statisticsTraceId)
				                 .updateCallbackReceived(callbackId, endTime, isContinuation, isOutOnlyFlow);
			}
		}
	}

	/**
	 * Removes specified callback info for a message flow after all the processing for that
	 * callback is ended.
	 *
	 * @param statisticsTraceId message context
	 * @param callbackId        callback identification number
	 */
	public static void removeCallback(String statisticsTraceId, String callbackId) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				runtimeStatistics.get(statisticsTraceId).removeCallback(callbackId);
				if (log.isDebugEnabled()) {
					log.debug("Removed callback from statistic entry");
				}
			}
		}
	}

	private static void createLogForCallbackReceived(MessageContext oldMessageContext, String msgID) {
		CallbackReceivedEvent callbackReceivedEvent =
				new CallbackReceivedEvent(oldMessageContext, msgID, System.currentTimeMillis());
		messageDataStore.enqueue(callbackReceivedEvent);
	}

	private static void createLogForRemoveCallback(MessageContext synOutCtx, String msgID) {
		CallbackHandledEvent callbackHandledEvent = new CallbackHandledEvent(synOutCtx, msgID);
		messageDataStore.enqueue(callbackHandledEvent);
	}

	private static void createLogForCallbackRegister(MessageContext messageContext, String MsgId) {
		CallbackSentEvent callbackSentEvent = new CallbackSentEvent(messageContext, MsgId);
		messageDataStore.enqueue(callbackSentEvent);
	}

	private static boolean isStatisticsTraced(MessageContext messageContext) {
		Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		return (statID != null && isStatisticsEnabled());
	}

}

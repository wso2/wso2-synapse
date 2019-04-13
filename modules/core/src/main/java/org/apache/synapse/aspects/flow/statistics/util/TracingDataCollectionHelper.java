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

package org.apache.synapse.aspects.flow.statistics.util;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingEvent;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingFlow;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingPayload;
import org.apache.synapse.aspects.flow.statistics.publishing.PublishingPayloadEvent;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides various methods used for tracing data collections.
 */
public class TracingDataCollectionHelper {

	/**
	 * Extract payload from the synapse message context.
	 *
	 * @param messageContext synapse message context
	 * @return payload
	 */
	public static String collectPayload(MessageContext messageContext) {
		String payload = null;
		try {
			org.apache.axis2.context.MessageContext a2mc =
					((Axis2MessageContext) messageContext).getAxis2MessageContext();
			// The message will not be built inside the EI unless the mediation flow includes a content aware mediator
			// Therefore when tracing enabled following will explicitly build the message..
			RelayUtils.buildMessage(a2mc, false);
			if (JsonUtil.hasAJsonPayload(a2mc)) {
				payload = JsonUtil.jsonPayloadToString(a2mc);
			} else {
				payload = messageContext.getEnvelope().toString();
			}
		} catch (Exception e) {
			// SOAP envelop is not created yet
			payload = "NONE";
		}
		return payload;
	}

	/**
	 * Extract properties from the synapse message context.
	 *
	 * @param synCtx synapse message context
	 * @return property map
	 */
	public static Map<String, Object> extractContextProperties(MessageContext synCtx) {
		Set<String> propertySet = synCtx.getPropertyKeySet();
		Map<String, Object> propertyMap = new TreeMap<>();

		for (String property : propertySet) {
			Object propertyValue = synCtx.getProperty(property);
			propertyMap.put(property, propertyValue);
		}

		// Remove message-flow-tracer properties
		propertyMap.remove(SynapseConstants.STATISTICS_STACK);
		propertyMap.remove(StatisticsConstants.STAT_COLLECTOR_PROPERTY);

		return propertyMap;
	}

	/**
	 * Extract transport headers from the synapse message context.
	 *
	 * @param synCtx synapse message context
	 * @return transport headers map
	 */
	public static Map<String, Object> extractTransportProperties(MessageContext synCtx) {
		Map<String, Object> transportPropertyMap = new TreeMap<>();

		Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
		org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
		Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

		if (headers != null && headers instanceof Map) {
			Map headersMap = (Map) headers;
			Set<String> axis2PropertySet = headersMap.keySet();
			for (String entry : axis2PropertySet) {
				transportPropertyMap.put(entry, headersMap.get(entry));
			}
		}

		// Adding important transport related properties
		if (axis2MessageCtx.getTo() != null) {
			transportPropertyMap.put(SynapseConstants.HEADER_TO, axis2MessageCtx.getTo().getAddress());
		}

		if (axis2MessageCtx.getFrom() != null) {
			transportPropertyMap.put(SynapseConstants.HEADER_FROM, axis2MessageCtx.getFrom().getAddress());
		}

		if (axis2MessageCtx.getWSAAction() != null) {
			transportPropertyMap.put("WSAction", axis2MessageCtx.getWSAAction());
		}

		if (axis2MessageCtx.getSoapAction() != null) {
			transportPropertyMap.put("SOAPAction", axis2MessageCtx.getSoapAction());
		}

		if (axis2MessageCtx.getReplyTo() != null) {
			transportPropertyMap.put(SynapseConstants.HEADER_REPLY_TO, axis2MessageCtx.getReplyTo().getAddress());
		}

		if (axis2MessageCtx.getMessageID() != null) {
			transportPropertyMap.put(SynapseConstants.HEADER_MESSAGE_ID, axis2MessageCtx.getMessageID());
		}

		// Remove unnecessary properties
		if (transportPropertyMap.get("Cookie") != null) {
			transportPropertyMap.remove("Cookie");
		}

		return transportPropertyMap;
	}

	public static PublishingFlow createPublishingFlow(List<StatisticsLog> messageFlowLogs) {

		// Data structure using to serialize thr statistic data while publishing.
		PublishingFlow publishingFlow = new PublishingFlow();

		// Payload map which contains all the payloads of the message flow.
		Map<String, PublishingPayload> payloadMap = new HashMap<>();

		// Constants
		final String REFER = "#REFER:";
		final String BEFORE = "before-";
		final String AFTER = "after-";
		final Integer BEFORE_PAYLOAD = 8; // 8th attribute setting @PublishingEvent
		final Integer AFTER_PAYLOAD = 9; // 9th attribute setting @PublishingEvent


		String entryPoint = messageFlowLogs.get(0).getComponentName();
		String flowId = messageFlowLogs.get(0).getMessageFlowId();
		Integer entrypointHashcode = messageFlowLogs.get(0).getHashCode();

		boolean isTracingEnabledForFlow = messageFlowLogs.get(0).isTracingEnabled();

		for (int index = 0; index < messageFlowLogs.size(); index++) {
			StatisticsLog currentStatLog = messageFlowLogs.get(index);
            if (currentStatLog == null) {
                continue;
            }

			// Add each event to Publishing Flow
			publishingFlow.addEvent(new PublishingEvent(flowId, index, currentStatLog, entryPoint, entrypointHashcode));

			// Skip the rest of things, if message tracing is disabled
			if (!RuntimeStatisticCollector.isCollectingPayloads()) {
				continue;
			}

			// Skip flow is tracing is not enabled for the flow (from UI)
			if (!isTracingEnabledForFlow) {
				continue;
			}

			// Update children's immediateParent index
			List<Integer> childrenOfCurrent = currentStatLog.getChildren();
			for(Integer child:childrenOfCurrent) {
				messageFlowLogs.get(child).setImmediateParent(currentStatLog.getCurrentIndex());
			}

			if (currentStatLog.getBeforePayload() != null && currentStatLog.getAfterPayload() == null) {
				currentStatLog.setAfterPayload(currentStatLog.getBeforePayload());
			}

			if (currentStatLog.getBeforePayload() == null) {
				int parentIndex = currentStatLog.getImmediateParent();
				StatisticsLog parentStatLog = messageFlowLogs.get(parentIndex);

				if (parentStatLog.getAfterPayload().startsWith(REFER)) {
					// Parent also referring to after-payload
					currentStatLog.setBeforePayload(parentStatLog.getAfterPayload());
					currentStatLog.setAfterPayload(parentStatLog.getAfterPayload());

					String referringIndex = parentStatLog.getAfterPayload().split(":")[1];

					payloadMap.get(AFTER + referringIndex)
					               .addEvent(new PublishingPayloadEvent(index, BEFORE_PAYLOAD));
					payloadMap.get(AFTER + referringIndex)
					               .addEvent(new PublishingPayloadEvent(index, AFTER_PAYLOAD));

				} else {
					// Create a new after-payload reference
					currentStatLog.setBeforePayload(REFER + parentIndex);
					currentStatLog.setAfterPayload(REFER + parentIndex);

					payloadMap.get(AFTER + parentIndex)
					               .addEvent(new PublishingPayloadEvent(index, BEFORE_PAYLOAD));
					payloadMap.get(AFTER + parentIndex)
					               .addEvent(new PublishingPayloadEvent(index, AFTER_PAYLOAD));
				}

			} else {

				// For content altering components
				PublishingPayload publishingPayloadBefore = new PublishingPayload();
				publishingPayloadBefore.setPayload(currentStatLog.getBeforePayload());
				publishingPayloadBefore.addEvent(new PublishingPayloadEvent(index, BEFORE_PAYLOAD));
				payloadMap.put(BEFORE + index, publishingPayloadBefore);

				PublishingPayload publishingPayloadAfter = new PublishingPayload();
				publishingPayloadAfter.setPayload(currentStatLog.getAfterPayload());
				publishingPayloadAfter.addEvent(new PublishingPayloadEvent(index, AFTER_PAYLOAD));
				payloadMap.put(AFTER + index, publishingPayloadAfter);

			}

		}

		publishingFlow.setMessageFlowId(flowId);
		// Move all payloads to publishingFlow object
		publishingFlow.setPayloads(payloadMap.values());

		return publishingFlow;
	}
}

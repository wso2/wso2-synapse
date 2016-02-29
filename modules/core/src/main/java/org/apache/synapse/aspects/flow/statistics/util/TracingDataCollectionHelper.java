/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TracingDataCollectionHelper {

	public static String collectPayload(MessageContext messageContext) {
		String payload = null;
		try {
			payload = messageContext.getEnvelope().toString();
		} catch (Exception e) {
			// SOAP envelop is not created yet
			payload = "NONE";
		}
		return payload;
	}

	/**
	 * Extract message context properties
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

		return propertyMap;
	}

	/**
	 * Extract transport headers from context
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
}

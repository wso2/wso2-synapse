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
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

import java.util.LinkedList;
import java.util.List;

public class StatisticDataCollectionHelper {

	public static int getMessageFlowId(MessageContext messageContext) {
		int cloneId;
		if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID) != null) {
			cloneId = (Integer) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID);
		} else {
			cloneId = 0;
		}
		return cloneId;
	}

	public static String getStatisticTraceId(MessageContext messageContext) {
		return (String) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
	}

	public static int getNextIndex(MessageContext messageContext) {
		UniqueIdentifierObject uniqueIdentifierObject = (UniqueIdentifierObject) messageContext
				.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT);
		if (uniqueIdentifierObject == null) {
			uniqueIdentifierObject = new UniqueIdentifierObject();
			messageContext
					.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT, uniqueIdentifierObject);
			return uniqueIdentifierObject.getCurrentLevel();
		} else {
			return uniqueIdentifierObject.getNextIndex();
		}
	}

	public static int getParentIndex(MessageContext messageContext, Integer newParentIndex) {
		Integer parentIndex =
				(Integer) messageContext.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX);
		if (newParentIndex != null) {
			messageContext.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_INDEX, newParentIndex);
		}
		if (parentIndex == null) {
			return StatisticsConstants.DEFAULT_PARENT_INDEX;
		} else {
			return parentIndex;
		}
	}

	public static List<Integer> getParentList(MessageContext messageContext) {
		Object parentList = messageContext.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST);
		if (parentList != null) {
			messageContext.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST, new LinkedList<>());
			return (List<Integer>) parentList;
		} else {
			return null;
		}
	}

	//	public static int getCurrentIndex(MessageContext messageContext) {
	//		UniqueIdentifierObject uniqueIdentifierObject = (UniqueIdentifierObject) messageContext
	//				.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_INDEXING_OBJECT);
	//		if (uniqueIdentifierObject != null) {
	//			return uniqueIdentifierObject.getCurrentLevel();
	//
	//		} else {
	//			return StatisticsConstants.DEFAULT_PARENT_INDEX;
	//		}
	//	}

	public static boolean isOutOnlyFlow(MessageContext messageContext) {
		return "true".equals(messageContext.getProperty(SynapseConstants.OUT_ONLY));
	}

	public static boolean isContinuationCall(MessageContext messageContext) {
		Boolean isContinuation = (Boolean) messageContext.getProperty(SynapseConstants.CONTINUATION_CALL);
		return (isContinuation != null && isContinuation);
	}

	public static void collectData(MessageContext messageContext, boolean isContentAltering,
	                               Boolean isCollectingTracing, StatisticDataUnit statisticDataUnit) {

		statisticDataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
		statisticDataUnit.setFlowId(StatisticDataCollectionHelper.getMessageFlowId(messageContext));
		statisticDataUnit.setSynapseEnvironment(messageContext.getEnvironment());
		statisticDataUnit.setParentList(getParentList(messageContext));

		statisticDataUnit.setTime(System.currentTimeMillis());

		if (isCollectingTracing != null && isCollectingTracing) {
			if (RuntimeStatisticCollector.isCollectingPayloads() && isContentAltering) {//Change to Other one
				statisticDataUnit.setPayload(TracingDataCollectionHelper.collectPayload(messageContext));
			}
			if (RuntimeStatisticCollector.isCollectingProperties()) {
				statisticDataUnit
						.setContextPropertyMap(TracingDataCollectionHelper.extractContextProperties(messageContext));
				statisticDataUnit.setTransportPropertyMap(
						TracingDataCollectionHelper.extractTransportProperties(messageContext));
			}
		}
	}

	/**
	 * This method is used to collect all the parent indexs from message contexts which contributes to aggregate
	 * message at the end of the aggregation.
	 *
	 * @param messages aggregated message list
	 * @param newCtx   new aggregated message context
	 */
	public static void collectAggregatedParents(List<MessageContext> messages, MessageContext newCtx) {
		List<Integer> aggregateParents = new LinkedList<>();
		for (MessageContext synCtx : messages) {
			aggregateParents.add(StatisticDataCollectionHelper.getParentIndex(synCtx, null));
		}
		newCtx.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST, aggregateParents);
	}

}

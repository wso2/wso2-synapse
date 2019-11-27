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
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides various methods used for statistic collections.
 */
public class StatisticDataCollectionHelper {

	/**
	 * Get statistic trace id for this message flow.
	 *
	 * @param messageContext synapse message context.
	 * @return statistic trace id.
	 */
	public static String getStatisticTraceId(MessageContext messageContext) {
		return (String) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) + System.nanoTime();
	}

	/**
	 * Get message flow position for the current component.
	 *
	 * @param messageContext synapse message context.
	 * @return message flow position
	 */
	public static int getFlowPosition(MessageContext messageContext) {
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

	/**
	 * Get parent of this statistic component and sets current message flow position as next components parent.
	 *
	 * @param messageContext synapse message context.
	 * @param newParentIndex current message flow position.
	 * @return parent flow position
	 */
	public static int getParentFlowPosition(MessageContext messageContext, Integer newParentIndex) {
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

	/**
	 * Get Parent list for this component.
	 *
	 * @param messageContext synapse message context.
	 * @return parent list.
	 */
	public static List<Integer> getParentList(MessageContext messageContext) {
		Object parentList = messageContext.getProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST);
		if (parentList != null) {
			messageContext.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST, new LinkedList<>());
			return (List<Integer>) parentList;
		} else {
			return null;
		}
	}

	/**
	 * Checks is this is a Out_Only message flow.
	 *
	 * @param messageContext synapse message context.
	 * @return true is message flow is Out_Only flow.
	 */
	public static boolean isOutOnlyFlow(MessageContext messageContext) {
		return "true".equals(messageContext.getProperty(SynapseConstants.OUT_ONLY));
	}

	/**
	 * Collect necessary statistics data from the message context.
	 *
	 * @param messageContext      synapse message context.
	 * @param isContentAltering   is this event is content altering event.
	 * @param isCollectingTracing is collecting tracing.
	 * @param statisticDataUnit   raw statistic carring object
	 */
	public static void collectData(MessageContext messageContext, boolean isContentAltering,
	                               Boolean isCollectingTracing, StatisticDataUnit statisticDataUnit) {

		statisticDataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
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
	 * This method is used to collect all the parent indexes from message contexts which contributes to aggregate
	 * message at the end of the aggregation.
	 *
	 * @param messages aggregated message list
	 * @param newCtx   new aggregated message context
	 */
	public static void collectAggregatedParents(List<MessageContext> messages, MessageContext newCtx) {
		if (RuntimeStatisticCollector.isStatisticsEnabled()) {
			List<Integer> aggregateParents = new LinkedList<>();
			for (MessageContext synCtx : messages) {
				if (RuntimeStatisticCollector.shouldReportStatistic(synCtx)) {
					aggregateParents.add(StatisticDataCollectionHelper.getParentFlowPosition(synCtx, null));
				}
			}
			newCtx.setProperty(StatisticsConstants.MEDIATION_FLOW_STATISTICS_PARENT_LIST, aggregateParents);
		}
	}
}

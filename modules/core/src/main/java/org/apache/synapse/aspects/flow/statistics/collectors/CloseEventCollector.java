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
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndFlowEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.StatisticsCloseEvent;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * CloseEventCollector receives  close statistic events from synapse mediation engine. It Receives Statistics for
 * Proxy Services, Inbound Endpoint, APIs, Sequences, Endpoints, Mediators and Resources.
 */
public class CloseEventCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(CloseEventCollector.class);

	/**
	 * Enqueue statistics event to the event queue. This method receives statistics events from synapse mediation
	 * engine for all the component types.
	 *
	 * @param messageContext    synapse message context.
	 * @param componentName     name of the component reporting statistics.
	 * @param componentType     component type of the reporting component.
	 * @param currentIndex      component's level in this message flow.
	 * @param isContentAltering true if content is altered
	 */
	public static void closeEntryEvent(MessageContext messageContext, String componentName, ComponentType componentType,
									   Integer currentIndex, boolean isContentAltering) {
		closeEntryEvent(messageContext, componentName, componentType, currentIndex, isContentAltering, null);
	}

	/**
	 * Enqueue statistics event to the event queue. This method receives statistics events from synapse mediation
	 * engine for all the component types.
	 *
	 * @param messageContext    synapse message context.
	 * @param componentName     name of the component reporting statistics.
	 * @param componentType     component type of the reporting component.
	 * @param currentIndex      component's level in this message flow.
	 * @param isContentAltering true if content is altered
	 * @param propertyValue     value of the property
	 */
	public static void closeEntryEvent(MessageContext messageContext, String componentName,
											   ComponentType componentType,
											   Integer currentIndex, boolean isContentAltering, String propertyValue) {

		if (shouldReportStatistic(messageContext)) {
			Boolean isCollectingTracing =
					(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			if (propertyValue != null) {
				statisticDataUnit.setPropertyValue(propertyValue);
			}
			statisticDataUnit.setComponentName(componentName);
			statisticDataUnit.setComponentType(componentType);
			if (currentIndex == null) {
				statisticDataUnit.setShouldTrackParent(true);
				statisticDataUnit
						.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(messageContext, null));
				statisticDataUnit.setContinuationCall(true);
			} else {
				statisticDataUnit.setCurrentIndex(currentIndex);
			}
			StatisticDataCollectionHelper
					.collectData(messageContext, isContentAltering, isCollectingTracing, statisticDataUnit);

			StatisticsCloseEvent closeEvent = new StatisticsCloseEvent(statisticDataUnit);
			if (currentIndex == null) {
				addEvent(messageContext, closeEvent);
			} else {
				addEventAndDecrementCount(messageContext, closeEvent);
			}

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler().
						handleCloseEntryEvent(statisticDataUnit, messageContext);
			}

		}
	}

	/**
	 * Enqueue statistics event to the event queue. This method invokes when fault sequence finished handling the fault
	 * occurred in the message flow.
	 *
	 * @param messageContext synapse message context.
	 */
	public static void closeFlowForcefully(MessageContext messageContext, boolean error) {
		if (shouldReportStatistic(messageContext)) {
			BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
			dataUnit.setTime(System.currentTimeMillis());
			dataUnit.setSynapseEnvironment(messageContext.getEnvironment());
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(messageContext, null));

            EndFlowEvent endFlowEvent = new EndFlowEvent(dataUnit);
            if (!error) {
                addEventAndDecrementCount(messageContext, endFlowEvent);
            } else {
                addEventAndCloseFlow(messageContext, endFlowEvent);
            }

            if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleCloseFlowForcefully(dataUnit, messageContext);
			}

        }
	}

	/**
	 * Enqueue statistics events to the event queue to close and try to finish the flow.
	 *
	 * @param messageContext    synapse message context.
	 * @param componentName     name of the component reporting statistics.
	 * @param componentType     component type of the reporting component.
	 * @param currentIndex      component's level in this message flow.
	 * @param isContentAltering true if content is altered
	 */
	public static void tryEndFlow(MessageContext messageContext, String componentName, ComponentType componentType,
								  Integer currentIndex, boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			closeEntryEvent(messageContext, componentName, componentType, currentIndex, isContentAltering);
//			closeFlowForcefully(messageContext);
		}
	}
}

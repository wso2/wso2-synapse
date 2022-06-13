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
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.templates.AsynchronousExecutionEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.ParentReopenEvent;
import org.apache.synapse.aspects.flow.statistics.log.templates.StatisticsOpenEvent;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;

/**
 * OpenEventCollector receives  open statistic events from synapse mediation engine. It Receives Statistics for Proxy
 * Services, Inbound Endpoints, APIs, Sequences, Endpoints, Mediators and Resources.
 */
public class OpenEventCollector extends RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(OpenEventCollector.class);

	/**
	 * Enqueue StatisticOpenEvent to the event Queue. This receives open events from Proxy Services, Endpoints, APIs,
	 * Inbound Endpoints and Sequences which are considered as entry components for statistics collection. These
	 * components can start statistic collection if their individual statistic collection is enabled. If statistics
	 * is already enabled, this will enqueue open event to the queue regardless of its individual statistics collection.
	 *
	 * @param messageContext      synapse message context.
	 * @param componentName       statistic reporting component name.
	 * @param aspectConfiguration aspect configuration of the reporting component.
	 * @param componentType       component type of the reporting component.
	 * @return component's level in this message flow.
	 */
	public static Integer reportEntryEvent(MessageContext messageContext, String componentName,
	                                       AspectConfiguration aspectConfiguration, ComponentType componentType) {
		boolean isCollectingStatistics = (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable());

		// Enable statistics, if user enabled for all artifacts
		if (!isCollectingStatistics) {
			isCollectingStatistics = isCollectingStatistics || RuntimeStatisticCollector.isCollectingAllStatistics();
		}

		boolean isCollectingTracing = false;
		if (isCollectingProperties() || isCollectingPayloads()) {
			isCollectingTracing = (aspectConfiguration != null && aspectConfiguration.isTracingEnabled());
		}

		Boolean isFlowStatisticEnabled =
				(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);//todo try to use single object for "FLOW_TRACE_IS_COLLECTED"
		Boolean isTracingEnabled;
		if (isCollectingStatistics) {
			messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
			setStatisticsTraceId(messageContext);
			if (isCollectingTracing) {
				messageContext.setProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED, true);
			}
		} else if (isFlowStatisticEnabled == null) {
			//To signal lower levels that statistics was disabled in upper component in the flow
			messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
		}

		isTracingEnabled = (Boolean) messageContext.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			statisticDataUnit.setComponentName(componentName);
			statisticDataUnit.setComponentType(componentType);
			statisticDataUnit.setTracingEnabled(isCollectingTracing);
			statisticDataUnit.setSynapseEnvironment(messageContext.getEnvironment());
			statisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getFlowPosition(messageContext));
			if (aspectConfiguration != null) {
				statisticDataUnit.setComponentId(aspectConfiguration.getUniqueId());
				statisticDataUnit.setHashCode(aspectConfiguration.getHashCode());
			}
			int parentIndex = StatisticDataCollectionHelper
					.getParentFlowPosition(messageContext, statisticDataUnit.getCurrentIndex());
			statisticDataUnit.setParentIndex(parentIndex);
			if (statisticDataUnit.getComponentType() != ComponentType.ENDPOINT) {
				statisticDataUnit.setFlowContinuableMediator(true);
			}

			if (aspectConfiguration != null) {
				statisticDataUnit.setIsIndividualStatisticCollected(isCollectingStatistics);
			}
			StatisticDataCollectionHelper.collectData(messageContext, true, isTracingEnabled, statisticDataUnit);

			StatisticsOpenEvent openEvent = new StatisticsOpenEvent(statisticDataUnit);
            addEventAndIncrementCount(messageContext, openEvent);

            if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenEntryEvent(statisticDataUnit, messageContext);
			}

			return statisticDataUnit.getCurrentIndex();
		}

		return null;
	}

	/**
	 * Enqueue StatisticOpenEvent to the event Queue. This receives open events from Mediators and Resources. These
	 * components can't start statistic collection. If statistics is already enabled, it will enqueue open event to
	 * the queue regardless of its individual statistics collection. If its disabled it will not enqueue open event
	 * to the event queue.
	 *
	 * @param messageContext      synapse message context.
	 * @param componentName       statistic reporting component name.
	 * @param componentType       component type of the reporting component.
	 * @param aspectConfiguration aspect configuration of the component
	 * @param isContentAltering   component is altering the content
	 * @return component's level in this message flow.
	 */
	public static Integer reportChildEntryEvent(MessageContext messageContext, String componentName,
	                                            ComponentType componentType, AspectConfiguration aspectConfiguration,
	                                            boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			reportMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit,
			                         aspectConfiguration);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenChildEntryEvent(statisticDataUnit, messageContext);
			}

			return statisticDataUnit.getCurrentIndex();
		}

		return null;
	}

	/**
	 * Enqueue StatisticOpenEvent to the event Queue. This receives open events from Flow Continuable Mediators. These
	 * components can't start statistic collection. If statistics is already enabled, it will enqueue open event to
	 * the queue regardless of its individual statistics collection. If its disabled it will not enqueue open event
	 * to the event queue.
	 *
	 * @param messageContext      synapse message context.
	 * @param componentName       statistic reporting component name.
	 * @param componentType       component type of the reporting component.
	 * @param aspectConfiguration aspect configuration of the component
	 * @param isContentAltering   component is altering the content
	 * @return component's level in this message flow.
	 */
	public static Integer reportFlowContinuableEvent(MessageContext messageContext, String componentName,
	                                                 ComponentType componentType,
	                                                 AspectConfiguration aspectConfiguration,
	                                                 boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			statisticDataUnit.setFlowContinuableMediator(true);
			reportMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit,
			                         aspectConfiguration);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenFlowContinuableEvent(statisticDataUnit, messageContext);
			}

			return statisticDataUnit.getCurrentIndex();
		}

		return null;

	}

	/**
	 * Enqueue StatisticOpenEvent to the event Queue. This receives open events from Flow Splitting Mediators like
	 * Clone Mediator and Iterate Mediator. These components can't start statistic collection. If statistics is
	 * already enabled, it will enqueue open event to the queue regardless of its individual statistics collection.
	 * If its disabled it will not enqueue open event to the event queue.
	 *
	 * @param messageContext      synapse message context.
	 * @param componentName       statistic reporting component name.
	 * @param componentType       component type of the reporting component.
	 * @param aspectConfiguration aspect configuration of the component
	 * @param isContentAltering   component is altering the content
	 * @return component's level in this message flow.
	 */
	public static Integer reportFlowSplittingEvent(MessageContext messageContext, String componentName,
	                                               ComponentType componentType, AspectConfiguration aspectConfiguration,
	                                               boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			statisticDataUnit.setFlowContinuableMediator(true);
			statisticDataUnit.setFlowSplittingMediator(true);
			reportMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit,
			                         aspectConfiguration);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenFlowSplittingEvent(statisticDataUnit, messageContext);
			}

			return statisticDataUnit.getCurrentIndex();
		}
		return null;

	}

	/**
	 * Enqueue StatisticOpenEvent to the event Queue. This receives open events from Flow Aggregate Mediator. These
	 * components can't start statistic collection. If statistics is already enabled, it will enqueue open event to
	 * the queue regardless of its individual statistics collection. If its disabled it will not enqueue open event
	 * to the event queue.
	 *
	 * @param messageContext      synapse message context.
	 * @param componentName       statistic reporting component name.
	 * @param componentType       component type of the component.
	 * @param aspectConfiguration aspect configuration of the component
	 * @param isContentAltering   component is altering the content
	 * @return component's level in this message flow.
	 */
	public static Integer reportFlowAggregateEvent(MessageContext messageContext, String componentName,
	                                               ComponentType componentType, AspectConfiguration aspectConfiguration,
	                                               boolean isContentAltering) {
		if (shouldReportStatistic(messageContext)) {
			StatisticDataUnit statisticDataUnit = new StatisticDataUnit();
			statisticDataUnit.setFlowContinuableMediator(true);
			statisticDataUnit.setFlowAggregateMediator(true);
			reportMediatorStatistics(messageContext, componentName, componentType, isContentAltering, statisticDataUnit,
			                         aspectConfiguration);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenFlowAggregateEvent(statisticDataUnit, messageContext);
			}

			return statisticDataUnit.getCurrentIndex();
		}
		return null;

	}

	/**
	 * Enqueue StatisticOpenEvent for asynchronous invocation.
	 *
	 * @param messageContext synapse message context.
	 */
	public static void reportFlowAsynchronousEvent(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			BasicStatisticDataUnit dataUnit = new BasicStatisticDataUnit();
			dataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(messageContext));
			dataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(messageContext, null));
			AsynchronousExecutionEvent asynchronousExecutionEvent = new AsynchronousExecutionEvent(dataUnit);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenFlowAsynchronousEvent(dataUnit, messageContext);
			}

            addEventAndIncrementCount(messageContext, asynchronousExecutionEvent);
		}
	}

	private static void reportMediatorStatistics(MessageContext messageContext, String componentName,
	                                             ComponentType componentType, boolean isContentAltering,
	                                             StatisticDataUnit statisticDataUnit,
	                                             AspectConfiguration aspectConfiguration) {
		Boolean isCollectingTracing = (Boolean) messageContext.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);
		statisticDataUnit.setComponentName(componentName);
		statisticDataUnit.setComponentType(componentType);
		statisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getFlowPosition(messageContext));
		if(aspectConfiguration != null) {
			statisticDataUnit.setComponentId(aspectConfiguration.getUniqueId());
			statisticDataUnit.setHashCode(aspectConfiguration.getHashCode());
		}
		int parentIndex = StatisticDataCollectionHelper
				.getParentFlowPosition(messageContext, statisticDataUnit.getCurrentIndex());
		statisticDataUnit.setParentIndex(parentIndex);
		StatisticDataCollectionHelper
				.collectData(messageContext, isContentAltering, isCollectingTracing, statisticDataUnit);

		StatisticsOpenEvent openEvent = new StatisticsOpenEvent(statisticDataUnit);
        addEventAndIncrementCount(messageContext, openEvent);
	}

    /**
     * Add event in to the event queue. This event will inform statistic collection to put all the flow continuable
     * mediators before the index specified by current Index to open state.
     *
     * @param synCtx synapse message context.
     */
    public static void openContinuationEvents(MessageContext synCtx) {
        if (shouldReportStatistic(synCtx)) {
            BasicStatisticDataUnit basicStatisticDataUnit = new BasicStatisticDataUnit();

            basicStatisticDataUnit.setCurrentIndex(StatisticDataCollectionHelper.getParentFlowPosition(synCtx, null));
            basicStatisticDataUnit.setStatisticId(StatisticDataCollectionHelper.getStatisticTraceId(synCtx));

            ParentReopenEvent parentReopenEvent = new ParentReopenEvent(basicStatisticDataUnit);
			addEvent(synCtx, parentReopenEvent);

			if (isOpenTelemetryEnabled()) {
				OpenTelemetryManagerHolder.getOpenTelemetryManager().getHandler()
						.handleOpenContinuationEvents(basicStatisticDataUnit, synCtx);
			}
		}
    }
}

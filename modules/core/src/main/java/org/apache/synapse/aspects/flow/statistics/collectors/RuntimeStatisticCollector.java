/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.apache.synapse.aspects.flow.statistics.collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.EndpointStatisticEntry;
import org.apache.synapse.aspects.flow.statistics.data.aggregate.StatisticsEntry;
import org.apache.synapse.aspects.flow.statistics.data.raw.EndpointStatisticLog;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.log.StatisticReportingLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.AddCallbacksLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.CloseStatisticEntryForcefullyLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.CreateEntryStatisticLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.EndpointLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.FinalizeEntryLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.InformFaultLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.OpenClosedStatisticLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.RemoveCallbackLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.RemoveContinuationStateLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.StatisticCloseLog;
import org.apache.synapse.aspects.flow.statistics.log.templates.UpdateForReceivedCallbackLog;
import org.apache.synapse.aspects.flow.statistics.util.StatisticMessageCountHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.rest.RESTConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * RuntimeStatisticCollector receives statistic events and responsible for handling each of these
 * events.
 */
public class RuntimeStatisticCollector {

	private static final Log log = LogFactory.getLog(RuntimeStatisticCollector.class);

	private static Map<String, StatisticsEntry> runtimeStatistics = new HashMap<>();

	private static Map<String, EndpointStatisticEntry> endpointStatistics = new HashMap<>();

	private static boolean isStatisticsEnable = false;

	private static MessageDataCollector messageDataCollector;

	/**
	 * Initialize statistics collection when ESB starts.
	 */
	public static void init() {
		isStatisticsEnable = Boolean.parseBoolean(
				SynapsePropertiesLoader.getPropertyValue(StatisticsConstants.STATISTICS_ENABLE, String.valueOf(false)));
		if (isStatisticsEnable) {
			if (log.isDebugEnabled()) {
				log.debug("Mediation statistics collection is enabled.");
			}
			int queueSize = Integer.parseInt(SynapsePropertiesLoader
					                                 .getPropertyValue(StatisticsConstants.FLOW_STATISTICS_QUEUE_SIZE,
					                                                   StatisticsConstants.FLOW_STATISTICS_DEFAULT_QUEUE_SIZE));
			messageDataCollector = new MessageDataCollector(queueSize);
			//Thread to consume queue and update data structures for publishing
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r);
					t.setName("Mediation Statistic Data consumer Task");
					return t;
				}
			});
			executor.scheduleAtFixedRate(messageDataCollector, 0, 1000, TimeUnit.MILLISECONDS);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Statistics is not enabled in \'synapse.properties\' file.");
			}
		}

	}

	/**
	 * Create statistic log for the the reporting component.
	 *
	 * @param statisticDataUnit Statistics raw data object.
	 */
	public static void recordStatisticCreateEntry(StatisticDataUnit statisticDataUnit) {
		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			runtimeStatistics.get(statisticDataUnit.getStatisticId()).createLog(statisticDataUnit);
		} else {
			StatisticsEntry statisticsEntry = new StatisticsEntry(statisticDataUnit);
			runtimeStatistics.put(statisticDataUnit.getStatisticId(), statisticsEntry);
			if (log.isDebugEnabled()) {
				log.debug("Creating New Entry in Running Statistics: Current size :" + runtimeStatistics.size());
			}
		}
	}

	/**
	 * Ends statistics collection log for the reported statistics component.
	 *
	 * @param statisticDataUnit Statistics raw data object.
	 */
	public static void recordStatisticCloseLog(StatisticDataUnit statisticDataUnit) {

		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry statisticsEntry = runtimeStatistics.get(statisticDataUnit.getStatisticId());
			boolean finished = statisticsEntry.closeLog(statisticDataUnit);
			if (finished) {
				endMessageFlow(statisticDataUnit, statisticsEntry, false);
			}
		}
	}

	/**
	 * Reports that message flow encountered a Fault During Mediation.
	 *
	 * @param statisticId Statistic ID of the message flow.
	 * @param cloneId     Message flow branching number.
	 */
	public static void reportFault(String statisticId, int cloneId) {
		if (runtimeStatistics.containsKey(statisticId)) {
			StatisticsEntry statisticsEntry = runtimeStatistics.get(statisticId);
			statisticsEntry.reportFault(cloneId);
		}
	}

	/**
	 * Creates Endpoint Statistics for the endpoint.
	 *
	 * @param statisticId        Statistic ID of the message flow.
	 * @param endpointId         Endpoint unique identification Number.
	 * @param endpointName       Endpoint name.
	 * @param synapseEnvironment Synapse environment of the message flow.
	 * @param time               Time of the Stat reporting.
	 * @param isCreateLog        Is this a creation of a log.
	 */
	public static void createEndpointStatistics(String statisticId, String endpointId, String endpointName,
	                                            SynapseEnvironment synapseEnvironment, long time, boolean isCreateLog) {
		if (isCreateLog) {
			EndpointStatisticEntry endpointStatisticEntry;
			if (endpointStatistics.containsKey(statisticId)) {
				endpointStatisticEntry = endpointStatistics.get(statisticId);
			} else {
				endpointStatisticEntry = new EndpointStatisticEntry();
				endpointStatistics.put(statisticId, endpointStatisticEntry);
			}
			endpointStatisticEntry.createEndpointLog(endpointId, endpointName, time);
		} else {
			EndpointStatisticEntry endpointStatisticEntry;
			if (endpointStatistics.containsKey(statisticId)) {
				endpointStatisticEntry = endpointStatistics.get(statisticId);
				EndpointStatisticLog endpointStatisticLog =
						endpointStatisticEntry.closeEndpointLog(endpointId, endpointName, time);
				if (endpointStatisticLog != null) {
					synapseEnvironment.getCompletedStatisticStore()
					                  .putCompletedEndpointStatisticEntry(endpointStatisticLog);
					if (endpointStatisticEntry.getSize() == 0) {
						endpointStatistics.remove(statisticId);
					}
					if (log.isDebugEnabled()) {
						log.debug("Endpoint statistic collected for Endpoint:" + endpointStatisticLog.getComponentId());
					}
				}
			}
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
			if (endpointStatistics.containsKey(statisticsTraceId)) {
				endpointStatistics.get(statisticsTraceId).registerCallback(callbackId);
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
	                                             Boolean isContinuation, SynapseEnvironment synapseEnvironment) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				runtimeStatistics.get(statisticsTraceId).updateCallbackReceived(callbackId, endTime, isContinuation);
			}

			if (endpointStatistics.containsKey(statisticsTraceId)) {
				EndpointStatisticEntry endpointStatisticEntry = endpointStatistics.get(statisticsTraceId);
				EndpointStatisticLog endpointStatisticLog =
						endpointStatisticEntry.unregisterCallback(callbackId, endTime);
				if (endpointStatisticLog != null) {
					synapseEnvironment.getCompletedStatisticStore()
					                  .putCompletedEndpointStatisticEntry(endpointStatisticLog);
					if (endpointStatisticEntry.getSize() == 0) {
						endpointStatistics.remove(statisticsTraceId);
					}
					if (log.isDebugEnabled()) {
						log.debug("Endpoint statistic collected for Endpoint:" + endpointStatisticLog.getComponentId());
					}
				}
			}
		}
	}

	/**
	 * Put respective mediator to the open entries due to continuation call.
	 *
	 * @param statisticsTraceId Statistic Id for the message flow.
	 * @param messageId         message Id correspoding to continuation flow
	 * @param componentId       component name
	 */
	public static void putComponentToOpenLogs(String statisticsTraceId, String messageId, String componentId) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				if (runtimeStatistics.containsKey(statisticsTraceId)) {
					runtimeStatistics.get(statisticsTraceId).openLogForContinuation(messageId, componentId);
				}
			}
		}
	}

	/**
	 * Removes specified continuation state for a message flow after all the processing that continuation entry
	 *
	 * @param statisticsTraceId message context
	 * @param messageId         message uuid
	 */
	public static void removeContinuationState(String statisticsTraceId, String messageId) {
		if (statisticsTraceId != null) {
			if (runtimeStatistics.containsKey(statisticsTraceId)) {
				runtimeStatistics.get(statisticsTraceId).removeContinuationEntry(messageId);
				if (log.isDebugEnabled()) {
					log.debug("Removed continuation state from the statistic entry.");
				}
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

	/**
	 * Check whether Statistics entry present for the message flow and if there is an entry try
	 * to finish ending statistics collection for that entry.
	 *
	 * @param statisticDataUnit Statistics raw data object.
	 */
	public static void finalizeEntry(StatisticDataUnit statisticDataUnit) {
		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry entry = runtimeStatistics.get(statisticDataUnit.getStatisticId());
			endMessageFlow(statisticDataUnit, entry, false);
		}
	}

	/**
	 * Close the statistic log after finishing the message flow forcefully. When we try to use this method to end
	 * statistic collection for a message flow it will not consider any thing and close all the remaining logs and
	 * will send the completed statistic entry for collection.
	 *
	 * @param statisticDataUnit Statistics raw data object.
	 */
	public static void closeStatisticEntryForcefully(StatisticDataUnit statisticDataUnit) {
		if (runtimeStatistics.containsKey(statisticDataUnit.getStatisticId())) {
			StatisticsEntry entry = runtimeStatistics.get(statisticDataUnit.getStatisticId());
			endMessageFlow(statisticDataUnit, entry, true);
		}
	}

	/**
	 * End the statistics collection for the message flow. If entry is successfully completed ending
	 * its statistics collection statistics store is updated with new statistics data. Then entry
	 * is removed from the running statistic map.
	 *
	 * @param statisticDataUnit Statistics raw data object.
	 * @param statisticsEntry   Statistic entry for the message flow.
	 * @param closeForceFully   Whether to close statistics forcefully.
	 */
	private synchronized static void endMessageFlow(StatisticDataUnit statisticDataUnit,
	                                                StatisticsEntry statisticsEntry, boolean closeForceFully) {
		boolean isMessageFlowEnded = statisticsEntry.endAll(statisticDataUnit.getTime(), closeForceFully);
		if (isMessageFlowEnded) {
			if (log.isDebugEnabled()) {
				log.debug("Statistic collection is ended for the message flow with statistic " +
				          "trace Id :" + statisticDataUnit.getStatisticId());
			}
			statisticDataUnit.getSynapseEnvironment().getCompletedStatisticStore()
			                 .putCompletedStatisticEntry(statisticsEntry.getMessageFlowLogs());
			runtimeStatistics.remove(statisticDataUnit.getStatisticId());
		}
	}

	//Reporting statistic events to the queue

	/**
	 * Reports statistics for API.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param apiName             API name.
	 * @param aspectConfiguration Aspect Configuration for the API.
	 */
	public static void reportApiStatistics(MessageContext messageContext, String apiName,
	                                       AspectConfiguration aspectConfiguration) {
		if (isStatisticsEnable()) {
			if (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable()) {
				setStatisticsTraceId(messageContext);
				createLogForMessageCheckpoint(messageContext, apiName, ComponentType.API, null, true, false, false);
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
			} else {
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
			}
		}
	}

	/**
	 * Reports statistics for Inbound.
	 *
	 * @param messageContext     Current MessageContext of the flow.
	 * @param inboundName        Inbound name.
	 * @param statisticEnabled   Whether statistic enabled for this inbound.
	 * @param createStatisticLog It is statistic flow start or end.
	 */
	public static void reportStatisticsForInbound(MessageContext messageContext, String inboundName,
	                                              boolean statisticEnabled, boolean createStatisticLog) {
		if (isStatisticsEnable()) {
			if (statisticEnabled && (inboundName != null)) {
				if (createStatisticLog) {
					setStatisticsTraceId(messageContext);
					createLogForMessageCheckpoint(messageContext, inboundName, ComponentType.INBOUNDENDPOINT, null,
					                              true, false, false);
					messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
				} else {
					if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) {
						createLogForFinalize(messageContext);
					} else {
						log.error("Trying close statistic entry without Statistic ID");
					}
				}
			} else {
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
			}
		}
	}

	/**
	 * Reports statistics for the Proxy.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param proxyName           Proxy name.
	 * @param aspectConfiguration Aspect Configuration for the Proxy.
	 * @param createStatisticLog  It is statistic flow start or end.
	 */
	public static void reportStatisticsForProxy(MessageContext messageContext, String proxyName,
	                                            AspectConfiguration aspectConfiguration, boolean createStatisticLog) {
		if (isStatisticsEnable()) {
			if (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable()) {
				if (createStatisticLog) {
					setStatisticsTraceId(messageContext);
					createLogForMessageCheckpoint(messageContext, proxyName, ComponentType.PROXYSERVICE, null, true,
					                              false, false);
					messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
				} else {
					if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) {
						createLogForFinalize(messageContext);
					} else {
						log.error("Trying close statistic entry without Statistic ID");
					}
				}
			} else {
				messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
			}
		}
	}

	/**
	 * End Statistic Flow for Proxy if Message Flow is Out_Only.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 */
	public static void reportEndProxy(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isOutOnly =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
			if (!isOutOnly) {
				isOutOnly = (!Boolean
						.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) && !messageContext.isResponse());
			}
			if (isOutOnly) {
				createLogForFinalize(messageContext);
			}
		}
	}

	/**
	 * End Statistic Flow for Proxy if Message Flow is Out_Only and there is no Sending Fault.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 */
	public static void reportEndSynapseMessageReceiver(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isOutOnly =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
			boolean isFault =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_FAULT)));
			if (!isOutOnly && !isFault) {
				isOutOnly = !Boolean.parseBoolean(
						String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) &&
				            !messageContext.isResponse();
			}
			if (isOutOnly && !isFault) {
				createLogForFinalize(messageContext);
			}
		}
	}

	/**
	 * Reports statistics for the Sequence.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param sequenceName        Sequence name.
	 * @param parentName          Parent component name.
	 * @param aspectConfiguration Aspect Configuration for the Proxy.
	 * @param isCreateLog         It is statistic flow start or end.
	 */
	public static void reportStatisticForSequence(MessageContext messageContext, String sequenceName, String parentName,
	                                              AspectConfiguration aspectConfiguration, boolean isCreateLog) {
		if (isStatisticsEnable()) {
			Boolean isStatCollected =
					(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);
			if (isStatCollected == null) {
				if (aspectConfiguration != null && aspectConfiguration.isStatisticsEnable()) {
					if (messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null && !isCreateLog) {
						log.error("Trying close statistic entry without Statistic ID");
						return;
					}
					setStatisticsTraceId(messageContext);
					createStatisticForSequence(messageContext, sequenceName, isCreateLog);
					messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, true);
				} else {
					messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED, false);
				}
			} else {
				if ((messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) != null) && isStatCollected) {
					createStatisticForSequence(messageContext, sequenceName, isCreateLog);
				}
			}
		}
	}

	private static void createStatisticForSequence(MessageContext messageContext, String sequenceName,
	                                               boolean isCreateLog) {
		if (isCreateLog) {
			createLogForMessageCheckpoint(messageContext, sequenceName, ComponentType.SEQUENCE, null, true, false,
			                              false);
		} else {
			createLogForMessageCheckpoint(messageContext, sequenceName, ComponentType.SEQUENCE, null, false, false,
			                              false);
		}
	}

	/**
	 * Reports Endpoint Statistics.
	 *
	 * @param messageContext               Current MessageContext of the flow.
	 * @param endpointId                   Endpoint identification number.
	 * @param endpointName                 Endpoint name.
	 * @param individualStatisticCollected Whether individual statistic of this endpoint is collected.
	 * @param isCreateLog                  It is statistic flow start or end.
	 */
	public static void reportStatisticForEndpoint(MessageContext messageContext, String endpointId, String endpointName,
	                                              boolean individualStatisticCollected, boolean isCreateLog) {
		if (shouldReportStatistic(messageContext)) {
			createLogForMessageCheckpoint(messageContext, endpointName, ComponentType.ENDPOINT, null, isCreateLog,
			                              false, false);
		}
		if (individualStatisticCollected) {
			StatisticReportingLog statisticReportingLog;
			if (isCreateLog) {
				setStatisticsTraceId(messageContext);
			}
			statisticReportingLog = new EndpointLog(messageContext, endpointId, endpointName, isCreateLog);
			messageDataCollector.enQueue(statisticReportingLog);
		}
	}

	/**
	 * Reports statistics for aggregation.
	 *
	 * @param messageContext      Current MessageContext of the flow.
	 * @param componentName       Component name.
	 * @param componentType       Component type of the component.
	 * @param parentName          Parent of the component.
	 * @param isCreateLog         It is statistic flow start or end.
	 * @param isAggregateComplete Whether aggregate completed.
	 */
	public static void reportStatisticForAggregateMediator(MessageContext messageContext, String componentName,
	                                                       ComponentType componentType, String parentName,
	                                                       boolean isCreateLog, boolean isAggregateComplete) {
		if (shouldReportStatistic(messageContext)) {
			if (isCreateLog) {
				createLogForMessageCheckpoint(messageContext, componentName, componentType, parentName, true, false,
				                              true);
			} else {
				createLogForMessageCheckpoint(messageContext, componentName, componentType, parentName, false, false,
				                              isAggregateComplete);
			}
		}
	}

	/**
	 * Report statistics for the Component
	 *
	 * @param messageContext Current MessageContext of the flow.
	 * @param componentName  Component name.
	 * @param componentType  Component type of the component.
	 * @param parentName     Parent of the component.
	 * @param isCreateLog    It is statistic flow start or end.
	 * @param isCloneLog     is this a clone incident
	 * @param isAggregateLog is this a Aggregate incident
	 */
	public static void reportStatisticForMessageComponent(MessageContext messageContext, String componentName,
	                                                      ComponentType componentType, String parentName,
	                                                      boolean isCreateLog, boolean isCloneLog,
	                                                      boolean isAggregateLog) {
		if (shouldReportStatistic(messageContext)) {
			createLogForMessageCheckpoint(messageContext, componentName, componentType, parentName, isCreateLog,
			                              isCloneLog, isAggregateLog);
		}
	}

	/**
	 * Reports statistics for Resource.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 * @param resourceId     Resource Id.
	 * @param parentName     parent name.
	 * @param isCreateLog    It is statistic flow start or end.
	 */
	public static void reportStatisticForResource(MessageContext messageContext, String resourceId, String parentName,
	                                              boolean isCreateLog) {
		if (shouldReportStatistic(messageContext)) {
			String resourceName = getResourceNameForStatistics(messageContext, resourceId);
			if (isCreateLog) {
				createLogForMessageCheckpoint(messageContext, resourceName, ComponentType.RESOURCE, parentName, true,
				                              false, false);
			} else {
				if (!messageContext.isResponse()) {
					boolean isOutOnly =
							Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
					if (!isOutOnly) {
						isOutOnly = (!Boolean.parseBoolean(
								String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) &&
						             !messageContext.isResponse());
					}
					if (isOutOnly) {
						reportStatisticForMessageComponent(messageContext, resourceName, ComponentType.RESOURCE,
						                                   parentName, false, false, false);
					}
				} else {
					RuntimeStatisticCollector
							.reportStatisticForMessageComponent(messageContext, resourceName, ComponentType.RESOURCE,
							                                    parentName, false, false, false);
				}
			}
		}
	}

	private static String getResourceNameForStatistics(MessageContext messageContext, String resourceId) {
		Object synapseRestApi = messageContext.getProperty(RESTConstants.REST_API_CONTEXT);
		Object restUrlPattern = messageContext.getProperty(RESTConstants.REST_URL_PATTERN);
		if (synapseRestApi != null) {
			String textualStringName;
			if (restUrlPattern != null) {
				textualStringName = (String) synapseRestApi + restUrlPattern;
			} else {
				textualStringName = (String) synapseRestApi;
			}
			return textualStringName;
		}
		return resourceId;
	}

	/**
	 * Return unique ID in message flow for the component.
	 *
	 * @param synCtx Current MessageContext of the flow.
	 * @return Returns unique ID.
	 */
	public static int getComponentUniqueId(MessageContext synCtx) {
		StatisticMessageCountHolder statisticMessageCountHolder;
		if (synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER) != null) {
			statisticMessageCountHolder = (StatisticMessageCountHolder) synCtx
					.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER);
		} else {
			statisticMessageCountHolder = new StatisticMessageCountHolder();
			synCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, statisticMessageCountHolder);
		}
		return statisticMessageCountHolder.incrementAndGetComponentId();
	}

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
			boolean isOutOnly =
					Boolean.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.OUT_ONLY)));
			if (!isOutOnly) {
				isOutOnly = (!Boolean
						.parseBoolean(String.valueOf(messageContext.getProperty(SynapseConstants.SENDING_REQUEST))) &&
				             !messageContext.isResponse());
			}
			if (!isOutOnly) {
				createLogForCallbackRegister(messageContext, callbackId);
			}
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
		if (shouldReportStatistic(synapseOutMsgCtx)) {
			createLogForRemoveCallback(synapseOutMsgCtx, callbackId);
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
				createLogForMessageCheckpoint(synapseOutMsgCtx, textualStringName, ComponentType.RESOURCE, null, false,
				                              false, false);
			} else if (synapseResource != null) {
				createLogForMessageCheckpoint(synapseOutMsgCtx, (String) synapseResource, ComponentType.RESOURCE, null,
				                              false, false, false);
			}
			createLogForFinalize(synapseOutMsgCtx);
		}
	}

	/**
	 * Asynchronously remove continuation state from the message flow.
	 *
	 * @param messageContext message context
	 */
	public static void removeContinuationState(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			StatisticReportingLog statisticReportingLog = new RemoveContinuationStateLog(messageContext);
			messageDataCollector.enQueue(statisticReportingLog);
		}
	}

	/**
	 * Reports fault in message flow.
	 *
	 * @param messageContext Current MessageContext of the flow.
	 */
	public static boolean reportFault(MessageContext messageContext) {
		if (shouldReportStatistic(messageContext)) {
			boolean isFaultCreated = isFaultAlreadyReported(messageContext);
			if (isFaultCreated) {
				CloseStatisticEntryForcefullyLog closeStatisticEntryForcefullyLog =
						new CloseStatisticEntryForcefullyLog(messageContext, System.currentTimeMillis());
				messageDataCollector.enQueue(closeStatisticEntryForcefullyLog);
			} else {
				InformFaultLog informFaultLog = new InformFaultLog(messageContext);
				messageDataCollector.enQueue(informFaultLog);
			}
			messageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_IS_FAULT_REPORTED, !isFaultCreated);
			return true;
		}
		return false;
	}

	private static boolean isFaultAlreadyReported(MessageContext synCtx) {
		Object object = synCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_FAULT_REPORTED);
		if (object == null) {
			return false;
		} else if ((Boolean) object) {
			return true;
		}
		return false;
	}

	public static void openLogForContinuation(MessageContext messageContext, String componentId) {
		if (shouldReportStatistic(messageContext)) {
			StatisticReportingLog statisticReportingLog;
			statisticReportingLog = new OpenClosedStatisticLog(messageContext, componentId);
			messageDataCollector.enQueue(statisticReportingLog);
		}
	}

	//Creating Statistic Logs to report statistic events

	private static void createLogForMessageCheckpoint(MessageContext messageContext, String componentName,
	                                                  ComponentType componentType, String parentName,
	                                                  boolean isCreateLog, boolean isCloneLog, boolean isAggregateLog) {
		StatisticReportingLog statisticLog;
		if (isCreateLog) {
			statisticLog = new CreateEntryStatisticLog(messageContext, componentName, componentType, parentName,
			                                           System.currentTimeMillis(), isCloneLog, isAggregateLog);
		} else {
			statisticLog = new StatisticCloseLog(messageContext, componentName, parentName, System.currentTimeMillis(),
			                                     isCloneLog, isAggregateLog);
		}
		messageDataCollector.enQueue(statisticLog);
	}

	private static void createLogForFinalize(MessageContext messageContext) {
		StatisticReportingLog statisticReportingLog;
		statisticReportingLog = new FinalizeEntryLog(messageContext, System.currentTimeMillis());
		messageDataCollector.enQueue(statisticReportingLog);
	}

	private static void createLogForRemoveCallback(MessageContext synOutCtx, String msgID) {
		RemoveCallbackLog removeCallbackLog = new RemoveCallbackLog(synOutCtx, msgID);
		messageDataCollector.enQueue(removeCallbackLog);
	}

	private static void createLogForCallbackReceived(MessageContext oldMessageContext, String msgID) {
		UpdateForReceivedCallbackLog updateForReceivedCallbackLog =
				new UpdateForReceivedCallbackLog(oldMessageContext, msgID, System.currentTimeMillis());
		messageDataCollector.enQueue(updateForReceivedCallbackLog);
	}

	private static void createLogForCallbackRegister(MessageContext messageContext, String MsgId) {
		AddCallbacksLog addCallbacksLog = new AddCallbacksLog(messageContext, MsgId);
		messageDataCollector.enQueue(addCallbacksLog);
	}

	//Setting properties for branching operation

	public static void setCloneProperties(MessageContext oldMessageContext, MessageContext newMessageContext) {
		if (shouldReportStatistic(oldMessageContext)) {
			StatisticMessageCountHolder cloneCount;
			int parentMsgId;
			if (oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER) != null) {
				cloneCount = (StatisticMessageCountHolder) oldMessageContext
						.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER);
				parentMsgId = (Integer) oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID);
			} else {
				parentMsgId = 0;
				cloneCount = new StatisticMessageCountHolder();
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID, 0);
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
				oldMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID, null);
			}
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID,
			                              cloneCount.incrementAndGetCloneCount());
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID, parentMsgId);
		}
	}

	public static void setAggregateProperties(MessageContext oldMessageContext, MessageContext newMessageContext) {
		if (shouldReportStatistic(oldMessageContext)) {
			StatisticMessageCountHolder cloneCount = (StatisticMessageCountHolder) oldMessageContext
					.getProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER);
			int parentMsgId =
					(Integer) oldMessageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_PARENT_MESSAGE_ID);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MESSAGE_ID, parentMsgId);
			newMessageContext.setProperty(StatisticsConstants.FLOW_STATISTICS_MSG_COUNT_HOLDER, cloneCount);
		}
	}

	//Information about statistics enable or disable at various places

	/**
	 * Returns whether statistics collection is enabled globally for the esb as specified in the
	 * synapse.properties file.
	 *
	 * @return true if statistics collection is enabled
	 */
	public static boolean isStatisticsEnable() {
		return isStatisticsEnable;
	}

	private static void setStatisticsTraceId(MessageContext msgCtx) {
		if (msgCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null) {
			msgCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, msgCtx.getMessageID());
		}
	}

	public static void stopConsumer() {
		if (isStatisticsEnable) {
			messageDataCollector.setStopped();
		}
	}

	private static boolean shouldReportStatistic(MessageContext messageContext) {
		Boolean isStatCollected =
				(Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);
		Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		return (statID != null && isStatCollected != null && isStatCollected && isStatisticsEnable);
	}

	private static boolean isStatisticsTraced(MessageContext messageContext) {
		Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
		return (statID != null && isStatisticsEnable);
	}

}

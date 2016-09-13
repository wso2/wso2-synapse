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
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;
import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEventHolder;
import org.apache.synapse.aspects.flow.statistics.util.MediationFlowController;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapsePropertiesLoader;

/**
 * RuntimeStatisticCollector receives statistic events and responsible for handling each of these events.
 */
public abstract class RuntimeStatisticCollector {

    private static final Log log = LogFactory.getLog(RuntimeStatisticCollector.class);

    /**
     * Is statistics collection enabled in synapse.properties file.
     */
    private static boolean isStatisticsEnabled;

    /**
     * Is payload collection enabled in synapse.properties file.
     */
    private static boolean isCollectingPayloads;

    /**
     * Is message context property collection enabled in synapse.properties file.
     */
    private static boolean isCollectingProperties;

    /**
     * Is collecting statistics of all artifacts
     */
    private static boolean isCollectingAllStatistics;

    public static long eventExpireTime;

    /**
     * Initialize statistics collection when ESB starts.
     */
    public static void init() {
        isStatisticsEnabled = Boolean.parseBoolean(
                SynapsePropertiesLoader.getPropertyValue(StatisticsConstants.STATISTICS_ENABLE, String.valueOf(false)));
        if (isStatisticsEnabled) {
            if (log.isDebugEnabled()) {
                log.debug("Mediation statistics collection is enabled.");
            }

            Long eventConsumerTime = Long.parseLong(SynapsePropertiesLoader.getPropertyValue(
                    StatisticsConstants.FLOW_STATISTICS_EVENT_CONSUME_TIME,
                    StatisticsConstants.FLOW_STATISTICS_DEFAULT_EVENT_CONSUME_INTERVAL));

            isCollectingPayloads = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
                    StatisticsConstants.COLLECT_MESSAGE_PAYLOADS, String.valueOf(false)));

            if (!isCollectingPayloads && log.isDebugEnabled()) {
                log.debug("Payload collecting is not enabled in \'synapse.properties\' file.");
            }

            isCollectingProperties = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
                    StatisticsConstants.COLLECT_MESSAGE_PROPERTIES, String.valueOf(false)));

            if (!isCollectingProperties && log.isDebugEnabled()) {
                log.debug("Property collecting is not enabled in \'synapse.properties\' file.");
            }

            isCollectingAllStatistics = Boolean.parseBoolean(SynapsePropertiesLoader.getPropertyValue(
                    StatisticsConstants.COLLECT_ALL_STATISTICS, String.valueOf(false)));

            eventExpireTime =
                    SynapseConfigUtils.getGlobalTimeoutInterval() + SynapseConfigUtils.getTimeoutHandlerInterval() +
                    eventConsumerTime;
            log.info("Statistics Entry Expiration time set to " + eventExpireTime + " milliseconds");
            new MediationFlowController();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Statistics is not enabled in \'synapse.properties\' file.");
            }
        }
    }

    /**
     * Set message Id of the message context as statistic trace Id at the beginning of the statistic flow.
     *
     * @param msgCtx synapse message context.
     */
    protected static void setStatisticsTraceId(MessageContext msgCtx) {
        if (msgCtx.getProperty(StatisticsConstants.FLOW_STATISTICS_ID) == null && msgCtx.getMessageID() != null) {
            msgCtx.setProperty(StatisticsConstants.FLOW_STATISTICS_ID, msgCtx.getMessageID().replace(':', '_'));
        } else if (msgCtx.getMessageID() == null) {
            log.error("Message ID is null");
        }
    }

    /**
     * Returns true if statistics is collected in this message flow path.
     *
     * @param messageContext synapse message context.
     * @return true if statistics is collected in the message flow.
     */
    public static boolean shouldReportStatistic(MessageContext messageContext) {
        if (!isStatisticsEnabled) {
            return false;
        }

        Boolean isStatCollected =
                (Boolean) messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_IS_COLLECTED);//todo try use one object
        Object statID = messageContext.getProperty(StatisticsConstants.FLOW_STATISTICS_ID);
        return (statID != null && isStatCollected != null && isStatCollected);
    }

    /**
     * Returns whether statistics collection is enabled globally for the esb as specified in the
     * synapse.properties file.
     *
     * @return true if statistics collection is enabled.
     */

    public static boolean isStatisticsEnabled() {
        return isStatisticsEnabled;
    }

    /**
     * Return whether collecting payloads is enabled.
     *
     * @return true if need to collect payloads.
     */
    public static boolean isCollectingPayloads() {
        return isStatisticsEnabled && isCollectingPayloads;
    }

    /**
     * Return whether collecting message-properties is enabled.
     *
     * @return true if need to collect message-properties.
     */
    public static boolean isCollectingProperties() {
        return isStatisticsEnabled && isCollectingProperties;
    }

    /**
     * Return whether collecting statistics for all artifacts is enabled (this also needs isStatisticsEnabled)
     *
     * @return true if need to collect statistics for all artifacts
     */
    public static boolean isCollectingAllStatistics() {
        return isStatisticsEnabled && isCollectingAllStatistics;
    }

    /**
     * Allow external to alter state of collecting statistics for all artifacts, during runtime
     */
    public static void setCollectingAllStatistics(boolean state) {
        isCollectingAllStatistics = state;
        log.info("Collecting statistics for all artifacts state changed to: " + state);
    }

    /**
     * Helper method to add event and increment stat count so that it denotes, open event is added.
     *
     * @param messageContext
     * @param event
     */
    protected static void addEventAndIncrementCount(MessageContext messageContext, StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }
        if (eventHolder.isEvenCollectionFinished()) {
            handleError(eventHolder, event);
            return;
        }

        eventHolder.addEvent(event);
        eventHolder.countHolder.incrementStatCount();

    }

    /**
     * Helper method to add event and decrement stat count, which denotes, closing event happened.
     *
     * @param messageContext
     * @param event
     */
    protected static void addEventAndDecrementCount(MessageContext messageContext, StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }

        if (eventHolder.isEvenCollectionFinished()) {
            handleError(eventHolder, event);
            return;
        }
        eventHolder.addEvent(event);

        if (eventHolder.countHolder.decrementAndGetStatCount() <= 0 && eventHolder.countHolder.getCallBackCount() <= 0) {
            eventHolder.setEvenCollectionFinished(true);
            messageContext.getEnvironment().getMessageDataStore().enqueue(eventHolder);
        }
    }

    /**
     * Helper method to add event and increment call back count, which notifies that a callback has been registered
     *
     * @param messageContext
     * @param event
     */
    protected static void addEventAndIncrementCallbackCount(MessageContext messageContext,
                                                            StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }

        if (eventHolder.isEvenCollectionFinished()) {
            handleError(eventHolder, event);
            return;
        }

        eventHolder.addEvent(event);

        eventHolder.countHolder.incrementCallBackCount();
    }

    /**
     * Helper method to add event and decrement call back count, which denotes, call back has been received.
     *
     * @param messageContext
     * @param event
     */
    protected static void addEventAndDecrementCallbackCount(MessageContext messageContext,
                                                            StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }
        if (eventHolder.isEvenCollectionFinished()) {
            handleError(eventHolder, event);
            return;
        }
        eventHolder.addEvent(event);

        if (eventHolder.countHolder.decrementAndGetCallbackCount() <= 0 && eventHolder.countHolder.getStatCount() <= 0) {
            eventHolder.setEvenCollectionFinished(true);
            messageContext.getEnvironment().getMessageDataStore().enqueue(eventHolder);
        }
    }

    /**
     * Helper method to just add event without changing counts.
     *
     * @param messageContext
     * @param event
     */
    protected static void addEvent(MessageContext messageContext, StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }

        if (eventHolder.isEvenCollectionFinished()) {
            handleError(eventHolder, event);
            return;
        }
        eventHolder.addEvent(event);
    }

    /**
     * Helper method to add event and close the message flow static collection.
     *
     * @param messageContext
     * @param event
     */
    protected static void addEventAndCloseFlow(MessageContext messageContext, StatisticsReportingEvent event) {
        StatisticsReportingEventHolder eventHolder = (StatisticsReportingEventHolder) messageContext.getProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY);
        if (eventHolder == null) {
            eventHolder = new StatisticsReportingEventHolder();
            messageContext.setProperty(StatisticsConstants.STAT_COLLECTOR_PROPERTY, eventHolder);
        }

        synchronized (eventHolder) {
            if (eventHolder.isEvenCollectionFinished()) {
                handleError(eventHolder, event);
                return;
            }
            eventHolder.addEvent(event);

            eventHolder.setEvenCollectionFinished(true);
            eventHolder.setMessageFlowError(true);
            messageContext.getEnvironment().getMessageDataStore().enqueue(eventHolder);
        }
    }

    /**
     * Helper method to handle errors.
     *
     * @param eventHolder
     * @param event
     */
    private static void handleError(StatisticsReportingEventHolder eventHolder, StatisticsReportingEvent event) {
        if (eventHolder.isMessageFlowError()) {
            if (log.isDebugEnabled()) {
                log.debug("Message flow error happened, dropping event - " + event.getDataUnit().getStatisticId());
            }
            return;
        }
        log.warn("Events occur after event collection is finished, event - " + event.getDataUnit().getStatisticId());
    }
}

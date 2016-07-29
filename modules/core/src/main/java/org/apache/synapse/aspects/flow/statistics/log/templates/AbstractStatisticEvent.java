package org.apache.synapse.aspects.flow.statistics.log.templates;

import org.apache.synapse.aspects.flow.statistics.log.StatisticsReportingEvent;

/**
 * Abstract statistic event class to hold common properties.
 */
public abstract class AbstractStatisticEvent implements StatisticsReportingEvent {

    protected EventType eventType;

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    /**
     * Event type enum
     */
    public enum EventType {
        ASYNCHRONOUS_EXECUTION_EVENT,
        CALLBACK_COMPLETION_EVENT,
        CALLBACK_HANDLED_EVENT,
        CALLBACK_RECEIVED_EVENT,
        CALLBACK_SENT_EVENT,
        ENDFLOW_EVENT,
        FAULT_EVENT,
        PARENT_REOPEN_EVENT,
        STATISTICS_CLOSE_EVENT,
        STATISTICS_OPEN_EVENT
    }
}

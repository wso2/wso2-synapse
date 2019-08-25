package org.apache.synapse.aspects.flow.statistics.tracing.store;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;

@Deprecated // TODO Confirm and remove
public class StackedSequenceInfo {
    private StatisticDataUnit statisticDataUnit;
    private String spanReferenceId;
    private boolean isStarted;

    public StackedSequenceInfo(StatisticDataUnit statisticDataUnit) {
        this.statisticDataUnit = statisticDataUnit;
        this.spanReferenceId = Util.extractId(statisticDataUnit);
        this.isStarted = false;
    }

    public StatisticDataUnit getStatisticDataUnit() {
        return statisticDataUnit;
    }

    public String getSpanReferenceId() {
        return spanReferenceId;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }
}

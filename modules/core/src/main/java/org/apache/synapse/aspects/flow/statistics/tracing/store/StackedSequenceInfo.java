package org.apache.synapse.aspects.flow.statistics.tracing.store;

import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers.Util;

public class StackedSequenceInfo {
    private StatisticDataUnit statisticDataUnit;
    private String spanReferenceId;
    private boolean isSpanActive;

    public StackedSequenceInfo(StatisticDataUnit statisticDataUnit) {
        this.statisticDataUnit = statisticDataUnit;
        this.spanReferenceId = Util.extractId(statisticDataUnit);
        this.isSpanActive = false;
    }

    public StatisticDataUnit getStatisticDataUnit() {
        return statisticDataUnit;
    }

    public String getSpanReferenceId() {
        return spanReferenceId;
    }

    public boolean isSpanActive() {
        return isSpanActive;
    }

    public void setSpanActive(boolean spanActive) {
        isSpanActive = spanActive;
    }
}

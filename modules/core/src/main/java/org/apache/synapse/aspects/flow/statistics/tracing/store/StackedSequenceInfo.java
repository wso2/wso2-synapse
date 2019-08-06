package org.apache.synapse.aspects.flow.statistics.tracing.store;

import org.apache.synapse.SequenceType;

public class StackedSequenceInfo {
    private String statisticDataUnitId;
    private SequenceType type;
    private String componentName; // TODO Hold the statisticDataUnit inside to get all info
    private boolean isStarted;

    public StackedSequenceInfo(String statisticDataUnitId, String componentName) {
        this.statisticDataUnitId = statisticDataUnitId;
        this.componentName = componentName;
        this.isStarted = false;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public String getStatisticDataUnitId() {
        return statisticDataUnitId;
    }

    public void setStatisticDataUnitId(String statisticDataUnitId) {
        this.statisticDataUnitId = statisticDataUnitId;
    }

    public SequenceType getType() {
        return type;
    }

    public void setType(SequenceType type) {
        this.type = type;
    }
}

package org.apache.synapse.messageflowtracer.data;

public class MessageFlowComponentId {
    int incrementId;

    public MessageFlowComponentId(int incrementId) {
        this.incrementId = incrementId;
    }

    public String getUpdatedId() {
        return String.valueOf(this.incrementId++);
    }

    public int getIncrementId() {
        return incrementId;
    }

    public void setIncrementId(int incrementId) {
        this.incrementId = incrementId;
    }
}

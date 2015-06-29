package org.apache.synapse.flowtracer.data;

public class MessageFlowComponentEntry {

    private String messageId;
    private String componentId;
    private String componentName;
    private boolean response;
    private boolean start;
    private String timestamp;
    private String propertySet;
    private String payload;

    public MessageFlowComponentEntry(String messageId, String componentId, String componentName, boolean response, boolean start, String timestamp, String propertySet, String payload) {
        this.messageId = messageId;
        this.componentId = componentId;
        this.componentName = componentName;
        this.response = response;
        this.start = start;
        this.timestamp = timestamp;
        this.propertySet = propertySet;
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public String getPropertySet() {
        return propertySet;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getComponentId() {
        return componentId;
    }

    public boolean isResponse() {
        return response;
    }

    public boolean isStart() {
        return start;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return ", componentName='" + componentName + '\'' +
                ", response=" + response +
                ", start=" + start +
                ", timestamp='" + timestamp ;
    }
}

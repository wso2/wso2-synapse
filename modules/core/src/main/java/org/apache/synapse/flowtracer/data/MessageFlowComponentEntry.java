package org.apache.synapse.flowtracer.data;

import org.apache.synapse.core.SynapseEnvironment;

import java.util.Map;

public class MessageFlowComponentEntry implements MessageFlowDataEntry {

    private String messageId;
    private String componentId;
    private String componentName;
    private boolean response;
    private boolean start;
    private String timestamp;
    private String payload;
    private SynapseEnvironment synapseEnvironment;
    private Map<String, String> propertyMap;

    public MessageFlowComponentEntry(String messageId, String componentId, String componentName, boolean response,
                                     boolean start, String timestamp, Map<String, String> propertyMap, String payload,
                                     SynapseEnvironment synapseEnvironment) {
        this.messageId = messageId;
        this.componentId = componentId;
        this.componentName = componentName;
        this.response = response;
        this.start = start;
        this.timestamp = timestamp;
        this.propertyMap = propertyMap;
        this.payload = payload;
        this.synapseEnvironment = synapseEnvironment;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, String> getPropertyMap() {
        return propertyMap;
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

    public void process(){
        this.synapseEnvironment.getMessageFlowDataHolder().addComponentInfoEntry(this);
    }
}

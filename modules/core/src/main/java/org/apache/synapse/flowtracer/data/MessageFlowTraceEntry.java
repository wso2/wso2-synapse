package org.apache.synapse.flowtracer.data;

public class MessageFlowTraceEntry {

    private String messageId;
    private String entryType;
    private String messageFlow;
    private String timeStamp;

    public MessageFlowTraceEntry(String messageId, String entryType, String timeStamp) {
        this.messageId = messageId;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
    }

    public MessageFlowTraceEntry(String messageId, String messageFlow, String entryType, String timeStamp) {
        this.messageId = messageId;
        this.messageFlow = messageFlow;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getMessageFlow() {
        return messageFlow;
    }
}

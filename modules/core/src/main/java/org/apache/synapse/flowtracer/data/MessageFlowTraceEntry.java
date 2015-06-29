package org.apache.synapse.flowtracer.data;

public class MessageFlowTraceEntry {

    private String messageId;
    private String entryType;
    private String timeStamp;

    public MessageFlowTraceEntry(String messageId, String entryType, String timeStamp) {
        this.messageId = messageId;
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
}

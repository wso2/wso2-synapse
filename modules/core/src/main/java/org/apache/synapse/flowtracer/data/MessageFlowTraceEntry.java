package org.apache.synapse.flowtracer.data;

import org.apache.synapse.aspects.data.MediationDataEntry;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.flowtracer.MessageFlowDataHolder;

public class MessageFlowTraceEntry implements MediationDataEntry {

    private String messageId;
    private String entryType;
    private String messageFlow;
    private String timeStamp;
    private SynapseEnvironment synapseEnvironment;

    public MessageFlowTraceEntry(String messageId, String entryType, String timeStamp, SynapseEnvironment
            synapseEnvironment) {
        this.messageId = messageId;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
        this.synapseEnvironment = synapseEnvironment;
    }

    public MessageFlowTraceEntry(String messageId, String messageFlow, String entryType, String timeStamp,
                                 SynapseEnvironment synapseEnvironment) {
        this.messageId = messageId;
        this.messageFlow = messageFlow;
        this.entryType = entryType;
        this.timeStamp = timeStamp;
        this.synapseEnvironment = synapseEnvironment;
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

    public void process(){
        this.synapseEnvironment.getMessageFlowDataHolder().addFlowInfoEntry(this);
    }
}

package org.apache.synapse.flowtracer;

import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

public class MessageFlowDbReporterTask implements Runnable{

    private boolean running = true;

    public void terminate(){
        running = false;
    }

    @Override
    public void run() {
        while(running){
            MessageFlowComponentEntry componentInfoEntry = MessageFlowDataHolder.getComponentInfoEntry();

            MessageFlowTraceEntry flowInfoEntry = MessageFlowDataHolder.getFlowInfoEntry();

            if(componentInfoEntry!=null){
                MessageFlowDbConnector.getInstance().persistMessageFlowComponentEntry(componentInfoEntry);
            }

            if(flowInfoEntry!=null){
                MessageFlowDbConnector.getInstance().persistMessageFlowTraceEntry(flowInfoEntry);
            }
        }
    }

}

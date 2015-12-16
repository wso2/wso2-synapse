package org.apache.synapse.flowtracer.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MessageFlowTracingDataConsumer implements Runnable {
    private static Log log = LogFactory.getLog(MessageFlowTracingDataConsumer.class);
    private boolean isStopped = false;

    public void run(){
        MessageFlowDataEntry mediationDataEntry;
        while (!isStopped) {
            try {
                mediationDataEntry = MessageFlowTracingDataCollector.deQueue();
                mediationDataEntry.process();
            } catch (Exception exception) {
                log.error("Error in Mediation Tracing data consumer", exception);
            }
        }
    }
}

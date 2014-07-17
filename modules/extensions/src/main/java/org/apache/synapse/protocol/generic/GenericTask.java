package org.apache.synapse.protocol.generic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.PollingConsumer;

public class GenericTask implements org.apache.synapse.task.Task, ManagedLifecycle{
    private static final Log logger = LogFactory.getLog(GenericTask.class.getName());

    private PollingConsumer pollingConsumer;
    
    public GenericTask(PollingConsumer pollingConsumer) {
    	logger.debug("Generic Task initalize.");
    	this.pollingConsumer = pollingConsumer;
    }

    public void execute() {
    	logger.debug("File Task executing.");
    	pollingConsumer.poll();
    }


    public void init(SynapseEnvironment synapseEnvironment) {
        logger.debug("Initializing Task.");
    }

    public void destroy() {
        logger.debug("Destroying Task. ");
    }
}

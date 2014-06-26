package org.apache.synapse.protocol.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;


public class FileTask implements org.apache.synapse.task.Task, ManagedLifecycle{
    private static final Log logger = LogFactory.getLog(FileTask.class.getName());

    private FilePollingConsumer fileScanner;
    
    public FileTask(FilePollingConsumer fileScanner) {
    	logger.debug("File Task initalize.");
    	this.fileScanner = fileScanner;
    }

    public void execute() {
    	logger.debug("File Task executing.");
    	fileScanner.execute();
    }


    public void init(SynapseEnvironment synapseEnvironment) {
        logger.debug("Initializing Task.");
    }

    public void destroy() {
        logger.debug("Destroying Task. ");
    }
}

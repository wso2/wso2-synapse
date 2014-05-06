package org.apache.synapse.protocol.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;


public class FileTask implements org.apache.synapse.task.Task, ManagedLifecycle{
    private static final Log logger = LogFactory.getLog(FileTask.class.getName());
    private static final int DEFAULT_CONNECTION_TIMEOUT = 20000;

    public FileTask() {
        System.out.println("##################################333 XXXXXXXXXXXXXXXXXXXXXX");
    }

    public void execute() {
        System.out.println("org.wso2.carbon.ntaskint.core.Task Executing task---------------->");

    }


    public void init(SynapseEnvironment synapseEnvironment) {
        logger.info("Initializing Task... XXXXXXXXXXXXXXXXXXXXXX");
    }

    public void destroy() {
        logger.info("Destroying Task... XXXXXXXXXXXXXXXXXXXXXX");
    }
}

package org.apache.synapse.protocol.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;


public class JMSTask implements org.apache.synapse.task.Task, ManagedLifecycle {
    private static final Log logger = LogFactory.getLog(JMSTask.class.getName());

    private JMSPollingConsumer jmsPollingConsumer;
    
    public JMSTask(JMSPollingConsumer jmsPollingConsumer) {
    	logger.debug("Initializing.");
    	this.jmsPollingConsumer = jmsPollingConsumer;
    }

    public void execute() {
    	logger.debug("Executing.");
    	jmsPollingConsumer.execute();
    }


    public void init(SynapseEnvironment synapseEnvironment) {
        logger.debug("Initializing.");
    }

    public void destroy() {
        logger.debug("Destroying.");
    }
}


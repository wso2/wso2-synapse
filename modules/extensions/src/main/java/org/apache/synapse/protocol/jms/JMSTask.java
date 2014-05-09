package org.apache.synapse.protocol.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;


public class JMSTask implements org.apache.synapse.task.Task, ManagedLifecycle{
    private static final Log logger = LogFactory.getLog(JMSTask.class.getName());

    private JMSPollingConsumer jmsPollingConsumer;
    
    public JMSTask(JMSPollingConsumer jmsPollingConsumer) {
    	logger.info("JMS Task initalize.");
    	this.jmsPollingConsumer = jmsPollingConsumer;
    }

    public void execute() {
    	logger.info("JMS Task executing.");
    	jmsPollingConsumer.execute();
    }


    public void init(SynapseEnvironment synapseEnvironment) {
        logger.info("Initializing Task.");
    }

    public void destroy() {
        logger.info("Destroying Task. ");
    }
}


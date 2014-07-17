package org.apache.synapse.protocol.generic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.GenericPollingConsumer;
import org.apache.synapse.inbound.PollingProcessor;
import org.apache.synapse.startup.quartz.StartUpController;
import org.apache.synapse.task.Task;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskStartupObserver;

public class GenericProcessor implements PollingProcessor, TaskStartupObserver {


	private GenericPollingConsumer pollingConsumer;
    private String name;
    private Properties properties;
    private long interval;
    private String injectingSeq;
    private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;
    private static final Log log = LogFactory.getLog(GenericProcessor.class);
    private StartUpController startUpController;
    private String classImpl;
    
    public GenericProcessor(String name, String classImpl, Properties properties, long scanInterval, String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment) {
        this.name = name;
        this.properties = properties;
        this.interval = scanInterval;
        this.injectingSeq = injectingSeq;
        this.onErrorSeq = onErrorSeq;
        this.synapseEnvironment = synapseEnvironment;
        this.classImpl = classImpl;
    }

    public void init() {
    	log.info("Inbound listener " + name + " for class " + classImpl + " starting ...");
		try{
			Class c = Class.forName(classImpl);
			Constructor cons = c.getConstructor(Properties.class, String.class, SynapseEnvironment.class, long.class, String.class, String.class);
			pollingConsumer = (GenericPollingConsumer)cons.newInstance(properties, name, synapseEnvironment, interval, injectingSeq, onErrorSeq);
		}catch(ClassNotFoundException e){
			log.error("Class " + classImpl + " not found. Please check the required class is added to the classpath.");
			log.error(e);
		}catch(NoSuchMethodException e){
			log.error("Required constructor is not implemented.");
			log.error(e);
		}catch(InvocationTargetException e){
			log.error("Unable to create the consumer");
			log.error(e);
		}catch(Exception e){
			log.error("Unable to create the consumer");
			log.error(e);					
		}    	
    	start();
    }
    
    public void destroy() {
        log.info("Inbound listener ended " + name);
        startUpController.destroy();
    }
      
    
    public void start() {        	
        try {
        	Task task = new GenericTask(pollingConsumer);
        	TaskDescription taskDescription = new TaskDescription();
        	taskDescription.setName(name + "-GENERIC-EP");
        	taskDescription.setTaskGroup("GENERIC-EP");
        	taskDescription.setInterval(interval);
        	taskDescription.setIntervalInMs(true);
        	taskDescription.addResource(TaskDescription.INSTANCE, task);
        	taskDescription.addResource(TaskDescription.CLASSNAME, task.getClass().getName());
        	startUpController = new StartUpController();
        	startUpController.setTaskDescription(taskDescription);
        	startUpController.init(synapseEnvironment);

        } catch (Exception e) {
            log.error("Could not start Generic Processor. Error starting up scheduler. Error: " + e.getLocalizedMessage());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	public void update() {
		start();
	}

}

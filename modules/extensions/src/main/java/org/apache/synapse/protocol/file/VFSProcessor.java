/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.protocol.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.PollingProcessor;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VFSProcessor implements PollingProcessor {

	private FilePollingConsumer fileScanner;
    private String name;
    private Properties vfsProperties;
    private long interval;
    private String injectingSeq;
    private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;
    private static final Log log = LogFactory.getLog(VFSProcessor.class);
    
    public VFSProcessor(String name, Properties vfsProperties, long scanInterval, String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment) {
        this.name = name;
        this.vfsProperties = vfsProperties;
        this.interval = scanInterval;
        this.injectingSeq = injectingSeq;
        this.onErrorSeq = onErrorSeq;
        this.synapseEnvironment = synapseEnvironment;
    }

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public void init() {
    	log.info("Inbound file listener started " + name);
    	fileScanner = new FilePollingConsumer(vfsProperties, name, synapseEnvironment, interval);
    	fileScanner.registerHandler(new FileInjectHandler(injectingSeq, onErrorSeq, synapseEnvironment, vfsProperties));
    	scheduledExecutorService.scheduleAtFixedRate(fileScanner, 0, this.interval, TimeUnit.SECONDS);
    }
    
    public void destroy() {
        log.info("Inbound file listener ended " + name);
        scheduledExecutorService.shutdown();
    }
    
    /*public void run() {        
        try {
        	Thread.sleep(10000);
        	System.out.println("####################################################");
        	start();
        } catch (Exception e) {
            System.err.println("error in executing: It will no longer be run!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    } */   
    
    /*public void start() {    	
        try {
            scheduledExecutorService.scheduleAtFixedRate(fileScanner, 0, this.interval, TimeUnit.SECONDS);
        	/*TaskDescription taskDescription = new TaskDescription();
        	taskDescription.setName("testVFS");
        	taskDescription.setTaskGroup("VFS");
        	taskDescription.setTaskClass(getClass().getClassLoader().loadClass("org.wso2.carbon.mediation.task.VFSTask"));
        	taskDescription.setInterval(5000);
        	String taskManagerClassName = "org.wso2.carbon.ntaskint.core.NTaskTaskManager";
            SynapseTaskManager synapseTaskManager = synapseEnvironment.getTaskManager();
            if (!synapseTaskManager.isInitialized()) {
                //log.warn("SynapseTaskManager is not properly initialized. Initializing now with " + "default parameters.");
                synapseTaskManager.init(null, null);
            }       
            
            TaskDescriptionRepository repository = synapseTaskManager.getTaskDescriptionRepository();
            if (repository == null) {
                //handleException("Task Description Repository cannot be found");
                return;
            }
            repository.addTaskDescription(taskDescription);
            /*if (!processPinnedServers(taskDescription, synapseEnvironment)) {
                return;
            }*/
            //resolveTaskImpl(taskDescription, synapseEnvironment);
            /*Set properties = taskDescription.getXmlProperties();
            for (Object property : properties) {
                OMElement prop = (OMElement) property;
                log.debug("Found Property : " + prop.toString());
                PropertyHelper.setStaticProperty(prop, task);
            }
            if (task instanceof ManagedLifecycle) {
                ((ManagedLifecycle) task).init(synapseEnvironment);
            }*/
            //taskDescription.addResource(TaskDescription.INSTANCE, task);
            /*taskDescription.addResource(TaskDescription.CLASSNAME, "org.wso2.carbon.mediation.task.VFSTask");
            //addTaskResources(taskDescription, synapseEnvironment);

            Map<String, Object> properties = new HashMap<String, Object>();
            //map.put(TaskConstants.SYNAPSE_ENV, synapseEnvironment);
            TaskManager taskManagerImpl = null;
            try {
                Object obj = getClass().getClassLoader().loadClass("org.wso2.carbon.ntaskint.core.NTaskTaskManager").newInstance();
                taskManagerImpl = (TaskManager) obj;
                taskManagerImpl.setProperties(properties);
            } catch (Exception e) {
                //handleException("Cannot instantiate task : ", e);
            	e.printStackTrace();
            }                        
            
            //addXmlProperties(taskDescription.getXmlProperties(), taskManagerImpl);
            TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
            taskScheduler.init(synapseEnvironment.getSynapseConfiguration().getProperties(), taskManagerImpl);
            taskScheduler.scheduleTask(taskDescription);
            /*if (!submitTask(taskScheduler, taskDescription)) {
                //log.error("Could not submit task [" + taskDescription.getName() + "] to the Scheduler.");
            }*/
        /*} catch (Exception e) {
            String msg = "Error starting up Scheduler : " + e.getMessage();
            e.printStackTrace();
            //log.fatal(msg, e);
            //throw new SynapseException(msg, e);
        }   	
    }*/

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}



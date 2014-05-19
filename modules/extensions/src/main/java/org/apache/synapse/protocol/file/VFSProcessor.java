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
import org.apache.synapse.startup.quartz.StartUpController;
import org.apache.synapse.task.Task;
import org.apache.synapse.task.TaskDescription;

import org.apache.synapse.task.TaskStartupObserver;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VFSProcessor implements PollingProcessor, TaskStartupObserver {

	private FilePollingConsumer fileScanner;
    private String name;
    private Properties vfsProperties;
    private long interval;
    private String injectingSeq;
    private String onErrorSeq;
    private SynapseEnvironment synapseEnvironment;
    private static final Log log = LogFactory.getLog(VFSProcessor.class);
    private StartUpController startUpController;
    
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
    	//scheduledExecutorService.scheduleAtFixedRate(fileScanner, 0, this.interval, TimeUnit.SECONDS);
    	start();
    }
    
    public void destroy() {
        log.info("Inbound file listener ended " + name);
        //scheduledExecutorService.shutdown();
        startUpController.destroy();
    }
      
    
    public void start() {    
    	log.info("Inbound file listener " + name + " starting ...");
        try {
        	Task task = new FileTask(fileScanner);
        	TaskDescription taskDescription = new TaskDescription();
        	taskDescription.setName("testVFS");
        	taskDescription.setTaskGroup("VFS");
        	taskDescription.setInterval(5000);
        	taskDescription.setIntervalInMs(true);
        	taskDescription.setAllowConcurrentExecutions(false);
        	taskDescription.setTaskStartupObserver(this);
        	taskDescription.addResource(TaskDescription.INSTANCE, task);
        	taskDescription.addResource(TaskDescription.CLASSNAME, task.getClass().getName());
        	startUpController = new StartUpController();
        	startUpController.setTaskDescription(taskDescription);
        	startUpController.init(synapseEnvironment);

        } catch (Exception e) {
            String msg = "Error starting up Scheduler : " + e.getMessage();
            e.printStackTrace();
            //log.fatal(msg, e);
            //throw new SynapseException(msg, e);
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



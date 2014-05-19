package org.apache.synapse.startup.quartz;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.commons.util.PropertyHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.task.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.*;

public class QuartzTaskManager implements TaskManager {
    private static final Log logger = LogFactory.getLog(QuartzTaskManager.class.getName());

    /**
     * scheduler instance
     */
    private Scheduler scheduler;

    /**
     * Determines whether scheduler has been initialized and is ready to schedule a task or not.
     */
    private boolean initialized = false;

    /**
     * Default trigger factory
     */
    private TaskTriggerFactory triggerFactory = new DefaultTaskTriggerFactory();
    /**
     * Default job detail factory
     */
    private TaskJobDetailFactory jobDetailFactory = new DefaultTaskJobDetailFactory();

    /**
     * Property look up key for get a quartz configuration
     */
    public final static String QUARTZ_CONF = "quartz.conf";

    private String name;

    protected final Properties configProperties = new Properties();

    public boolean schedule(TaskDescription taskDescription) {
        assertInitialized();
        assertStarted();
        if (taskDescription == null) {
            throw new SynapseTaskException("Task Description cannot be found", logger);
        }
        if (triggerFactory == null) {
            throw new SynapseTaskException("TriggerFactory cannot be found", logger);
        }
        if (jobDetailFactory == null) {
            throw new SynapseTaskException("JobDetailFactory cannot be found", logger);
        }
        Trigger trigger = triggerFactory.createTrigger(taskDescription);
        if (trigger == null) {
            throw new SynapseTaskException("Trigger cannot be created from : "
                    + taskDescription, logger);
        }
        JobDetail jobDetail = jobDetailFactory.createJobDetail(taskDescription,
                taskDescription.getResources(), SimpleQuartzJob.class);
        if (jobDetail == null) {
            throw new SynapseTaskException("JobDetail cannot be created from : " + taskDescription +
                    " and job class " + taskDescription.getTaskImplClassName(), logger);
        }
        Object clsInstance = taskDescription.getResource(TaskDescription.INSTANCE);

        if (clsInstance == null) {
            String className = (String) taskDescription.getProperty(TaskDescription.CLASSNAME);
            try {
                clsInstance = Class.forName(className).newInstance();
                if (clsInstance instanceof ManagedLifecycle) {
                    Object se = properties.get(TaskConstants.SYNAPSE_ENV);
                    if (!(se instanceof SynapseEnvironment)) {
                        return false;
                    }
                    ((ManagedLifecycle) clsInstance).init((SynapseEnvironment) se);
                }
                for (Object property : taskDescription.getXmlProperties()) {
                    OMElement prop = (OMElement) property;
                    logger.debug("Found Property : " + prop.toString());
                    PropertyHelper.setStaticProperty(prop, clsInstance);
                }
            } catch (ClassNotFoundException e) {
                logger.error("Could not schedule task[" + name + "].", e);
                return false;
            } catch (InstantiationException e) {
                logger.error("Could not schedule task[" + name + "].", e);
                return false;
            } catch (IllegalAccessException e) {
                logger.error("Could not schedule task[" + name + "].", e);
                return false;
            }
        }
        if (!(clsInstance instanceof Task)) {
            logger.error("Could not schedule task[" + name + "]. Cannot load class " + "org.apache.synapse.startup.quartz.SimpleQuartzJob");
            return false;
        }
        jobDetail.getJobDataMap().put(TaskDescription.INSTANCE, clsInstance);
        jobDetail.getJobDataMap().put(TaskDescription.CLASSNAME, clsInstance.getClass().toString());
        jobDetail.getJobDataMap().put(TaskConstants.SYNAPSE_ENV, getProperty(TaskConstants.SYNAPSE_ENV));
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("scheduling job : " + jobDetail + " with trigger " + trigger);
            }
            if (taskDescription.getCount() != 0 && !isTaskRunning(jobDetail.getKey())) {
                try {
                    scheduler.scheduleJob(jobDetail, trigger);
                } catch (ObjectAlreadyExistsException e) {
                    logger.warn("did not schedule the job : " + jobDetail + ". the job is already running.");
                }

            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("did not schedule the job : " + jobDetail + ". count is zero.");
                }
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error scheduling job : " + jobDetail
                    + " with trigger " + trigger);
        }
        return true;
    }

    public boolean reschedule(String name, TaskDescription taskDescription) {
        logger.error("reschedule not supported. Task name: " + name);
        return false;
    }

    public boolean delete(String nameGroup) {
        if (nameGroup == null) {
            return false;
        }
        assertInitialized();
        assertStarted();
        String list[] = nameGroup.split("::");
        String name = list[0];
        String group = list[1];
        if (name == null || "".equals(name)) {
            throw new SynapseTaskException("Task Name can not be null", logger);
        }
        if (group == null || "".equals(group)) {
            group = TaskDescription.DEFAULT_GROUP;
            if (logger.isDebugEnabled()) {
                logger.debug("Task group is null or empty , using default group :"
                        + TaskDescription.DEFAULT_GROUP);
            }
        }
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting a Job with [ Name :" + name + " ]" +
                        " [ Group :" + group + " ]");
            }
            scheduler.deleteJob(new JobKey(name, group));
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error deleting a job with  [ Name :" + name + " ]" +
                    " [ Group :" + group + " ]");
        }
        return true;
    }

    public boolean pause(String name) {
        logger.error("pause not supported. Task name : " + name);
        return false;
    }

    public boolean pauseAll() {
        try {
            assertInitialized();
            assertStarted();
            scheduler.pauseAll();
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error pausing tasks ", e, logger);
        }
        return true;
    }

    public boolean resume(String name) {
        return false;
    }

    public boolean resumeAll() {
        try {
            assertInitialized();
            assertStarted();
            scheduler.resumeAll();
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error resuming tasks ", e, logger);
        }
        return true;
    }

    public TaskDescription getTask(String name) {

        return null;
    }

    public String[] getTaskNames() {
        return new String[0];
    }

    public boolean init(Properties properties) {
        StdSchedulerFactory sf = new StdSchedulerFactory();
        if (properties != null) {
            String quartzConf = properties.getProperty(QUARTZ_CONF);
            try {
                if (quartzConf != null && !"".equals(quartzConf)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Initiating a Scheduler with configuration : " + quartzConf);
                    }
                    sf.initialize(quartzConf);
                }
            } catch (SchedulerException e) {
                throw new SynapseTaskException("Error initiating scheduler factory "
                        + sf + "with configuration loaded from " + quartzConf, e, logger);
            }
        }
        try {
            if (name != null) {
                scheduler = sf.getScheduler(name);
            }
            if (scheduler == null) {
                scheduler = sf.getScheduler();
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error getting a  scheduler instance form scheduler" +
                    " factory " + sf, e, logger);
        }
        initialized = true;
        return true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean start() {
        assertInitialized();
        try {
            if (!scheduler.isStarted()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Starting a Scheduler : [ " + scheduler.getMetaData() + " ]");
                }
                scheduler.start();
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error starting scheduler ", e, logger);
        }
        return true;
    }

    public boolean stop() {
        if (isInitialized()) {
            try {
                if (scheduler != null && scheduler.isStarted()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ShuttingDown Task Scheduler : " + scheduler.getMetaData());
                    }
                    scheduler.shutdown();
                }
                initialized = false;
            } catch (SchedulerException e) {
                throw new SynapseTaskException("Error ShuttingDown task scheduler ", e, logger);
            }
        }
        return false;
    }

    public int getRunningTaskCount() {
        int runningTasks = 0;
        try {
            if (scheduler != null) {
                runningTasks = scheduler.getCurrentlyExecutingJobs().size();
            }
        } catch (SchedulerException e) {
            logger.error("Error querying currently executing jobs", e);
        }
        return runningTasks;
    }

    public boolean isTaskRunning(Object taskKey) {
        if (!(taskKey instanceof JobKey)) {
            return false;
        }
        try {
            List<JobExecutionContext> currentJobs = this.scheduler.getCurrentlyExecutingJobs();
            JobKey currentJobKey;
            for (JobExecutionContext jobCtx : currentJobs) {
                currentJobKey = jobCtx.getJobDetail().getKey();
                if (currentJobKey.compareTo((JobKey) taskKey) == 0) {
                    logger.warn("the job is already running");
                    return true;
                }
            }
        } catch (SchedulerException e) {
            return false;
        }
        return false;
    }

    private Map<String, Object> properties = new HashMap<String, Object>(5);

    public boolean setProperties(Map<String, Object> properties) {
        Iterator i = properties.keySet().iterator();
        while (i.hasNext()) {
            String k = (String) i.next();
            Object v = properties.get(k);
            this.properties.put(k, v);
        }
        return true;
    }

    public boolean setProperty(String name, Object property) {
        if ("Q_TASK_TRIGGER_FACTORY".equals(name) && (property instanceof TaskTriggerFactory)) {
            triggerFactory = (TaskTriggerFactory) property;
        }
        if ("Q_TASK_JOB_DETAIL_FACTORY".equals(name) && (property instanceof TaskJobDetailFactory)) {
            jobDetailFactory = (TaskJobDetailFactory) property;
        }
        properties.put(name, property);
        return true;
    }

    public Object getProperty(String name) {
        if (name == null) {
            return null;
        }
        return properties.get(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getProviderClass() {
        return this.getClass().getName();
    }

    public Properties getConfigurationProperties() {
        return configProperties;
    }

    public void setConfigurationProperties(Properties properties) {
        this.configProperties.putAll(properties);
    }

    private void assertInitialized() {
        if (!initialized) {
            throw new SynapseTaskException("Scheduler has not been initialled yet", logger);
        }
    }

    private void assertStarted() {
        try {
            if (!scheduler.isStarted()) {
                throw new SynapseTaskException("Scheduler has not been started yet", logger);
            }
        } catch (SchedulerException e) {
            throw new SynapseTaskException("Error determine start state of the scheduler ", e, logger);
        }
    }
}

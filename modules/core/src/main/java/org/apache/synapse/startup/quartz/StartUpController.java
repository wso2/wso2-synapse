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

package org.apache.synapse.startup.quartz;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.commons.util.PropertyHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.startup.AbstractStartup;
import org.apache.synapse.startup.tasks.MessageInjector;
import org.apache.synapse.task.SynapseTaskManager;
import org.apache.synapse.task.Task;
import org.apache.synapse.task.TaskConstants;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionRepository;
import org.apache.synapse.task.TaskManager;
import org.apache.synapse.task.TaskScheduler;

import javax.xml.namespace.QName;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StartUpController extends AbstractStartup implements AspectConfigurable {
    private static final Log logger = LogFactory.getLog(StartUpController.class.getName());

    private SynapseEnvironment synapseEnvironment;
    
    private TaskDescription taskDescription;

    private SynapseTaskManager synapseTaskManager;

    private AspectConfiguration aspectConfiguration;

    private Object task = null;

    public QName getTagQName() {
        return SimpleQuartzFactory.TASK;
    }

    /**
     * Remove the scheduled task completely. So only un-deployment will remove the task. Server node shutdown will keep
     * task intact in the registry.
     * <p>
     * This was introduced as a fix for product-ei#1206.
     *
     * @param removeTask whether keep the task or not.
     */
    public void destroy(boolean removeTask) {
        if (!destroyTask()) {
            return;
        }
        //Need to re initialize startup controller to support updates from source view
        if (!synapseTaskManager.isInitialized() && synapseEnvironment != null) {
            init(synapseEnvironment);
        }        
        if (synapseTaskManager.isInitialized()) {
            TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
            if (taskScheduler != null && taskScheduler.isTaskSchedulerInitialized() && removeTask) {
                taskScheduler.deleteTask(taskDescription.getName(), taskDescription.getTaskGroup());
            }
            TaskDescriptionRepository repository = synapseTaskManager.getTaskDescriptionRepository();
            if (repository != null) {
                repository.removeTaskDescription(taskDescription.getName());
            }
        }
    }

    public void destroy() {
        destroy(true);
    }

    /**
     * Deactivates the associated task by pausing its execution.
     *
     * @return {@code true} if the task was successfully paused; {@code false} otherwise.
     *         Possible reasons for returning {@code false} include:
     *         - The Synapse Task Manager is not initialized.
     *         - The Task Scheduler is null or not properly initialized.
     */
    public boolean deactivateTask() {
        if (!synapseTaskManager.isInitialized()) {
            return false;
        }
        TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
        if (taskScheduler == null || !taskScheduler.isTaskSchedulerInitialized()) {
            return false;
        }
        return taskScheduler.pauseTask(taskDescription.getName());
    }

    /**
     * Activates the associated task by resuming its execution.
     *
     * @return {@code true} if the task was successfully resumed; {@code false} otherwise.
     *         Possible reasons for returning {@code false} include:
     *         - The Synapse Task Manager is not initialized.
     *         - The Task Scheduler is null or not properly initialized.
     */
    public boolean activateTask() {
        if (!synapseTaskManager.isInitialized()) {
            return false;
        }
        TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
        if (taskScheduler == null || !taskScheduler.isTaskSchedulerInitialized()) {
            return false;
        }
        return taskScheduler.resumeTask(taskDescription.getName());
    }

    /**
     * Checks if the associated task is currently active.
     *
     * @return {@code true} if the task is active (i.e., not deactivated); {@code false} otherwise.
     *         Possible reasons for returning {@code false} include:
     *         - The Synapse Task Manager is not initialized.
     *         - The Task Scheduler is null or not properly initialized.
     *         - The task is explicitly marked as deactivated.
     */
    public boolean isTaskActive() {
        if (!synapseTaskManager.isInitialized()) {
            return false;
        }
        TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
        if (taskScheduler == null || !taskScheduler.isTaskSchedulerInitialized()) {
            return false;
        }
        return !taskScheduler.isTaskDeactivated(taskDescription.getName());
    }

    public void init(SynapseEnvironment synapseEnvironment) {
        this.synapseEnvironment = synapseEnvironment;
        if (taskDescription == null) {
            handleException("Error while initializing the startup. TaskDescription is null.");
        }
        initSynapseTaskManager(synapseEnvironment);
        TaskDescriptionRepository repository = synapseTaskManager.getTaskDescriptionRepository();
        if (repository == null) {
            handleException("Task Description Repository cannot be found");
            return;
        }
        repository.addTaskDescription(taskDescription);
        if (!processPinnedServers(taskDescription, synapseEnvironment)) {
            return;
        }
        resolveTaskImpl(taskDescription, synapseEnvironment);
        loadTaskProperties();
        initializeTask(synapseEnvironment);
        if(taskDescription.getResource(TaskDescription.INSTANCE) == null
                || taskDescription.getResource(TaskDescription.CLASSNAME) == null) {
            taskDescription.addResource(TaskDescription.INSTANCE, task);
            taskDescription.addResource(TaskDescription.CLASSNAME, task.getClass().getName());
        }
        try {
            Map<String, Object> map = new HashMap<>();
            map.put(TaskConstants.SYNAPSE_ENV, synapseEnvironment);
            TaskScheduler taskScheduler = synapseTaskManager.getTaskScheduler();
            TaskManager taskManager = synapseTaskManager.getTaskManagerImpl();
            if (taskManager == null) {
                logger.error("Could not initialize Start up controller. TaskManager not found.");
                return;
            }
            taskManager.setProperties(map);
            taskScheduler.init(synapseEnvironment.getSynapseConfiguration().getProperties(),
                    taskManager);
            submitTask(taskScheduler, taskDescription);
            logger.debug("Submitted task [" + taskDescription.getName() + "] to Synapse task scheduler.");
        } catch (Exception e) {
            String msg = "Error starting up Scheduler : " + e.getLocalizedMessage();
            logger.fatal(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    private void initializeTask(SynapseEnvironment synapseEnvironment) {
        if (task instanceof MessageInjector) {
            ((MessageInjector) task).init(synapseEnvironment, this.getName());
        } else if (task instanceof ManagedLifecycle) {
            ((ManagedLifecycle) task).init(synapseEnvironment);
        }
    }

    private boolean destroyTask() {
        if (taskDescription == null) {
            logger.debug("No task found to delete.");
            return false;
        }
        if (task instanceof ManagedLifecycle) {
            ((ManagedLifecycle) task).destroy();
        }
        return true;
    }

    private void loadTaskProperties() {
        Set properties = taskDescription.getXmlProperties();
        for (Object property : properties) {
            OMElement prop = (OMElement) property;
            logger.debug("loaded task property : " + prop.toString());
            PropertyHelper.setStaticProperty(prop, task);
        }
    }

    private boolean initSynapseTaskManager(SynapseEnvironment synapseEnvironment) {
        synapseTaskManager = synapseEnvironment.getTaskManager();
        if (!synapseTaskManager.isInitialized()) {
            logger.warn("SynapseTaskManager is not properly initialized. Initializing now with " +
                    "default parameters.");
            synapseTaskManager.init(null, null, null);
        }
        return true;
    }

    private boolean submitTask(TaskScheduler taskScheduler, TaskDescription taskDescription) {
        if (taskDescription == null) {
            return false;
        }
        if (taskScheduler != null) {
            return taskScheduler.scheduleTask(taskDescription);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("TaskScheduler cannot be found for :" +
                        TaskConstants.TASK_SCHEDULER + " , " +
                        "therefore ignore scheduling of Task  " + taskDescription);
            }
            return false;
        }
    }

    private boolean processPinnedServers(TaskDescription taskDescription, SynapseEnvironment synapseEnvironment) {
        String thisServerName = synapseEnvironment.getServerContextInformation()
                .getServerConfigurationInformation().getServerName();
        if (thisServerName == null || thisServerName.equals("")) {
            try {
                InetAddress address = InetAddress.getLocalHost();
                thisServerName = address.getHostName();
            } catch (UnknownHostException e) {
                logger.warn("Could not get the host name", e);
            }
            if (thisServerName == null || thisServerName.equals("")) {
                thisServerName = "localhost";
            }
        }
        logger.debug("Synapse server name : " + thisServerName);
        List pinnedServers = taskDescription.getPinnedServers();
        if (pinnedServers != null && !pinnedServers.isEmpty()) {
            if (!pinnedServers.contains(thisServerName)) {
                logger.info("Server name not in pinned servers list. Not starting Task : " +
                        getName());
                return false; // do not continue the caller of this method.
            }
        }
        return true;
    }

    private boolean resolveTaskImpl(TaskDescription taskDescription, SynapseEnvironment synapseEnvironment) {
        if (synapseEnvironment == null) {
            return false;
        }
        String taskImplClassName = taskDescription.getTaskImplClassName();
        if (taskImplClassName == null || taskImplClassName.isEmpty()) {
            taskImplClassName = "org.apache.synapse.startup.tasks.MessageInjector";
            taskDescription.setTaskImplClassName(taskImplClassName);
        }
        try {
            task = taskDescription.getResource(TaskDescription.INSTANCE);
            if (Objects.isNull(task)) {
                task = getClass().getClassLoader().loadClass(
                        taskDescription.getTaskImplClassName()).newInstance();
            }
            if (!(task instanceof Task)) {
                logger.warn("Task implementation is not a Synapse Task.");
            }
        } catch (Exception e) {
            handleException("Cannot instantiate task : " + taskDescription.getTaskImplClassName(), e);
        }
        return true;
    }

    private static void handleException(String message) {
        logger.error(message);
        throw new SynapseException(message);
    }

    private static void handleException(String message, Exception e) {
        logger.error(message, e);
        throw new SynapseException(message, e);
    }

    public TaskDescription getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(TaskDescription taskDescription) {
        this.taskDescription = taskDescription;
    }

    @Override
    public void configure(AspectConfiguration aspectConfiguration) {
        this.aspectConfiguration = aspectConfiguration;
    }

    @Override
    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }

    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (aspectConfiguration == null) {
            aspectConfiguration = new AspectConfiguration(name);
        }
        String apiId = StatisticIdentityGenerator.getIdForComponent(name, ComponentType.TASK, holder);
        aspectConfiguration.setUniqueId(apiId);
        StatisticIdentityGenerator.reportingEndEvent(apiId, ComponentType.TASK, holder);
    }
}

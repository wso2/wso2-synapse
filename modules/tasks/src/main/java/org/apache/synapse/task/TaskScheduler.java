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
package org.apache.synapse.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Properties;

@SuppressWarnings({"UnusedDeclaration"})
public class TaskScheduler {
    private static final Log logger = LogFactory.getLog(TaskScheduler.class.getName());

    private String name;

    private TaskManager taskManager;

    private boolean initialized = false;

    public TaskScheduler(String name) {
        this.name = name;
    }

    public void init(Properties properties, TaskManager taskManager) {
        initialized = false;
        setTaskManager(taskManager, properties);
        start();
    }

    private boolean setTaskManager(TaskManager taskManager, Properties properties) {
        if (taskManager == null) {
            logger.error("Task scheduler initialization failed. Task manager is invalid.");
            return false;
        }
        this.taskManager = taskManager;
        this.taskManager.setName(name);
        this.taskManager.init(properties);
        initialized = true;
        return true;
    }

    public void start() {
        if (!initialized) {
            logger.error("Could not start task scheduler. Task scheduler not properly initialized.");
            return;
        }
        taskManager.start();
    }

    public void pauseAll() {
        if (!initialized) {
            logger.error("Could not pause tasks. Task scheduler not properly initialized.");
            return;
        }
        taskManager.pauseAll();
    }
    
    public void resumeAll() {
        if (!initialized) {
            logger.error("Could not resume tasks. Task scheduler not properly initialized.");
            return;
        }
        taskManager.resumeAll();
    }

    public void scheduleTask(TaskDescription taskDescription, Map<String,
            Object> resources, Class taskClass) {
        if (!initialized) {
            logger.error("Could not schedule task ["+ taskDescription.getName() + "]. Task scheduler not properly initialized.");
            return;
        }
        taskManager.schedule(taskDescription);
    }

    public void scheduleTask(TaskDescription taskDescription, Map<String,
            Object> resources, Class jobClass, Task task) {
        if (taskDescription == null) {
            return;
        }
        if (!initialized) {
            logger.error("Could not schedule task [" + taskDescription.getName() + "]. Task scheduler not properly initialized.");
            return;
        }
        taskManager.schedule(taskDescription);
    }

    public void scheduleTask(TaskDescription taskDescription) {
        if (taskDescription == null) {
            return;
        }
        if (!initialized) {
            logger.error("Could not schedule task [" + taskDescription.getName() + "]. Task scheduler not properly initialized.");
            return;
        }
        taskManager.schedule(taskDescription);
    }

    public void shutDown() {
        if (!initialized) {
            logger.error("Could not shut down task scheduler. Task scheduler not properly initialized.");
            return;
        }
        taskManager.stop();
    }

    public boolean isInitialized() {
        if (!initialized) {
            return false;
        }
        return taskManager.isInitialized();
    }

    public void deleteTask(String name, String group) {
        if (!initialized) {
            logger.error("Could not delete task[" + name + "," + group + "]. Task scheduler not properly initialized.");
            return;
        }
        taskManager.delete(name + "::" + group);
    }
    
    public int getRunningTaskCount() {
        if (!initialized) {
            logger.error("Could not determine running task count. Task scheduler not properly initialized.");
            return -1;
        }
        return taskManager.getRunningTaskCount();
    }

    public boolean isTaskAlreadyRunning(Object taskKey) {
        if (!initialized) {
            logger.error("Could not determine task status. Task scheduler not properly initialized.");
            return false;
        }
        return taskManager.isTaskRunning(taskKey);
    }

    public void setTriggerFactory(Object triggerFactory) {
        if (!initialized) {
            logger.error("Could not modify task manager. Task scheduler not properly initialized.");
            return;
        }
        taskManager.setProperty("Q_TASK_TRIGGER_FACTORY", triggerFactory);
    }

    public void setJobDetailFactory(Object jobDetailFactory) {
        if (!initialized) {
            logger.error("Could not modify task manager. Task scheduler not properly initialized.");
            return;
        }
        taskManager.setProperty("Q_TASK_JOB_DETAIL_FACTORY", jobDetailFactory);
    }

}

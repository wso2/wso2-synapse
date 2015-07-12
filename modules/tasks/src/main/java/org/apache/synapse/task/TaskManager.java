/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.task;

import java.util.Map;
import java.util.Properties;

public interface TaskManager {
    /**
     * Schedules a task on an external task scheduler based on the parameters of the <tt>TaskDescription</tt>.
     * @param taskDescription Contains parameters for the task scheduler. eg. target type, url, trigger parameters etc.
     * @return <tt>true</tt> if the scheduling is successful, <tt>false</tt> otherwise.
     */
    public boolean schedule(TaskDescription taskDescription);

    public boolean reschedule(String name, TaskDescription taskDescription);

    public boolean delete(String name);

    public boolean pause(String name);

    public boolean pauseAll();

    public boolean resume(String name);

    public boolean resumeAll();

    public TaskDescription getTask(String name);

    public String[] getTaskNames();


    public boolean init(Properties properties);

    public boolean isInitialized();

    public boolean start();

    public boolean stop();

    public int getRunningTaskCount();

    public boolean isTaskRunning(Object taskKey);

    public boolean setProperties(Map<String, Object> properties);

    public boolean setProperty(String name, Object property);

    public Object getProperty(String name);

    public void setName(String name);

    public String getName();

    public String getProviderClass();

    public Properties getConfigurationProperties();

    public void setConfigurationProperties(Properties properties);

    /**
     * Adds a new Observer to the list of observers of this task manager.
     *
     * @param o
     *            New Observer to be registered or subscribed.
     */
    void addObserver(TaskManagerObserver o);

    /**
     * Checks whether the {@link Task} with the given name is deactivated.
     *
     * @param taskName
     *            name of the Task
     * @return <code>true</code> if the task is deactivated,<code>false</code>
     *         otherwise
     */
    boolean isTaskDeactivated(final String taskName);

    /**
     * Checks whether the {@link Task} with the given name is blocked without
     * giving the control back to the schedular.
     *
     * @param taskName
     *            name of the Task
     * @return <code>true</code> if the task is blocked, <code>false</code>
     *         otherwise.
     */
    boolean isTaskBlocked(final String taskName);

    /**
     * Checks whether the {@link Task} with the given name is running.
     *
     * @param taskName
     *            name of the Task
     * @return <code>true</code> if the task is running,<code>false</code>
     *         otherwise.
     */
    boolean isTaskRunning(final String taskName);

}

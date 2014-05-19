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

}

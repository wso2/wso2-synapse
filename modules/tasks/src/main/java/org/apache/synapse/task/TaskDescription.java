package org.apache.synapse.task;

import org.apache.axiom.om.OMElement;

import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public final class TaskDescription {
    public static final String CLASSNAME = "ClassName";
    public static final String PROPERTIES = "Properties";
    public static final String INSTANCE = "Instance";
    public static final String DEFAULT_GROUP = "synapse.simple.quartz";

    private String taskName;

    private String taskGroup;

    /** Name of the task manager class */
    private String taskMgrClassName;

    /** Where to receive the message */
    private int triggerCount = -1;

    private long triggerInterval = 0;

    private boolean isIntervalInMs = false;

    private String cronExpression;

    private Calendar startTime;

    private Calendar endTime;

    private List<String> pinnedServersList;
    /** Other properties */
    private Map<String, String> properties;

    private Map<String, Object> resources;

    private final Set<OMElement> xmlProperties = new HashSet<OMElement>();

    private String taskDescription;

    public String getName() {
        return taskName;
    }

    public void setName(String name) {
        this.taskName = name;
    }

    public void setProperties(Map<String, String> properties) {
        if (properties == null) {
            return;
        }
        if (this.properties == null) {
            this.properties = properties;
            return;
        } else {
            this.properties = new HashMap<String, String>();
        }
        for (String key : properties.keySet()) {
            this.properties.put(key, properties.get(key));
        }
    }

    public void addProperty(String name, String value) {
        if (properties == null) {
            properties = new HashMap<String, String>();
        }
        if (name == null) {
            return;
        }
        properties.put(name, value);
    }

    public Object getProperty(String name) {
        return properties == null || name == null ? null : properties.get(name);
    }

    public Map<String, String> getProperties() {
        return properties == null ? Collections.<String, String>emptyMap() : unmodifiableMap(properties);
    }

    public String getTaskGroup() {
        return taskGroup;
    }

    public void setTaskGroup(String taskGroup) {
        this.taskGroup = taskGroup;
    }

    public String getTaskImplClassName() {
        if (getProperty(CLASSNAME) != null) {
            return (String) getProperty(CLASSNAME);
        }
        return taskMgrClassName;
    }

    public void setTaskImplClassName(String taskMgrClassName) {
        this.taskMgrClassName = taskMgrClassName;
    }

    public int getCount() {
        return triggerCount;
    }

    public void setCount(int count) {
        this.triggerCount = count;
    }

    public long getInterval() {
        return triggerInterval;
    }

    public boolean getIntervalInMs() {
        return isIntervalInMs;
    }

    public void setInterval(long interval) {
        this.triggerInterval = interval;
    }

    public List<String> getPinnedServers() {
        return pinnedServersList == null ? Collections.<String>emptyList() : unmodifiableList(pinnedServersList);
    }

    public void setPinnedServers(List<String> pinnedServersList) {
        if (pinnedServersList == null) {
            return;
        }
        this.pinnedServersList = pinnedServersList;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    public void setIntervalInMs(boolean isIntervalInMs) {
        this.isIntervalInMs = isIntervalInMs;
    }

    public Map<String, Object> getResources() {
        return resources == null ? Collections.<String, Object>emptyMap() : unmodifiableMap(resources);
    }

    public void setResources(Map<String, Object> resources) {
        if (resources == null) {
            return;
        }
        this.resources = resources;
    }

    public void addResource(String key, Object value) {
        if (resources == null) {
            resources = new HashMap<String, Object>();
        }
        if (key != null) {
            resources.put(key, value);
        }
    }

    public Object getResource(String key) {
        if (resources != null && key != null) {
            return resources.get(key);
        }
        return null;
    }

    public Set<OMElement> getXmlProperties() {
        return xmlProperties == null ? Collections.<OMElement>emptySet() : unmodifiableSet(xmlProperties);
    }

    public void setXmlProperty(OMElement property) {
        if (property == null) {
            return;
        }
        xmlProperties.add(property);
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        if (taskDescription == null) {
            return;
        }
        this.taskDescription = taskDescription;
    }
}

package org.apache.synapse.task;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Collections.unmodifiableMap;

public final class TaskDescription {
    public static final String TASK_MGR_ClASS_NAME = "org.apache.synapse.task.taskMgrImpl";
    public static final String DEF_TASKMGR_CLASS_NAME = "org.apache.synapse.startup.quartz.QuartzTaskManager";
    public static final String CLASSNAME = "ClassName";
    public static final String PROPERTIES = "Properties";
    public static final String INSTANCE = "Instance";
    public static final String DEFAULT_GROUP = "synapse.simple.quartz";
    private static final Log logger = LogFactory.getLog(TaskDescription.class.getName());
    public static final int SEQUENCE = 0;
    public static final int MAIN = 1;
    public static final int PROXY = 2;
    public static final int RECIPE = 3;
    public static final int URL = 4;
    public static final int OTHER = 5;
    public static final int NOT_SET = 6;

    private String taskName;

    private String taskGroup;
    /** Name of the task manager class */
    private String taskMgrClassName;
    /** Where to receive the message */
    private String sequenceName;
    private TaskStartupObserver taskStartupObserver;
    private String proxyName;
    private String recipeName;
    private String receiver;
    private int receiverType = NOT_SET;
    /** Message data */
    private String message;
    private InputStream messageIs;
    private String format;
    private String mediaType;
    /** Trigger parameters */
    private boolean isSimpleTrigger = true;
    private int triggerCount = -1;
    private long triggerInterval = 0;
    private boolean isIntervalInMs = false;
    private String cronExpression;
    private Calendar startTime;
    private Calendar endTime;

    private String status = "new";

    private String pinnedServers;

    private List<String> pinnedServersList;

    private String targetURI;

    private boolean allowConcurrentExecutions;
    /** Other properties */
    private Map<String, String> properties;

    private Map<String, Object> resources;

    private Class taskClass;

    private Task synapseTask;

    private final Set<OMElement> xmlProperties = new HashSet<OMElement>();
    private String taskDescription;
    private boolean volatility = true;

//    public Object getTaskObject() {
//        return taskObject;
//    }
//
//    public void setTaskObject(Object taskObject) {
//        this.taskObject = taskObject;
//    }

//    private Object taskObject;

//    public TaskManager getTaskManager() {
//        return taskManager;
//    }
//
//    public void setTaskManager(TaskManager taskManager) {
//        this.taskManager = taskManager;
//    }
//
//    private TaskManager taskManager;

    public String getName() {
        return taskName;
    }

    public void setName(String name) {
        this.taskName = name;
    }
//
//    public String getTargetURI() {
//        return targetURI;
//    }
//
//    public void setTargetURI(String targetURI) {
//        this.targetURI = targetURI;
//    }
//
//    public boolean isAllowConcurrentExecutions() {
//        return allowConcurrentExecutions;
//    }

//    public void setAllowConcurrentExecutions(boolean allowConcurrentExecutions) {
//        this.allowConcurrentExecutions = allowConcurrentExecutions;
//    }

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
        Iterator i = properties.keySet().iterator();
        while (i.hasNext()) {
            Object o = i.next();
            this.properties.put((String) o, properties.get((String) o));
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
        if (properties == null) {
            return Collections.emptyMap();
        }
        return unmodifiableMap(properties);
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

//    public String getProxyName() {
//        return proxyName;
//    }
//
//    public void setProxyName(String proxyName) {
//        this.proxyName = proxyName;
//    }

//    public String getReceiver() {
//        return receiver;
//    }

//    public void setReceiver(String receiver) {
//        if (receiver == null || receiver.isEmpty()) {
//            return;
//        }
//        if ("sequence".equals(receiver)) {
//            receiverType = SEQUENCE;
//        } else if ("main".equals(receiver)) {
//            receiverType = MAIN;
//        } else if ("proxy".equals(receiver)) {
//            receiverType = PROXY;
//        } else if ("recipe".equals(receiver)) {
//            receiverType = RECIPE;
//        } else if ("url".equals(receiver)) {
//            receiverType = URL;
//        } else {
//            receiverType = OTHER;
//        }
//        this.receiver = receiver;
//    }

//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getFormat() {
//        return format;
//    }
//
//    public void setFormat(String format) {
//        this.format = format;
//    }

//    public String getSequenceName() {
//        return sequenceName;
//    }
//
//    public void setSequenceName(String sequenceName) {
//        this.sequenceName = sequenceName;
//    }
//
//    public boolean isSimpleTrigger() {
//        return isSimpleTrigger;
//    }
//
//    public void setSimpleTrigger(boolean isSimpleTrigger) {
//        this.isSimpleTrigger = isSimpleTrigger;
//    }

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

//    public void setPinnedServersS(String pinnedServers) {
//        this.pinnedServers = pinnedServers;
//    }
//
//    public String getPinnedServersS() {
//        return this.pinnedServers;
//    }

    public List<String> getPinnedServers() {
        return pinnedServersList;
    }

    public void setPinnedServers(List<String> pinnedServersList) {
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

//    public void setStartTime(String timeString) { setStartTime(getTime(timeString)); }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

//    public void setEndTime(String timeString) { setEndTime(getTime(timeString)); }

    public void setIntervalInMs(boolean isIntervalInMs) {
        this.isIntervalInMs = isIntervalInMs;
    }

//    public InputStream getMessageIs() { return messageIs; }
//
//    public void setMessageIs(InputStream messageIs) { this.messageIs = messageIs; }
//
//    public String getMediaType() { return mediaType; }
//
//    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
//
//    public String getRecipeName() {
//        return recipeName;
//    }
//
//    public void setRecipeName(String recipeName) {
//        this.recipeName = recipeName;
//    }
//
//    public int getReceiverType() {
//        return receiverType;
//    }

//    private static Calendar getTime(String startTimeAsString) {
//        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//        Date startTime;
//        try {
//            startTime = df.parse(startTimeAsString);
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(startTime);
//            return cal;
//        } catch (ParseException e) {
//            logger.error("Invalid time format ["
//                    + startTimeAsString + "] for date format [yyyy/MM/dd HH:mm:ss].");
//        }
//        return null;
//    }

//    public void setStatus(String status) {
//        if (status == null || status.isEmpty()) {
//            return;
//        }
//        this.status = status;
//    }
//
//    public String getStatus() { return status; }

    public Map<String, Object> getResources() {
        return resources;
    }

    public void setResources(Map<String, Object> resources) {
        this.resources = resources;
    }

    public void addResource(String key, Object value) {
        if (resources == null) {
            resources = new HashMap<String, Object>();
        }
        if (resources != null && key != null) {
            resources.put(key, value);
        }
    }

    public Object getResource(String key) {
        if (resources != null && key != null) {
            return resources.get(key);
        }
        return null;
    }

//    public Class getTaskClass() {
//        return taskClass;
//    }
//
//    public void setTaskClass(Class taskClass) {
//        this.taskClass = taskClass;
//    }
//
//    public Task getSynapseTask() {
//        return synapseTask;
//    }
//
//    public void setSynapseTask(Task task) {
//        this.synapseTask = task;
//    }

    public Set<OMElement> getXmlProperties() {
        return xmlProperties;
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
        this.taskDescription = taskDescription;
    }

//    public boolean isVolatility() {
//        return volatility;
//    }
//
//    public void setVolatility(boolean volatility) {
//        this.volatility = volatility;
//    }
//
//	public TaskStartupObserver getTaskStartupObserver() {
//		return taskStartupObserver;
//	}
//
//	public void setTaskStartupObserver(TaskStartupObserver taskStartupObserver) {
//		this.taskStartupObserver = taskStartupObserver;
//	}

}

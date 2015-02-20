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
package org.apache.synapse.message.processor.impl;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.processor.impl.forwarder.ForwardingProcessorConstants;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.task.Task;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskManager;
import org.wso2.carbon.mediation.ntask.NTaskTaskManager;
import org.wso2.carbon.mediation.ntask.TaskManagerObserver;

public abstract class ScheduledMessageProcessor extends AbstractMessageProcessor implements TaskManagerObserver{
    private static final Log logger = LogFactory.getLog(ScheduledMessageProcessor.class.getName());

    /**
     * The interval at which this processor runs , default value is 1000ms
     */
    protected long interval = MessageProcessorConstants.THRESHOULD_INTERVAL;;

    /**
     * A cron expression to run the sampler
     */
    protected String cronExpression = null;

    /**
     * This only needed for the associated service. This value could not be changed manually. Moving to this state
     * only happens when the service reaches the maximum retry limit
     */
    protected AtomicBoolean isPaused = new AtomicBoolean(false);

    private AtomicBoolean isActivated = new AtomicBoolean(true);

    /**
     * This is specially used for REST scenarios where http status codes can take semantics in a RESTful architecture.
     */
    protected String[] nonRetryStatusCodes = null;
    
	protected BlockingMsgSender sender;

	protected SynapseEnvironment synapseEnvironment;

	private TaskManager nTaskManager = null;

	/*
	 * This was used to guard the messageProcessorState enum against concurrent
	 * modifications.
	 */
	private final Object lock = new Object();

	private MessageProcessorState messageProcessorState = MessageProcessorState.OTHER;

	private int memberCount = 1;
    
    private static final String TASK_PREFIX = "MSMP_";

	public void init(SynapseEnvironment se) {
		this.synapseEnvironment = se;
		initMessageSender(parameters);
		if (!isPinnedServer(se.getServerContextInformation().getServerConfigurationInformation()
		                      .getServerName())) {
			/*
			 * If it is not a pinned server we do not start the message
			 * processor. In that server.
			 */
			setActivated(false);
		}
		super.init(se);
		/*
		 * initialize the task manager only once to alleviate complexities
		 * related to the pending tasks.
		 */
		if (nTaskManager == null) {
			nTaskManager = TaskManagerFactory.createNTaskTaskManager();
			nTaskManager.setName(name + " Schedular");
			nTaskManager.init(synapseEnvironment.getSynapseConfiguration().getProperties());
		}
		/*
		 * If the task manager is not initialized yet, subscribe to
		 * initialization completion event here.
		 */
		if (!nTaskManager.isInitialized()) {
			((NTaskTaskManager) nTaskManager).addObserver(this);
			return;
		}
		if (!isDeactivated()) {
			this.start();
		}

	}

	public boolean start() {
		for (int i = 0; i < memberCount; i++) {
			/*
			 * Make sure to fetch the task after initializing the message sender
			 * and consumer properly. Otherwise you may get NullPointer
			 * exceptions.
			 */
			Task task = this.getTask();
			TaskDescription taskDescription = new TaskDescription();
			taskDescription.setName(TASK_PREFIX + name + i);
			taskDescription.setTaskGroup(MessageProcessorConstants.SCHEDULED_MESSAGE_PROCESSOR_GROUP);
			/*
			 * If this interval value is less than 1000 ms, ntask will throw an
			 * exception while building the task. So to get around that we are
			 * setting threshold interval value of 1000 ms to the task
			 * description here. But actual interval value may be less than 1000
			 * ms, and hence isThrotling is set to TRUE.
			 */
			if (interval < MessageProcessorConstants.THRESHOULD_INTERVAL) {
				taskDescription.setInterval(MessageProcessorConstants.THRESHOULD_INTERVAL);
			} else {
				taskDescription.setInterval(interval);
			}
			taskDescription.setIntervalInMs(true);
			taskDescription.addResource(TaskDescription.INSTANCE, task);
			taskDescription.addResource(TaskDescription.CLASSNAME, task.getClass().getName());
			nTaskManager.schedule(taskDescription);
		}
		messageProcessorState = MessageProcessorState.STARTED;
		if (logger.isDebugEnabled()) {
			logger.info("Started message processor. [" + getName() + "].");
		}

		return true;
	}
	
	/*
	 * Fetches the number of tasks running on behalf of this message processor
	 * at the moment.
	 */
	private int getRunningTaskCount() {
		int runningTaskCount = 0;
		List<String> runningTaskList = ((NTaskTaskManager) nTaskManager).getRunningTaskList();
		if (runningTaskList != null) {
			for (String taskName : runningTaskList) {
				if (taskName.contains(TASK_PREFIX + name)) {
					runningTaskCount++;
				}
			}
		}
		return runningTaskCount;
	}

	public boolean isDeactivated() {
		boolean deactivated;
		synchronized (lock) {
			deactivated = messageProcessorState.equals(MessageProcessorState.STOPPED);
		}

		if (nTaskManager.isInitialized() && ((NTaskTaskManager) nTaskManager).isMgrNode() &&
		    ((NTaskTaskManager) nTaskManager).getServerCount() > 0) {
			deactivated = deactivated || getRunningTaskCount() == 0 ? true : false;

		}

		return deactivated;
	}

    public void setParameters(Map<String, Object> parameters) {

        super.setParameters(parameters);

        if (parameters != null && !parameters.isEmpty()) {
            Object o = parameters.get(MessageProcessorConstants.CRON_EXPRESSION);
            if (o != null) {
                cronExpression = o.toString();
            }
            o = parameters.get(MessageProcessorConstants.INTERVAL);
            if (o != null) {
                interval = Integer.parseInt(o.toString());
            }
			o = parameters.get(MessageProcessorConstants.MEMBER_COUNT);
			if (o != null) {
				memberCount = Integer.parseInt(o.toString());
			}
            o = parameters.get(MessageProcessorConstants.IS_ACTIVATED);
            if (o != null) {
                isActivated.set(Boolean.valueOf(o.toString()));
            }
            o = parameters.get(ForwardingProcessorConstants.NON_RETRY_STATUS_CODES);
            if (o != null) {
                // we take it out of param set and send it because we need split the array.
                nonRetryStatusCodes = o.toString().split(",");
            }
        }
    }


	public boolean stop() {
		/*
		 * There could be servers that are disabled at startup time.
		 * therefore not started but initiated.
		 */
		if (nTaskManager != null && nTaskManager.isInitialized()) {
			for (int i = 0; i < memberCount; i++) {
				/*
				 * This is to immediately stop the scheduler to avoid firing new
				 * services
				 */
				nTaskManager.pause(TASK_PREFIX + name + i);
				if (logger.isDebugEnabled()) {
					logger.debug("ShuttingDown Message Processor Scheduler : " +
					             nTaskManager.getName());
				}
				// gracefully shutdown
				/*
				 * This value should be given in the format -->
				 * taskname::taskgroup.
				 * Otherwise a default group is assigned by the ntask task
				 * manager.
				 */
				nTaskManager.delete(TASK_PREFIX + name + i + "::" +
				                    MessageProcessorConstants.SCHEDULED_MESSAGE_PROCESSOR_GROUP);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Stopped message processor [" + getName() + "].");
			}

			return true;
		}

		return false;
	}

	public void destroy() {
		/*
		 * Since for one scheduler there is only one job, we can simply shutdown
		 * the scheduler
		 * which will cause to shutdown the job
		 */
		try {
			stop();
			// TODO: Check whether this is really needed.
			messageProcessorState = MessageProcessorState.DESTROYED;
		}

		finally {
			if (getMessageConsumer() != null) {
				boolean success = getMessageConsumer().cleanup();
				if (!success) {
					logger.error("[" + getName() + "] Could not cleanup message consumer.");
				}
			} else {
				logger.warn("[" + getName() + "] Could not find the message consumer to cleanup.");
			}
		}

		if (logger.isDebugEnabled()) {
			logger.info("Successfully destroyed message processor [" + getName() + "].");
		}
	}

	public boolean deactivate() {
		if (nTaskManager != null && nTaskManager.isInitialized()) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Deactivating message processor [" + getName() + "]");
				}

				pauseService();

				logger.info("Successfully deactivated the message processor [" + getName() + "]");

				setActivated(false);
			} finally {
				// This is to remove the consumer from the queue.
				/*
				 * This will close the connection with the JMS Provider/message
				 * store.
				 */
				if (messageConsumer != null) {
					messageConsumer.cleanup();
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean activate() {
		if (messageConsumer == null) {
			/*
			 * This is for the message processors who are deactivated at
			 * startup time.
			 */
			setMessageConsumer(configuration.getMessageStore(messageStore).getConsumer());
		}

		/*
		 * Checking whether it is already deactivated. If it is deactivated only
		 * we can activate again.
		 */
		if (nTaskManager != null && isDeactivated()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting Message Processor Scheduler : " + nTaskManager.getName());
			}

			resumeService();

			logger.info("Successfully re-activated the message processor [" + getName() + "]");

			setActivated(true);

			return true;
		} else {
			return false;
		}
	}

	public void pauseService() {
		for (int i = 0; i < memberCount; i++) {
			nTaskManager.pause(TASK_PREFIX + name + i);
		}
		synchronized (lock) {
			messageProcessorState = MessageProcessorState.PAUSED;
		}
	}

	public void resumeService() {
		for (int i = 0; i < memberCount; i++) {
			nTaskManager.resume(TASK_PREFIX + name + i);
		}
		synchronized (lock) {
			messageProcessorState = MessageProcessorState.STARTED;
		}
	}

	public boolean isActive() {
		return !isDeactivated();
	}

	public boolean isPaused() {
		return messageProcessorState.equals(MessageProcessorState.PAUSED);
	}

	public boolean getActivated() {
		return messageProcessorState.equals(MessageProcessorState.STARTED);
	}

    public void setActivated(boolean activated) {
        isActivated.set(activated);
        parameters.put(MessageProcessorConstants.IS_ACTIVATED, String.valueOf(getActivated()));
    }

    private Properties getSchedulerProperties(String name) {
        Properties config = new Properties();

        // This is important to have a separate scheduler for each message processor.
        config.put(MessageProcessorConstants.SCHEDULER_INSTANCE_NAME, name);
        config.put(MessageProcessorConstants.SCHEDULER_RMI_EXPORT, "false");
        config.put(MessageProcessorConstants.SCHEDULER_RMI_PROXY, "false");
        config.put(MessageProcessorConstants.SCHEDULER_WRAP_JOB_EXE_IN_USER_TRANSACTION, "false");
        config.put(MessageProcessorConstants.THREAD_POOL_CLASS, "org.quartz.simpl.SimpleThreadPool");
        // This is set to one because according to the current implementation one scheduler
        // only have one job
        config.put(MessageProcessorConstants.THREAD_POOL_THREAD_COUNT, "1");
        config.put(MessageProcessorConstants.THREAD_POOL_THREAD_PRIORITY, "5");
        config.put(MessageProcessorConstants.JOB_STORE_MISFIRE_THRESHOLD, "60000");
        config.put(MessageProcessorConstants.THREAD_INHERIT_CONTEXT_CLASSLOADER_OF_INIT_THREAD, "true");
        config.put(MessageProcessorConstants.JOB_STORE_CLASS, "org.quartz.simpl.RAMJobStore");

        return config;
    }

    private boolean isPinnedServer(String serverName) {
        boolean pinned = false;
        Object pinnedServersObj = this.parameters.get(MessageProcessorConstants.PINNED_SERVER);

        if (pinnedServersObj != null && pinnedServersObj instanceof String) {

            String pinnedServers = (String) pinnedServersObj;
            StringTokenizer st = new StringTokenizer(pinnedServers, " ,");

            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim();
                if (serverName.equals(token)) {
                    pinned = true;
                    break;
                }
            }
            if (!pinned) {
                logger.info("Message processor '" + name + "' pinned on '" + pinnedServers + "' not starting on" +
                        " this server '" + serverName + "'");
            }
        } else {
            // this means we have to use the default value that is to start the message processor
            pinned = true;
        }

        return pinned;
    }

	/**
	 * nTask does not except values less than 0 for its schedule interval. Therefore when the
	 * interval is zero we have
	 * to handle as a separate case.
	 * 
	 * @param interval
	 *            in which scheduler triggers its job.
	 * @return true if it has run on non-throttle mode.
	 */
	protected boolean isThrottling(long interval) {
		return interval < MessageProcessorConstants.THRESHOULD_INTERVAL;
	}

	protected boolean isThrottling(String cronExpression) {
		return cronExpression != null;
	}
	
	private BlockingMsgSender initMessageSender(Map<String, Object> params) {

		String axis2repo = (String) params.get(ForwardingProcessorConstants.AXIS2_REPO);
		String axis2Config = (String) params.get(ForwardingProcessorConstants.AXIS2_CONFIG);

		sender = new BlockingMsgSender();
		if (axis2repo != null) {
			sender.setClientRepository(axis2repo);
		}
		if (axis2Config != null) {
			sender.setAxis2xml(axis2Config);
		}
		sender.init();

		return sender;
	}
	
	/**
	 * Retrieves the message processors current state.
	 * 
	 * @return current state of the message processor.
	 */
	public MessageProcessorState getMessageProcessorState() {
		return messageProcessorState;
	}

	/**
	 * Gives the {@link Task} instance associated with this processor.
	 * 
	 * @return {@link Task} associated with this processor.
	 */
	protected abstract Task getTask();

	public void update() {
		start();

	}

}

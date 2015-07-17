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

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessorCleanupService;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.processor.impl.forwarder.ForwardingProcessorConstants;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.task.Task;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskManager;
import org.apache.synapse.task.TaskManagerObserver;

import sun.misc.Service;

/**
 * Implements the common message processor infrastructure which is used by the
 * both <code>Forwarding</code> and <code>Sampling</code> message Processors.
 * Mainly
 * responsible for handling life cycle states of the message processors. Some of
 * the well known life cycle states are <code>start</code>, <code>pause</code> ,
 * <code>destroy</code>, <code>deactivate</code> etc.
 *
 *
 */
public abstract class ScheduledMessageProcessor extends AbstractMessageProcessor implements TaskManagerObserver{
    private static final Log logger = LogFactory.getLog(ScheduledMessageProcessor.class.getName());

    /**
     * The interval at which this processor runs , default value is 1000ms
     */
    protected long interval = MessageProcessorConstants.THRESHOULD_INTERVAL;

    /**
     * A cron expression to run the sampler
     */
    protected String cronExpression = null;

    /**
     * This is specially used for REST scenarios where http status codes can take semantics in a RESTful architecture.
     */
    protected String[] nonRetryStatusCodes = null;

	protected BlockingMsgSender sender;

	protected SynapseEnvironment synapseEnvironment;

	private TaskManager taskManager = null;

	private int memberCount = 1;

    private static final String TASK_PREFIX = "MSMP_";

    private static final String DEFAULT_TASK_SUFFIX = "0";

    @Override
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
		if (taskManager == null) {
			taskManager = synapseEnvironment.getSynapseConfiguration().getTaskManager();
		}

        if (taskManager == null) {
            throw new SynapseException("Task Manager not defined in the configuration.");
        }


		/*
		 * If the task manager is not initialized yet, subscribe to
		 * initialization completion event here.
		 */
		if (!taskManager.isInitialized()) {
			taskManager.addObserver(this);
			return;
		}

		if (!isManuallyDeactivated()) {
			this.start();
		}

	}

    private boolean isManuallyDeactivated() {
        Object isActiveParam = parameters.get(MessageProcessorConstants.IS_ACTIVATED);
        if (isActiveParam != null) {
            if (!Boolean.parseBoolean(String.valueOf(isActiveParam))) {
                return true;
            }
        }
        return isDeactivated();
    }

    @Override
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

			/*
			 * If there is a Cron Expression we need to set it into the
			 * TaskDescription so that the framework will take care of it.
			 */
			if (cronExpression != null) {
				taskDescription.setCronExpression(cronExpression);
			}
			taskManager.schedule(taskDescription);
		}
		logger.info("Started message processor. [" + getName() + "].");

		return true;
	}

    @Override
    public boolean isDeactivated() {
		return taskManager.isTaskDeactivated(TASK_PREFIX + name +
		                                                           DEFAULT_TASK_SUFFIX);
	}

    @Override
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
				setActivated(Boolean.valueOf(o.toString()));
			}
			o = parameters.get(ForwardingProcessorConstants.NON_RETRY_STATUS_CODES);
			if (o != null) {
				// we take it out of param set and send it because we need split
				// the array.
				nonRetryStatusCodes = o.toString().split(",");
			}
		}
	}


    @Override
    public boolean stop() {
		/*
		 * There could be servers that are disabled at startup time.
		 * therefore not started but initiated.
		 */
		if (taskManager != null && taskManager.isInitialized()) {
			for (int i = 0; i < memberCount; i++) {
				/*
				 * This is to immediately stop the scheduler to avoid firing new
				 * services
				 */
				taskManager.pause(TASK_PREFIX + name + i);
				if (logger.isDebugEnabled()) {
					logger.debug("ShuttingDown Message Processor Scheduler : " +
					             taskManager.getName());
				}
				/*
				 * This value should be given in the format -->
				 * taskname::taskgroup.
				 * Otherwise a default group is assigned by the ntask task
				 * manager.
				 */
				taskManager.delete(TASK_PREFIX + name + i + "::" +
				                    MessageProcessorConstants.SCHEDULED_MESSAGE_PROCESSOR_GROUP);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Stopped message processor [" + getName() + "].");
			}

			return true;
		}

		return false;
	}

    @Override
    public void destroy() {
        try {
            stop();
        }

        finally {
            if (getMessageConsumer() != null && messageConsumers.size() > 0) {
                boolean success = getMessageConsumer().get(0).cleanup();
                if (!success) {
                    logger.error("[" + getName() + "] Could not cleanup message consumer.");
                }
            } else {
                logger.warn("[" + getName() + "] Could not find the message consumer to cleanup.");
            }
            /*
             * Cleaning up the resources in the cluster mode here.
             */
            taskManager.sendClusterMessage(getMessageProcessorCleanupTask());
        }

        if (logger.isDebugEnabled()) {
            logger.info("Successfully destroyed message processor [" + getName() + "].");
        }

    }

    @Override
    public boolean deactivate() {
        if (taskManager != null && taskManager.isInitialized()) {
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deactivating message processor [" + getName() + "]");
                }

                pauseService();

                logger.info("Successfully deactivated the message processor [" + getName() + "]");

            } finally {
                /*
                 * This will close the connection with the JMS Provider/message
                 * store.
                 */
                if (messageConsumers != null && messageConsumers.size() > 0) {
                    messageConsumers.get(0).cleanup();
                }

                /*
                 * Cleaning up the resources in the cluster mode here.
                 */
                taskManager.sendClusterMessage(getMessageProcessorCleanupTask());
            }
            return true;
        } else {
            return false;
        }
	}

    @Override
    public boolean activate() {
		/*
		 * Checking whether it is already deactivated. If it is deactivated only
		 * we can activate again.
		 */
		if (taskManager != null && isDeactivated()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting Message Processor Scheduler : " + taskManager.getName());
			}

			resumeService();

			logger.info("Successfully re-activated the message processor [" + getName() + "]");

			return true;
		} else {
			return false;
		}
	}

    @Override
    public void pauseService() {
		for (int i = 0; i < memberCount; i++) {
			taskManager.pause(TASK_PREFIX + name + i);
		}
	}

    @Override
    public void resumeService() {
		for (int i = 0; i < memberCount; i++) {
			taskManager.resume(TASK_PREFIX + name + i);
		}
	}

	public boolean isActive() {
		/*
		 * If the interval value is less than 1000 ms, then the task is run
		 * inside the while loop. Due to that control is not returned back to
		 * the taskmanager and hence the task is in BLOCKED state. This
		 * situation is handled separately.
		 */
		if (isThrottling(interval)) {
			return taskManager.isTaskBlocked(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX) ||
			       taskManager.isTaskRunning(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX);
		}
		return taskManager.isTaskRunning(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX);
	}

    @Override
    public boolean isPaused() {
		return taskManager.isTaskDeactivated(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX);
	}

	public boolean getActivated() {
		return taskManager.isTaskRunning(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX);
	}

	private void setActivated(boolean activated) {
		parameters.put(MessageProcessorConstants.IS_ACTIVATED, String.valueOf(activated));
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
	 * nTask does not except values less than 1000 for its schedule interval.
	 * Therefore when the
	 * interval is less than 1000 ms we have
	 * to handle it as a separate case.
	 *
	 * @param interval
	 *            in which scheduler triggers its job.
	 * @return true if it needs to run on throttle mode, <code>false</code>
	 *         otherwise.
	 */
	protected boolean isThrottling(final long interval) {
		return interval < MessageProcessorConstants.THRESHOULD_INTERVAL;
	}

	public boolean isThrottling(final String cronExpression) {
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
	 * Gives the {@link Task} instance associated with this processor.
	 *
	 * @return {@link Task} associated with this processor.
	 */
	protected abstract Task getTask();

    @Override
    public void update() {
		if (!isManuallyDeactivated()) {
			start();
		}
	}

    @Override
    public void cleanupLocalResources() {
        if (messageConsumers != null) {
            for (MessageConsumer messageConsumer : messageConsumers) {
                messageConsumer.cleanup();
            }
        }
    }

    private Callable<Void> getMessageProcessorCleanupTask() {
        MessageProcessorCleanupService cleanupTask = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to fetch InboundRequestProcessor from classpath.. ");
        }
        Iterator<MessageProcessorCleanupService> it =
                                                      Service.providers(MessageProcessorCleanupService.class);
        while (it.hasNext()) {
            cleanupTask = it.next();
            cleanupTask.setName(name);
            if (cleanupTask != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Message Processor Cleanup Service found  : " +
                                 cleanupTask.getClass().getName());
                }
            }
            return cleanupTask;
        }
        return null;
    }
}

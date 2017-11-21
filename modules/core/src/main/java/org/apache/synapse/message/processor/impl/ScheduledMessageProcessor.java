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
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.processor.MessageProcessorCleanupService;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.processor.impl.failover.FailoverForwardingService;
import org.apache.synapse.message.processor.impl.forwarder.ForwardingProcessorConstants;
import org.apache.synapse.message.processor.impl.forwarder.ForwardingService;
import org.apache.synapse.message.processor.impl.sampler.SamplingService;
import org.apache.synapse.message.senders.blocking.BlockingMsgSender;
import org.apache.synapse.registry.Registry;
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
	/**
	 * Task associated with Message Processor
	 */
	private Task task;

	private int memberCount = 1;

	private Registry registry;

	private static final String REG_PROCESSOR_BASE_PATH = "/repository/components/org.apache.synapse.message.processor/";

	private static final String MP_STATE = "MESSAGE_PROCESSOR_STATE";

    private static final String TASK_PREFIX = "MSMP_";

    private static final String DEFAULT_TASK_SUFFIX = "0";

    @Override
    public void init(SynapseEnvironment se) {
		this.synapseEnvironment = se;
		initMessageSender(parameters);
		registry = se.getSynapseConfiguration().getRegistry();
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

        /*
         * Schedule the task despite if it is ACTIVATED OR DEACTIVATED
         * initially. Eventhough the Message Processor is explicitly deactivated
         * initially, we need to have a Task to handle subsequent updates.
         */
        this.start();

	}

    /*
     * Fetches the value of the IsActivated Property of Message Processor. The
     * IsActivated property resides under the Advanced parameters section of the
     * Message Processor.
     */
    public boolean getIsActivatedParamValue() {
        Object isActiveParam = parameters.get(MessageProcessorConstants.IS_ACTIVATED);
        // Message Processor is ACTIVATED by default.
        boolean isActivated = true;
        if (isActiveParam != null) {
            isActivated = Boolean.parseBoolean(String.valueOf(isActiveParam));
        }
        return isActivated;
    }

    protected boolean isProcessorStartAsDeactivated() {
        // the Activate parameter is only helpful for the message processor to deploy for the very first time.
        // once it is deployed, the message processor should you its own state

        if (getProcessorState() == ProcessorState.INITIAL) {
            return !getIsActivatedParamValue();
        }
        return (getProcessorState() == ProcessorState.PAUSED);
    }

    @Override
    public boolean start() {
		for (int i = 0; i < memberCount; i++) {
			/*
			 * Make sure to fetch the task after initializing the message sender
			 * and consumer properly. Otherwise you may get NullPointer
			 * exceptions.
			 */
			task = this.getTask();
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
        
        /*
         * If the Message Processor is Deactivated through the Advanced parameter 
         * explicitly, then we deactivate the task immediately.
         */
        if (isProcessorStartAsDeactivated()) {
            deactivate();
        }
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


    public boolean stop(boolean isShuttingDown) {
		/*
		 * There could be servers that are disabled at startup time.
		 * therefore not started but initiated.
		 */
		if (taskManager != null && taskManager.isInitialized()) {

			for (int i = 0; i < memberCount; i++) {
				/*
				 * This is to immediately stop the scheduler to avoid firing new
				 * services
				 * The task will not be paused if undeployment is being done in a
				 * shutdown of a node in a cluster, to allow another node to pick up the task.
				 */
				if (!isShuttingDown || !taskManager.isClusteredTaskManager()) {
					if (taskManager.isTaskExist(TASK_PREFIX + name + i) &&
							taskManager.isTaskRunning(TASK_PREFIX + name + i)) {
						taskManager.pause(TASK_PREFIX + name + i);
					}
				}
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
				                    MessageProcessorConstants.SCHEDULED_MESSAGE_PROCESSOR_GROUP, isShuttingDown);
				//even the task is existed or not at Task REPO we need to clear the NTaskAdaptor
				//synapseTaskProperties map which holds taskName and TASK Instance for expired TASK at undeployment.
				//taskManager.delete() method does that. There is a possibility to reinitialize those expired tasks
				//from Ntask core if the expired task existed in mentioned property map in worker nodes.
				//Even the manager first deletes the Task from Ntask core task repository, at that
				//point all the workers see this task as non existing task but the property map will not be updated
				//as we are not triggering taskManager.delete() to non existing task. Which in that case all worker node's
				//property map leaves expired tasks which ultimate lead to re initialize them.
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Stopped message processor [" + getName() + "].");
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean stop() {
        return stop(false);
	}

	@Override
	public void destroy(){
		destroy(false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy(boolean preserveState) {
		destroy(preserveState, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy(boolean preserveState, boolean isShuttingDown) {
        try {
            stop(isShuttingDown);
        }

        finally {
            /*
             * If the Task is scheduled with an interval value < 1000 ms, it is
             * executed outside Quartz. For an example Task with interval 200ms.
             * Here we directly pause the task, move on and cleanup the JMS
             * connection.But actual pausing the task through TaskManager takes
             * few ms (say 300 ms). During this time the Task is executed
             * outside Quartz and
             * a new JMS connection is created (After the previous cleanup).
             * Once
             * the task is paused and destroyed successfully, we create a new
             * task, for which a new JMS
             * connection is created. This leads to multiple JMS connections and
             * is a Bug. This 1000 ms sleep is used to make sure that the task
             * is paused before cleaning up the JMS connection, which prevents
             * multiple JMS connections being created.
             */
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("The thread was interrupted while sleeping");
            }
            if (getMessageConsumer() != null && messageConsumers.size() > 0) {
                cleanupLocalResources();
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

        if (!preserveState) {
            deleteMessageProcessorState();
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
                setMessageProcessorState(ProcessorState.PAUSED);

                logger.info("Successfully deactivated the message processor [" + getName() + "]");

            } finally {
                /*
                 * This will close the connection with the JMS Provider/message
                 * store.
                 */
                cleanupLocalResources();

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
			setMessageProcessorState(ProcessorState.RUNNING);

			logger.info("Successfully re-activated the message processor [" + getName() + "]");

			return true;
		} else {
			return false;
		}
	}

    @Override
    public void pauseService() {
	    if (task instanceof ForwardingService) {
		    ((ForwardingService) task).terminate();
	    } else if (task instanceof SamplingService) {
		    ((SamplingService) task).terminate();
	    } else if (task instanceof FailoverForwardingService) {
		    ((FailoverForwardingService) task).terminate();
	    }
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
         * Sometimes when the backend is down, we may retry the delivery for a
         * specified number of attempts. Due to that control is not returned
         * back to the Quartz for some time (may be few secs depending on the
         * config). Therefore the task is in Blocked
         * state as far as Quartz is concerned. But we are running the execute
         * method of the task in the meantime with our own logic. Therefore
         * though the task is blocked from Quartz, still we are executing it. So
         * that implies it is running.
         */
        return taskManager.isTaskRunning(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX) ||
               taskManager.isTaskBlocked(TASK_PREFIX + name + DEFAULT_TASK_SUFFIX);
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
	    start();
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

	private ProcessorState getProcessorState() {
		Properties resourceProperties = registry.getResourceProperties(REG_PROCESSOR_BASE_PATH + getName());

		if (resourceProperties == null) {
			return ProcessorState.INITIAL;
		}

		String state = resourceProperties.getProperty(MP_STATE);
		if (ProcessorState.RUNNING.toString().equalsIgnoreCase(state)) {
			return ProcessorState.RUNNING;
		}
		return ProcessorState.PAUSED;
	}

	private enum ProcessorState {
		INITIAL, RUNNING, PAUSED
	}

	private void setMessageProcessorState(ProcessorState state) {
		registry.newNonEmptyResource(REG_PROCESSOR_BASE_PATH + getName(), false, "text/plain", state.toString(),
				MP_STATE);
	}

    private void deleteMessageProcessorState() {
        if (registry.getResourceProperties(REG_PROCESSOR_BASE_PATH + getName()) != null) {
            registry.delete(REG_PROCESSOR_BASE_PATH + getName());
        }
    }
}

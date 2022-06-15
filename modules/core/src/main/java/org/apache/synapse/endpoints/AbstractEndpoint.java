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

package org.apache.synapse.endpoints;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.PropertyInclude;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessController;
import org.apache.synapse.commons.throttle.core.ConcurrentAccessReplicator;
import org.apache.synapse.analytics.AnalyticsPublisher;
import org.apache.synapse.transport.util.MessageHandlerProvider;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.transport.passthru.util.RelayConstants;
import org.apache.synapse.util.logging.LoggingUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An abstract base class for all Endpoint implementations
 */
@SuppressWarnings({"UnusedDeclaration"})
public abstract class AbstractEndpoint extends FaultHandler implements Endpoint, PropertyInclude {

    protected Log log;
    protected static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /** Hold the logical name of an endpoint */
    private String endpointName = null;

    /** Hold the description of an endpoint */
    private String description = null;

    /** The parent endpoint for this endpoint */
    private Endpoint parentEndpoint = null;

    /** The child endpoints of this endpoint - if any */
    private List<Endpoint> children = null;

    /** The Endpoint definition for this endpoint - i.e. holds all static endpoint information */
    private EndpointDefinition definition = null;

    /** Has this endpoint been initialized ? */
    protected volatile boolean initialized = false;

    /** The endpoint context - if applicable - that will hold the runtime state of the endpoint */
    private EndpointContext context = null;

    /** Is clustering enabled */
    protected Boolean isClusteringEnabled = null;

    /** The MBean managing the endpoint */
    EndpointView metricsMBean = null;

    /** The name of the file where this endpoint is defined */
    protected String fileName;

    /** Map for storing configuration parameters */
    private Map<String, MediatorProperty> properties = new HashMap<String, MediatorProperty>();

    protected boolean anonymous = false;

    /** The Sequence name associated with the endpoint*/
    protected String errorHandler = null;

    private boolean enableMBeanStats = true;

    private boolean contentAware = false;

    private boolean forceBuildMC =false;

    protected String artifactContainerName;

    private boolean isEdited = false;

    /** Attributes for json representation  */
    public static final String NAME_JSON_ATT = "name";
    public static final String TYPE_JSON_ATT = "type";
    public static final String CHILDREN_JSON_ATT = "children";

    /** json representation of the endpoint */
    JSONObject endpointJson = null;

    /**
     * Holds the list of comments associated with the proxy service.
     */
    private List<String> commentsList = new ArrayList<String>();

    protected AbstractEndpoint() {
        log = LogFactory.getLog(this.getClass());
    }

    //------------------------------- getters and setters ------------------------------------------

    public EndpointView getMetricsMBean() {
        return metricsMBean;
    }

    public EndpointContext getContext() {
        return context;
    }

    public String getName() {
        return endpointName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public EndpointDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(EndpointDefinition definition) {
        this.definition = definition;
        definition.setLeafEndpoint(this);
    }

    public Endpoint getParentEndpoint() {
        return parentEndpoint;
    }

    public void setParentEndpoint(Endpoint parentEndpoint) {
        this.parentEndpoint = parentEndpoint;
    }

    public List<Endpoint> getChildren() {
        return children;
    }

    public void setChildren(List<Endpoint> children) {
        this.children = children;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public String toString() {
        if (endpointName != null) {
            return "Endpoint [" + endpointName + "]";
        }
        return SynapseConstants.ANONYMOUS_ENDPOINT;
    }

    public void setName(String endpointName) {
        this.endpointName = endpointName;
        if (enableMBeanStats) {
            /*if (endpointName != null && !"".equals(endpointName.trim())){
                //we skip stat collection for endpoints with no defined name
                log.warn("Endpoint Name not found. Skipped JMX statistics collection for this endpoint");
                return;
            }*/
            metricsMBean = new EndpointView(endpointName, this);
//            if(metricsMBean != null) {
//                metricsMBean.destroy();
//            }

            MBeanRegistrar.getInstance().registerMBean(metricsMBean, "Endpoint", endpointName);
        }
    }

    /**
     * Set the car File name which this endpoint deployed from
     * @param name
     */
    public void setArtifactContainerName (String name) {
        artifactContainerName = name;
    }

    /**
     * Get the car File name which this endpoint deployed from
     * @return artifactContainerName
     */
    public String getArtifactContainerName () {
        return artifactContainerName;
    }

    /**
     * Get the edit state of the endpoint
     * @return
     */
    public boolean getIsEdited() {
        return isEdited;
    }

    /**
     * Set the edit state of the endpoint
     * @param isEdited
     */
    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    /**
     * set whether this endpoint needs to be registered for JMX MBeans. some endpoints may not need
     * to register under MBean and setting false will cut the additional overhead.
     * @param flag set true/false
     */
    public void setEnableMBeanStats(boolean flag){
        enableMBeanStats = flag;
    }

    //----------------------- default method implementations and common code -----------------------

    public void init(SynapseEnvironment synapseEnvironment) {
        ConfigurationContext cc =
                ((Axis2SynapseEnvironment) synapseEnvironment).getAxis2ConfigurationContext();
        if (!initialized) {
            // The check for clustering environment
            ClusteringAgent clusteringAgent = cc.getAxisConfiguration().getClusteringAgent();
            if (clusteringAgent != null && clusteringAgent.getStateManager() != null) {
                isClusteringEnabled = Boolean.TRUE;
            } else {
                isClusteringEnabled = Boolean.FALSE;
            }

            context = new EndpointContext(getName(), getDefinition(), isClusteringEnabled,
                    cc, metricsMBean);
        }
        initialized = true;

        if (children != null) {
            for (Endpoint e : children) {
                e.init(synapseEnvironment);
            }
        }

        contentAware = definition != null && ((definition.getFormat() != null && !definition.getFormat().equals(SynapseConstants.FORMAT_REST)) ||
                definition.isSecurityOn() || definition.isReliableMessagingOn() ||
                definition.isAddressingOn() || definition.isUseMTOM()|| definition.isUseSwa());
    }

    public List<String> getCommentsList() {
        return commentsList;
    }

    public void setCommentsList(List<String> commentsList) {
        this.commentsList = commentsList;
    }

    public boolean readyToSend() {
        if (!initialized) {
            //can't send to a non-initialized endpoint. This is a program fault
            throw new IllegalStateException("not initialized, " +
                    "endpoint must be in initialized state");
        }

        return context != null && context.readyToSend();
    }

    public void send(MessageContext synCtx) {

        synCtx.recordLatency();
        logSetter();

        Integer statisticReportingIndex = null;
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        if (isStatisticsEnabled) {
            statisticReportingIndex = OpenEventCollector.reportEntryEvent(synCtx, getReportingName(),
                    definition.getAspectConfiguration(), ComponentType.ENDPOINT);
        }

        boolean traceOn = isTraceOn(synCtx);
        boolean traceOrDebugOn = isTraceOrDebugOn(traceOn);

        if (!initialized) {
            //can't send to a non-initialized endpoint. This is a program fault
            throw new IllegalStateException("not initialized, " +
                                            "endpoint must be in initialized state");
        }

        prepareForEndpointStatistics(synCtx);

        if (traceOrDebugOn) {
            String address = definition.getAddress();
            if (address == null && synCtx.getTo() != null && synCtx.getTo().getAddress() != null) {
                // compute address for the default endpoint only for logging purposes
                address = synCtx.getTo().getAddress();
            }

            traceOrDebug(traceOn, "Sending message through endpoint : " +
                                  getName() + " resolving to address = " + address);
            traceOrDebug(traceOn, "SOAPAction: " + (synCtx.getSoapAction() != null ?
                                                    synCtx.getSoapAction() : "null"));
            traceOrDebug(traceOn, "WSA-Action: " + (synCtx.getWSAAction() != null ?
                                                    synCtx.getWSAAction() : "null"));
            if (traceOn && trace.isTraceEnabled()) {
                trace.trace("Envelope : \n" + synCtx.getEnvelope());
            }
        }

        // push the errorHandler sequence into the current message as the fault handler
        if (errorHandler != null) {
            Mediator errorHandlerMediator = synCtx.getSequence(errorHandler);
            if (errorHandlerMediator != null) {
                if (traceOrDebugOn) {
                    traceOrDebug(traceOn, "Setting the onError handler : " +
                                          errorHandler + " for the endpoint : " + endpointName);
                }
                synCtx.pushFaultHandler(
                        new MediatorFaultHandler(errorHandlerMediator));
            } else {
                log.warn("onError handler sequence : " + errorHandler + " for : " +
                         endpointName + " cannot be found");
            }
        }

        // register this as the immediate fault handler for this message.
        synCtx.pushFaultHandler(this);
        // add this as the last endpoint to process this message - used by statistics counting code
        synCtx.setProperty(SynapseConstants.LAST_ENDPOINT, this);
        // set message level metrics collector
        org.apache.axis2.context.MessageContext axis2Ctx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        axis2Ctx.setProperty(BaseConstants.METRICS_COLLECTOR, metricsMBean);

        if (contentAware) {
            try {
                MessageHandlerProvider.getMessageHandler(axis2Ctx).buildMessage(axis2Ctx, false);

                axis2Ctx.setProperty(RelayConstants.FORCE_RESPONSE_EARLY_BUILD, Boolean.TRUE);
                if (forceBuildMC) {
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().getEnvelope().build();
                }
            } catch (Exception e) {
                handleException("Error while building message", e, synCtx);
            }
        }

        evaluateProperties(synCtx);

        // if the envelope preserving set build the envelope
        MediatorProperty preserveEnv = getProperty(SynapseConstants.PRESERVE_ENVELOPE);
        if (preserveEnv != null && JavaUtils.isTrueExplicitly(preserveEnv.getValue() != null ?
                                                              preserveEnv.getValue() : preserveEnv.getEvaluatedExpression(synCtx))) {
            if (traceOrDebugOn) {
                traceOrDebug(traceOn, "Preserving the envelope by building it before " +
                                      "sending, since it is explicitly set");
            }
            synCtx.getEnvelope().build();
        }

        // Send the message through this endpoint
        synCtx.getEnvironment().send(definition, synCtx);

        if (isStatisticsEnabled) {
            CloseEventCollector.closeEntryEvent(synCtx, getReportingName(), ComponentType.ENDPOINT,
                    statisticReportingIndex, false);
        }

        AnalyticsPublisher.publishEndpointAnalytics(synCtx, definition);
    }

    /**
     * Is this a leaf level endpoint? or parent endpoint that has children?
     * @return true if there is no children - a leaf endpoint
     */
    public boolean isLeafEndpoint() {
        return children == null || children.size() == 0;
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        // do nothing, the LB/FO endpoints will override this
    }

    public void executeEpTypeSpecificFunctions(MessageContext synCtx) {
        // do nothing, the Http endpoint will override this
    }

    /**
     * Is this [fault] message a timeout?
     * @param synCtx the current fault message
     * @return true if this is defined as a timeout
     */
    protected boolean isTimeout(MessageContext synCtx) {

		Object error = synCtx.getProperty(SynapseConstants.ERROR_CODE);
		Integer errorCode = 0;
		if (error != null) {
			try {
				errorCode = Integer.parseInt(error.toString());
			} catch (NumberFormatException e) {
				errorCode = 0;
			}
		}
        if (errorCode != null) {
            if (definition.getTimeoutErrorCodes().isEmpty()) {
                // if timeout codes are not defined, assume only HTTP timeout and connection close
                boolean isTimeout = SynapseConstants.NHTTP_CONNECTION_TIMEOUT == errorCode;
                boolean isClosed = SynapseConstants.NHTTP_CONNECTION_CLOSED == errorCode;

                if (isTimeout || isClosed) {

                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a default HTTP connection " +
                                (isClosed ? "close" : "timeout") + " error : " + errorCode);
                    }
                    return true;
                }
            } else {
                if (definition.getTimeoutErrorCodes().contains(errorCode)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a mark for suspension error : " + errorCode
                                + " defined " + "error codes are : "
                                + definition.getTimeoutErrorCodes());
                    }
                    return true;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Encountered a non-timeout error sending to " + this.toString() +
                ", error code : " + errorCode);
        }
        return false;
    }


    protected boolean isRetry(MessageContext synCtx) {
        Integer errorCode = (Integer) synCtx.getProperty(SynapseConstants.ERROR_CODE);
        if (errorCode != null && definition != null) {
            if (definition.getRetryDisabledErrorCodes().contains(errorCode)) {
                if (log.isDebugEnabled()) {
                    log.debug("Encountered a retry disabled error : " + errorCode
                            + ", defined retry disabled error codes are : "
                            + definition.getRetryDisabledErrorCodes());
                }
                // for given disabled error codes system wont retry
                return false;
            } else if (definition.getRetryEnableErrorCodes().size() > 0) {
                if (definition.getRetryEnableErrorCodes().contains(errorCode)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a retry enabled error : " + errorCode
                                + ", defined retry Enable error codes are : "
                                + definition.getRetryEnableErrorCodes());
                    }
                    // for given error codes in EnableErrorCodes only system retries
                    return true;
                } else {
                    return false;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Encountered an error sending to endpoint : " + endpointName +
                    ", with error code : " + errorCode + ", but not a retry disabled error");
        }
        return true;
    }

    /**
     * Is this a fault that should put the endpoint on SUSPEND? or is this a fault to ignore?
     * @param synCtx the current fault message
     * @return true if this fault should suspend the endpoint
     */
    protected boolean isSuspendFault(MessageContext synCtx) {
        Integer errorCode = (Integer) synCtx.getProperty(SynapseConstants.ERROR_CODE);
        if (errorCode != null) {
            if (definition.getSuspendErrorCodes().isEmpty()) {
                // if suspend codes are not defined, any error will be fatal for the endpoint
                if (log.isDebugEnabled()) {
                    log.debug(this.toString() + " encountered a fatal error : " + errorCode);
                }
                return true;

            } else {
                if (definition.getSuspendErrorCodes().contains(errorCode)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encountered a suspend error : " + errorCode +
                            " defined suspend codes are : " + definition.getSuspendErrorCodes());
                    }
                    return true;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Encountered a non-fatal error sending to " + this.toString()
                    + ", error code : " + errorCode
                    + ". Error will be handled, but endpoint will not fail");
        }
        return false;
    }

    /**
     * On a fault, propagate to parent if any, or call into the fault handler
     *
     * @param synCtx the message at hand
     */
    public void onFault(MessageContext synCtx) {
        EndpointDefinition endpointDefinition = getDefinition();
        if (endpointDefinition != null && endpointDefinition.getTimeoutAction() == SynapseConstants.DISCARD) {
            log.info("Ignoring fault handlers since the timeout action is set to DISCARD");
        } else {
            logSetter();
            invokeNextFaultHandler(synCtx);
        }
    }

    /**
     * The SynapseCallback Receiver notifies an endpoint, if a message was successfully processed
     * to give it a chance to clear up or reset its state to active
     */
    public void onSuccess() {
        // do nothing
    }


    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     *
     * @param msgCtx the current message
     * @return true if tracing should be performed
     */
    protected boolean isTraceOn(MessageContext msgCtx) {
        return (definition.getAspectConfiguration() != null && definition.getAspectConfiguration().isTracingEnabled());
    }

    /**
     * Is tracing or debug logging on?
     *
     * @param isTraceOn is tracing known to be on?
     * @return true, if either tracing or debug logging is on
     */
    protected boolean isTraceOrDebugOn(boolean isTraceOn) {
        return isTraceOn || log.isDebugEnabled();
    }

    /**
     * Perform Trace and Debug logging of a message @INFO (trace) and DEBUG (log)
     *
     * @param traceOn is runtime trace on for this message?
     * @param msg     the message to log/trace
     */
    protected void traceOrDebug(boolean traceOn, String msg) {
        if (traceOn) {
            trace.info(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    /**
     * Process statistics for this message
     * @param synCtx the current message
     */
    protected void prepareForEndpointStatistics(MessageContext synCtx) {
        // Setting Required property to reportForComponent the End Point aspects
        if (definition != null && definition.isStatisticsEnable()) {
            String opName = null;

            if (synCtx.getProperty(SynapseConstants.ENDPOINT_OPERATION) != null) {
                opName = synCtx.getProperty(SynapseConstants.ENDPOINT_OPERATION).toString();
            } else if (synCtx instanceof Axis2MessageContext) {
                AxisOperation operation
                        = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getAxisOperation();
                if (operation != null) {
                    opName = operation.getName().getLocalPart();
                }
                if (opName == null ||
                        SynapseConstants.SYNAPSE_OPERATION_NAME.getLocalPart().equals(opName)) {
                    String soapAction = synCtx.getSoapAction();
                    opName = null;
                    if (soapAction != null) {
                        int index = soapAction.indexOf("urn:");
                        if (index >= 0) {
                            opName = soapAction.substring("urn:".length());
                        } else {
                            opName = soapAction;
                        }
                    }
                }
            }

            AspectConfiguration oldConfiguration = definition.getAspectConfiguration();
            if (opName != null) {
                AspectConfiguration newConfiguration = new AspectConfiguration(
                        oldConfiguration.getId() + SynapseConstants.STATISTICS_KEY_SEPARATOR +
                        opName);
                if (oldConfiguration.isStatisticsEnable()) {
                    newConfiguration.enableStatistics();
                }
                if (oldConfiguration.isTracingEnabled()) {
                    newConfiguration.enableTracing();
                }
            }
        }
    }

    /**
     * Helper methods to handle errors.
     *
     * @param msg The error message
     */
    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Helper methods to handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Helper methods to handle errors.
     *
     * @param msg    The error message
     * @param e      The exception
     * @param msgCtx The message context
     */
    protected void handleException(String msg, Exception e, MessageContext msgCtx) {

        String formattedLog = LoggingUtils.getFormattedLog(msgCtx, msg);
        log.error(formattedLog, e);
        throw new SynapseException(msg, e);
    }

    protected void logOnChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        log.warn(this + " Detect a Failure in a child endpoint : " + endpoint);
    }

    protected void informFailure(MessageContext synCtx, int errorCode, String errorMsg) {

        Boolean isConcurrencyThrottleEnabled = (Boolean) synCtx
                .getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE);

        if (isConcurrencyThrottleEnabled != null && isConcurrencyThrottleEnabled) {
            ConcurrentAccessController concurrentAccessController = (ConcurrentAccessController)
                    synCtx
                            .getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_CONTROLLER);
            int available = concurrentAccessController.incrementAndGet();
            int concurrentLimit = concurrentAccessController.getLimit();
            if (log.isDebugEnabled()) {
                log.debug("Concurrency Throttle : Connection returned" + " :: " +
                        available + " of available of " + concurrentLimit + " connections");
            }
            ConcurrentAccessReplicator concurrentAccessReplicator = (ConcurrentAccessReplicator)
                    synCtx
                            .getProperty(SynapseConstants.SYNAPSE_CONCURRENT_ACCESS_REPLICATOR);
            String throttleKey = (String) synCtx
                    .getProperty(SynapseConstants.SYNAPSE_CONCURRENCY_THROTTLE_KEY);
            if (concurrentAccessReplicator != null) {
                concurrentAccessReplicator.replicate(throttleKey, true);
            }
        }

        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
            setErrorOnMessage(synCtx, errorCode, errorMsg);
        }
        invokeNextFaultHandler(synCtx);
    }


    protected void setErrorOnMessage(MessageContext synCtx, Integer errorCode, String errorMsg) {
		Map<String, Integer> mEndpointLog =
		                                    (Map<String, Integer>) synCtx.getProperty(SynapseConstants.ENDPOINT_LOG);
		if (mEndpointLog != null) {
			AbstractEndpoint lastEndpoint =
			                                (AbstractEndpoint) synCtx.getProperty(SynapseConstants.LAST_ENDPOINT);
			Object oErrorCode = synCtx.getProperty(SynapseConstants.ERROR_CODE);
			if (lastEndpoint != null && lastEndpoint.getName() != null && oErrorCode != null) {
				try {
					mEndpointLog.put(lastEndpoint.getName(), (Integer) oErrorCode);
				} catch (NumberFormatException nfe) {
					log.error("Unable to get the error code for endpoint");
				}
			}
		}

        synCtx.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMsg);
        synCtx.setProperty(SynapseConstants.ERROR_DETAIL, errorMsg);
        synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, errorMsg);
    }

    private void invokeNextFaultHandler(MessageContext synCtx) {

        Stack faultStack = synCtx.getFaultStack();
        if (!faultStack.isEmpty()) {
            Object faultHandler = faultStack.pop();
            if (faultHandler instanceof Endpoint) {
                // This is the parent . need to inform parent with fault child
                ((Endpoint) faultHandler).onChildEndpointFail(this, synCtx);
            } else if (faultHandler instanceof MediatorFaultHandler) {
                if(!executeLastSequenceFaultHandler(synCtx)){
                    ((FaultHandler) faultHandler).handleFault(synCtx);
                }
            } else {
                ((FaultHandler) faultHandler).handleFault(synCtx);
            }
        } else {
            executeLastSequenceFaultHandler(synCtx);
        }
    }

    /**
     * If LAST_SEQ_FAULT_HANDLER property is not null, this method will execute the sequence specified by it.
     *
     * @param synCtx message context
     * @return true if LAST_SEQ_FAULT_HANDLER get executed.
     */
    private boolean executeLastSequenceFaultHandler(MessageContext synCtx) {
        Object errorCode = synCtx.getProperty(SynapseConstants.ERROR_CODE);
        Object lastSequenceFaultHandler = synCtx.getProperty(SynapseConstants.LAST_SEQ_FAULT_HANDLER);

        if (lastSequenceFaultHandler != null && errorCode != null && errorCode instanceof Integer &&
                (((Integer) errorCode) == SynapseConstants.NHTTP_CONNECTION_FAILED)) {
            ((FaultHandler) lastSequenceFaultHandler).handleFault(synCtx, null);
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
        if(metricsMBean != null) {
            metricsMBean.destroy();
        }

        if (enableMBeanStats) {
            MBeanRegistrar.getInstance().unRegisterMBean("Endpoint", endpointName);
        }
        metricsMBean = null;

        this.initialized = false;
    }

    /**
     * Add a property to the endpoint.
      * @param property property to be added
     */
    public void addProperty(MediatorProperty property) {
        properties.put(property.getName(), property);
    }

    /**
     * Get a property with the given name
     * @param name name of the property
     *
     * @return a property with the given name
     */
    public MediatorProperty getProperty(String name) {
        MediatorProperty value = properties.get(name);
        if (value == null) {
            if (getParentEndpoint() instanceof PropertyInclude) {
                value = ((PropertyInclude)getParentEndpoint()).getProperty(name);
            }
        }
        return value;
    }

    /**
     * Return the <code>Collection</code> of properties specified
     *
     * @return <code>Collection</code> of properties
     */
    public Collection<MediatorProperty> getProperties() {
        return properties.values();
    }

    /**
     * Remove a property with the given name
     * @param name name of the property to be removed
     *
     * @return the remove property or <code>null</code> if a property doesn't exists
     */
    public MediatorProperty removeProperty(String name) {
        return properties.remove(name);
    }

    /**
     * Add all the properties to the endpoint
     * @param mediatorProperties <code>Collection</code> of properties to be added
     */
    public void addProperties(Collection<MediatorProperty> mediatorProperties) {
        for (MediatorProperty property : mediatorProperties) {
            properties.put(property.getName(), property);
        }
    }

    public String getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(String errorHandler) {
        this.errorHandler = errorHandler;
    }



    public void setContentAware(boolean contentAware) {
    	this.contentAware = contentAware;
    }



	public void setForceBuildMC(boolean forceBuildMC) {
    	this.forceBuildMC = forceBuildMC;
    }

	/**
     * Evaluates the endpoint properties based on the current message context and set
     * the properties to the message context appropriately
     * @param synCtx the current message context
     */
    protected void evaluateProperties(MessageContext synCtx) {
        // evaluate the properties
        Set<Map.Entry<String, MediatorProperty>> propertySet = properties.entrySet();
        for (Map.Entry<String, MediatorProperty> e : propertySet) {
            e.getValue().evaluate(synCtx);
        }
    }

    public void logSetter() {
        CustomLogSetter.getInstance().setLogAppender(artifactContainerName);
    }

    public String getReportingName() {
        if (this.endpointName != null) {
            return this.endpointName;
        } else {
            return SynapseConstants.ANONYMOUS_ENDPOINT;
        }
    }

    private boolean isStatisticCollected() {
        return (definition.getAspectConfiguration() != null &&
                definition.getAspectConfiguration().isStatisticsEnable() && this.endpointName != null);
    }

    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (this instanceof IndirectEndpoint) {
            String sequenceId = StatisticIdentityGenerator
                    .getIdReferencingComponent(((IndirectEndpoint) (this)).getKey(), ComponentType.ENDPOINT, holder);

            StatisticIdentityGenerator.reportingEndEvent(sequenceId, ComponentType.ENDPOINT, holder);
        } else {
            if (definition == null) {
                EndpointDefinition definition = new EndpointDefinition();
                this.setDefinition(definition);
            }
            if (definition.getAspectConfiguration() == null) {
                definition.configure(new AspectConfiguration(getReportingName()));
            }
            String sequenceId = StatisticIdentityGenerator.getIdForComponent(getReportingName(), ComponentType.ENDPOINT, holder);
            definition.getAspectConfiguration().setUniqueId(sequenceId);

            StatisticIdentityGenerator.reportingEndEvent(sequenceId, ComponentType.ENDPOINT, holder);
        }
    }

    /**
     * Set advanced properties of the endpoint to json object.
     */
    protected void setAdvancedProperties() {

        JSONObject advancedProps = new JSONObject();
        endpointJson.put("advanced", advancedProps);
        setSuspendStateProperties(getDefinition(), advancedProps);
        setTimeoutStateProperties(getDefinition(), advancedProps);
    }

    /**
     * Set time-out state properties of the endpoint to json object.
     */
    private void setTimeoutStateProperties(EndpointDefinition definition, JSONObject advancedProps) {

        JSONObject timeoutStateProps = new JSONObject();
        advancedProps.put("timeoutState", timeoutStateProps);
        timeoutStateProps.put("errorCodes", definition.getTimeoutErrorCodes());
        timeoutStateProps.put("reties", definition.getRetriesOnTimeoutBeforeSuspend());
    }

    /**
     * Set suspend state properties of the endpoint to json object.
     */
    private void setSuspendStateProperties(EndpointDefinition definition, JSONObject advancedProps) {

        JSONObject suspendStatePros = new JSONObject();
        advancedProps.put("suspendState", suspendStatePros);
        suspendStatePros.put("errorCodes", definition.getSuspendErrorCodes());
        suspendStatePros.put("maxDuration", definition.getSuspendMaximumDuration());
        suspendStatePros.put("initialDuration", definition.getInitialSuspendDuration());
    }

    protected JSONArray getEndpointChildrenAsJson(List<Endpoint> children) {
        JSONArray childrenJsonList = new JSONArray();

        if (children != null && children.size() != 0) {
            for (Endpoint child : children) {
                childrenJsonList.put(child.getJsonRepresentation());
            }
        }
        return childrenJsonList;
    }

    @Override
    public JSONObject getJsonRepresentation() {

        if (endpointJson == null) {
            createJsonRepresentation();
        }
        return endpointJson;
    }

    protected abstract void createJsonRepresentation();

}

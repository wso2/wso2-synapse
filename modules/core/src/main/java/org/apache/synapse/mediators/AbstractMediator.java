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

package org.apache.synapse.mediators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfigurable;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.debug.constructs.SynapseMediationFlowPoint;
import org.apache.synapse.util.logging.LoggingUtils;

import java.util.ArrayList;
import java.util.List;

import java.util.Stack;

/**
 * This is the super class of all mediators, and defines common logging, tracing other aspects
 * for all mediators who extend from this.
 * elements of a mediator class.
 */
public abstract class AbstractMediator implements Mediator, AspectConfigurable {

    /** the standard log for mediators, will assign the logger for the actual subclass */
    protected Log log;
    /** The runtime trace log for mediators */
    protected static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /**
     * State of tracing for this mediator
     */
    protected int traceState = SynapseConstants.TRACING_UNSET;

    private AspectConfiguration aspectConfiguration;

    private String description;

    private String shortDescription;

    private int mediatorPosition = 0;

    private boolean isBreakPoint = false;

    private boolean isSkipEnabled = false;

    private SynapseMediationFlowPoint flowPoint = null;


    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractMediator() {
        log = LogFactory.getLog(this.getClass());
    }

    /**
     * Comment Texts List associated with the mediator
     */
    private List<String> commentsList = new ArrayList<String>();

    /**
     * This method is invoked when mediation happens in debug mode, branches execution to
     * the Debug Manager, further behavior is governed by the Debug Manager.
     *
     * @return false if the mediation should be continued after this method call, true if mediation
     * of current child mediator position should be skipped
     */
    public boolean divertMediationRoute(MessageContext synCtx) {
        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (isSkipEnabled()) {
                synCtx.getEnvironment().getSynapseDebugManager()
                        .advertiseMediationFlowSkip(synCtx, getRegisteredMediationFlowPoint());
                return true;
            } else if (isBreakPoint()) {
                synCtx.getEnvironment().getSynapseDebugManager()
                        .advertiseMediationFlowBreakPoint(synCtx, getRegisteredMediationFlowPoint());
            }
        }
        return false;
    }

    /**
     * Returns the class name of the mediator
     * @return the class name of the mediator
     */
    public String getType() {
        String cls = getClass().getName();
        int p = cls.lastIndexOf(".");
        if (p == -1)
            return cls;
        else
            return cls.substring(p + 1);
    }

    /**
     * Returns the tracing state
     * @return the tracing state for this mediator (see SynapseConstants)
     */
    public int getTraceState() {
        return traceState;
    }

    /**
     * Set the tracing state variable
     * @param traceState the new tracing state for this mediator (see SynapseConstants)
     */
    public void setTraceState(int traceState) {
        this.traceState = traceState;
    }

    /**
     * Set the description of the mediator
     * @param description tobe set to the mediator
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gives the description of the mediator
     * @return description of the mediator
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set the short description of the mediator
     * @param shortDescription to be set to the mediator
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     * Gives the short description of the mediator
     * @return short description of the mediator
     */
    public String getShortDescription() {
        return this.shortDescription;
    }

    /**
     * This method is used to save previous tracing state and set next the tracing
     * state for a child mediator
     *
     * @param synCtx current message
     */
    public void setEffectiveTraceState(MessageContext synCtx) {
        // if I have been explicitly asked to enable or disable tracing, use it and pass it on
        // else, do nothing -> i.e. let the parents state flow
        if (traceState != SynapseConstants.TRACING_UNSET) {
            synCtx.setTracingState(traceState);
        }
    }

    /**
     * Get a SynapseLog instance appropriate for the given context.
     * 
     * @param synCtx  the current message context
     * @return MediatorLog instance - an implementation of the SynapseLog
     */
    protected SynapseLog getLog(MessageContext synCtx) {
        return new MediatorLog(log, isTraceOn(synCtx), synCtx);
    }

    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     * @param parentTraceState parents trace state
     * @return true if tracing should be performed
     */
    public boolean shouldTrace(int parentTraceState){
        return
            (traceState == SynapseConstants.TRACING_ON) ||
            (traceState == SynapseConstants.TRACING_UNSET &&
                parentTraceState == SynapseConstants.TRACING_ON);
    }

    public boolean shouldTrace(MessageContext msgCtx){
        return isTracingEnabled() || shouldCaptureTracing(msgCtx);
    }

    /**
     * Should this mediator perform tracing? True if its explicitly asked to
     * trace, or its parent has been asked to trace and it does not reject it
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param msgCtx the current message
     * @return true if tracing should be performed
     */
    @Deprecated
    protected boolean isTraceOn(MessageContext msgCtx) {
        return isTracingEnabled() || shouldCaptureTracing(msgCtx);
//        return
//            (traceState == SynapseConstants.TRACING_ON) ||
//            (traceState == SynapseConstants.TRACING_UNSET &&
//                msgCtx.getTracingState() == SynapseConstants.TRACING_ON);
    }

    /**
     * Is tracing or debug logging on?
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param isTraceOn is tracing known to be on?
     * @return true, if either tracing or debug logging is on
     */
    @Deprecated
    protected boolean isTraceOrDebugOn(boolean isTraceOn) {
        return isTraceOn || log.isDebugEnabled();
    }

    /**
     * Perform Trace and Debug logging of a message @INFO (trace) and DEBUG (log)
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param traceOn is runtime trace on for this message?
     * @param msg the message to log/trace
     */
    @Deprecated
    protected void traceOrDebug(boolean traceOn, String msg) {
        if (traceOn) {
            trace.info(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    /**
     * Perform Trace and Debug logging of a message @WARN
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param traceOn is runtime trace on for this message?
     * @param msg the message to log/trace
     */
    @Deprecated
    protected void traceOrDebugWarn(boolean traceOn, String msg) {
        if (traceOn) {
            trace.warn(msg);
        }
        if (log.isDebugEnabled()) {
            log.warn(msg);
        }
    }

    /**
     * Perform an audit log message to all logs @ INFO. Writes to the general log, the service log
     * and the trace log (of trace is on)
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param msg the log message
     * @param msgContext the message context
     */
    @Deprecated
    protected void auditLog(String msg, MessageContext msgContext) {

        String formattedMsg = LoggingUtils.getFormattedLog(msgContext, msg);
        log.info(formattedMsg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().info(msg);
        }
        if (shouldTrace(msgContext)) {
            trace.info(formattedMsg);
        }
    }

    /**
     * Perform an error log message to all logs @ ERROR. Writes to the general log, the service log
     * and the trace log (of trace is on) and throws a SynapseException
     * @param msg the log message
     * @param msgContext the message context
     */
    protected void handleException(String msg, MessageContext msgContext) {

        String formattedLog = LoggingUtils.getFormattedLog(msgContext, msg);
        log.error(formattedLog);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        if (shouldTrace(msgContext)) {
            trace.error(formattedLog);
        }
        throw new SynapseException(msg);
    }

    /**
     * Write an audit entry at WARN and trace and standard logs @WARN
     * 
     * @deprecated This method will be removed in a future version of Synapse.
     *             Please use the {@link SynapseLog} instance returned by
     *             {@link #getLog(MessageContext)} for all logging inside a
     *             mediator.
     * 
     * @param msg the message to log
     * @param msgContext message context
     */
    @Deprecated
    protected void auditWarn(String msg, MessageContext msgContext) {

        String formattedMsg = LoggingUtils.getFormattedLog(msgContext, msg);
        log.warn(formattedMsg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
        if (shouldTrace(msgContext)) {
            trace.warn(formattedMsg);
        }
    }

    /**
     * Perform an error log message to all logs @ ERROR. Writes to the general log, the service log
     * and the trace log (of trace is on) and throws a SynapseException
     * @param msg the log message
     * @param e an Exception encountered
     * @param msgContext the message context
     */
    protected void handleException(String msg, Exception e, MessageContext msgContext) {

        String formattedLog = LoggingUtils.getFormattedLog(msgContext, msg);
        log.error(formattedLog, e);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg, e);
        }
        if (shouldTrace(msgContext)) {
            trace.error(formattedLog, e);
        }
        throw new SynapseException(msg, e);
    }

    public boolean isStatisticsEnable() {
        return this.aspectConfiguration != null
                && this.aspectConfiguration.isStatisticsEnable();
    }

    public void disableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.disableStatistics();
        }
    }

    public void enableStatistics() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.enableStatistics();
        }
    }

    public boolean isTracingEnabled() {
        return this.aspectConfiguration != null
               && this.aspectConfiguration.isTracingEnabled();
    }

    public void disableTracing() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.disableTracing();
        }
    }

    public void enableTracing() {
        if (this.aspectConfiguration != null) {
            this.aspectConfiguration.enableTracing();
        }
    }

    /**
     * Configure aspects according to the given configuration
     *
     * @param aspectConfiguration AspectConfiguration instance
     */
    public void configure(AspectConfiguration aspectConfiguration) {
       this.aspectConfiguration = aspectConfiguration;
    }

    /**
     * Get the aspects  configuration
     *
     * @return AspectConfiguration instance
     */
    public AspectConfiguration getAspectConfiguration() {
        return aspectConfiguration;
    }

    public boolean isContentAware() {
        return true;
    }

    public boolean isContentAltering() {
        return false;
    }

    public int getMediatorPosition() {
       return mediatorPosition;
    }

    public void setMediatorPosition(int position) {
       mediatorPosition = position;
    }

	public String getInputType() {
        return null;
    }

    public String getOutputType() {
        return null;
    }

    /**
     * Returns Comment List
     *
     * @return String List of comments
     */
    public List<String> getCommentsList() {
        return commentsList;
    }

    /**
     * Sets comment list for the mediator
     *
     * @param commentsList String List of comments
     */
    public void setCommentsList(List<String> commentsList) {
        this.commentsList = commentsList;
    }

    /**
     * Returns the name of the class of respective mediator. This was introduced to provide a unique way to get the
     * mediator name because getType is implemented in different ways in different mediators (e.g.
     * PayloadFactoryMediator)
     * @return
     */
    public String getMediatorName(){
        String cls = getClass().getName();
        return cls.substring(cls.lastIndexOf(".") + 1);
    }

    public Integer reportOpenStatistics(MessageContext messageContext, boolean isContentAltering) {
        if (this instanceof FlowContinuableMediator) {
            return OpenEventCollector
                    .reportFlowContinuableEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR,
                                                getAspectConfiguration(), isContentAltering() || isContentAltering);
        } else {
            return OpenEventCollector.reportChildEntryEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR,
                                                            getAspectConfiguration(),
                                                            isContentAltering() || isContentAltering);
        }
    }

    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {
        CloseEventCollector.closeEntryEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR, currentIndex,
                                            isContentAltering());
    }
    public void registerMediationFlowPoint(SynapseMediationFlowPoint flowPoint) {
        this.flowPoint = flowPoint;
    }

    public void unregisterMediationFlowPoint() {
        if (this.flowPoint != null) {
            if (!(isBreakPoint && isSkipEnabled)) {
                this.flowPoint = null;
            }
        }
    }

    public SynapseMediationFlowPoint getRegisteredMediationFlowPoint() {
        return flowPoint;
    }

    public boolean isBreakPoint() {
        return isBreakPoint;
    }

    public boolean isSkipEnabled() {
        return isSkipEnabled;
    }

    public void setBreakPoint(boolean isBreakPoint) {
        this.isBreakPoint = isBreakPoint;
    }

    public void setSkipEnabled(boolean isSkipEnabled) {
        this.isSkipEnabled = isSkipEnabled;
    }

    protected boolean shouldCaptureTracing(MessageContext synCtx) {
        Boolean isCollectingTraces = (Boolean) synCtx.getProperty(StatisticsConstants.FLOW_TRACE_IS_COLLECTED);

        if (isCollectingTraces == null) {
            return false;
        }
        else {
            return isCollectingTraces;
        }
    }

    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (aspectConfiguration == null) {
            aspectConfiguration = new AspectConfiguration(getMediatorName());
        }
        String sequenceId = StatisticIdentityGenerator.getIdForComponent(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(sequenceId);

        StatisticIdentityGenerator.reportingEndEvent(sequenceId, ComponentType.MEDIATOR, holder);
    }

    protected MediatorFaultHandler getLastSequenceFaultHandler(MessageContext synCtx) {
        Stack faultStack = synCtx.getFaultStack();
        if (faultStack != null && !faultStack.isEmpty()) {
            Object o = faultStack.peek();

            if (o instanceof MediatorFaultHandler) {
                return (MediatorFaultHandler) o;
            }
        }
        return null;
    }
}

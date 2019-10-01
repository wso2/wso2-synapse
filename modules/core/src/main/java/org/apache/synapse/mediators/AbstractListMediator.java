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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the base class for all List mediators
 *
 * @see ListMediator
 */
public abstract class AbstractListMediator extends AbstractMediator
        implements ListMediator {

    private static final String MSG_BUILD_FAILURE_EXCEPTION_PATTERN = ".*(Wstx)(.*Exception)" +
            "|.*MalformedJsonException|.*(synapse\\.commons\\.staxon\\.core)";

    // Create a Pattern object
    protected Pattern msgBuildFailureExpattern = Pattern.compile(MSG_BUILD_FAILURE_EXCEPTION_PATTERN);

    /** the list of child mediators held. These are executed sequentially */
    protected final List<Mediator> mediators = new ArrayList<Mediator>();

    private boolean sequenceContentAware = false;

    /**
     * Whether Streaming Xpath is enabled in synapse.properties file.
     */
    private static boolean isStreamXpathEnabled = SynapsePropertiesLoader.
            getBooleanProperty(SynapseConstants.STREAMING_XPATH_PROCESSING, Boolean.FALSE);

    public boolean mediate(MessageContext synCtx) {
        return  mediate(synCtx,0);
    }

    public boolean mediate(MessageContext synCtx, int mediatorPosition) {

        boolean returnVal = true;
        int parentsEffectiveTraceState = synCtx.getTracingState();
        // if I have been explicitly asked to enable or disable tracing, set it to the message
        // to pass it on; else, do nothing -> i.e. let the parents state flow
        setEffectiveTraceState(synCtx);
        int myEffectiveTraceState = synCtx.getTracingState();
        try {
            SynapseLog synLog = getLog(synCtx);
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Sequence <" + getType() + "> :: mediate()");
                synLog.traceOrDebug("Mediation started from mediator position : " + mediatorPosition);
            }

            for (int i = mediatorPosition; i < mediators.size(); i++) {
                // ensure correct trace state after each invocation of a mediator
                Mediator mediator = mediators.get(i);

                if (sequenceContentAware && (mediator.isContentAware() || isStreamXpathEnabled) &&
                        (!Boolean.TRUE.equals(synCtx.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)))) {
                    buildMessage(synCtx, synLog);
                }

                if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                    Integer statisticReportingIndex = mediator.reportOpenStatistics(synCtx, i == mediatorPosition);
                    synCtx.setTracingState(myEffectiveTraceState);
                    if (!mediator.mediate(synCtx)) {
                        mediator.reportCloseStatistics(synCtx, statisticReportingIndex);
                        returnVal = false;
                        break;
                    }
                    mediator.reportCloseStatistics(synCtx, statisticReportingIndex);
                } else {
                    synCtx.setTracingState(myEffectiveTraceState);
                    if (!mediator.mediate(synCtx)) {
                        returnVal = false;
                        break;
                    }
                }
            }
        } catch (SynapseException synEx) {
            // Now create matcher object.
            Matcher msgBuildFailureExMatcher = msgBuildFailureExpattern.matcher(ExceptionUtils.getStackTrace(synEx));
            if (msgBuildFailureExMatcher.find()) {
                // Setting error details for parsing failures
                synCtx.setProperty(SynapseConstants.ERROR_CODE, SynapseConstants.MESSAGE_PARSING_ERROR);
                synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, synEx.getMessage().split("\n")[0]);
                synCtx.setProperty(SynapseConstants.ERROR_DETAIL, ExceptionUtils.getStackTrace(synEx));
                synCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, synEx.toString());
                
                consumeInputOnOmException(synCtx);
            }
            throw synEx;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();

            // Now create matcher object.
            Matcher msgBuildFailureExMatcher = msgBuildFailureExpattern.matcher(ExceptionUtils.getStackTrace(ex));
            if (errorMsg == null) {
                errorMsg = "Runtime error occurred while mediating the message";
            }
            if (msgBuildFailureExMatcher.find()) {
                consumeInputOnOmException(synCtx);
            }
            handleException(errorMsg, ex, synCtx);
        } finally {
            synCtx.setTracingState(parentsEffectiveTraceState);
        }
        return returnVal;
    }

    private void buildMessage(MessageContext synCtx, SynapseLog synLog) {

        try {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Building message. Sequence <" + getType() + "> is content aware");
            }
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
        } catch (Exception e) {
            handleException("Error while building message. " + e.getMessage(), e, synCtx);
        }
    }

    public List<Mediator> getList() {
        return mediators;
    }

    public boolean addChild(Mediator m) {
        return mediators.add(m);
    }

    public void addChild(int index, Mediator m) {
        mediators.add(index, m);
    }

    public boolean addAll(List<Mediator> c) {
        return mediators.addAll(c);
    }

    public Mediator getChild(int pos) {
        return mediators.get(pos);
    }

    public boolean removeChild(Mediator m) {
        return mediators.remove(m);
    }

    public Mediator removeChild(int pos) {
        return mediators.remove(pos);
    }

    /**
     * Initialize child mediators recursively
     * @param se synapse environment
     */
    public void init(SynapseEnvironment se) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing child mediators of mediator : " + getType());
        }

        for (int i = 0; i < mediators.size(); i++) {
            Mediator mediator = mediators.get(i);
            mediator.setMediatorPosition(i);

            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).init(se);
            }

            if (mediator.isContentAware()) {
                if (log.isDebugEnabled()) {
                    log.debug(mediator.getType() + " is content aware, setting sequence <" + getType() + "> as content aware");
                }
                sequenceContentAware = true;
            }
        }
    }

    /**
     * Destroy child mediators recursively
     */
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying child mediators of mediator : " + getType());
        }

        for (Mediator mediator : mediators) {
            if (mediator instanceof ManagedLifecycle) {
                ((ManagedLifecycle) mediator).destroy();
            }
        }
    }

    @Override
    public boolean isContentAware() {
        return sequenceContentAware;
    }

    public void setStatisticIdForMediators(ArtifactHolder holder){
        for (Mediator mediator : mediators) {
            mediator.setComponentStatisticsId(holder);
        }
    }


    /**
     * This method will read the entire content from the input stream of the request if there is a parsing error.
     *
     * @param synCtx Synapse message context.
     */
    private void consumeInputOnOmException(MessageContext synCtx) {
        try {
            RelayUtils.consumeAndDiscardMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        } catch (AxisFault axisFault) {
            log.error("Exception while consuming the input stream on Om Exception", axisFault);
        }
        SOAPEnvelope soapEnvelope;
        if (synCtx.isSOAP11()) {
            soapEnvelope = OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope();
            soapEnvelope.addChild(OMAbstractFactory.getSOAP11Factory().createSOAPBody());
        } else {
            soapEnvelope = OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope();
            soapEnvelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        }
        try {
            synCtx.setEnvelope(soapEnvelope);
        } catch (AxisFault e) {
            log.error("Exception or Error occurred resetting SOAP Envelope", e);
        }
    }
}

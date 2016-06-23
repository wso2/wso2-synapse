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
import org.apache.axiom.om.OMException;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.util.RelayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for all List mediators
 *
 * @see ListMediator
 */
public abstract class AbstractListMediator extends AbstractMediator
        implements ListMediator {

    /** the list of child mediators held. These are executed sequentially */
    protected final List<Mediator> mediators = new ArrayList<Mediator>();

    private boolean contentAware = false;

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

            if (isContentAware(mediatorPosition)) {
                try {
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Building message. Sequence <" + getType() + "> is content aware");
                    }
                    RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext(), false);
                } catch (Exception e) {
                    handleException("Error while building message", e, synCtx);
                }
            }

            for (int i = mediatorPosition; i < mediators.size(); i++) {
                // ensure correct trace state after each invocation of a mediator
                Mediator mediator = mediators.get(i);
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
            if (synEx.getCause() instanceof OMException) {
                consumeInputOnOmException(synCtx);
            }
            throw synEx;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            if (errorMsg == null) {
                errorMsg = "Runtime error occurred while mediating the message";
            }
            if (ex instanceof OMException || ex.getCause() instanceof OMException) {
                consumeInputOnOmException(synCtx);
            }
            handleException(errorMsg, ex, synCtx);
        } finally {
            synCtx.setTracingState(parentsEffectiveTraceState);
        }
        return returnVal;
    }

    public List<Mediator> getList() {
        return mediators;
    }

    public boolean addChild(Mediator m) {
        return mediators.add(m);
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
                contentAware = true;
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
        return contentAware;
    }

    private boolean isContentAware(int position) {
        // For first mediator, we can take it from variable initialized at init()
        if (position == 0) {
           return contentAware;
        }

        for (int i = position; i < mediators.size(); i++) {
            Mediator mediator = mediators.get(i);
            if (mediator.isContentAware()) {
                return true;
            }
        }
        return false;
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

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

package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.analytics.AnalyticsPublisher;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.transport.http.conn.SynapseDebugInfoHolder;
import org.apache.synapse.transport.http.conn.SynapseWireLogHolder;
import org.apache.synapse.util.logging.LoggingUtils;

import java.util.Iterator;
import java.util.List;

/**
 * This is the MessageReceiver set to act on behalf of Proxy services.
 */
public class ProxyServiceMessageReceiver extends SynapseMessageReceiver {

    private static final Log log = LogFactory.getLog(ProxyServiceMessageReceiver.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    /** The name of the Proxy Service */
    private String name = null;
    /** The proxy service */
    private ProxyService proxy = null;

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        boolean traceOn = proxy.getAspectConfiguration().isTracingEnabled();
        boolean traceOrDebugOn = traceOn || log.isDebugEnabled();

        CustomLogSetter.getInstance().setLogAppender(proxy.getArtifactContainerName());

        String remoteAddr = (String) mc.getProperty(
            org.apache.axis2.context.MessageContext.REMOTE_ADDR);

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Proxy Service " + name + " received a new message" +
                (remoteAddr != null ? " from : " + remoteAddr : "..."));
            traceOrDebug(traceOn, ("Message To: " +
                (mc.getTo() != null ? mc.getTo().getAddress() : "null")));
            traceOrDebug(traceOn, ("SOAPAction: " +
                (mc.getSoapAction() != null ? mc.getSoapAction() : "null")));
            traceOrDebug(traceOn, ("WSA-Action: " +
                (mc.getWSAAction() != null ? mc.getWSAAction() : "null")));

            if (traceOn && trace.isTraceEnabled()) {
                String[] cids = null;
                try {
                    cids = mc.getAttachmentMap().getAllContentIDs();
                } catch (Exception ex){
                    //partially read stream could lead to corrupted attachment map and hence this exception
                    //corrupted attachment map leads to inconsistent runtime exceptions and behavior
                    //discard the attachment map for the fault handler invocation
                    //ensure the successful completion for fault handler flow
                    mc.setAttachmentMap(null);
                    log.error("Synapse encountered an exception when reading attachments from bytes stream. " +
                            "Hence Attachments map is dropped from the message context.", ex);
                }
                if (cids != null && cids.length > 0) {
                    for (String cid : cids) {
                        trace.trace("With attachment content ID : " + cid);
                    }
                }
                trace.trace("Envelope : " + mc.getEnvelope());
            }
        }

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);
        synCtx.recordLatency();
        Integer statisticReportingIndex = null;
        //Statistic reporting
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        if (isStatisticsEnabled) {
            statisticReportingIndex = OpenEventCollector.reportEntryEvent(synCtx, this.name,
                    proxy.getAspectConfiguration(), ComponentType.PROXYSERVICE);
        }

        Object inboundServiceParam =
                proxy.getParameterMap().get(SynapseConstants.INBOUND_PROXY_SERVICE_PARAM);
        Object inboundMsgCtxParam = mc.getProperty(SynapseConstants.IS_INBOUND);

        //check whether the message is from Inbound EP
        if (inboundMsgCtxParam == null || !(boolean) inboundMsgCtxParam) {
            //check whether service parameter is set to true or null, then block this request
            if (inboundServiceParam != null && (Boolean.valueOf((String) inboundServiceParam))) {
                /*
                return because same proxy is exposed via InboundEP and service parameter(inbound.only) is set to
                true, which disable normal http transport proxy
                */
                if (!synCtx.getFaultStack().isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Executing fault handler - message discarded due to the proxy is allowed only via InboundEP");
                    }
                    (synCtx.getFaultStack().pop()).
                            handleFault(synCtx, new Exception("Proxy Service " + name +
                                                              " message discarded due to the proxy is allowed only via InboundEP"));
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Proxy Service " + name + " message discarded due to the proxy is " +
                                  "allowed only via InboundEP");
                    }
                }
                return;
            }
        }

        TenantInfoConfigurator configurator = synCtx.getEnvironment().getTenantInfoConfigurator();
        if (configurator != null) {
            configurator.extractTenantInfo(synCtx);
        }
        TransportInDescription trpInDesc = mc.getTransportIn();
        if (trpInDesc != null) {
            synCtx.setProperty(SynapseConstants.TRANSPORT_IN_NAME, trpInDesc.getName());
        }

        // get service log for this message and attach to the message context also set proxy name
        Log serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX + name);
        ((Axis2MessageContext) synCtx).setServiceLog(serviceLog);

        synCtx.setProperty(SynapseConstants.PROXY_SERVICE, name);
        synCtx.setProperty(SynapseConstants.ARTIFACT_NAME, SynapseConstants.PROXY_SERVICE_TYPE + name);
//        synCtx.setTracingState(proxy.getTraceState());

        synCtx.setProperty(SynapseConstants.IS_CLIENT_DOING_REST, mc.isDoingREST());
        synCtx.setProperty(SynapseConstants.IS_CLIENT_DOING_SOAP11, mc.isSOAP11());
        synCtx.setProperty(CorrelationConstants.CORRELATION_ID, mc.getProperty(CorrelationConstants.CORRELATION_ID));

        try {
            if(synCtx.getEnvironment().isDebuggerEnabled()) {
                SynapseDebugManager debugManager = synCtx.getEnvironment().getSynapseDebugManager();
                debugManager.acquireMediationFlowLock();
                debugManager.advertiseMediationFlowStartPoint(synCtx);
                if (!synCtx.isResponse()) {
                    SynapseWireLogHolder wireLogHolder = (SynapseWireLogHolder) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                            .getProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
                    if (wireLogHolder == null) {
                        wireLogHolder = new SynapseWireLogHolder();
                    }
                    if (synCtx.getProperty(SynapseConstants.PROXY_SERVICE) != null && !synCtx.getProperty(SynapseConstants.PROXY_SERVICE).toString().isEmpty()) {
                        wireLogHolder.setProxyName(synCtx.getProperty(SynapseConstants.PROXY_SERVICE).toString());
                    }
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, wireLogHolder);
                }
            }
            synCtx.setProperty(SynapseConstants.RESPONSE_STATE, new ResponseState());
            List handlers = synCtx.getEnvironment().getSynapseHandlers();
            Iterator<SynapseHandler> iterator = handlers.iterator();
            while (iterator.hasNext()) {
                SynapseHandler handler = iterator.next();
                if (!handler.handleRequestInFlow(synCtx)) {
                    return;
                }
            }

            Mediator mandatorySeq = synCtx.getConfiguration().getMandatorySequence();
            if (mandatorySeq != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Start mediating the message in the " +
                        "pre-mediate state using the mandatory sequence");
                }

                if(!mandatorySeq.mediate(synCtx)) {
                    if(log.isDebugEnabled()) {
                        log.debug("Request message for the proxy service " + name + " dropped in " +
                                "the pre-mediation state by the mandatory sequence : \n" + synCtx);
                    }
                    return;
                }
            }

            // setup fault sequence - i.e. what happens when something goes wrong with this message
            proxy.registerFaultHandler(synCtx);

            boolean inSequenceResult = true;

            // Using inSequence for the incoming message mediation
            if (proxy.getTargetInSequence() != null) {

                Mediator inSequence = synCtx.getSequence(proxy.getTargetInSequence());
                if (inSequence != null) {
                    traceOrDebug(traceOn, "Using sequence named : "
                        + proxy.getTargetInSequence() + " for incoming message mediation");
                    inSequenceResult = inSequence.mediate(synCtx);

                } else {
                    handleException("Unable to find in-sequence : "
                            + proxy.getTargetInSequence(), synCtx);
                }

            } else if (proxy.getTargetInLineInSequence() != null) {
                traceOrDebug(traceOn, "Using the anonymous " +
                    "in-sequence of the proxy service for mediation");
                inSequenceResult = proxy.getTargetInLineInSequence().mediate(synCtx);
            }

            // if inSequence returns true, forward message to endpoint
            if(inSequenceResult) {
                if (proxy.getTargetEndpoint() != null) {
                    Endpoint endpoint = synCtx.getEndpoint(proxy.getTargetEndpoint());

                    if (endpoint != null) {
                        traceOrDebug(traceOn, "Forwarding message to the endpoint : "
                            + proxy.getTargetEndpoint());
                        endpoint.send(synCtx);

                    } else {
                        handleException("Unable to find the endpoint specified : " +
                            proxy.getTargetEndpoint(), synCtx);
                    }

                } else if (proxy.getTargetInLineEndpoint() != null) {
                    traceOrDebug(traceOn, "Forwarding the message to the anonymous " +
                        "endpoint of the proxy service");
                    proxy.getTargetInLineEndpoint().send(synCtx);
                }
            }

        } catch (SynapseException syne) {

            if (!synCtx.getFaultStack().isEmpty()) {
                warn(traceOn, "Executing fault handler due to exception encountered", synCtx);
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, syne);

            } else {
                warn(traceOn, "Exception encountered but no fault handler found - " +
                    "message dropped", synCtx);
            }
        } finally {
            //Statistic reporting
            if (isStatisticsEnabled) {
                CloseEventCollector.tryEndFlow(synCtx, this.name, ComponentType.PROXYSERVICE,
                        statisticReportingIndex, true);
            }
            if(synCtx.getEnvironment().isDebuggerEnabled()) {
                SynapseDebugManager debugManager = synCtx.getEnvironment().getSynapseDebugManager();
                debugManager.advertiseMediationFlowTerminatePoint(synCtx);
                debugManager.releaseMediationFlowLock();
            }
            doPostInjectUpdates(synCtx);
            AnalyticsPublisher.publishProxyServiceAnalytics(synCtx, this.proxy);
        }
    }

    /**
     * Set the name of the corresponding proxy service
     *
     * @param name the proxy service name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set reference to actual proxy service
     * @param proxy  ProxyService instance
     */
    public void setProxy(ProxyService proxy) {
        this.proxy = proxy;
    }

    private void traceOrDebug(boolean traceOn, String msg) {

        String formattedLog = LoggingUtils.getFormattedLog(SynapseConstants.PROXY_SERVICE_TYPE, name, msg);
        if (traceOn) {
            trace.info(formattedLog);
        }
        log.debug(formattedLog);
    }

    private void warn(boolean traceOn, String msg, MessageContext msgContext) {

        String formattedLog = LoggingUtils.getFormattedLog(SynapseConstants.PROXY_SERVICE_TYPE, name, msg);
        if (traceOn) {
            trace.warn(formattedLog);
        }
        if (log.isDebugEnabled()) {
            log.warn(formattedLog);
        }
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
    }

    private void handleException(String msg, MessageContext msgContext) {

        String formattedLog = LoggingUtils.getFormattedLog(SynapseConstants.PROXY_SERVICE_TYPE, name, msg);
        log.error(formattedLog);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        if (proxy.getAspectConfiguration().isTracingEnabled()) {
            trace.error(formattedLog);
        }
        throw new SynapseException(msg);
    }
}

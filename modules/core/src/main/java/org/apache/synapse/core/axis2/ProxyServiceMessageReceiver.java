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
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.statistics.StatisticsReporter;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.endpoints.Endpoint;

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

        boolean traceOn = proxy.getTraceState() == SynapseConstants.TRACING_ON;
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
                String[] cids = mc.getAttachmentMap().getAllContentIDs();
                if (cids != null && cids.length > 0) {
                    for (String cid : cids) {
                        trace.trace("With attachment content ID : " + cid);
                    }
                }
                trace.trace("Envelope : " + mc.getEnvelope());
            }
        }

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);

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

        StatisticsReporter.reportForComponent(synCtx,
                proxy.getAspectConfiguration(),
                ComponentType.PROXYSERVICE);
        
        // get service log for this message and attach to the message context also set proxy name
        Log serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX + name);
        ((Axis2MessageContext) synCtx).setServiceLog(serviceLog);

        synCtx.setProperty(SynapseConstants.PROXY_SERVICE, name);
        synCtx.setTracingState(proxy.getTraceState());

        try {

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
            StatisticsReporter.endReportForAllOnRequestProcessed(synCtx);
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
        if (traceOn) {
            trace.info(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    private void warn(boolean traceOn, String msg, MessageContext msgContext) {
        if (traceOn) {
            trace.warn(msg);
        }
        if (log.isDebugEnabled()) {
            log.warn(msg);
        }
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().warn(msg);
        }
    }

    private void handleException(String msg, MessageContext msgContext) {
        log.error(msg);
        if (msgContext.getServiceLog() != null) {
            msgContext.getServiceLog().error(msg);
        }
        if (proxy.getTraceState() == SynapseConstants.TRACING_ON) {
            trace.error(msg);
        }
        throw new SynapseException(msg);
    }

}

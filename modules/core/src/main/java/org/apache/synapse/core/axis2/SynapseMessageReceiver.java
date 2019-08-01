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
import org.apache.axis2.engine.MessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.carbonext.TenantInfoConfigurator;
import org.apache.synapse.transport.nhttp.NhttpConstants;

/**
 * This message receiver should be configured in the Axis2 configuration as the
 * default message receiver, which will handle all incoming messages through the
 * synapse mediation
 */
public class SynapseMessageReceiver implements MessageReceiver {

    private static final Log log = LogFactory.getLog(SynapseMessageReceiver.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);

    public void receive(org.apache.axis2.context.MessageContext mc) throws AxisFault {

        MessageContext synCtx = MessageContextCreatorForAxis2.getSynapseMessageContext(mc);

        boolean traceOn = synCtx.getMainSequence().getTraceState() == SynapseConstants.TRACING_ON;
        boolean traceOrDebugOn = traceOn || log.isDebugEnabled();

        if (traceOrDebugOn) {
            traceOrDebug(traceOn, "Synapse received a new message for message mediation...");
            traceOrDebug(traceOn, "Received To: " +
                (mc.getTo() != null ? mc.getTo().getAddress() : "null"));
            traceOrDebug(traceOn, "SOAPAction: " +
                (mc.getSoapAction() != null ? mc.getSoapAction() : "null"));
            traceOrDebug(traceOn, "WSA-Action: " +
                (mc.getWSAAction() != null ? mc.getWSAAction() : "null"));

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
                        trace.trace("Attachment : " + cid);
                    }
                }
                trace.trace("Envelope : " + mc.getEnvelope());
            }
        }

        // get service log for this message and attach to the message context
        Log serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX +
            SynapseConstants.SYNAPSE_SERVICE_NAME);
        ((Axis2MessageContext) synCtx).setServiceLog(serviceLog);

        synCtx.setProperty(SynapseConstants.IS_CLIENT_DOING_REST, mc.isDoingREST());
        synCtx.setProperty(SynapseConstants.IS_CLIENT_DOING_SOAP11, mc.isSOAP11());

        TenantInfoConfigurator configurator = synCtx.getEnvironment().getTenantInfoConfigurator();
        if (configurator != null) {
            configurator.extractTenantInfo(synCtx);
        }

        try {
            // set response state for the request incoming via main sequence or API
            synCtx.setProperty(SynapseConstants.RESPONSE_STATE, new ResponseState());
            // invoke synapse message mediation through the APIs/main sequence
            synCtx.getEnvironment().injectMessage(synCtx);

        } catch (SynapseException syne) {

            if (!synCtx.getFaultStack().isEmpty()) {
                warn(traceOn, "Executing fault handler due to exception encountered", synCtx);
                ((FaultHandler) synCtx.getFaultStack().pop()).handleFault(synCtx, syne);

            } else {
                warn(traceOn, "Exception encountered but no fault handler found - " +
                    "message dropped", synCtx);
            }
        } finally {
            doPostInjectUpdates(synCtx);
        }
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

    /**
     * Do the cleanup work, metadata updates in the return path of Message Receiver tread
     * after injecting the message to the engine
     *
     * @param messageContext Synapse Message Context
     */
    public static void doPostInjectUpdates(MessageContext messageContext) {

        org.apache.axis2.context.MessageContext axis2Ctx = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        // If FORCE_SC_ACCEPTED is set, mark response is sent and prevent multiple response being sent
        if (axis2Ctx.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED) && Axis2Sender
                .preventMultipleResponses(messageContext)) {
            throw new SynapseException("Trying to send a 202 Accepted response to an already responded client request");
        }
    }

}

/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.message.senders.blocking;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.TransportSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;

import java.util.Stack;

/**
 * A {@link BlockingMsgSender} that delegates the actual HTTP call to the VT
 * transport's {@link TransportSender} (at runtime: {@code VTHttpSender}),
 * reusing its shared connection pool.
 *
 * <h3>How it prevents async callback registration</h3>
 * <p>{@code Axis2FlexibleMEPClient.send()} checks for
 * {@link SynapseConstants#BLOCKING_MSG_SENDER} before setting up any MEP client
 * or registering an async callback. When that property is set to this sender,
 * {@code Axis2FlexibleMEPClient} calls {@link #send} and returns immediately —
 * the callback / continuation-stack machinery is never reached.</p>
 *
 * <h3>Response path</h3>
 * <p>Before delegating to {@link TransportSender#invoke}, this class sets the
 * {@code VT_BLOCKING_CALL} flag on the Axis2 message context. That flag tells
 * {@code VTHttpSender.sendToBackend()} to call
 * {@code populateResponseOnContext()} instead of handing the response to
 * {@code VTHttpSender} / {@code AxisEngine.receive()}.
 * The response (envelope, headers, status) is placed directly on the original
 * message context — same thread, no callbacks.</p>
 *
 * <p>Instantiated by {@code VTCallMediator.init()} by looking up the
 * {@code "vt-http"} transport sender from the Axis2 configuration.</p>
 */
public class VTBlockingMsgSender extends BlockingMsgSender {

    private static final Log log = LogFactory.getLog(VTBlockingMsgSender.class);

    /**
     * MessageContext property key used to signal the VT transport sender to
     * populate the response directly on the original context instead of
     * dispatching through {@code AxisEngine.receive()}.
     * Must match {@code VTConstants.VT_BLOCKING_CALL} in the transport module.
     */
    private static final String VT_BLOCKING_CALL = "VT_BLOCKING_CALL";

    /**
     * Axis2 MessageContext property key for the HTTP status code.
     * Matches {@code PassThroughConstants.HTTP_SC} / {@code NhttpConstants.HTTP_SC}.
     */
    private static final String HTTP_SC = "HTTP_SC";

    /** The VT transport sender whose invoke() / sendToBackend() we reuse. */
    private final TransportSender vtSender;

    public VTBlockingMsgSender(TransportSender vtSender) {
        this.vtSender = vtSender;
    }

    /**
     * Sends the outbound request via the VT transport sender and populates the
     * response back on {@code synapseInMsgCtx}.
     *
     * <p>Called by {@code Axis2FlexibleMEPClient} when
     * {@link SynapseConstants#BLOCKING_MSG_SENDER} is set. The MEP client
     * returns immediately after — no async callback is registered.</p>
     */
    @Override
    public void send(EndpointDefinition endpointDefinition,
                     org.apache.synapse.MessageContext synapseInMsgCtx) throws AxisFault {

        MessageContext axis2MsgCtx =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();

        try {
            // ---- Resolve target URL onto the axis2 context ----
            // VTHttpSender.invoke() reads the EPR via
            // PassThroughTransportUtils.getDestinationEPR(msgContext).
            String url = null;
            if (endpointDefinition != null
                    && endpointDefinition.getAddress() != null) {
                url = endpointDefinition.getAddress(synapseInMsgCtx);
            }
            if (url == null && axis2MsgCtx.getTo() != null) {
                url = axis2MsgCtx.getTo().getAddress();
            }
            if (url != null) {
                axis2MsgCtx.setTo(new EndpointReference(url));
            }

            // ---- Signal the VT sender to use the blocking response path ----
            // sendToBackend() checks this flag and calls populateResponseOnContext()
            // instead of VTHttpSender → AxisEngine.receive().
            axis2MsgCtx.setProperty(VT_BLOCKING_CALL, Boolean.TRUE);

            // ---- Invoke VT sender — fully blocking on the same Virtual Thread ----
            vtSender.invoke(axis2MsgCtx);

            // ---- Map HTTP status to BLOCKING_SENDER_ERROR ----
            // populateResponseOnContext() stored HTTP_SC on the axis2 context;
            // VTCallMediator reads SynapseConstants.BLOCKING_SENDER_ERROR.
            Object httpSC = axis2MsgCtx.getProperty(HTTP_SC);
            int status = httpSC instanceof Integer ? (Integer) httpSC : 200;

            if (status >= 400) {
                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "true");
                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_CODE,
                        SynapseConstants.BLOCKING_SENDER_OPERATION_FAILED);
                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE,
                        "Backend returned HTTP " + status);
            } else {
                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "false");
            }

        } catch (Exception e) {
            axis2MsgCtx.setProperty(SynapseConstants.HTTP_SC, "");
            synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "true");
            synapseInMsgCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
            synapseInMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE, e.getMessage());
            log.error("Error during VT blocking call", e);
        } finally {
            // Always clean up the flag so it cannot leak into subsequent calls
            // on a keep-alive connection that reuses the same context.
            axis2MsgCtx.removeProperty(VT_BLOCKING_CALL);
        }

        // ---- Fault-stack handling (mirrors BlockingMsgSender pattern) ----
        if ("true".equals(
                synapseInMsgCtx.getProperty(SynapseConstants.BLOCKING_SENDER_ERROR))) {
            Stack faultStack = synapseInMsgCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                synapseInMsgCtx.getFaultStack().pop().handleFault(
                        synapseInMsgCtx,
                        (Exception) synapseInMsgCtx
                                .getProperty(SynapseConstants.ERROR_EXCEPTION));
            }
        } else {
            Stack faultStack = synapseInMsgCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()
                    && faultStack.peek() instanceof AbstractEndpoint) {
                ((AbstractEndpoint) faultStack.pop()).onSuccess();
            }
        }
    }
}

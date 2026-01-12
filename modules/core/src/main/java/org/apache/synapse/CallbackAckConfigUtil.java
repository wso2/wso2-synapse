/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.config.mapper.ConfigParser;

import java.util.Map;

/**
 * Utility class for reading RabbitMQ/Synapse callback-acknowledgement related configuration.
 * <p>
 * Configurations are parsed once at startup from deployment.toml (via ConfigParser).
 * Provides getters for:
 * <ul>
 *     <li>{@link #isCallbackControlledAckEnabled()}</li>
 *     <li>{@link #isClientApiNonBlockingModeEnabled()}</li>
 *     <li>{@link #isPreservePayloadOnTimeout()}</li>
 * </ul>
 * These values control how RabbitMQ inbound flows behave in callback-controlled acknowledgement mode.
 */
public final class CallbackAckConfigUtil {

    private static final Log log = LogFactory.getLog(CallbackAckConfigUtil.class);

    private static final boolean callbackControlledAckEnabled;
    private static final boolean clientApiNonBlockingModeEnabled;
    private static final boolean preservePayloadOnTimeout;

    static {
        boolean tmpCallbackAck = false;
        boolean tmpClientApiNonBlocking = false;
        boolean tmpPreservePayload = false;

        Map<String, Object> configs = ConfigParser.getParsedConfigs();

        // Callback Controlled Ack
        Object callBackControlledAckEnabled = null;
        if (configs != null) {
            callBackControlledAckEnabled = configs.get(SynapseConstants.MESSAGING_CALLBACK_CONFIGS + "." +
                    SynapseConstants.CALLBACK_CONTROLLED_ACK);
        }
        if (callBackControlledAckEnabled != null) {
            try {
                tmpCallbackAck = Boolean.parseBoolean(callBackControlledAckEnabled.toString());
                log.info("Callback Controlled Ack is set to: " + tmpCallbackAck);
            } catch (Exception e) {
                log.warn("Invalid value for " + SynapseConstants.CALLBACK_CONTROLLED_ACK +
                        ". Should be true or false. Defaulting to false", e);
                tmpCallbackAck = false;
            }
        }

        // Client API Non-Blocking Mode
        Object clientApiNonBlockingEnabled = null;
        if (configs != null) {
            clientApiNonBlockingEnabled = configs.get(SynapseConstants.MESSAGING_CALLBACK_CONFIGS + "." +
                    SynapseConstants.ENABLE_CLIENT_API_NON_BLOCKING_MODE);
        }
        if (clientApiNonBlockingEnabled != null) {
            try {
                tmpClientApiNonBlocking = Boolean.parseBoolean(clientApiNonBlockingEnabled.toString());
                log.info("Client API Non Blocking Mode is set to: " + tmpClientApiNonBlocking);
            } catch (Exception e) {
                log.warn("Invalid value for " + SynapseConstants.ENABLE_CLIENT_API_NON_BLOCKING_MODE +
                        ". Should be true or false. Defaulting to false", e);
                tmpClientApiNonBlocking = false;
            }
        }

        // Preserve Payload on Timeout (only meaningful if callback ack enabled)
        if (tmpCallbackAck) {
            Object preservePayload =
                    configs.get(SynapseConstants.MESSAGING_CALLBACK_CONFIGS + "." +
                            SynapseConstants.PRESERVE_PAYLOAD_ON_TIMEOUT);
            if (preservePayload != null) {
                try {
                    tmpPreservePayload = Boolean.parseBoolean(preservePayload.toString());
                    log.info("Preserve payload on timeout is set to: " + tmpPreservePayload);
                } catch (Exception e) {
                    log.warn("Invalid value for " + SynapseConstants.PRESERVE_PAYLOAD_ON_TIMEOUT +
                            ". Should be true or false. Defaulting to false", e);
                    tmpPreservePayload = false;
                }
            }
        }

        callbackControlledAckEnabled = tmpCallbackAck;
        clientApiNonBlockingModeEnabled = tmpClientApiNonBlocking;
        preservePayloadOnTimeout = tmpPreservePayload;
    }

    private CallbackAckConfigUtil() {
        // Prevent instantiation
    }

    /**
     * @return true if callback-controlled acknowledgement is enabled.
     *         When enabled, the mediation flow decides when to ack/nack messages.
     */
    public static boolean isCallbackControlledAckEnabled() {
        return callbackControlledAckEnabled;
    }

    /**
     * @return true if Client API Non-Blocking Mode is enabled.
     *         This controls whether responses to callback-based APIs run in non-blocking mode.
     */
    public static boolean isClientApiNonBlockingModeEnabled() {
        return clientApiNonBlockingModeEnabled;
    }

    /**
     * @return true if message payloads should be preserved when mediation
     *         times out while in callback-controlled acknowledgement mode.
     */
    public static boolean isPreservePayloadOnTimeout() {
        return preservePayloadOnTimeout;
    }

    /**
     * Check whether the given Axis2 message context is using RabbitMQ transport.
     *
     * @param axis2OutMsgCtx Axis2 message context
     * @return true if transportOut corresponds to RabbitMQ
     */
    public static boolean isRabbitMQTransport(MessageContext axis2OutMsgCtx) {
        TransportOutDescription transportOut = axis2OutMsgCtx.getTransportOut();
        if (transportOut != null && transportOut.getSender() != null) {
            String name = transportOut.getName() != null ? transportOut.getName().toLowerCase() : "";
            return "rabbitmq".equals(name) || name.contains("rabbitmq");
        }
        return false;
    }
}

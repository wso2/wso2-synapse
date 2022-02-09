/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.util;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.netty.BridgeConstants;

import java.util.Objects;

/**
 * Provider class for the {@code MessageHandler}.
 */
public class MessageHandlerProvider {

    /**
     * Provides the suitable MessageHandler class based on the underline transport implementation, which invokes
     * this method.
     *
     * @param messageContext axis2 message context
     * @return instance of a {@code MessageHandler} that is assigned to "transport.message.handler" axis2 property.
     * Default is {@code PassThroughMessageHandler}.
     */
    public static MessageHandler getMessageHandler(MessageContext messageContext) {

        MessageHandler messageHandler =
                (MessageHandler) messageContext.getProperty(BridgeConstants.TRANSPORT_MESSAGE_HANDLER);
        return Objects.nonNull(messageHandler) ? messageHandler : new PassThroughMessageHandler();

    }

}

/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.transport.passthru.util;


import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;

public class WSAddressingHandler extends AbstractHandler {

    private static final Log log = LogFactory.getLog(WSAddressingHandler.class);

    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {

        Pipe pipe = (Pipe) messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null) {
            if (messageContext.getAxisService() != null) {
                if (messageContext.getAxisService().getParameter(
                           PassThroughConstants.ENABLE_WS_ADDRESSING) != null &&
                    Boolean.parseBoolean((String) messageContext.getAxisService().
                               getParameter(PassThroughConstants.ENABLE_WS_ADDRESSING).getValue())) {
                    build(messageContext);
                }
            } else {
                log.error("Axis Service is null");
            }
        }
        return InvocationResponse.CONTINUE;
    }


    private void build(MessageContext messageContext) {
        try {
            RelayUtils.buildMessage(messageContext, false);
        } catch (Exception e) {
            log.error("Error while executing ws addressing handler", e);
        }
    }
}

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
package org.apache.synapse.transport.netty.sender;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.ErrorCodes;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * {@code Axis2HttpTargetRespListener} listens for the response expected for the sent request.
 */
public class Axis2HttpTargetRespListener implements HttpConnectorListener {

    private static final Log LOG = LogFactory.getLog(Axis2HttpTargetRespListener.class);

    private final MessageContext requestMsgCtx;
    private final WorkerPool workerPool;
    private final TargetConfiguration targetConfiguration;
    private final TargetErrorHandler errorHandler;

    public Axis2HttpTargetRespListener(WorkerPool workerPool, MessageContext requestMsgContext,
                                       TargetConfiguration targetConfiguration) {

        this.workerPool = workerPool;
        this.requestMsgCtx = requestMsgContext;
        this.targetConfiguration = targetConfiguration;
        this.errorHandler = new TargetErrorHandler(targetConfiguration);
    }

    @Override
    public void onMessage(HttpCarbonMessage httpResponse) {

        LOG.debug(BridgeConstants.BRIDGE_LOG_PREFIX + "Response received");
        workerPool.execute(new HttpTargetResponseWorker(requestMsgCtx, httpResponse, targetConfiguration));
    }

    @Override
    public void onError(Throwable throwable) {

        LOG.error("Error while sending the request to the backend service or "
                + "processing the response from backend service", throwable);
        if (requestMsgCtx != null) {
            requestMsgCtx.setProperty(BridgeConstants.INTERNAL_EXCEPTION_ORIGIN,
                    BridgeConstants.INTERNAL_ORIGIN_ERROR_HANDLER);
            errorHandler.handleError(requestMsgCtx, ErrorCodes.SND_IO_ERROR, "Error in Sender", throwable);
        }
    }

}

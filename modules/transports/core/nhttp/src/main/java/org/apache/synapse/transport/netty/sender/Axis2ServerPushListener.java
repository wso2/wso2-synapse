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
 */

package org.apache.synapse.transport.netty.sender;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.wso2.transport.http.netty.contract.HttpClientConnectorListener;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.message.Http2PushPromise;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * {@code Axis2ServerPushListener} is the class which listens for HTTP/2 server pushes from the backend.
 */
public class Axis2ServerPushListener implements HttpClientConnectorListener {

    private static final Log LOG = LogFactory.getLog(Axis2ServerPushListener.class);

    private final HttpResponseFuture future;
    private final MessageContext requestMsgCtx;
    private final WorkerPool workerPool;
    private Http2PushPromise pushPromise;

    public Axis2ServerPushListener(HttpResponseFuture future, MessageContext requestMsgCtx, WorkerPool workerPool) {

        this.future = future;
        this.requestMsgCtx = requestMsgCtx;
        this.workerPool = workerPool;
    }

    public Axis2ServerPushListener(Http2PushPromise pushPromise, HttpResponseFuture future,
                                   MessageContext requestMsgCtx, WorkerPool workerPool) {

        this.pushPromise = pushPromise;
        this.future = future;
        this.requestMsgCtx = requestMsgCtx;
        this.workerPool = workerPool;
    }

    @Override
    public void onPushPromiseAvailability(boolean isPromiseAvailable) {

        if (isPromiseAvailable) {
            future.setPushPromiseListener(new Axis2ServerPushListener(future, requestMsgCtx, workerPool));
        }
    }

    @Override
    public void onPushPromise(Http2PushPromise pushPromise) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(BridgeConstants.BRIDGE_LOG_PREFIX + "HTTP/2 push promise received");
        }
        workerPool.execute(new HttpServerPushWorker(requestMsgCtx, pushPromise));
        future.setPushResponseListener(new Axis2ServerPushListener(pushPromise, future, requestMsgCtx, workerPool),
                pushPromise.getPromisedStreamId());
        future.setPromiseAvailabilityListener(new Axis2ServerPushListener(future, requestMsgCtx, workerPool));
    }

    @Override
    public void onPushResponse(int promiseId, HttpCarbonMessage httpMessage) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(BridgeConstants.BRIDGE_LOG_PREFIX + "HTTP/2 push response received");
        }
        workerPool.execute(new HttpServerPushWorker(requestMsgCtx, pushPromise, httpMessage));
    }
}

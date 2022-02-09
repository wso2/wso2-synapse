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
package org.apache.synapse.transport.netty.listener;

import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.config.SourceConfiguration;
import org.wso2.transport.http.netty.contract.HttpConnectorListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * {@code PassThroughHttpConnectorListener} receives the {@code HttpCarbonMessage} coming from the Netty HTTP transport,
 * hands it over to the {@code HttpRequestWorker} to convert it to {@code MessageContext} and finally deliver it
 * to the axis engine.
 */
public class Axis2HttpConnectorListener implements HttpConnectorListener {

    private static final Log LOG = LogFactory.getLog(Axis2HttpConnectorListener.class);

    private final SourceConfiguration sourceConfiguration;

    public Axis2HttpConnectorListener(SourceConfiguration sourceConfiguration) {

        this.sourceConfiguration = sourceConfiguration;
    }

    public void onMessage(HttpCarbonMessage httpCarbonMessage) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Message received to HTTP transport, submitting a worker to the pool to process the request.");
        }
        WorkerPool workerPool = sourceConfiguration.getWorkerPool();
        workerPool.execute(new HttpRequestWorker(httpCarbonMessage, sourceConfiguration));
    }

    public void onError(Throwable throwable) {

        LOG.error("Error in HTTP server connector:", throwable);
    }

}

/**
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org)
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

package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.apache.synapse.transport.passthru.util.RelayUtils;

public class MessageDiscardWorker implements Runnable {

    private Log log = LogFactory.getLog(MessageDiscardWorker.class);

    TargetConfiguration targetConfiguration = null;

    private TargetResponse response = null;

    private MessageContext requestMessageContext;

    NHttpClientConnection conn = null;

    private Long queuedTime = null;

    private PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    private WorkerState state;

    public MessageDiscardWorker(MessageContext requestMsgContext, TargetResponse response,
                                TargetConfiguration targetConfiguration, NHttpClientConnection conn) {
        this.state = WorkerState.CREATED;
        this.response = response;
        this.requestMessageContext = requestMsgContext;
        this.targetConfiguration = targetConfiguration;
        this.conn = conn;
        this.queuedTime = System.currentTimeMillis();
    }

    public void run() {

        // Mark the start of the request message discard worker at the beginning of the worker thread
        setWorkerState(WorkerState.RUNNING);
        Long expectedMaxQueueingTime = conf.getExpectedMaxQueueingTimeForMessageDiscardWorker();
        if (queuedTime != null && expectedMaxQueueingTime != null) {
            Long messageDiscardWorkerQueuedTime = System.currentTimeMillis() - queuedTime;
            if (messageDiscardWorkerQueuedTime >= expectedMaxQueueingTime) {
                log.warn("Message discard worker queued time exceeds the expected max queueing time. Expected "
                        + "max queueing time : " + expectedMaxQueueingTime + "ms. Actual queued time : "
                        + messageDiscardWorkerQueuedTime + "ms"+ ", CORRELATION_ID : "
                        + requestMessageContext.getProperty(CorrelationConstants.CORRELATION_ID));
            }

        }

        // If an error has happened in the request processing, consumes the data in pipe completely and discard it
        try {
            RelayUtils.discardRequestMessage(requestMessageContext);
        } catch (AxisFault af) {
            log.error("Fault discarding request message", af);
        }

        // Mark the end of the request message discard worker at the end of the worker thread
        setWorkerState(WorkerState.FINISHED);
        ClientWorker clientWorker = new ClientWorker(targetConfiguration, requestMessageContext, response);
        conn.getContext().setAttribute(PassThroughConstants.CLIENT_WORKER_REFERENCE
                , clientWorker);
        targetConfiguration.getWorkerPool().execute(clientWorker);

        targetConfiguration.getMetrics().incrementMessagesReceived();

        NHttpServerConnection sourceConn = (NHttpServerConnection) requestMessageContext.getProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
        if (sourceConn != null) {
            PassThroughTransportUtils.setSourceConnectionContextAttributes(sourceConn, conn);
        }

    }

    private void setWorkerState(WorkerState workerState) {
        this.state = workerState;
    }

    public WorkerState getWorkerState() {
        return this.state;
    }
}

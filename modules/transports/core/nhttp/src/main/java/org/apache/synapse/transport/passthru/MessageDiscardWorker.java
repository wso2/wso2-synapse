/**
 *  Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.util.RelayUtils;

public class MessageDiscardWorker implements Runnable {

    private Log log = LogFactory.getLog(MessageDiscardWorker.class);
    private ClientWorker clientWorker = null;

    TargetConfiguration targetConfiguration = null;

    private TargetResponse response = null;

    private MessageContext requestMessageContext;

    NHttpClientConnection conn = null;

    public MessageDiscardWorker(MessageContext requestMsgContext, TargetResponse response,
                                TargetConfiguration targetConfiguration, ClientWorker clientWorker, NHttpClientConnection conn) {
        this.response = response;
        this.requestMessageContext = requestMsgContext;
        this.targetConfiguration = targetConfiguration;
        this.clientWorker = clientWorker;
        this.conn = conn;
    }

    public void run() {

        // If an error has happened in the request processing, consumes the data in pipe completely and discard it
        try {
            RelayUtils.discardRequestMessage(requestMessageContext);
        } catch (AxisFault af) {
            log.error("Fault discarding request message", af);
        }

        targetConfiguration.getWorkerPool().execute(clientWorker);

        targetConfiguration.getMetrics().incrementMessagesReceived();

        NHttpServerConnection sourceConn = (NHttpServerConnection) requestMessageContext.getProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
        if (sourceConn != null) {
            sourceConn.getContext().setAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME,
                    conn.getContext()
                            .getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME)
            );
            conn.getContext().removeAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME);

            sourceConn.getContext().setAttribute(PassThroughConstants.REQ_DEPARTURE_TIME,
                    conn.getContext()
                            .getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME)
            );
            conn.getContext().removeAttribute(PassThroughConstants.REQ_DEPARTURE_TIME);
            sourceConn.getContext().setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME,
                    conn.getContext()
                            .getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME)
            );

            conn.getContext().removeAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
            sourceConn.getContext().setAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME,
                    conn.getContext()
                            .getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME)
            );
            conn.getContext().removeAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME);
            sourceConn.getContext().setAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME,
                    conn.getContext()
                            .getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME)
            );
            conn.getContext().removeAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME);

        }

    }
}

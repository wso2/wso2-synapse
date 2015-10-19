/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.synapse.transport.passthru.connections.HostConnection;

public class ConnectCallback implements SessionRequestCallback {

    private static Log log = LogFactory.getLog(ConnectCallback.class);     

    /** The agent used for delivering requests */
    private DeliveryAgent deliveryAgent;

    /**
     * Create the callback for the handling events on a given connection     
     */
    public ConnectCallback() {

    }

    public void completed(SessionRequest request) {
        if (log.isDebugEnabled()) {
            if (request.getSession() != null &&
                    request.getSession().getLocalAddress() != null) {
                log.debug("Connected to remote address: " +
                        request.getSession().getRemoteAddress() +
                        " from local address: " + request.getSession().getLocalAddress());
            }
        }
    }

    public void failed(SessionRequest request) {
        HostConnection hostConnection = (HostConnection) request.getAttachment();
        deliveryAgent.errorConnecting(hostConnection.getRoute(),
                ErrorCodes.CONNECTION_FAILED, "Connection Failed");

        handleError("Connection refused or failed for : " + request.getRemoteAddress());
    }

    public void timeout(SessionRequest request) {
        HostConnection hostConnection = (HostConnection) request.getAttachment();
        deliveryAgent.errorConnecting(hostConnection.getRoute(),
                ErrorCodes.CONNECT_TIMEOUT, "Connection Timeout");

        handleError("Timeout connecting to : " + request.getRemoteAddress());
        request.cancel();
    }

    public void cancelled(SessionRequest request) {
        HostConnection hostConnection = (HostConnection) request.getAttachment();
        deliveryAgent.errorConnecting(hostConnection.getRoute(),
                ErrorCodes.CONNECT_CANCEL, "Connection Cancel");

        handleError("Connection cancelled for : " + request.getRemoteAddress());
    }

    private void handleError(String errorMessage) {
        log.warn(errorMessage);
    }

    public void setDeliveryAgent(DeliveryAgent deliveryAgent) {
        this.deliveryAgent = deliveryAgent;
    }
}

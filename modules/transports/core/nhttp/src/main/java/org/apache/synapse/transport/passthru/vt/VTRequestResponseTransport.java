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

package org.apache.synapse.transport.passthru.vt;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.RequestResponseTransport;

/**
 * A simple {@link RequestResponseTransport} implementation for the VT transport.
 * In the blocking VT model, acknowledge is a no-op since the response is written
 * synchronously on the same thread.
 */
public class VTRequestResponseTransport implements RequestResponseTransport {

    private RequestResponseTransportStatus status = RequestResponseTransportStatus.WAITING;
    private final MessageContext msgContext;
    private boolean responseWritten = false;

    public VTRequestResponseTransport(MessageContext msgContext) {
        this.msgContext = msgContext;
    }

    @Override
    public void acknowledgeMessage(MessageContext msgContext) throws org.apache.axis2.AxisFault {
        status = RequestResponseTransportStatus.ACKED;
    }

    @Override
    public void awaitResponse() throws InterruptedException, org.apache.axis2.AxisFault {
        // In the VT model, response is written synchronously, so nothing to wait for.
        status = RequestResponseTransportStatus.WAITING;
    }

    @Override
    public void signalResponseReady() {
        status = RequestResponseTransportStatus.SIGNALLED;
    }

    @Override
    public RequestResponseTransportStatus getStatus() {
        return status;
    }

    @Override
    public void signalFaultReady(org.apache.axis2.AxisFault fault) {
        status = RequestResponseTransportStatus.SIGNALLED;
    }

    public boolean isResponseWritten() {
        return responseWritten;
    }

    public void setResponseWritten(boolean responseWritten) {
        this.responseWritten = responseWritten;
    }
}

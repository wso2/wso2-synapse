/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.apache.synapse.commons.emulator.http.dsl;

import org.apache.synapse.commons.emulator.core.AbstractEmulatorContext;
import org.apache.synapse.commons.emulator.core.AbstractProtocolEmulator;
import org.apache.synapse.commons.emulator.http.HTTPProtocolEmulator;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.IncomingMessage;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.OutgoingMessage;

import java.util.HashMap;
import java.util.Map;

public class HttpProducerContext extends AbstractEmulatorContext {

    private IncomingMessage incoming;
    private Map<IncomingMessage, OutgoingMessage> inOutCorrelation = new HashMap<IncomingMessage, OutgoingMessage>();
    private HTTPProtocolEmulator httpProtocolEmulator;


    public HttpProducerContext(HTTPProtocolEmulator httpProtocolEmulator) {
        this.httpProtocolEmulator = httpProtocolEmulator;
    }

    public HttpProducerContext host(String host) {
        super.host(host);
        return this;
    }

    public HttpProducerContext port(int port) {
        super.port(port);
        return this;
    }

    public HttpProducerContext when(IncomingMessage incoming) {
        this.incoming = incoming;
        return this;
    }

    public HttpProducerContext respond(OutgoingMessage outgoing) {
        this.inOutCorrelation.put(incoming, outgoing);
        return this;
    }

    public AbstractProtocolEmulator operations() {
        return httpProtocolEmulator;
    }

    public IncomingMessage getIncoming() {
        return incoming;
    }

    public Map<IncomingMessage, OutgoingMessage> getInOutCorrelation() {
        return inOutCorrelation;
    }
}

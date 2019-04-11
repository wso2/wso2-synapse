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

package org.apache.synapse.commons.emulator.http;

import org.apache.synapse.commons.emulator.core.Emulator;
import org.apache.synapse.commons.emulator.core.EmulatorType;
import org.apache.synapse.commons.emulator.core.AbstractProtocolEmulator;
import org.apache.synapse.commons.emulator.http.consumer.HttpEmulatorConsumerInitializer;
import org.apache.synapse.commons.emulator.http.dsl.HttpConsumerContext;
import org.apache.synapse.commons.emulator.http.dsl.HttpProducerContext;
import org.apache.synapse.commons.emulator.http.producer.HttpEmulatorProducerInitializer;

public class HTTPProtocolEmulator extends AbstractProtocolEmulator {

    private HttpEmulatorConsumerInitializer httpEmulatorConsumerInitializer;
    private HttpEmulatorProducerInitializer httpEmulatorProducerInitializer;
    private HttpConsumerContext consumerContext;
    private HttpProducerContext httpProducerContext;

    public HTTPProtocolEmulator(Emulator emulator) {
        super(emulator);
    }

    @Override
    public HttpConsumerContext consumer() {
        consumerContext = new HttpConsumerContext(this);
        setEmulatorType(EmulatorType.HTTP_CONSUMER);
        httpEmulatorConsumerInitializer = new HttpEmulatorConsumerInitializer(consumerContext);
        return consumerContext;
    }

    @Override
    public HttpProducerContext producer() {
        httpProducerContext = new HttpProducerContext(this);
        setEmulatorType(EmulatorType.HTTP_PRODUCER);
        this.httpEmulatorProducerInitializer = new HttpEmulatorProducerInitializer(httpProducerContext);
        return httpProducerContext;
    }

    public HttpEmulatorConsumerInitializer getHttpEmulatorConsumerInitializer() {
        return httpEmulatorConsumerInitializer;
    }

    public HttpEmulatorProducerInitializer getHttpEmulatorProducerInitializer() {
        return httpEmulatorProducerInitializer;
    }

    public HttpConsumerContext getConsumerContext() {
        return consumerContext;
    }

    public HttpProducerContext getHttpProducerContext() {
        return httpProducerContext;
    }
}

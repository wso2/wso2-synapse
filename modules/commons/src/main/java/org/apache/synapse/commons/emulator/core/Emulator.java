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

package org.apache.synapse.commons.emulator.core;

import org.apache.log4j.Logger;
import org.apache.synapse.commons.emulator.http.HTTPProtocolEmulator;

public class Emulator extends Thread {
    private static final Logger log = Logger.getLogger(Emulator.class);
    private HTTPProtocolEmulator httpProtocolEmulator;
    private EmulatorType emulatorType;

    /**
     * Constructor of the class.
     */
    public Emulator() {
        httpProtocolEmulator = new HTTPProtocolEmulator(this);
    }

    /**
     * Get the HTTPProtocolEmulator object.
     *
     * @return object of HTTPProtocolEmulator
     */
    public HTTPProtocolEmulator getHttpProtocolEmulator() {
        return httpProtocolEmulator;
    }

    public void run() {

        try {
            if (EmulatorType.HTTP_CONSUMER.equals(emulatorType)) {
                validateInput(httpProtocolEmulator.getConsumerContext());
                httpProtocolEmulator.getHttpEmulatorConsumerInitializer().initialize();
            } else if (EmulatorType.HTTP_PRODUCER.equals(emulatorType)) {
                validateInput(httpProtocolEmulator.getHttpProducerContext());
                httpProtocolEmulator.getHttpEmulatorProducerInitializer().initialize();
            }
        } catch (Exception e) {
            log.error("Exception occurred while initialize the Emulator", e);
        }
    }

    public void shutdown(EmulatorType emulatorType) {
        if (EmulatorType.HTTP_CONSUMER.equals(emulatorType)) {
            httpProtocolEmulator.getHttpEmulatorConsumerInitializer().shutdown();
        } else if (EmulatorType.HTTP_PRODUCER.equals(emulatorType)) {
            httpProtocolEmulator.getHttpEmulatorProducerInitializer().shutdown();
        }
        log.info("Emulator shutdown successfully.......");
    }

    private void validateInput(AbstractEmulatorContext abstractEmulatorContext) {
        if (abstractEmulatorContext.getHost() == null || abstractEmulatorContext.getPort() == null) {
           log.error("Invalid host [" + abstractEmulatorContext.getHost() + "] and port [" + abstractEmulatorContext
                   .getPort() + "]");
        }
    }

    void setEmulatorType(EmulatorType emulatorType) {
        this.emulatorType = emulatorType;
    }
}

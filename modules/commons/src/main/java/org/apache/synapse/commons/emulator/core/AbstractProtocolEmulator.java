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

public abstract class AbstractProtocolEmulator {

    private Emulator emulator;
    private EmulatorType emulatorType;
    private static final Logger log = Logger.getLogger(AbstractProtocolEmulator.class);

    public AbstractProtocolEmulator(Emulator emulator) {
        this.emulator = emulator;
    }

    public abstract AbstractEmulatorContext consumer();

    public abstract AbstractEmulatorContext producer();

    public AbstractProtocolEmulator start() {
        try {
            emulator.setEmulatorType(emulatorType);
            emulator.start();
        } catch (Exception e) {
            log.error("Exception occurred while initialize the emulator", e);
        }
        return this;
    }

    public AbstractProtocolEmulator send() {
        try {
            emulator.setEmulatorType(emulatorType);
            emulator.run();
        } catch (Exception e) {
            log.error("Exception occurred while initialize the emulator", e);
        }
        return this;
    }

    public void shutdown() {
        try {
            emulator.shutdown(emulatorType);
        } catch (Exception e) {
            log.error("Exception occurred while shutdown the emulator", e);
        }
    }

    protected void setEmulatorType(EmulatorType emulatorType) {
        this.emulatorType = emulatorType;
    }
}

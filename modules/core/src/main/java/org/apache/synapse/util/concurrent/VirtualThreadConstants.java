/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util.concurrent;

/**
 * Configuration keys and defaults for virtual-thread dispatch limits.
 */
public final class VirtualThreadConstants {

    public static final String VT_MAX_ACCEPT_CONNECTIONS =
            "synapse.vt.accept.max.connections";
    public static final int DEFAULT_VT_MAX_ACCEPT_CONNECTIONS = 1000;

    public static final String VT_MAX_SYNAPSE_THREADS =
            "synapse.vt.synapse.max.threads";
    public static final int DEFAULT_VT_MAX_SYNAPSE_THREADS = 100;

    public static final String VT_MAX_INBOUND_THREADS =
            "synapse.vt.inbound.max.threads";
    public static final int DEFAULT_VT_MAX_INBOUND_THREADS = 100;

    private VirtualThreadConstants() {
    }

    public static int getSystemInt(String key, int defaultValue) {
        return parseInt(System.getProperty(key), defaultValue);
    }

    public static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : defaultValue;
        } catch (NumberFormatException ignore) {
            return defaultValue;
        }
    }
}

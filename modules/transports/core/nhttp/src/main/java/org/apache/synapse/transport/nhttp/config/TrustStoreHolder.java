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
package org.apache.synapse.transport.nhttp.config;

import java.security.KeyStore;

/**
 * A data holder class to store the client trust store.
 */
public class TrustStoreHolder {

    private static volatile TrustStoreHolder instance;
    private KeyStore clientTrustStore;

    private TrustStoreHolder() {}

    public static TrustStoreHolder getInstance() {

        if (instance == null) {
            synchronized (TrustStoreHolder.class) {
                if (instance == null) {
                    instance = new TrustStoreHolder();
                }
            }
        }
        return instance;
    }

    public KeyStore getClientTrustStore() {
        return clientTrustStore;
    }

    public void setClientTrustStore(KeyStore clientTrustStore) {
        this.clientTrustStore = clientTrustStore;
    }

    /**
     * Reset the instance.
     */
    public static void resetInstance() {

        instance = null;
    }
}

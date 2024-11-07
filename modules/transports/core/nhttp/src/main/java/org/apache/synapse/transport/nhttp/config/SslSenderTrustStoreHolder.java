/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.nhttp.config;

/**
 * The SSL Sender TrustStore Holder class to store the client trust store's configurable details.
 */
public class SslSenderTrustStoreHolder {

    private static volatile SslSenderTrustStoreHolder instance;

    private SslSenderTrustStoreHolder() {}

    private String location;
    private String password;
    private String type;

    public static SslSenderTrustStoreHolder getInstance() {

        if (instance == null) {
            synchronized (TrustStoreHolder.class) {
                if (instance == null) {
                    instance = new SslSenderTrustStoreHolder();
                }
            }
        }
        return instance;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}

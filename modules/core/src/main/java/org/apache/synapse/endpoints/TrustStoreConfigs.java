/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com/).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints;

/**
 * This class represents a model for trust store configurations which is used for the token endpoint connection
 * in OAuth protected endpoints.
 */
public class TrustStoreConfigs {
    private String trustStoreLocation;
    private String trustStoreType;
    private char[] trustStorePassword;
    private boolean trustStoreEnabled;

    public void setTrustStoreEnabled(boolean trustStoreEnabled) {
        this.trustStoreEnabled = trustStoreEnabled;
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        this.trustStoreLocation = trustStoreLocation;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public void setTrustStorePassword(char[] trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public boolean isTrustStoreEnabled() {
        return trustStoreEnabled;
    }

    public String getTrustStoreLocation() {
        return trustStoreLocation;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }
}

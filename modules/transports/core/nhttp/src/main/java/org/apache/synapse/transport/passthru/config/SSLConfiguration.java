/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.transport.passthru.config;


import org.apache.axiom.om.OMElement;

public class SSLConfiguration {

    /**
     * Key Store Information for Inbound Listener
     */
    private OMElement keystore;

    /**
     * Trust Store Information for Inbound Listener
     */
    private OMElement trustore;

    /**
     * Client Authentication require or optional
     */
    private OMElement clientAuth;

    /**
     * Operation HTTPS protocol
     */
    private OMElement httpsProtocol;

    /**
     * Revocation Verification information
     */
    private OMElement revocationVerifier;

    /**
     * SSL protocol
     */
    private OMElement sslProtocol;


    public SSLConfiguration(OMElement keystore, OMElement trustore, OMElement clientAuth,
                            OMElement httpsProtocol, OMElement revocationVerifier,
                            OMElement sslProtocol) {
        this.keystore = keystore;
        this.trustore = trustore;
        this.clientAuth = clientAuth;
        this.httpsProtocol = httpsProtocol;
        this.revocationVerifier = revocationVerifier;
        this.sslProtocol = sslProtocol;
    }

    public OMElement getKeystore() {
        return keystore;
    }

    public OMElement getTrustore() {
        return trustore;
    }

    public OMElement getClientAuth() {
        return clientAuth;
    }

    public OMElement getHttpsProtocol() {
        return httpsProtocol;
    }

    public OMElement getRevocationVerifier() {
        return revocationVerifier;
    }

    public OMElement getSslProtocol() {
        return sslProtocol;
    }
}

/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.apache.synapse.transport.netty.api.config;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import javax.xml.stream.XMLStreamException;

/**
 * {@code SSLConfiguration} encapsulates the transport level security related configurations.
 */
public class SSLConfiguration {

    private static final Log LOG = LogFactory.getLog(SSLConfiguration.class);

    private final String keyStore;
    private final String trustStore;
    private final String clientAuthEl;
    private final String httpsProtocolsEl;
    private final String revocationVerifier;
    private final String sslProtocol;
    private final String preferredCiphersEl;
    private final String sessionTimeout;
    private final String handshakeTimeout;

    public SSLConfiguration(SSLConfigurationBuilder builder) {

        this.keyStore = builder.keyStore;
        this.trustStore = builder.trustStore;
        this.clientAuthEl = builder.clientAuthEl;
        this.httpsProtocolsEl = builder.httpsProtocolsEl;
        this.revocationVerifier = builder.revocationVerifier;
        this.sslProtocol = builder.sslProtocol;
        this.preferredCiphersEl = builder.preferredCiphersEl;
        this.sessionTimeout = builder.sessionTimeout;
        this.handshakeTimeout = builder.handshakeTimeout;
    }

    public OMElement getKeyStoreElement() {

        if (keyStore != null) {
            try {
                return AXIOMUtil.stringToOM(keyStore);
            } catch (XMLStreamException e) {
                LOG.error("Keystore may not be well formed XML", e);
            }
        }
        return null;
    }

    public OMElement getClientAuthElement() {

        if (clientAuthEl == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement clientAuthElement = fac.createOMElement(BridgeConstants.SSL_VERIFY_CLIENT, "", "");
        clientAuthElement.setText(clientAuthEl);
        return clientAuthElement;
    }

    public OMElement getTrustStoreElement() {

        if (trustStore != null) {
            try {
                return AXIOMUtil.stringToOM(trustStore);
            } catch (XMLStreamException e) {
                LOG.error("TrustStore may not be well formed XML", e);
            }
        }
        return null;
    }

    public OMElement getRevocationVerifierElement() {

        if (revocationVerifier != null) {
            try {
                return AXIOMUtil.stringToOM(revocationVerifier);
            } catch (XMLStreamException e) {
                LOG.error("CertificateRevocationVerifier may not be well formed XML", e);
            }
        }
        return null;
    }

    public OMElement getHttpsProtocolElement() {

        if (httpsProtocolsEl == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement httpsProtocolElement = fac.createOMElement(BridgeConstants.HTTPS_PROTOCOL, "", "");
        httpsProtocolElement.setText(httpsProtocolsEl);
        return httpsProtocolElement;
    }

    public OMElement getSslProtocolElement() {

        if (sslProtocol == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement sslProtocolElement = fac.createOMElement(BridgeConstants.SSL_PROTOCOL, "", "");
        sslProtocolElement.setText(sslProtocol);
        return sslProtocolElement;
    }

    public OMElement getSessionTimeoutElement() {

        if (sessionTimeout == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement sessionTimeoutElement = fac.createOMElement(BridgeConstants.SSL_SESSION_TIMEOUT, "", "");
        sessionTimeoutElement.setText(sessionTimeout);
        return sessionTimeoutElement;
    }

    public OMElement getHandshakeTimeoutElement() {

        if (handshakeTimeout == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement handshakeTimeoutElement = fac.createOMElement(BridgeConstants.SSL_HANDSHAKE_TIMEOUT, "", "");
        handshakeTimeoutElement.setText(handshakeTimeout);
        return handshakeTimeoutElement;
    }

    /**
     * Return a OMElement of preferred ciphers parameter values.
     *
     * @return OMElement
     */
    public OMElement getPreferredCiphersElement() {

        if (preferredCiphersEl == null) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement preferredCiphersElement = fac.createOMElement(NhttpConstants.PREFERRED_CIPHERS, "", "");
        preferredCiphersElement.setText(preferredCiphersEl);
        return preferredCiphersElement;
    }

    public String getSslProtocol() {

        return sslProtocol;
    }

    public String getKeyStore() {

        return keyStore;
    }

    public String getTrustStore() {

        return trustStore;
    }

    public String getClientAuthEl() {

        return clientAuthEl;
    }

    public String getHttpsProtocolsEl() {

        return httpsProtocolsEl;
    }

    public String getRevocationVerifier() {

        return revocationVerifier;
    }

    public String getHandshakeTimeout() {

        return handshakeTimeout;
    }

    public String getSessionTimeout() {

        return sessionTimeout;
    }

    /**
     * Builder class for the SSLConfiguration.
     */
    public static class SSLConfigurationBuilder {

        private String keyStore;
        private String trustStore;
        private String clientAuthEl;
        private String httpsProtocolsEl;
        private String revocationVerifier;
        private String sslProtocol;
        private String preferredCiphersEl;
        private String sessionTimeout;
        private String handshakeTimeout;

        public SSLConfiguration build() {

            return new SSLConfiguration(this);
        }

        public SSLConfigurationBuilder keyStore(String keyStore) {

            this.keyStore = keyStore;
            return this;
        }

        public SSLConfigurationBuilder trustStore(String trustStore) {

            this.trustStore = trustStore;
            return this;
        }

        public SSLConfigurationBuilder clientAuthEl(String clientAuthEl) {

            this.clientAuthEl = clientAuthEl;
            return this;
        }

        public SSLConfigurationBuilder httpsProtocolsEl(String httpsProtocolsEl) {

            this.httpsProtocolsEl = httpsProtocolsEl;
            return this;
        }

        public SSLConfigurationBuilder revocationVerifier(String revocationVerifier) {

            this.revocationVerifier = revocationVerifier;
            return this;
        }

        public SSLConfigurationBuilder sslProtocol(String sslProtocol) {

            this.sslProtocol = sslProtocol;
            return this;
        }

        public SSLConfigurationBuilder preferredCiphersEl(String preferredCiphersEl) {

            this.preferredCiphersEl = preferredCiphersEl;
            return this;
        }

        public SSLConfigurationBuilder sessionTimeout(String sessionTimeout) {

            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public SSLConfigurationBuilder handshakeTimeout(String handshakeTimeout) {

            this.handshakeTimeout = handshakeTimeout;
            return this;
        }
    }

}

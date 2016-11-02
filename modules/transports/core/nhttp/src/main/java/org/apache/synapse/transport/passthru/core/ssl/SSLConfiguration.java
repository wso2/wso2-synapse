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

package org.apache.synapse.transport.passthru.core.ssl;


import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.log4j.Logger;
import org.apache.synapse.transport.nhttp.util.NhttpConstants;

import javax.xml.stream.XMLStreamException;

public class SSLConfiguration {

    private Logger log = Logger.getLogger(SSLConfiguration.class);

    private String keyStore;
    private String trustStore;
    private String clientAuthEl;
    private String httpsProtocolsEl;
    private String revocationVerifier;
    private String sslProtocol;
    private String preferredCiphersEl;


    private OMElement keyStoreElement;
    private OMElement trustStoreElement;
    private OMElement clientAuthElement;
    private OMElement revocationVerifierElement;
    private OMElement httpsProtocolElement;
    /** Config of Preferred cipher suites **/
    private OMElement preferredCiphersElement;


    public SSLConfiguration(String keyStore, String trustStore, String clientAuthEl,
                            String httpsProtocolsEl, String revocationVerifier,
                            String sslProtocol, String preferredCiphersEl) {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.clientAuthEl = clientAuthEl;
        this.httpsProtocolsEl = httpsProtocolsEl;
        this.revocationVerifier = revocationVerifier;
        this.sslProtocol = sslProtocol;
        this.preferredCiphersEl = preferredCiphersEl;
    }



    public OMElement getKeyStoreElement() {
        if (keyStore != null) {
            try {
                keyStoreElement = AXIOMUtil.stringToOM(keyStore);
            } catch (XMLStreamException e) {
                log.error("Keystore may not be well formed XML", e);
            }
        }
        return keyStoreElement;
    }

    public OMElement getClientAuthElement() {
        if (clientAuthEl != null) {
            OMFactory fac = OMAbstractFactory.getOMFactory();
            clientAuthElement = fac.createOMElement("SSLVerifyClient", "", "");
            clientAuthElement.setText(clientAuthEl);
        }
        return clientAuthElement;
    }

    public OMElement getTrustStoreElement() {
        if (trustStore != null) {
            try {
                trustStoreElement = AXIOMUtil.stringToOM(trustStore);
            } catch (XMLStreamException e) {
                log.error("TrustStore may not be well formed XML", e);
            }
        }
        return trustStoreElement;
    }


    public OMElement getRevocationVerifierElement() {
        if (revocationVerifier != null) {
            try {
                revocationVerifierElement = AXIOMUtil.stringToOM(revocationVerifier);
            } catch (XMLStreamException e) {
                log.error("CertificateRevocationVerifier may not be well formed XML", e);
            }
        }
        return revocationVerifierElement;
    }

    public OMElement getHttpsProtocolElement() {
        if (httpsProtocolsEl != null) {
            OMFactory fac = OMAbstractFactory.getOMFactory();
            httpsProtocolElement = fac.createOMElement("HttpsProtocols", "", "");
            httpsProtocolElement.setText(httpsProtocolsEl);
        }
        return httpsProtocolElement;
    }

    public String getPreferredCiphersEl() {
        return preferredCiphersEl;
    }

    /**
     * Return a OMElement of preferred ciphers parameter values.
     * @return OMElement
     */
    public OMElement getPreferredCiphersElement() {
        if (preferredCiphersEl != null) {
            OMFactory fac = OMAbstractFactory.getOMFactory();
            preferredCiphersElement = fac.createOMElement(NhttpConstants.PREFERRED_CIPHERS, "", "");
            preferredCiphersElement.setText(preferredCiphersEl);
        }
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


}

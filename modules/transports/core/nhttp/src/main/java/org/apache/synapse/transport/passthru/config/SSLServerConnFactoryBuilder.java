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
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.synapse.transport.certificatevalidation.RevocationVerificationManager;
import org.apache.synapse.transport.nhttp.config.ServerConnFactoryBuilder;

import javax.xml.namespace.QName;

public class SSLServerConnFactoryBuilder extends ServerConnFactoryBuilder {

    private final Log log = LogFactory.getLog(SSLServerConnFactoryBuilder.class);

    public SSLServerConnFactoryBuilder(TransportInDescription transportIn,
                                       HttpHost host) {
        super(transportIn, host);
    }


    public ServerConnFactoryBuilder parseSSL(OMElement keyStoreEl, OMElement trustStoreEl,
                                             OMElement clientAuthEl, OMElement httpsProtocolsEl,
                                             OMElement revocationVerifier, OMElement sslProtocol) throws AxisFault {

        final String sslProtocolVal = sslProtocol != null ? sslProtocol.getText() : "TLS";
        final String cvEnable = revocationVerifier != null ?
                                revocationVerifier.getAttribute(new QName("enable")).getAttributeValue() : null;
        RevocationVerificationManager revocationVerifierManager = null;
        if ("true".equalsIgnoreCase(cvEnable)) {
            String cacheSizeString = revocationVerifier.getFirstChildWithName(new QName("CacheSize")).getText();
            String cacheDelayString = revocationVerifier.getFirstChildWithName(new QName("CacheDelay")).getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            } catch (NumberFormatException e) {
                log.error("Cache Size or Cache Delay need to be an Integer");
            }
            revocationVerifierManager = new RevocationVerificationManager(cacheSize, cacheDelay);
        }
        ssl = createSSLContext(keyStoreEl, trustStoreEl, clientAuthEl, httpsProtocolsEl, revocationVerifierManager, sslProtocolVal);
        return this;
    }


}

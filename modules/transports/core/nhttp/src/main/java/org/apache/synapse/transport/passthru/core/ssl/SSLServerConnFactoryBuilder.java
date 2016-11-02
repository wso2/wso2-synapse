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
import java.util.Iterator;

public class SSLServerConnFactoryBuilder extends ServerConnFactoryBuilder {

    private final Log log = LogFactory.getLog(SSLServerConnFactoryBuilder.class);

    public SSLServerConnFactoryBuilder(TransportInDescription transportIn,
                                       HttpHost host) {
        super(transportIn, host);
    }


    public ServerConnFactoryBuilder parseSSL(OMElement keyStoreEl, OMElement trustStoreEl,
                                             OMElement clientAuthEl, OMElement httpsProtocolsEl,
                                             String sslProtocol, OMElement cvp, OMElement preferredCiphers) throws
            AxisFault {
        final String cvEnable = cvp != null ?
                                cvp.getAttribute(new QName("enable")).getAttributeValue() : null;
        RevocationVerificationManager revocationVerifier = null;

        if ("true".equalsIgnoreCase(cvEnable)) {
            Iterator iterator = cvp.getChildElements();
            String cacheDelayString = null;
            String cacheSizeString = null;
            while(iterator.hasNext()) {
                Object obj = iterator.next();
                if (obj instanceof OMElement && ((OMElement) obj).getLocalName().equals("CacheSize")) {
                    cacheSizeString = ((OMElement)obj).getText();
                } else if (obj instanceof OMElement && ((OMElement) obj).getLocalName().equals("CacheDelay")) {
                    cacheDelayString = ((OMElement)obj).getText();
                }
            }
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                if (cacheDelayString != null && cacheSizeString != null) {
                    cacheSize = new Integer(cacheSizeString);
                    cacheDelay = new Integer(cacheDelayString);
                }
            } catch (NumberFormatException e) {
                log.error("Please specify correct Integer numbers for CacheDelay and CacheSize");
            }
            revocationVerifier = new RevocationVerificationManager(cacheSize, cacheDelay);
        }
        ssl = createSSLContext(keyStoreEl, trustStoreEl, clientAuthEl, httpsProtocolsEl, preferredCiphers,
                revocationVerifier,
                sslProtocol);
        return this;
    }


}

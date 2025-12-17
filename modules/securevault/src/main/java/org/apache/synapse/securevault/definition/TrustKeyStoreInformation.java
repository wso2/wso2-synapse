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
package org.apache.synapse.securevault.definition;

import org.apache.commons.lang.StringUtils;

import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

/**
 * Represents the abstraction - Trusted Certificate Store Information
 */
public class TrustKeyStoreInformation extends KeyStoreInformation {

    private static final String PKIX = "PKIX";
    private static final String JCE_PROVIDER = "security.jce.provider";

    /**
     * Returns the TrustManagerFactory instance
     *
     * @return TrustManagerFactory instance
     */
    public TrustManagerFactory getTrustManagerFactoryInstance() {

        try {
            if (log.isDebugEnabled()) {
                log.debug("Creating a TrustManagerFactory instance");
            }
            KeyStore trustStore = this.getTrustStore();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(getManagerType());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory;
        } catch (Exception e) {
            handleException("Error getting TrustManagerFactory: ", e);
        }

        return null;
    }

    /**
     * Returns a KeyStore instance that has been created using trust store
     *
     * @return KeyStore Instance
     */
    public KeyStore getTrustStore() {
        return super.getKeyStore();

    }

    private static String getManagerType() {
        String provider = System.getProperty(JCE_PROVIDER);
        if (StringUtils.isNotEmpty(provider)) {
            return PKIX;
        }
        return TrustManagerFactory.getDefaultAlgorithm();
    }
}

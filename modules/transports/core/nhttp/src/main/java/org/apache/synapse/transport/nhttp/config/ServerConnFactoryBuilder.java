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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.certificatevalidation.RevocationVerificationManager;
import org.apache.synapse.transport.http.conn.SSLClientAuth;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.http.conn.ServerSSLSetupHandler;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

public class ServerConnFactoryBuilder {

    private final Log log = LogFactory.getLog(ServerConnFactoryBuilder.class);

    private final TransportInDescription transportIn;
    private final HttpHost host;
    private final String name;

    protected SSLContextDetails ssl;
    private Map<InetSocketAddress, SSLContextDetails> sslByIPMap = null;
    private ConfigurationContext configurationContext;

    public ServerConnFactoryBuilder(final TransportInDescription transportIn, final HttpHost host,
                                    ConfigurationContext configurationContext) {
        this.transportIn = transportIn;
        this.host = host;
        this.name = transportIn.getName().toUpperCase(Locale.US);
        this.configurationContext = configurationContext;
    }

    public ServerConnFactoryBuilder(final TransportInDescription transportIn, final HttpHost host) {
        this.transportIn = transportIn;
        this.host = host;
        this.name = transportIn.getName().toUpperCase(Locale.US);
    }

    protected SSLContextDetails createSSLContext(
               final OMElement keyStoreEl,
               final OMElement trustStoreEl,
               final OMElement cientAuthEl,
               final OMElement httpsProtocolsEl,
               final OMElement preferredCiphersEl,
               final RevocationVerificationManager verificationManager,
               final String sslProtocol) throws AxisFault {

        KeyManager[] keymanagers  = null;
        TrustManager[] trustManagers = null;
        SecretResolver resolver;
        if (configurationContext != null && configurationContext.getAxisConfiguration() != null) {
            resolver = configurationContext.getAxisConfiguration().getSecretResolver();
        } else {
            resolver = SecretResolverFactory.create(keyStoreEl, false);
        }

        if (keyStoreEl != null) {
            String location      = getValueOfElementWithLocalName(keyStoreEl,"Location");
            String type          = getValueOfElementWithLocalName(keyStoreEl,"Type");
            OMElement storePasswordEl = keyStoreEl.getFirstChildWithName(new QName("Password"));
            OMElement keyPasswordEl = keyStoreEl.getFirstChildWithName(new QName("KeyPassword"));
            if (storePasswordEl == null) {
                throw new AxisFault("Cannot proceed because Password element is missing in KeyStore");
            }
            if (keyPasswordEl == null) {
                throw new AxisFault("Cannot proceed because KeyPassword element is missing in KeyStore");
            }
            String storePassword = SecureVaultValueReader.getSecureVaultValue(resolver, storePasswordEl);
            String keyPassword   = SecureVaultValueReader.getSecureVaultValue(resolver, keyPasswordEl);

            FileInputStream fis = null;
            try {
                KeyStore keyStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Identity Keystore from : " + location);
                }

                keyStore.load(fis, storePassword.toCharArray());

                KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                           KeyManagerFactory.getDefaultAlgorithm());
                kmfactory.init(keyStore, keyPassword.toCharArray());
                keymanagers = kmfactory.getKeyManagers();
                if (log.isInfoEnabled() && keymanagers != null) {
                    for (KeyManager keymanager: keymanagers) {
                        if (keymanager instanceof X509KeyManager) {
                            X509KeyManager x509keymanager = (X509KeyManager) keymanager;
                            Enumeration<String> en = keyStore.aliases();
                            while (en.hasMoreElements()) {
                                String s = en.nextElement();
                                X509Certificate[] certs = x509keymanager.getCertificateChain(s);
                                if (certs==null) continue;
                                for (X509Certificate cert: certs) {
                                    log.debug(name + " Subject DN: " + cert.getSubjectDN());
                                    log.debug(name + " Issuer DN: " + cert.getIssuerDN());
                                }
                            }
                        }
                    }
                }

            } catch (GeneralSecurityException gse) {
                log.error(name + " Error loading Key store : " + location, gse);
                throw new AxisFault("Error loading Key store : " + location, gse);
            } catch (IOException ioe) {
                log.error(name + " Error opening Key store : " + location, ioe);
                throw new AxisFault("Error opening Key store : " + location, ioe);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            }
        }

        if (trustStoreEl != null) {
            String location      = getValueOfElementWithLocalName(trustStoreEl, "Location");
            String type          = getValueOfElementWithLocalName(trustStoreEl, "Type");
            OMElement storePasswordEl = trustStoreEl.getFirstChildWithName(new QName("Password"));
            if (storePasswordEl == null) {
                throw new AxisFault("Cannot proceed because Password element is missing in TrustStore");
            }
            String storePassword = SecureVaultValueReader.getSecureVaultValue(resolver, storePasswordEl);

            FileInputStream fis = null;
            try {
                KeyStore trustStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Trust Keystore from : " + location);
                }

                trustStore.load(fis, storePassword.toCharArray());
                TrustManagerFactory trustManagerfactory = TrustManagerFactory.getInstance(
                           TrustManagerFactory.getDefaultAlgorithm());
                trustManagerfactory.init(trustStore);
                trustManagers = trustManagerfactory.getTrustManagers();

            } catch (GeneralSecurityException gse) {
                log.error(name + " Error loading Key store : " + location, gse);
                throw new AxisFault("Error loading Key store : " + location, gse);
            } catch (IOException ioe) {
                log.error(name + " Error opening Key store : " + location, ioe);
                throw new AxisFault("Error opening Key store : " + location, ioe);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            }
        }
        final String s = cientAuthEl != null ? cientAuthEl.getText() : null;
        final SSLClientAuth clientAuth;
        if ("optional".equalsIgnoreCase(s)) {
            clientAuth = SSLClientAuth.OPTIONAL;
        } else if ("require".equalsIgnoreCase(s)) {
            clientAuth = SSLClientAuth.REQUIRED;
        } else {
            clientAuth = null;
        }

        String[] httpsProtocols = null;
        final String configuredHttpsProtocols =
                   httpsProtocolsEl != null ? httpsProtocolsEl.getText() : null;
        if (configuredHttpsProtocols != null && configuredHttpsProtocols.trim().length() != 0) {
            String[] configuredValues = configuredHttpsProtocols.trim().split(",");
            List<String> protocolList = new ArrayList<String>(configuredValues.length);
            for (String protocol : configuredValues) {
                if (!protocol.trim().isEmpty()) {
                    protocolList.add(protocol.trim());
                }
            }

            httpsProtocols = protocolList.toArray(new String[protocolList.size()]);
        }

        String[] preferredCiphers = null;
        final String configuredWeakCiphers =
                preferredCiphersEl != null ? preferredCiphersEl.getText() : null;
        if (configuredWeakCiphers != null && configuredWeakCiphers.trim().length() != 0) {
            String[] configuredValues = configuredWeakCiphers.trim().split(",");
            List<String> ciphersList = new ArrayList<String>(configuredValues.length);
            for (String cipher : configuredValues) {
                cipher = cipher.trim();
                if (!cipher.isEmpty()) {
                    ciphersList.add(cipher);
                }
            }

            preferredCiphers = ciphersList.toArray(new String[ciphersList.size()]);
        }


        try {
            final String sslProtocolValue = sslProtocol != null ? sslProtocol : "TLS";
            SSLContext sslContext = SSLContext.getInstance(sslProtocolValue);
            sslContext.init(keymanagers, trustManagers, null);

            ServerSSLSetupHandler sslSetupHandler =
                       (clientAuth != null || httpsProtocols != null || preferredCiphers != null) ?
                       new ServerSSLSetupHandler(clientAuth, httpsProtocols, verificationManager, preferredCiphers) :
                               null;

            return new SSLContextDetails(sslContext, sslSetupHandler);
        } catch (GeneralSecurityException gse) {
            log.error(name + " Unable to create SSL context with the given configuration", gse);
            throw new AxisFault("Unable to create SSL context with the given configuration", gse);
        }
    }

    public ServerConnFactoryBuilder parseSSL() throws AxisFault {
        Parameter keyParam = transportIn.getParameter("keystore");
        Parameter trustParam = transportIn.getParameter("truststore");
        Parameter clientAuthParam = transportIn.getParameter("SSLVerifyClient");
        Parameter httpsProtocolsParam = transportIn.getParameter("HttpsProtocols");
        final Parameter sslpParameter = transportIn.getParameter("SSLProtocol");
        Parameter preferredCiphersParam = transportIn.getParameter(NhttpConstants.PREFERRED_CIPHERS);
        final String sslProtocol = sslpParameter != null ? sslpParameter.getValue().toString() : "TLS";
        OMElement keyStoreEl = keyParam != null ? keyParam.getParameterElement().getFirstElement() : null;
        OMElement trustStoreEl = trustParam != null ? trustParam.getParameterElement().getFirstElement() : null;
        OMElement clientAuthEl = clientAuthParam != null ? clientAuthParam.getParameterElement() : null;
        OMElement httpsProtocolsEl = httpsProtocolsParam != null ? httpsProtocolsParam.getParameterElement() : null;
        OMElement preferredCiphersEl = preferredCiphersParam != null ? preferredCiphersParam.getParameterElement() : null;

        final Parameter cvp = transportIn.getParameter("CertificateRevocationVerifier");
        final String cvEnable = cvp != null ?
                                cvp.getParameterElement().getAttribute(new QName("enable")).getAttributeValue() : null;
        RevocationVerificationManager revocationVerifier = null;

        if ("true".equalsIgnoreCase(cvEnable)) {
            String cacheSizeString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheSize")).getText();
            String cacheDelayString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheDelay")).getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            }
            catch (NumberFormatException e) {}
            revocationVerifier = new RevocationVerificationManager(cacheSize, cacheDelay);
        }

        ssl = createSSLContext(keyStoreEl, trustStoreEl, clientAuthEl, httpsProtocolsEl, preferredCiphersEl,
                revocationVerifier,
                sslProtocol);
        return this;
    }

    public ServerConnFactoryBuilder parseMultiProfileSSL() throws AxisFault {

        TransportInDescription loadedTransportIn = loadMultiProfileSSLConfig();
        if (loadedTransportIn == null)
            return this;

        Parameter profileParam    = transportIn.getParameter("SSLProfiles");
        OMElement profilesEl = profileParam.getParameterElement();
        Iterator<?> profiles = profilesEl.getChildrenWithName(new QName("profile"));
        while (profiles.hasNext()) {
            OMElement profileEl = (OMElement) profiles.next();
            OMElement bindAddressEl = profileEl.getFirstChildWithName(new QName("bindAddress"));
            if (bindAddressEl == null) {
                String msg = "SSL profile must define a bind address";
                log.error(name + " " + msg);
                throw new AxisFault(msg);
            }
            InetSocketAddress address = new InetSocketAddress(bindAddressEl.getText(), host.getPort());

            OMElement keyStoreEl = profileEl.getFirstChildWithName(new QName("KeyStore"));
            OMElement trustStoreEl = profileEl.getFirstChildWithName(new QName("TrustStore"));
            OMElement clientAuthEl = profileEl.getFirstChildWithName(new QName("SSLVerifyClient"));
            OMElement httpsProtocolsEl = profileEl.getFirstChildWithName(new QName("HttpsProtocols"));
            OMElement preferredCiphersEl = profileEl.getFirstChildWithName(new QName(NhttpConstants.PREFERRED_CIPHERS));
            final Parameter sslpParameter = transportIn.getParameter("SSLProtocol");
            final String sslProtocol = sslpParameter != null ? sslpParameter.getValue().toString() : "TLS";
            SSLContextDetails ssl = createSSLContext(keyStoreEl, trustStoreEl, clientAuthEl, httpsProtocolsEl,
                    preferredCiphersEl, null,
                    sslProtocol);
            if (sslByIPMap == null) {
                sslByIPMap = new HashMap<InetSocketAddress, SSLContextDetails>();
            }
            sslByIPMap.put(address, ssl);
        }
        return this;
    }

    /**
     * Loads MultiProfileSSLConfiguration when the configuration is in a different file
     * than axis2.xml. If the configuration file path is in axis2.xml and its successfully loaded, it will be
     * added to transportIn as "SSLProfiles" parameter or else if it isn't loaded it will return null.
     * @return
     */
    public TransportInDescription loadMultiProfileSSLConfig () {

        Parameter profilePathParam = transportIn.getParameter("dynamicSSLProfilesConfig");

        //Custom SSL Profile configuration file not configured
        if (profilePathParam == null) {
            //Custom SSL Profiles configured in Axis2 configuration
            if (transportIn.getParameter("SSLProfiles") != null) {
                return transportIn;
            } else {
                return null;
            }
        }

        ////Custom SSL Profile configured. Ignore Axis2 configurations
        OMElement pathEl = profilePathParam.getParameterElement();
        String path = pathEl.getFirstChildWithName(new QName("filePath")).getText();

        try {
            if (path != null) {

                String separator = path.startsWith(System.getProperty("file.separator")) ?
                                   "" : System.getProperty("file.separator");
                String fullPath = System.getProperty("user.dir") + separator + path;

                OMElement profileEl = new StAXOMBuilder(fullPath).getDocumentElement();
                Parameter profileParam = new Parameter();
                profileParam.setParameterElement(profileEl);
                profileParam.setName("SSLProfiles");
                profileParam.setValue(profileEl);

                transportIn.addParameter(profileParam);
                log.info("SSLProfile configuration is loaded from path: " + fullPath);

                return transportIn;
            }
        } catch (Exception e) {
            log.error("Could not load SSLProfileConfig from file path: " + path, e);
        }
        return null;
    }

    public ServerConnFactory build(final HttpParams params) throws AxisFault {
        if (ssl != null || sslByIPMap != null) {
            return new ServerConnFactory(ssl, sslByIPMap, params);
        } else {
            return new ServerConnFactory(params);
        }
    }

    private String getValueOfElementWithLocalName(OMElement element, String localName) {
        Iterator iterator = element.getChildrenWithLocalName(localName);
        String value = null;
        Object obj = iterator.next();
        if (obj instanceof OMElement) {
            value = ((OMElement) obj).getText();
        }
        return value;
    }

}

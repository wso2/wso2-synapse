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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.util.Utils;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.certificatevalidation.CertificateVerificationManager;
import org.apache.synapse.transport.exceptions.InvalidConfigurationException;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.http.conn.ClientSSLSetupHandler;
import org.apache.synapse.transport.http.conn.RequestDescriptor;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.NoValidateCertTrustManager;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

public class ClientConnFactoryBuilder {

    private static final Log log = LogFactory.getLog(ClientConnFactoryBuilder.class);

    private final TransportOutDescription transportOut;
    private final String name;

    private SSLContextDetails ssl = null;
    private Map<RequestDescriptor, SSLContext> sslByHostMap = null;
    private  ConfigurationContext configurationContext;
    private static final String BOUNCY_CASTLE_PROVIDER = "BC";
    private static final String BOUNCY_CASTLE_FIPS_PROVIDER = "BCFIPS";
    private static final String SECURITY_JCE_PROVIDER = "security.jce.provider";
    private static final String PKIX = "PKIX";
    private static final String BCJSSE = "BCJSSE";
    private static final String TLS = "TLS";

    public ClientConnFactoryBuilder(final TransportOutDescription transportOut, ConfigurationContext configurationContext) {
        this(transportOut);
        this.configurationContext = configurationContext;
    }

    public ClientConnFactoryBuilder(final TransportOutDescription transportOut) {
        super();
        this.transportOut = transportOut;
        this.name = transportOut.getName().toUpperCase(Locale.US);
    }

    public ClientConnFactoryBuilder parseSSL() throws AxisFault {
        Parameter keyParam = transportOut.getParameter("keystore");
        Parameter trustParam = transportOut.getParameter("truststore");
        Parameter httpsProtocolsParam = transportOut.getParameter("HttpsProtocols");
        Parameter preferredCiphersParam = transportOut.getParameter(NhttpConstants.PREFERRED_CIPHERS);

        OMElement ksEle = null;
        OMElement tsEle = null;

        if (keyParam != null) {
            ksEle = keyParam.getParameterElement().getFirstElement();
        }

        boolean novalidatecert = ParamUtils.getOptionalParamBoolean(transportOut,
                "novalidatecert", false);

        if (trustParam != null) {
            if (novalidatecert) {
                if (log.isWarnEnabled()) {
                    log.warn(name + " Ignoring novalidatecert parameter since a truststore has been specified");
                }
            }
            tsEle = trustParam.getParameterElement().getFirstElement();
        }

        SSLContext sslContext = createSSLContext(ksEle, tsEle, novalidatecert);

        final Parameter hvp = transportOut.getParameter("HostnameVerifier");
        final String hvs = hvp != null ? hvp.getValue().toString() : null;
        final X509HostnameVerifier hostnameVerifier;


        if ("Strict".equalsIgnoreCase(hvs)) {
            hostnameVerifier = ClientSSLSetupHandler.STRICT;
        } else if ("AllowAll".equalsIgnoreCase(hvs)) {
            hostnameVerifier = ClientSSLSetupHandler.ALLOW_ALL;
        } else if ("DefaultAndLocalhost".equalsIgnoreCase(hvs)) {
            hostnameVerifier = ClientSSLSetupHandler.DEFAULT_AND_LOCALHOST;
        } else {
            hostnameVerifier = ClientSSLSetupHandler.DEFAULT;
        }

        final Parameter cvp = transportOut.getParameter("CertificateRevocationVerifier");
        final String cvEnable = cvp != null ?
                cvp.getParameterElement().getAttribute(new QName("enable")).getAttributeValue() : null;
        CertificateVerificationManager certificateVerifier = null;

        if ("true".equalsIgnoreCase(cvEnable)) {
            String cacheSizeString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheSize")).getText();
            String cacheDelayString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheDelay"))
                    .getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            } catch (NumberFormatException e) {
            }
            certificateVerifier = new CertificateVerificationManager(cacheSize, cacheDelay);
        }

        // Process HttpProtocols
        OMElement httpsProtocolsEl = httpsProtocolsParam != null ? httpsProtocolsParam.getParameterElement() : null;
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

        // Initiated separately to cater setting https protocols
        ClientSSLSetupHandler clientSSLSetupHandler = new ClientSSLSetupHandler(hostnameVerifier, certificateVerifier);

        if (null != httpsProtocols) {
            clientSSLSetupHandler.setHttpsProtocols(httpsProtocols);
        }

        //Process enabled ciphers
        OMElement preferredCiphersEl = preferredCiphersParam != null ? preferredCiphersParam.getParameterElement() :
                null;
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
            clientSSLSetupHandler.setPreferredCiphers(preferredCiphers);
        }

        ssl = new SSLContextDetails(sslContext, clientSSLSetupHandler);
        sslByHostMap = getCustomSSLContexts(transportOut);
        return this;
    }

    /**
     * Looks for a transport parameter named customSSLProfiles and initializes zero or more
     * custom SSLContext instances. The syntax for defining custom SSL profiles is as follows.
     * <p>
     * <parameter name="customSSLProfiles>
     *      <profile>
     *          <servers>www.test.org:80, www.test2.com:9763</servers>
     *          <KeyStore>
     *              <Location>/path/to/identity/store</Location>
     *              <Type>JKS</Type>
     *              <Password>password</Password>
     *              <KeyPassword>password</KeyPassword>
     *          </KeyStore>
     *          <TrustStore>
     *              <Location>path/tp/trust/store</Location>
     *              <Type>JKS</Type>
     *              <Password>password</Password>
     *          </TrustStore>
     *      </profile>
     * </parameter>
     * <p>
     * Any number of profiles can be defined under the customSSLProfiles parameter.
     *
     * @param transportOut transport out description
     * @return a map of server addresses and SSL contexts
     * @throws AxisFault if at least on SSL profile is not properly configured
     */
    private Map<RequestDescriptor, SSLContext> getCustomSSLContexts(TransportOutDescription transportOut)
            throws AxisFault {

        TransportOutDescription customSSLProfileTransport = loadDynamicSSLConfig(transportOut);

        Parameter customProfilesParam = customSSLProfileTransport.getParameter("customSSLProfiles");
        if (customProfilesParam == null) {
            return null;
        }

        if (log.isInfoEnabled()) {
            log.info(name + " Loading custom SSL profiles for the HTTPS sender");
        }

        OMElement customProfilesElt = customProfilesParam.getParameterElement();
        Utils.resolveOMElementChildValues(customProfilesElt);
        SecretResolver secretResolver = SecretResolverFactory.create(customProfilesElt, true);
        Iterator<?> profiles = customProfilesElt.getChildrenWithName(new QName("profile"));
        Map<RequestDescriptor, SSLContext> contextMap = new HashMap<RequestDescriptor, SSLContext>();

        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement serversElt = profile.getFirstChildWithName(new QName("servers"));
            if (serversElt == null || serversElt.getText() == null) {
                String msg = "Each custom SSL profile must define at least one host:port " +
                        "pair under the servers element";
                log.error(name + " " + msg);
                throw new AxisFault(msg);
            }

            String[] servers = serversElt.getText().split(",");
            if (log.isDebugEnabled()) {
                log.debug("Servers list of the custom SSL profile : " + Arrays.toString(servers));
            }
            Iterator<?> clients = profile.getChildrenWithName(new QName("client"));
            if (clients.hasNext()) {
                while (clients.hasNext()) {
                    OMElement client = (OMElement) clients.next();
                    OMElement ksElt = client.getFirstChildWithName(new QName("KeyStore"));
                    OMElement trElt = client.getFirstChildWithName(new QName("TrustStore"));
                    String noValCert = client.getAttributeValue(new QName("novalidatecert"));
                    String clientID = "";
                    if (client.getFirstChildWithName(new QName("clientID")) != null) {
                        clientID = client.getFirstChildWithName(new QName("clientID")).getText();
                    }
                    boolean novalidatecert = "true".equals(noValCert);

                    SSLContext sslContext = null;
                    try {
                        sslContext = createSSLContext(ksElt, trElt, novalidatecert, secretResolver);
                    } catch (AxisFault axisFault) {
                        String err = "Error occurred while creating SSL context for the servers " + serversElt.getText();
                        // This runtime exception stop the server startup But it will not affect for dynamic change
                        throw new InvalidConfigurationException(err, axisFault);
                    }
                    for (String server : servers) {
                        server = server.trim();
                        if (!contextMap.containsKey(server)) {
                            contextMap.put(new RequestDescriptor(server, clientID), sslContext);
                            if (log.isDebugEnabled()) {
                                log.debug("Update the SSL context map for the server: " + server);
                            }
                        } else {
                            if (log.isWarnEnabled()) {
                                log.warn(name + " Multiple SSL profiles were found for the server : " +
                                        server + ". Ignoring the excessive profiles.");
                            }
                        }
                    }

                }
            } else {
                OMElement ksElt = profile.getFirstChildWithName(new QName("KeyStore"));
                OMElement trElt = profile.getFirstChildWithName(new QName("TrustStore"));
                String noValCert = profile.getAttributeValue(new QName("novalidatecert"));
                boolean novalidatecert = "true".equals(noValCert);

                SSLContext sslContext = null;
                try {
                    sslContext = createSSLContext(ksElt, trElt, novalidatecert, secretResolver);
                } catch (AxisFault axisFault) {
                    String err = "Error occurred while creating SSL context for the servers " + serversElt.getText();
                    // This runtime exception stop the server startup But it will not affect for dynamic change
                    throw new InvalidConfigurationException(err, axisFault);
                }
                for (String server : servers) {
                    server = server.trim();
                    if (!contextMap.containsKey(server)) {
                        contextMap.put(new RequestDescriptor(server, ""), sslContext);
                        if (log.isDebugEnabled()) {
                            log.debug("Update the SSL context map for the server: " + server);
                        }
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn(name + " Multiple SSL profiles were found for the server : " +
                                    server + ". Ignoring the excessive profiles.");
                        }
                    }
                }
            }
        }

        if (contextMap.size() > 0) {
            if (log.isInfoEnabled()) {
                log.info(name + " Custom SSL profiles initialized for " + contextMap.size() +
                        " mappings");
            }
            return contextMap;
        }
        return null;
    }

    private SSLContext createSSLContext(OMElement keyStoreElt, OMElement trustStoreElt,
                                        boolean novalidatecert) throws AxisFault {

        KeyManager[] keymanagers = null;
        TrustManager[] trustManagers = null;
        SecretResolver resolver;
        String jceProvider = getPreferredJceProvider();
        if (configurationContext != null && configurationContext.getAxisConfiguration() != null) {
            resolver = configurationContext.getAxisConfiguration().getSecretResolver();
        } else {
            resolver = SecretResolverFactory.create(keyStoreElt, false);
        }

        if (keyStoreElt != null) {
            String location = keyStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type = keyStoreElt.getFirstChildWithName(new QName("Type")).getText();
            OMElement passwordElement = keyStoreElt.getFirstChildWithName(new QName("Password"));
            OMElement keyPasswordElement = keyStoreElt.getFirstChildWithName(new QName("KeyPassword"));
            if (passwordElement == null) {
                throw new AxisFault("Cannot proceed because Password element is missing in KeyStore");
            }
            if (keyPasswordElement == null) {
                throw new AxisFault("Cannot proceed because KeyPassword element is missing in KeyStore");
            }
            String  storePassword = SecureVaultValueReader.getSecureVaultValue(resolver, passwordElement);
            String keyPassword = SecureVaultValueReader.getSecureVaultValue(resolver, keyPasswordElement);
            FileInputStream fis = null;
            try {
                KeyStore keyStore;
                if (jceProvider != null) {
                    keyStore = KeyStore.getInstance(type, jceProvider);
                } else {
                    keyStore = KeyStore.getInstance(type);
                }
                fis = new FileInputStream(location);
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Identity Keystore from : " + location);
                }

                keyStore.load(fis, storePassword.toCharArray());
                KeyManagerFactory kmfactory;
                if (jceProvider != null) {
                    kmfactory = KeyManagerFactory.getInstance(PKIX, BCJSSE);
                } else {
                    kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                }
                kmfactory.init(keyStore, keyPassword.toCharArray());
                keymanagers = kmfactory.getKeyManagers();

            } catch (GeneralSecurityException gse) {
                log.error(name + " Error loading Keystore : " + location, gse);
                throw new AxisFault("Error loading Keystore : " + location, gse);
            } catch (IOException ioe) {
                log.error(name + " Error opening Keystore : " + location, ioe);
                throw new AxisFault("Error opening Keystore : " + location, ioe);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        if (trustStoreElt != null) {
            if (novalidatecert && log.isWarnEnabled()) {
                log.warn(name + " Ignoring novalidatecert parameter since a truststore has been specified");
            }

            String location = trustStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type = trustStoreElt.getFirstChildWithName(new QName("Type")).getText();
            OMElement passwordElement = trustStoreElt.getFirstChildWithName(new QName("Password"));
            if (passwordElement == null) {
                throw new AxisFault("Cannot proceed because Password element is missing in TrustStore");
            }
            String storePassword = SecureVaultValueReader.getSecureVaultValue(resolver, passwordElement);
            FileInputStream fis = null;
            try {
                KeyStore trustStore;
                if (jceProvider != null) {
                    trustStore = KeyStore.getInstance(type, jceProvider);
                } else {
                    trustStore = KeyStore.getInstance(type);
                }
                fis = new FileInputStream(location);
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Trust Keystore from : " + location);
                }

                trustStore.load(fis, storePassword.toCharArray());
                TrustManagerFactory trustManagerfactory;
                if (jceProvider != null) {
                    trustManagerfactory = TrustManagerFactory.getInstance(PKIX, BCJSSE);
                } else {
                    trustManagerfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                }
                trustManagerfactory.init(trustStore);
                trustManagers = trustManagerfactory.getTrustManagers();

                SslSenderTrustStoreHolder sslSenderTrustStoreHolder = SslSenderTrustStoreHolder.getInstance();
                sslSenderTrustStoreHolder.setKeyStore(trustStore);
                sslSenderTrustStoreHolder.setLocation(location);
                sslSenderTrustStoreHolder.setPassword(storePassword);
                sslSenderTrustStoreHolder.setType(type);
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
                    } catch (IOException ignore) {
                    }
                }
            }
        } else if (novalidatecert) {
            if (log.isWarnEnabled()) {
                log.warn(name + " Server certificate validation (trust) has been disabled. " +
                        "DO NOT USE IN PRODUCTION!");
            }
            trustManagers = new TrustManager[]{new NoValidateCertTrustManager()};
        }

        try {
            final Parameter sslpParameter = transportOut.getParameter("SSLProtocol");
            final String sslProtocol = sslpParameter != null ? sslpParameter.getValue().toString() : TLS;
            SSLContext sslcontext;
            if (jceProvider != null) {
                sslcontext = SSLContext.getInstance(sslProtocol, BCJSSE);
            } else {
                sslcontext = SSLContext.getInstance(sslProtocol);
            }
            sslcontext.init(keymanagers, trustManagers, null);
            return sslcontext;

        } catch (GeneralSecurityException gse) {
            log.error(name + " Unable to create SSL context with the given configuration", gse);
            throw new AxisFault("Unable to create SSL context with the given configuration", gse);
        }
    }

    private SSLContext createSSLContext(OMElement keyStoreElt, OMElement trustStoreElt,
                                        boolean novalidatecert, SecretResolver secretResolver) throws AxisFault {

        KeyManager[] keymanagers = null;
        TrustManager[] trustManagers = null;
        String jceProvider = getPreferredJceProvider();

        if (keyStoreElt != null) {
            String location = keyStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type = keyStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = SecureVaultValueReader.getSecureVaultValue(secretResolver,
                    keyStoreElt.getFirstChildWithName(new QName("Password")));
            String keyPassword = SecureVaultValueReader.getSecureVaultValue(secretResolver,
                    keyStoreElt.getFirstChildWithName(new QName("KeyPassword")));

            try (FileInputStream fis = new FileInputStream(location)) {
                KeyStore keyStore;
                if (jceProvider != null) {
                    keyStore = KeyStore.getInstance(type, jceProvider);
                } else {
                    keyStore = KeyStore.getInstance(type);
                }
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Identity Keystore from : " + location);
                }

                keyStore.load(fis, storePassword.toCharArray());
                KeyManagerFactory kmfactory;
                if (jceProvider != null) {
                    kmfactory = KeyManagerFactory.getInstance(PKIX, BCJSSE);
                } else {
                    kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                }
                kmfactory.init(keyStore, keyPassword.toCharArray());
                keymanagers = kmfactory.getKeyManagers();

            } catch (GeneralSecurityException gse) {
                log.error(name + " Error loading Keystore : " + location, gse);
                throw new AxisFault("Error loading Keystore : " + location, gse);
            } catch (IOException ioe) {
                log.error(name + " Error opening Keystore : " + location, ioe);
                throw new AxisFault("Error opening Keystore : " + location, ioe);
            } 
        }

        if (trustStoreElt != null) {
            if (novalidatecert && log.isWarnEnabled()) {
                log.warn(name + " Ignoring novalidatecert parameter since a truststore has been specified");
            }

            String location = trustStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type = trustStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = SecureVaultValueReader
                    .getSecureVaultValue(secretResolver, trustStoreElt.getFirstChildWithName(new QName("Password")));
            try (FileInputStream fis = new FileInputStream(location)) {
                KeyStore trustStore;
                if (jceProvider != null) {
                    trustStore = KeyStore.getInstance(type, jceProvider);
                } else {
                    trustStore = KeyStore.getInstance(type);
                }
        
                if (log.isDebugEnabled()) {
                    log.debug(name + " Loading Trust Keystore from : " + location);
                }

                trustStore.load(fis, storePassword.toCharArray());
                TrustManagerFactory trustManagerfactory;
                if (jceProvider != null) {
                    trustManagerfactory = TrustManagerFactory.getInstance(PKIX, BCJSSE);
                } else {
                    trustManagerfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                }
                trustManagerfactory.init(trustStore);
                trustManagers = trustManagerfactory.getTrustManagers();

                SslSenderTrustStoreHolder sslSenderTrustStoreHolder = SslSenderTrustStoreHolder.getInstance();
                sslSenderTrustStoreHolder.setKeyStore(trustStore);
                sslSenderTrustStoreHolder.setLocation(location);
                sslSenderTrustStoreHolder.setPassword(storePassword);


            } catch (GeneralSecurityException gse) {
                log.error(name + " Error loading Key store : " + location, gse);
                throw new AxisFault("Error loading Key store : " + location, gse);
            } catch (IOException ioe) {
                log.error(name + " Error opening Key store : " + location, ioe);
                throw new AxisFault("Error opening Key store : " + location, ioe);
            }
        } else if (novalidatecert) {
            if (log.isWarnEnabled()) {
                log.warn(name + " Server certificate validation (trust) has been disabled. " +
                        "DO NOT USE IN PRODUCTION!");
            }
            trustManagers = new TrustManager[]{new NoValidateCertTrustManager()};
        }

        try {
            final Parameter sslpParameter = transportOut.getParameter("SSLProtocol");
            final String sslProtocol = sslpParameter != null ? sslpParameter.getValue().toString() : TLS;
            SSLContext sslcontext;
            if (jceProvider != null) {
                sslcontext = SSLContext.getInstance(sslProtocol,  BCJSSE);
            } else {
                sslcontext = SSLContext.getInstance(sslProtocol);
            }
            sslcontext.init(keymanagers, trustManagers, null);
            return sslcontext;

        } catch (GeneralSecurityException gse) {
            log.error(name + " Unable to create SSL context with the given configuration", gse);
            throw new AxisFault("Unable to create SSL context with the given configuration", gse);
        }
    }

    public ClientConnFactory createConnFactory(final HttpParams params) {
        if (ssl != null) {
            return new ClientConnFactory(ssl, sslByHostMap, params);
        } else {
            return new ClientConnFactory(params);
        }
    }

    /**
     * Extracts Dynamic SSL profiles configuration from given TransportOut Configuration
     *
     * @param transportOut TransportOut Configuration of the connection
     * @return TransportOut configuration with extracted Dynamic SSL profiles information
     */
    public TransportOutDescription loadDynamicSSLConfig(TransportOutDescription transportOut) {
        Parameter profilePathParam = transportOut.getParameter("dynamicSSLProfilesConfig");
        //No Separate configuration file configured. Therefore using Axis2 Configuration
        if (profilePathParam == null) {
            return transportOut;
        }

        //Using separate SSL Profile configuration file, ignore Axis2 configurations
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
                profileParam.setName("customSSLProfiles");
                profileParam.setValue(profileEl);

                transportOut.addParameter(profileParam);
                log.info("customSSLProfiles configuration is loaded from path: " + fullPath);

                return transportOut;
            }
        } catch (XMLStreamException xmlEx) {
            log.error("XMLStreamException - Could not load customSSLProfiles from file path: " + path, xmlEx);
        } catch (FileNotFoundException fileEx) {
            log.error("FileNotFoundException - Could not load customSSLProfiles from file path: " + path, fileEx);
        } catch (AxisFault axisFault) {
            log.error("AxisFault - Could not load customSSLProfiles from file path: " + path, axisFault);
        } catch (Exception ex) {
            log.error("Exception - Could not load customSSLProfiles from file path: " + path, ex);
        }
        return null;
    }

    public Map<RequestDescriptor, SSLContext> getSslByHostMap() {
        return sslByHostMap;
    }

    public SSLContextDetails getSSLContextDetails() {
        return ssl;
    }

    /**
     * Get the preferred JCE provider.
     *
     * @return the preferred JCE provider
     */
    public static String getPreferredJceProvider() {
        String provider = System.getProperty(SECURITY_JCE_PROVIDER);
        if (provider != null && (provider.equalsIgnoreCase(BOUNCY_CASTLE_FIPS_PROVIDER) ||
                provider.equalsIgnoreCase(BOUNCY_CASTLE_PROVIDER))) {
            return provider;
        }
        return null;
    }
}

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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.certificatevalidation.RevocationVerificationManager;
import org.apache.synapse.transport.http.conn.ClientConnFactory;
import org.apache.synapse.transport.http.conn.ClientSSLSetupHandler;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.nhttp.NoValidateCertTrustManager;

public class ClientConnFactoryBuilder {

    private static final Log log = LogFactory.getLog(ClientConnFactoryBuilder.class);

    private final TransportOutDescription transportOut;
    private final String name;

    private SSLContextDetails ssl = null;
    private Map<String, SSLContext> sslByHostMap = null;
    
    public ClientConnFactoryBuilder(final TransportOutDescription transportOut) {
        super();
        this.transportOut = transportOut;
        this.name = transportOut.getName().toUpperCase(Locale.US);
    }
    
    public ClientConnFactoryBuilder parseSSL() throws AxisFault {
        Parameter keyParam    = transportOut.getParameter("keystore");
        Parameter trustParam  = transportOut.getParameter("truststore");
        Parameter httpsProtocolsParam = transportOut.getParameter("HttpsProtocols");

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
        RevocationVerificationManager revocationVerifier = null;

        if ("true".equalsIgnoreCase(cvEnable)) {
            String cacheSizeString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheSize")).getText();
            String cacheDelayString = cvp.getParameterElement().getFirstChildWithName(new QName("CacheDelay")).getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            } catch (NumberFormatException e) {
            }
            revocationVerifier = new RevocationVerificationManager(cacheSize, cacheDelay);
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
        ClientSSLSetupHandler clientSSLSetupHandler = new ClientSSLSetupHandler(hostnameVerifier, revocationVerifier);

        if (null != httpsProtocols) {
            clientSSLSetupHandler.setHttpsProtocols(httpsProtocols);
        }

        ssl = new SSLContextDetails(sslContext, clientSSLSetupHandler);
        sslByHostMap = getCustomSSLContexts(transportOut);
        return this;
    }

    /**
     * Looks for a transport parameter named customSSLProfiles and initializes zero or more
     * custom SSLContext instances. The syntax for defining custom SSL profiles is as follows.
     *
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
     *
     * Any number of profiles can be defined under the customSSLProfiles parameter.
     *
     * @param transportOut transport out description
     * @return a map of server addresses and SSL contexts
     * @throws AxisFault if at least on SSL profile is not properly configured
     */
    private Map<String, SSLContext> getCustomSSLContexts(TransportOutDescription transportOut)
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
        Iterator<?> profiles = customProfilesElt.getChildrenWithName(new QName("profile"));
        Map<String, SSLContext> contextMap = new HashMap<String, SSLContext>();

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
            OMElement ksElt = profile.getFirstChildWithName(new QName("KeyStore"));
            OMElement trElt = profile.getFirstChildWithName(new QName("TrustStore"));
            String noValCert = profile.getAttributeValue(new QName("novalidatecert"));
            boolean novalidatecert = "true".equals(noValCert);
            SSLContext sslContext = createSSLContext(ksElt, trElt, novalidatecert);

            for (String server : servers) {
                server = server.trim();
                if (!contextMap.containsKey(server)) {
                    contextMap.put(server, sslContext);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn(name + " Multiple SSL profiles were found for the server : " +
                                 server + ". Ignoring the excessive profiles.");
                    }
                }
            }
        }

        if (contextMap.size() > 0) {
            if (log.isInfoEnabled()) {
                log.info(name + " Custom SSL profiles initialized for " + contextMap.size() +
                         " servers");
            }
            return contextMap;
        }
        return null;
    }

    private SSLContext createSSLContext(OMElement keyStoreElt, OMElement trustStoreElt,
                                        boolean novalidatecert) throws AxisFault {

        KeyManager[] keymanagers  = null;
        TrustManager[] trustManagers = null;


        if (keyStoreElt != null) {
            String location      = keyStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type          = keyStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = keyStoreElt.getFirstChildWithName(new QName("Password")).getText();
            String keyPassword   = keyStoreElt.getFirstChildWithName(new QName("KeyPassword")).getText();

            FileInputStream fis = null;
            try {
                KeyStore keyStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                if (log.isInfoEnabled()) {
                    log.info(name + " Loading Identity Keystore from : " + location);
                }

                keyStore.load(fis, storePassword.toCharArray());
                KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
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
                    } catch (IOException ignore) {}
                }
            }
        }

        if (trustStoreElt != null) {
            if (novalidatecert && log.isWarnEnabled()) {
                log.warn(name + " Ignoring novalidatecert parameter since a truststore has been specified");
            }

            String location      = trustStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type          = trustStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = trustStoreElt.getFirstChildWithName(new QName("Password")).getText();

            FileInputStream fis = null;
            try {
                KeyStore trustStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                if (log.isInfoEnabled()) {
                    log.info(name + " Loading Trust Keystore from : " + location);
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
        } else if (novalidatecert) {
            if (log.isWarnEnabled()) {
                log.warn(name + " Server certificate validation (trust) has been disabled. " +
                    "DO NOT USE IN PRODUCTION!");
            }            
            trustManagers = new TrustManager[] { new NoValidateCertTrustManager() };
        }

        try {
            final Parameter sslpParameter = transportOut.getParameter("SSLProtocol");
            final String sslProtocol = sslpParameter != null ? sslpParameter.getValue().toString() : "TLS";
            SSLContext sslcontext = SSLContext.getInstance(sslProtocol);
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
    public TransportOutDescription loadDynamicSSLConfig (TransportOutDescription transportOut) {
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
}

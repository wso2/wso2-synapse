/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty.sender;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.TargetConfiguration;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.wso2.securevault.SecretResolver;
import org.wso2.transport.http.netty.contract.config.SslConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

/**
 * Builder class for the Client SSLConfiguration.
 */
public class ClientSSLConfigurationBuilder {

    private static final Log LOG = LogFactory.getLog(ClientSSLConfigurationBuilder.class);

    private String keyStore;
    private String keyStorePass;
    private String certPass;
    private String trustStore;
    private String trustStorePass;
    private String tlsStoreType;
    private String sslProtocol = BridgeConstants.TLS_PROTOCOL;
    private boolean validateCertEnabled;
    private int cacheValidityPeriod;
    private int cacheSize;
    private int sessionTimeOut;
    private long handshakeTimeOut;
    private boolean disableCertValidation;
    private boolean hostnameVerifier = false;
    List<org.wso2.transport.http.netty.contract.config.Parameter> clientParamList = new ArrayList<>();

    /**
     * Populate the SSL configuration values in the given client SslConfiguration.
     *
     * @param sslConfiguration instance that represents the client's ssl configuration
     */
    public void setClientSSLConfig(SslConfiguration sslConfiguration) {

        sslConfiguration.setKeyStoreFile(keyStore);
        sslConfiguration.setKeyStorePass(keyStorePass);
        sslConfiguration.setCertPass(certPass);
        sslConfiguration.setTLSStoreType(tlsStoreType);
        if (disableCertValidation) {
            sslConfiguration.disableSsl();
            return;
        }
        sslConfiguration.setTrustStoreFile(trustStore);
        sslConfiguration.setTrustStorePass(trustStorePass);
        sslConfiguration.setSSLProtocol(sslProtocol);
        if (validateCertEnabled) {
            sslConfiguration.setValidateCertEnabled(true);
            if (cacheValidityPeriod > 0) {
                sslConfiguration.setCacheValidityPeriod(cacheValidityPeriod);
            }
            if (cacheSize > 0) {
                sslConfiguration.setCacheSize(cacheSize);
            }
        }
        sslConfiguration.setSslSessionTimeOut(sessionTimeOut);
        sslConfiguration.setSslHandshakeTimeOut(handshakeTimeOut);
        sslConfiguration.setParameters(clientParamList);
        sslConfiguration.setHostNameVerificationEnabled(hostnameVerifier);
    }

    /**
     * Parse SSL configuration from axis2.xml.
     *
     * @param targetConfiguration the configuration of the sender
     * @param transportOut        TransportOutDescription of the configuration
     */
    public ClientSSLConfigurationBuilder parseSSL(TargetConfiguration targetConfiguration,
                                                  TransportOutDescription transportOut) throws AxisFault {

        SecretResolver secretResolver = targetConfiguration.getConfigurationContext()
                .getAxisConfiguration().getSecretResolver();

        // Populate KeyStore configs
        Parameter keyParam = transportOut.getParameter(BridgeConstants.KEY_STORE);
        populateKeyStoreConfigs(keyParam, secretResolver);

        // Populate Truststore configs
        Parameter trustParam = transportOut.getParameter(BridgeConstants.TRUST_STORE);
        boolean novalidateCert = ParamUtils.getOptionalParamBoolean(transportOut, BridgeConstants.NO_VALIDATE_CERT,
                false);

        if (isCertValidationDisabled(novalidateCert, trustParam)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Server certificate validation (trust) has been disabled.");
            }
            disableCertValidation = true;
            return this;
        }
        populateTrustStoreConfigs(trustParam, secretResolver, novalidateCert);

        // Populate HttpsProtocols and SSLProtocol configs
        Parameter httpsProtocolsParam =
                transportOut.getParameter(BridgeConstants.HTTPS_PROTOCOL);
        Parameter sslProtocolParam =
                transportOut.getParameter(BridgeConstants.SSL_PROTOCOL);
        populateProtocolConfigs(sslProtocolParam, httpsProtocolsParam);

        // Populate certificate validation configs
        Parameter cvp = transportOut.getParameter(BridgeConstants.CLIENT_REVOCATION);
        populateCertValidationConfigs(cvp);

        // Populate preferred ciphers configs
        Parameter preferredCiphersParam =
                transportOut.getParameter(BridgeConstants.PREFERRED_CIPHERS);
        populateCiphersConfigs(preferredCiphersParam);

        // Populate session and handshake timeout configs
        Parameter sessionTimeoutParam =
                transportOut.getParameter(BridgeConstants.SSL_SESSION_TIMEOUT);
        Parameter handshakeTimeoutParam =
                transportOut.getParameter(BridgeConstants.SSL_HANDSHAKE_TIMEOUT);
        populateTimeoutConfigs(sessionTimeoutParam, handshakeTimeoutParam);

        //TODO: populateHostnameVerifierConfigs with Strict, AllowAll, etc.
        Parameter hostnameVerifierParam = transportOut.getParameter(BridgeConstants.HOSTNAME_VERIFIER);
        populateHostnameVerifierConfigs(hostnameVerifierParam);
        return this;
    }

    private boolean isCertValidationDisabled(boolean novalidateCert,
                                             Parameter trustParam) {

        if (trustParam != null && trustParam.getParameterElement().getFirstElement() != null) {
            return false;
        }
        return novalidateCert;
    }

    private void populateKeyStoreConfigs(Parameter keyParam,
                                         SecretResolver secretResolver) throws AxisFault {

        OMElement keyStoreElt = null;
        if (keyParam != null) {
            keyStoreElt = keyParam.getParameterElement().getFirstElement();
        }
        if (keyStoreElt == null) {
            throw new AxisFault("KeyStore must be provided for secure connection");
        }
        OMElement keyStoreLocationElement = keyStoreElt
                .getFirstChildWithName(new QName(BridgeConstants.STORE_LOCATION));
        OMElement tlsStoreTypeElement = keyStoreElt.getFirstChildWithName(new QName(BridgeConstants.TYPE));
        OMElement passwordElement = keyStoreElt.getFirstChildWithName(new QName(BridgeConstants.PASSWORD));
        OMElement keyPasswordElement = keyStoreElt.getFirstChildWithName(new QName(BridgeConstants.KEY_PASSWORD));

        if (Objects.nonNull(keyStoreLocationElement)) {
            keyStore = keyStoreLocationElement.getText();
        }
        if (Objects.isNull(keyStore) || keyStore.isEmpty()) {
            throw new AxisFault("KeyStore file location must be provided for secure connection");
        }
        if (Objects.nonNull(tlsStoreTypeElement)) {
            tlsStoreType = tlsStoreTypeElement.getText();
        }
        if (passwordElement == null) {
            throw new AxisFault("Cannot proceed because Password element is missing in KeyStore");
        }
        if (keyPasswordElement == null) {
            throw new AxisFault("Cannot proceed because KeyPassword element is missing in KeyStore");
        }
        keyStorePass = SecureVaultValueReader.getSecureVaultValue(secretResolver, passwordElement);
        certPass = SecureVaultValueReader.getSecureVaultValue(secretResolver, keyPasswordElement);
    }

    private void populateTrustStoreConfigs(Parameter trustParam,
                                           SecretResolver secretResolver, boolean novalidateCert) throws AxisFault {

        OMElement trustStoreElt = null;
        if (trustParam != null) {
            trustStoreElt = trustParam.getParameterElement().getFirstElement();
        }
        if (trustStoreElt == null) {
            throw new AxisFault("If server certification validation (novalidatecert) parameter is not configured to "
                    + "true, Truststore should be specified for secure connection");
        }
        if (novalidateCert) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Ignoring novalidatecert parameter since a truststore has been specified");
            }
        }
        trustStore = trustStoreElt.getFirstChildWithName(new QName(BridgeConstants.STORE_LOCATION)).getText();
        String type = trustStoreElt.getFirstChildWithName(new QName(BridgeConstants.TYPE)).getText();
        OMElement passwordElement = trustStoreElt.getFirstChildWithName(new QName(BridgeConstants.PASSWORD));
        if (Objects.isNull(passwordElement)) {
            throw new AxisFault("Cannot proceed because Password element is missing in TrustStore");
        }
        trustStorePass = SecureVaultValueReader.getSecureVaultValue(secretResolver, passwordElement);
    }

    private void populateProtocolConfigs(Parameter sslProtocolParam,
                                         Parameter httpsProtocolsParam) {

        if (Objects.nonNull(sslProtocolParam) && !sslProtocolParam.getValue().toString().isEmpty()) {
            this.sslProtocol = sslProtocolParam.getValue().toString();
        }

        if (Objects.isNull(httpsProtocolsParam) || Objects.isNull(httpsProtocolsParam.getParameterElement())) {
            return;
        }

        String configuredHttpsProtocols = httpsProtocolsParam.getParameterElement().getText().replaceAll("\\s", "");

        if (!configuredHttpsProtocols.isEmpty()) {
            org.wso2.transport.http.netty.contract.config.Parameter serverProtocols
                    = new org.wso2.transport.http.netty.contract.config.Parameter(BridgeConstants.SSL_ENABLED_PROTOCOLS,
                    configuredHttpsProtocols);
            clientParamList.add(serverProtocols);
        }
    }

    private void populateCertValidationConfigs(Parameter cvp) {

        final String cvEnable = cvp != null ?
                cvp.getParameterElement().getAttribute(new QName("enable")).getAttributeValue() : null;

        if (BridgeConstants.VALUE_TRUE.equalsIgnoreCase(cvEnable)) {
            validateCertEnabled = true;
            String cacheSizeString = cvp.getParameterElement().getFirstChildWithName(
                    new QName(BridgeConstants.CACHE_SIZE)).getText();
            String cacheDelayString = cvp.getParameterElement().getFirstChildWithName(
                    new QName(BridgeConstants.CACHE_DELAY)).getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            } catch (NumberFormatException e) {
                // do nothing
            }

            if (Objects.nonNull(cacheDelay) && cacheDelay != 0) {
                this.cacheValidityPeriod = Math.toIntExact(cacheDelay);
            }
            if (Objects.nonNull(cacheSize) && cacheSize != 0) {
                this.cacheSize = Math.toIntExact(cacheSize);
            }
        }
    }

    private void populateCiphersConfigs(Parameter preferredCiphersParam) {

        if (Objects.isNull(preferredCiphersParam) || Objects.isNull(preferredCiphersParam.getParameterElement())) {
            return;
        }

        String preferredCiphers = preferredCiphersParam.getParameterElement().getText().replaceAll("\\s", "");
        if (!preferredCiphers.isEmpty()) {
            org.wso2.transport.http.netty.contract.config.Parameter serverParameters =
                    new org.wso2.transport.http.netty.contract.config.Parameter(
                            BridgeConstants.CIPHERS, preferredCiphers);
            clientParamList.add(serverParameters);
        }
    }

    private void populateTimeoutConfigs(Parameter sessionTimeoutParam,
                                        Parameter handshakeTimeoutParam) {

        if (Objects.nonNull(sessionTimeoutParam) && Objects.nonNull(sessionTimeoutParam.getParameterElement())) {
            String sessionTimeoutStr = sessionTimeoutParam.getParameterElement().getText();
            try {
                int sessionTimeout = Integer.parseInt(sessionTimeoutStr);
                if (sessionTimeout > 0) {
                    this.sessionTimeOut = sessionTimeout;
                } else {
                    LOG.warn("SessionTimeout should be a valid positive number. But found : " + sessionTimeoutStr
                            + ". Hence, using the default value of 86400s/24h");
                }
            } catch (NumberFormatException e) {
                LOG.warn("Invalid number found for SSL SessionTimeout : " + sessionTimeoutStr
                        + ". Hence, using the default value of 86400s/24h");
            }
        }

        if (Objects.nonNull(handshakeTimeoutParam) && Objects.nonNull(handshakeTimeoutParam.getParameterElement())) {
            String handshakeTimeoutStr = handshakeTimeoutParam.getParameterElement().getText();
            try {
                int handshakeTimeOut = Integer.parseInt(handshakeTimeoutStr);
                if (handshakeTimeOut > 0) {
                    this.handshakeTimeOut = handshakeTimeOut;
                } else {
                    LOG.warn("HandshakeTimeout should be a valid positive number. But found : " + handshakeTimeoutStr
                            + ". Hence, using the default value of 10s");
                }
            } catch (NumberFormatException e) {
                LOG.warn("Invalid number found for ssl handshakeTimeout : " + handshakeTimeoutStr +
                        ". Hence, using the default value of 10s");
            }
        }
    }

    private void populateHostnameVerifierConfigs(Parameter hostnameVerifierParam) {

        if (Objects.nonNull(hostnameVerifierParam) && Objects.nonNull(hostnameVerifierParam.getParameterElement())) {
            String hostnameVerifierStr = hostnameVerifierParam.getParameterElement().getText();
            if ("Enabled".equalsIgnoreCase(hostnameVerifierStr)) {
                this.hostnameVerifier = true;
            }
        }
    }
}

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
package org.apache.synapse.transport.certificatevalidation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.certificatevalidation.cache.CertCache;
import org.apache.synapse.transport.certificatevalidation.crl.CRLCache;
import org.apache.synapse.transport.certificatevalidation.crl.CRLVerifier;
import org.apache.synapse.transport.certificatevalidation.ocsp.OCSPCache;
import org.apache.synapse.transport.certificatevalidation.ocsp.OCSPVerifier;
import org.apache.synapse.transport.certificatevalidation.pathvalidation.CertificatePathValidator;
import org.apache.synapse.transport.nhttp.config.TrustStoreHolder;

import java.security.cert.Certificate;
import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;

/**
 * Manager class responsible for verifying certificates. This class will use the available verifiers according to
 * a predefined policy.
 */
public class CertificateVerificationManager {

    private int cacheSize = Constants.CACHE_DEFAULT_ALLOCATED_SIZE;
    private int cacheDelayMins = Constants.CACHE_DEFAULT_DELAY_MINS;
    private boolean isFullCertChainValidationEnabled = true;
    private boolean isCertExpiryValidationEnabled = false;
    private static final Log log = LogFactory.getLog(CertificateVerificationManager.class);
    private static final String BOUNCY_CASTLE_FIPS_PROVIDER = "BCFIPS";
    private static final String SECURITY_JCE_PROVIDER = "security.jce.provider";
    private static final String BOUNCY_CASTLE_PROVIDER = "BC";

    public CertificateVerificationManager(Integer cacheAllocatedSize, Integer cacheDelayMins) {

        this(cacheAllocatedSize, cacheDelayMins, true, false);
    }

    public CertificateVerificationManager(Integer cacheAllocatedSize, Integer cacheDelayMins,
                                          boolean isFullCertChainValidationEnabled,
                                          boolean isCertExpiryValidationEnabled) {

        if (cacheAllocatedSize != null && cacheAllocatedSize > Constants.CACHE_MIN_ALLOCATED_SIZE
                && cacheAllocatedSize < Constants.CACHE_MAX_ALLOCATED_SIZE) {
            this.cacheSize = cacheAllocatedSize;
        } else {
            log.warn("The cache size is out of range. Hence, using the default cache size value of "
                    + Constants.CACHE_DEFAULT_ALLOCATED_SIZE + ".");
        }
        if (cacheDelayMins != null && cacheDelayMins > Constants.CACHE_MIN_DELAY_MINS
                && cacheDelayMins < Constants.CACHE_MAX_DELAY_MINS) {
            this.cacheDelayMins = cacheDelayMins;
        } else {
            log.warn("The cache delay is out of range. Hence, using the default cache delay value of "
                    + Constants.CACHE_DEFAULT_DELAY_MINS + ".");
        }

        this.isFullCertChainValidationEnabled = isFullCertChainValidationEnabled;
        this.isCertExpiryValidationEnabled = isCertExpiryValidationEnabled;
    }

    /**
     * This method verifies the given certificate chain or given peer certificate for revocation based on the
     * requirement of full certificate chain validation. If full chain validation is enabled (default),
     * the full certificate chain will be validated before checking the chain for revocation. If full chain validation
     * is disabled, this method expects a single peer certificate, and it is validated with the immediate issuer
     * certificate in the truststore (The truststore must contain the immediate issuer of the peer certificate).
     * In both cases, OCSP and CRL verifiers are used for revocation verification.
     * It first tries to verify using OCSP since OCSP verification is faster. If that fails it tries to do the
     * verification using CRL.
     *
     * @param peerCertificates  java.security.cert.Certificate[] array of peer certificate chain from peer/client.
     * @throws CertificateVerificationException
     */
    public void verifyCertificateValidity(Certificate[] peerCertificates)
            throws CertificateVerificationException {

        X509Certificate[] convertedCertificates = convert(peerCertificates);

        X509Certificate peerCert = null;
        X509Certificate issuerCert = null;

        if (!isFullCertChainValidationEnabled) {
            peerCert = getPeerCertificate(convertedCertificates);
            issuerCert = getVerifiedIssuerCertOfPeerCert(peerCert, CertCache.getCache());
        }

        OCSPCache ocspCache = OCSPCache.getCache(cacheSize, cacheDelayMins);
        CRLCache crlCache = CRLCache.getCache(cacheSize, cacheDelayMins);

        RevocationVerifier[] verifiers = {new OCSPVerifier(ocspCache), new CRLVerifier(crlCache)};
        RevocationStatus revocationStatus = null;

        for (RevocationVerifier verifier : verifiers) {
            try {
                if (isFullCertChainValidationEnabled) {

                    if (isCertExpiryValidationEnabled) {
                        log.debug("Validating certificate chain for expiry");
                        if (isExpired(convertedCertificates)) {
                            throw new CertificateVerificationException("One of the provided certificates are expired");
                        }
                    }

                    log.debug("Doing full certificate chain validation");
                    CertificatePathValidator pathValidator = new CertificatePathValidator(convertedCertificates,
                            verifier);
                    pathValidator.validatePath();
                    return;
                } else {
                    if (isCertExpiryValidationEnabled) {
                        log.debug("Validating the client certificate for expiry");
                        if (isExpired(convertedCertificates)) {
                            throw new CertificateVerificationException("The provided certificate is expired");
                        }
                    }

                    log.debug("Validating client certificate with the issuer certificate retrieved from" +
                            "the trust store");
                    revocationStatus = verifier.checkRevocationStatus(peerCert, issuerCert);
                    if (RevocationStatus.GOOD.toString().equals(revocationStatus.toString())) {
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("Certificate verification with " + verifier.getClass().getSimpleName() + " failed. ", e);
            }
        }
        throw new CertificateVerificationException("Path Verification Failed for both OCSP and CRL");
    }

    /**
     * @param certs array of java.security.cert.Certificate[] s.
     * @return the converted array of java.security.cert.X509Certificate[] s.
     * @throws CertificateVerificationException
     */
    private X509Certificate[] convert(Certificate[] certs)
            throws CertificateVerificationException {
        X509Certificate[] certChain = new X509Certificate[certs.length];
        Throwable exceptionThrown;
        String provider = getPreferredJceProvider();
        for (int i = 0; i < certs.length; i++) {
            try {
                byte[] encoded = certs[i].getEncoded();
                ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
                java.security.cert.CertificateFactory cf;
                if (provider != null) {
                    cf = java.security.cert.CertificateFactory.getInstance("X.509", provider);
                } else {
                    cf = java.security.cert.CertificateFactory.getInstance("X.509");
                }
                certChain[i]=((X509Certificate)cf.generateCertificate(bis));
                continue;
            } catch (CertificateException e) {
                exceptionThrown = e;
            } catch (NoSuchProviderException e) {
                throw new CertificateVerificationException("Specified security provider is not available in " +
                        "this environment: ", e);
            }
            throw new CertificateVerificationException("Cant Convert certificates from javax to java", exceptionThrown);
        }
        return certChain;
    }

    /**
     * Checks whether a provided certificate is expired or not at the time it is validated.
     *
     * @param certificates certificates to be validated for expiry
     * @return true if one of the certs are expired, false otherwise
     */
    public boolean isExpired(X509Certificate[] certificates) {

        for (X509Certificate cert : certificates) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                log.error("Peer certificate is expired", e);
                return true;
            } catch (CertificateNotYetValidException e) {
                log.error("Peer certificate is not valid yet", e);
                return true;
            }
        }
        return false;
    }

    public X509Certificate getPeerCertificate(X509Certificate[] convertedCertificates)
            throws CertificateVerificationException {

        Optional<X509Certificate> peerCertOpt = Arrays.stream(convertedCertificates).findFirst();
        if (peerCertOpt.isPresent()) {
            return peerCertOpt.get();
        } else {
            throw new CertificateVerificationException("Peer certificate is not provided");
        }
    }

    public X509Certificate getVerifiedIssuerCertOfPeerCert(X509Certificate peerCert, CertCache certCache)
            throws CertificateVerificationException {

        if (certCache.getCacheValue(peerCert.getSerialNumber().toString()) != null) {

            X509Certificate cachedIssuerCert = certCache.getCacheValue(peerCert.getSerialNumber().toString());
            if (!isPeerCertVerified(peerCert, cachedIssuerCert)) {
                throw new CertificateVerificationException("Unable to verify the signature of the certificate.");
            } else {
                return cachedIssuerCert;
            }
        } else {
            boolean isIssuerCertVerified = false;
            KeyStore trustStore = TrustStoreHolder.getInstance().getClientTrustStore();
            Enumeration<String> aliases;
            X509Certificate issuerCert = null;

            try {
                aliases = trustStore.aliases();
            } catch (KeyStoreException e) {
                throw new CertificateVerificationException("Error while retrieving aliases from truststore", e);
            }

            while (aliases.hasMoreElements()) {

                String alias = aliases.nextElement();
                try {
                    issuerCert = (X509Certificate) trustStore.getCertificate(alias);
                } catch (KeyStoreException e) {
                    throw new CertificateVerificationException("Unable to read the certificate from " +
                            "truststore with the alias: " + alias, e);
                }

                if (issuerCert == null) {
                    throw new CertificateVerificationException("Issuer certificate not found in truststore");
                }

                try {
                    peerCert.verify(issuerCert.getPublicKey());
                    isIssuerCertVerified = true;
                    break;
                } catch (SignatureException | CertificateException | NoSuchAlgorithmException |
                         InvalidKeyException | NoSuchProviderException e) {
                    // Unable to verify the signature. Check with the next certificate in the next loop traversal.
                }
            }

            if (isIssuerCertVerified) {
                log.debug("Valid issuer certificate found in the client truststore. Caching..");
                // Store the valid issuer cert in cache for future use
                certCache.setCacheValue(peerCert.getSerialNumber().toString(), issuerCert);
                if (log.isDebugEnabled()) {
                    log.debug("Issuer certificate with serial number: " + issuerCert.getSerialNumber()
                            .toString() + " has been cached against the serial number:  " + peerCert
                            .getSerialNumber().toString() + " of the peer certificate.");
                }
                return issuerCert;
            } else {
               throw new CertificateVerificationException("Certificate verification failed.");
            }
        }
    }

    public boolean isPeerCertVerified(X509Certificate peerCert, X509Certificate issuerCert) {

        try {
            peerCert.verify(issuerCert.getPublicKey());
            return true;
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException
                 | SignatureException e) {
            return false;
        }
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

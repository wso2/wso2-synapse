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
    private boolean isFullCertChainValidationEnabled = false;
    private static final Log log = LogFactory.getLog(CertificateVerificationManager.class);

    public CertificateVerificationManager(Integer cacheAllocatedSize, Integer cacheDelayMins) {

        if (cacheAllocatedSize != null && cacheAllocatedSize > Constants.CACHE_MIN_ALLOCATED_SIZE
                && cacheAllocatedSize < Constants.CACHE_MAX_ALLOCATED_SIZE) {
            this.cacheSize = cacheAllocatedSize;
        }
        if (cacheDelayMins != null && cacheDelayMins > Constants.CACHE_MIN_DELAY_MINS
                && cacheDelayMins < Constants.CACHE_MAX_DELAY_MINS) {
            this.cacheDelayMins = cacheDelayMins;
        }
    }

    public CertificateVerificationManager(Integer cacheAllocatedSize, Integer cacheDelayMins,
                                          boolean isFullCertChainValidationEnabled) {

        if (cacheAllocatedSize != null && cacheAllocatedSize > Constants.CACHE_MIN_ALLOCATED_SIZE
                && cacheAllocatedSize < Constants.CACHE_MAX_ALLOCATED_SIZE) {
            this.cacheSize = cacheAllocatedSize;
        }
        if (cacheDelayMins != null && cacheDelayMins > Constants.CACHE_MIN_DELAY_MINS
                && cacheDelayMins < Constants.CACHE_MAX_DELAY_MINS) {
            this.cacheDelayMins = cacheDelayMins;
        }
        this.isFullCertChainValidationEnabled = isFullCertChainValidationEnabled;
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
     * @param peerCertificates  javax.security.cert.X509Certificate[] array of peer certificate chain from peer/client.
     * @throws CertificateVerificationException
     */
    public void verifyRevocationStatus(javax.security.cert.X509Certificate[] peerCertificates)
            throws CertificateVerificationException {

        X509Certificate[] convertedCertificates = convert(peerCertificates);

        Optional<X509Certificate> peerCertOpt;
        X509Certificate peerCert = null;
        X509Certificate issuerCert = null;
        String alias;

        if (!isFullCertChainValidationEnabled) {

            if (log.isDebugEnabled()) {
                log.debug("Retrieving the issuer certificate from client truststore since full certificate chain " +
                        "validation is disabled");
            }

            KeyStore trustStore = TrustStoreHolder.getInstance().getClientTrustStore();
            Enumeration<String> aliases;

            // When full chain validation is disabled, only one cert is expected
            peerCertOpt = Arrays.stream(convertedCertificates).findFirst();
            if (peerCertOpt.isPresent()) {
                peerCert = peerCertOpt.get();
            } else {
                throw new CertificateVerificationException("Peer certificate is not provided");
            }

            // Get cert cache and initialize it
            CertCache certCache = CertCache.getCache();
            certCache.init(cacheSize,cacheDelayMins);

            if (certCache.getCacheValue(peerCert.getSerialNumber().toString()) == null) {

                try {
                    aliases = trustStore.aliases();
                } catch (KeyStoreException e) {
                    throw new CertificateVerificationException("Error while retrieving aliases from truststore", e);
                }

                while (aliases.hasMoreElements()) {
                    try {
                        alias = aliases.nextElement();
                        try {
                            issuerCert = (X509Certificate) trustStore.getCertificate(alias);
                        } catch (KeyStoreException e) {
                            throw new CertificateVerificationException("Unable to read the certificate from " +
                                    "truststore with the alias: " + alias, e);
                        }

                        if (issuerCert == null) {
                            throw new CertificateVerificationException("Issuer certificate not found in truststore");
                        }

                        peerCert.verify(issuerCert.getPublicKey());
                        log.debug("Valid issuer certificate found in the client truststore. Caching..");

                        // Store the valid issuer cert in cache for future use
                        certCache.setCacheValue(peerCert.getSerialNumber().toString(), issuerCert);
                        if (log.isDebugEnabled()) {
                            log.debug("Issuer certificate with serial number: " + issuerCert.getSerialNumber()
                                    .toString() + " has been cached against the serial number:  " + peerCert
                                    .getSerialNumber().toString() + " of the peer certificate.");
                        }
                        break;
                    } catch (SignatureException | CertificateException | NoSuchAlgorithmException |
                             InvalidKeyException | NoSuchProviderException e) {
                        // Unable to verify the signature. Check with the next certificate.
                    }
                }
            } else {
                X509Certificate cachedIssuerCert = certCache.getCacheValue(peerCert.getSerialNumber().toString());
                try {
                    peerCert.verify(cachedIssuerCert.getPublicKey());
                } catch (SignatureException | CertificateException | NoSuchAlgorithmException |
                         InvalidKeyException |
                         NoSuchProviderException e) {
                    // Unable to verify the signature.
                    throw new CertificateVerificationException("Unable to verify the signature of the certificate.");
                }
            }
        }

        long start = System.currentTimeMillis();

        OCSPCache ocspCache = OCSPCache.getCache();
        ocspCache.init(cacheSize, cacheDelayMins);
        CRLCache crlCache = CRLCache.getCache();
        crlCache.init(cacheSize, cacheDelayMins);

        RevocationVerifier[] verifiers = {new OCSPVerifier(ocspCache), new CRLVerifier(crlCache)};

        for (RevocationVerifier verifier : verifiers) {
            try {
                if (isFullCertChainValidationEnabled) {
                    log.debug("Doing full certificate chain validation");
                    CertificatePathValidator pathValidator = new CertificatePathValidator(convertedCertificates,
                            verifier);
                    pathValidator.validatePath();
                    log.info("Path verification Successful. Took " + (System.currentTimeMillis() - start) + " ms.");
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Validating client certificate with the issuer certificate retrieved from" +
                                "the trust store");
                    }
                    verifier.checkRevocationStatus(peerCert, issuerCert);
                }
                return;
            } catch (Exception e) {
                log.info(verifier.getClass().getSimpleName() + " failed.");
                log.debug("Certificate verification with " + verifier.getClass().getSimpleName() + " failed. ", e);
            }
        }
        throw new CertificateVerificationException("Path Verification Failed for both OCSP and CRL");
    }

    /**
     * @param certs array of javax.security.cert.X509Certificate[] s.
     * @return the converted array of java.security.cert.X509Certificate[] s.
     * @throws CertificateVerificationException
     */
    private X509Certificate[] convert(javax.security.cert.X509Certificate[] certs)
            throws CertificateVerificationException {
        X509Certificate[] certChain = new X509Certificate[certs.length];
        Throwable exceptionThrown;
        for (int i = 0; i < certs.length; i++) {
            try {
                byte[] encoded = certs[i].getEncoded();
                ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
                java.security.cert.CertificateFactory cf
                        = java.security.cert.CertificateFactory.getInstance("X.509");
                certChain[i]=((X509Certificate)cf.generateCertificate(bis));
                continue;
            } catch (java.security.cert.CertificateEncodingException e) {
                exceptionThrown = e;
            } catch (javax.security.cert.CertificateEncodingException e) {
                exceptionThrown = e;
            } catch (java.security.cert.CertificateException e) {
                exceptionThrown = e;
            }
            throw new CertificateVerificationException("Cant Convert certificates from javax to java", exceptionThrown);
        }
        return certChain;
    }

    /**
     * Checks whether a provided certificate is expired or not at the time it is validated.
     *
     * @param certificates
     * @throws CertificateNotYetValidException
     * @throws CertificateExpiredException
     * @throws CertificateVerificationException
     */
    public void isExpired(javax.security.cert.X509Certificate[] certificates) throws CertificateVerificationException {

        X509Certificate[] convertedCertificates = convert(certificates);

        for (X509Certificate cert : convertedCertificates) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException e) {
                String msg = "Peer certificate is expired";
                throw new CertificateVerificationException(msg);
            } catch (CertificateNotYetValidException e) {
                String msg = "Peer certificate is not valid yet";
                throw new CertificateVerificationException(msg);
            }
        }
    }
}
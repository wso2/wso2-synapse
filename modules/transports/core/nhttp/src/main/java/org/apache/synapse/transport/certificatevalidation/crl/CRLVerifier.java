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
package org.apache.synapse.transport.certificatevalidation.crl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.apache.synapse.transport.certificatevalidation.*;
import org.bouncycastle.asn1.x509.Extension;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used to verify a certificate is revoked or not by using the Certificate Revocation List published
 * by the CA.
 */
public class CRLVerifier implements RevocationVerifier {

    private CRLCache cache;
    private static final Log log = LogFactory.getLog(CRLVerifier.class);

    public CRLVerifier(CRLCache cache) {
        this.cache = cache;
    }

    /**
     * Checks revocation status (Good, Revoked) of the peer certificate. IssuerCertificate can be used
     * to check if the CRL URL has the Issuers Domain name. But this is not implemented at the moment.
     *
     * @param peerCert   peer certificate
     * @param issuerCert issuer certificate of the peer. not used currently.
     * @return revocation status of the peer certificate.
     * @throws CertificateVerificationException
     *
     */
    public RevocationStatus checkRevocationStatus(X509Certificate peerCert, X509Certificate issuerCert)
            throws CertificateVerificationException {

        List<String> list = getCrlDistributionPoints(peerCert);
        //check with distributions points in the list one by one. if one fails go to the other.
        for (String crlUrl : list) {
            log.info("Trying to get CRL for URL: " + crlUrl);

            if (cache != null) {
                X509CRL x509CRL = cache.getCacheValue(crlUrl);
                if (x509CRL != null) {
                    //If cant be casted, we have used the wrong cache.
                    RevocationStatus status = getRevocationStatus(x509CRL, peerCert);
                    log.info("CRL taken from cache....");
                    return status;
                }
            }

            //todo: Do we need to check if URL has the same domain name as issuerCert?
            //todo: What if this certificate is Unknown?????
            try {
                X509CRL x509CRL = downloadCRLFromWeb(crlUrl);
                if (x509CRL != null) {
                    if (cache != null)
                        cache.setCacheValue(crlUrl, x509CRL);
                    return getRevocationStatus(x509CRL, peerCert);
                }
            } catch (Exception e) {
                log.info("Either url is bad or cant build X509CRL. So check with the next url in the list.", e);
            }
        }
        throw new CertificateVerificationException("Cannot check revocation status with the certificate");
    }

    private RevocationStatus getRevocationStatus(X509CRL x509CRL, X509Certificate peerCert) {
        if (x509CRL.isRevoked(peerCert)) {
            return RevocationStatus.REVOKED;
        } else {
            return RevocationStatus.GOOD;
        }
    }

    /**
     * Downloads CRL from the crlUrl. Does not support HTTPS
     */
    protected X509CRL downloadCRLFromWeb(String crlURL)
            throws IOException, CertificateVerificationException {
        InputStream crlStream = null;
        try {
            URL url = new URL(crlURL);
            crlStream = url.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(crlStream);
        } catch (MalformedURLException e) {
            throw new CertificateVerificationException("CRL Url is malformed", e);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cant reach URI: " + crlURL + " - only support HTTP", e);
        } catch (CertificateException e) {
            throw new CertificateVerificationException(e);
        } catch (CRLException e) {
            throw new CertificateVerificationException("Cannot generate X509CRL from the stream data", e);
        } finally {
            if (crlStream != null)
                crlStream.close();
        }
    }

    /**
     * Extracts all CRL distribution point URLs from the "CRL Distribution Point"
     * extension in a X.509 certificate. If CRL distribution point extension is
     * unavailable, returns an empty list.
     */
    private List<String> getCrlDistributionPoints(X509Certificate cert)
            throws CertificateVerificationException {

        //Gets the DER-encoded OCTET string for the extension value for CRLDistributionPoints
        byte[] crlDPExtensionValue = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());
        if (crlDPExtensionValue == null)
            throw new CertificateVerificationException("Certificate doesn't have CRL Distribution points");
        //crlDPExtensionValue is encoded in ASN.1 format.
        ASN1InputStream asn1In = new ASN1InputStream(crlDPExtensionValue);
        //DER (Distinguished Encoding Rules) is one of ASN.1 encoding rules defined in ITU-T X.690, 2002, specification.
        //ASN.1 encoding rules can be used to encode any data object into a binary file. Read the object in octets.
        CRLDistPoint distPoint;
        try {
            DEROctetString crlDEROctetString = (DEROctetString) asn1In.readObject();
            //Get Input stream in octets
            ASN1InputStream asn1InOctets = new ASN1InputStream(crlDEROctetString.getOctets());
            ASN1Primitive crlDERObject = asn1InOctets.readObject();
            distPoint = CRLDistPoint.getInstance(crlDERObject);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot read certificate to get CRL urls", e);
        }

        List<String> crlUrls = new ArrayList<String>();
        //Loop through ASN1Encodable DistributionPoints
        for (DistributionPoint dp : distPoint.getDistributionPoints()) {
            //get ASN1Encodable DistributionPointName
            DistributionPointName dpn = dp.getDistributionPoint();
            if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
                //Create ASN1Encodable General Names
                GeneralName[] genNames = GeneralNames.getInstance(dpn.getName()).getNames();
                // Look for a URI
                //todo: May be able to check for OCSP url specifically.
                for (GeneralName genName : genNames) {
                    if (genName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        //DERIA5String contains an ascii string.
                        //A IA5String is a restricted character string type in the ASN.1 notation
                        String url = DERIA5String.getInstance(genName.getName()).getString().trim();
                        crlUrls.add(url);
                    }
                }
            }
        }

        if (crlUrls.isEmpty())
            throw new CertificateVerificationException("Cant get CRL urls from certificate");

        return crlUrls;
    }
}

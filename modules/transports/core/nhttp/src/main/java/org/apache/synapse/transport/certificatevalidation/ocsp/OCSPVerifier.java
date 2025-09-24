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
package org.apache.synapse.transport.certificatevalidation.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.synapse.transport.certificatevalidation.CertificateVerificationException;
import org.apache.synapse.transport.certificatevalidation.Constants;
import org.apache.synapse.transport.certificatevalidation.RevocationStatus;
import org.apache.synapse.transport.certificatevalidation.RevocationVerifier;
import org.bouncycastle.asn1.ASN1IA5String;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to check if a Certificate is revoked or not by its CA using Online Certificate Status Protocol (OCSP).
 */
public class OCSPVerifier implements RevocationVerifier {

    private OCSPCache cache;
    private static final Log log = LogFactory.getLog(OCSPVerifier.class);

    public OCSPVerifier(OCSPCache cache) {
        this.cache = cache;
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String JSON_TYPE ="application/json";
    public static final String ACCEPT_TYPE = "Accept";
    public static final String OCSP_REQUEST_TYPE = "application/ocsp-request";
    public static final String OCSP_RESPONSE_TYPE = "application/ocsp-response";


    /**
     * Gets the revocation status (Good, Revoked or Unknown) of the given peer certificate.
     *
     * @param peerCert   The certificate we want to check if revoked.
     * @param issuerCert Needed to create OCSP request.
     * @return revocation status of the peer certificate.
     * @throws CertificateVerificationException
     *
     */
    public RevocationStatus checkRevocationStatus(X509Certificate peerCert, X509Certificate issuerCert)
            throws CertificateVerificationException {

        //check cache
        if (cache != null) {
            SingleResp resp = cache.getCacheValue(peerCert.getSerialNumber());
            if (resp != null) {
                //If cant be casted, we have used the wrong cache.
                RevocationStatus status = getRevocationStatus(resp);
                log.info("OCSP response taken from cache....");
                return status;
            }
        }

        OCSPReq request = generateOCSPRequest(issuerCert, peerCert.getSerialNumber());
        //This list will sometimes have non ocsp urls as well.
        List<String> locations = getAIALocations(peerCert);

        for (String serviceUrl : locations) {

            SingleResp[] responses;
            try {
                OCSPResp ocspResponse = getOCSPResponce(serviceUrl, request);
                if (OCSPResponseStatus.SUCCESSFUL != ocspResponse.getStatus()) {
                    continue; // Server didn't give the response right.
                }

                BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
                responses = (basicResponse == null) ? null : basicResponse.getResponses();
                //todo use the super exception
            } catch (Exception e) {
                continue;
            }

            if (responses != null && responses.length == 1) {
                SingleResp resp = responses[0];
                RevocationStatus status = getRevocationStatus(resp);
                if (cache != null)
                    cache.setCacheValue(peerCert.getSerialNumber(), resp, request, serviceUrl);
                return status;
            }
        }
        throw new CertificateVerificationException("Cant get Revocation Status from OCSP.");
    }

    private RevocationStatus getRevocationStatus(SingleResp resp) throws CertificateVerificationException {
        Object status = resp.getCertStatus();
        if (status == CertificateStatus.GOOD) {
            return RevocationStatus.GOOD;
        } else if (status instanceof org.bouncycastle.cert.ocsp.RevokedStatus) {
            return RevocationStatus.REVOKED;
        } else if (status instanceof org.bouncycastle.cert.ocsp.UnknownStatus) {
            return RevocationStatus.UNKNOWN;
        }
        throw new CertificateVerificationException("Cant recognize Certificate Status");
    }

    /**
     * Gets an ASN.1 encoded OCSP response (as defined in RFC 2560) from the given service URL. Currently supports
     * only HTTP.
     *
     * @param serviceUrl URL of the OCSP endpoint.
     * @param request    an OCSP request object.
     * @return OCSP response encoded in ASN.1 structure.
     * @throws CertificateVerificationException
     *
     */
    protected OCSPResp getOCSPResponce(String serviceUrl, OCSPReq request) throws CertificateVerificationException {

        if (log.isDebugEnabled()) {
            log.debug("Initiating HTTP request to URL: " + serviceUrl + " to get the OCSP response");
        }

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(serviceUrl);

            // adding request timeout configurations
            if (httpPost.getConfig() == null) {
                httpPost.setConfig(RequestConfig.custom().build());
            }
            httpPost.addHeader(CONTENT_TYPE, OCSP_REQUEST_TYPE);
            httpPost.addHeader(ACCEPT_TYPE, OCSP_RESPONSE_TYPE);
            httpPost.setEntity(new ByteArrayEntity(request.getEncoded(), ContentType.create(JSON_TYPE)));
            HttpResponse httpResponse = client.execute(httpPost);

            //Check errors in response, if response status code is not 200 (success) range, throws exception
            // eg: if response code is 200 (success) or 201 (accepted) return true,
            //     if response code is 404 (not found) or 500 throw exception
            if (httpResponse.getStatusLine().getStatusCode() / 100 != 2) {
                throw new CertificateVerificationException("Error getting ocsp response." +
                        "Response code is " + httpResponse.getStatusLine().getStatusCode());
            }
            InputStream in = httpResponse.getEntity().getContent();
            return new OCSPResp(in);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot get ocspResponse from url: " + serviceUrl, e);
        }
    }

    /**
     * This method generates an OCSP Request to be sent to an OCSP endpoint.
     *
     * @param issuerCert   is the Certificate of the Issuer of the peer certificate we are interested in.
     * @param serialNumber of the peer certificate.
     * @return generated OCSP request.
     * @throws CertificateVerificationException
     *
     */
    private OCSPReq generateOCSPRequest(X509Certificate issuerCert, BigInteger serialNumber)
            throws CertificateVerificationException {
        String jceProvider = getPreferredJceProvider();
        String providerClass;
        if (jceProvider.equals(Constants.BOUNCY_CASTLE_PROVIDER)) {
            providerClass = "org.bouncycastle.jce.provider.BouncyCastleProvider";
        } else if (jceProvider.equals(Constants.BOUNCY_CASTLE_FIPS_PROVIDER)) {
            providerClass = "org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider";
        } else {
            throw new CertificateVerificationException("Unsupported JCE provider: " + jceProvider);
        }
        try {
            Security.addProvider((Provider) Class.forName(providerClass).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new CertificateVerificationException("Error while initializing the JCE provider: "
                    + providerClass, e);
        }

        try {

            byte[] issuerCertEnc = issuerCert.getEncoded();
            X509CertificateHolder certificateHolder = new X509CertificateHolder(issuerCertEnc);
            DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder().setProvider(jceProvider)
                    .build();

            //  CertID structure is used to uniquely identify certificates that are the subject of
            // an OCSP request or response and has an ASN.1 definition. CertID structure is defined in RFC 2560
            CertificateID id = new CertificateID(digCalcProv.get(CertificateID.HASH_SHA1), certificateHolder, serialNumber);

            // basic request generation with nonce
            OCSPReqBuilder builder = new OCSPReqBuilder();
            builder.addRequest(id);

            // create details for nonce extension. The nonce extension is used to bind
            // a request to a response to prevent replay attacks. As the name implies,
            // the nonce value is something that the client should only use once within a reasonably small period.
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());

            //to create the request Extension
            builder.setRequestExtensions(new Extensions(new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce,false, new DEROctetString(nonce.toByteArray()))));

            return builder.build();
        } catch (Exception e) {
            throw new CertificateVerificationException("Cannot generate OSCP Request with the given certificate", e);
        }
    }

    /**
     * Authority Information Access (AIA) is a non-critical extension in an X509 Certificate. This contains the
     * URL of the OCSP endpoint if one is available.
     * TODO: This might contain non OCSP urls as well. Handle this.
     *
     * @param cert is the certificate
     * @return a lit of URLs in AIA extension of the certificate which will hopefully contain an OCSP endpoint.
     * @throws CertificateVerificationException
     *
     */
    private List<String> getAIALocations(X509Certificate cert) throws CertificateVerificationException {

        //Gets the DER-encoded OCTET string for the extension value for Authority information access Points
        byte[] aiaExtensionValue = cert.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (aiaExtensionValue == null)
            throw new CertificateVerificationException("Certificate Doesn't have Authority Information Access points");
        AuthorityInformationAccess authorityInformationAccess;

        try {
            DEROctetString oct = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(aiaExtensionValue)).readObject());
            authorityInformationAccess = AuthorityInformationAccess.getInstance(new ASN1InputStream(oct.getOctets()).readObject());
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot read certificate to get OSCP urls", e);
        }

        List<String> ocspUrlList = new ArrayList<String>();
        AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
        for (AccessDescription accessDescription : accessDescriptions) {

            GeneralName gn = accessDescription.getAccessLocation();
            if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                ASN1IA5String str = ASN1IA5String.getInstance(gn.getName());
                String accessLocation = str.getString();
                ocspUrlList.add(accessLocation);
            }
        }
        if(ocspUrlList.isEmpty())
            throw new CertificateVerificationException("Cant get OCSP urls from certificate");

        return ocspUrlList;
    }

    /**
     * This method returns the preferred JCE provider to be used.
     *modules/transports/core/nhttp/src/main/java/org/apache/synapse/transport/certificatevalidation/ocsp/OCSPVerifier.java
     * @return
     */
    private static String getPreferredJceProvider() {
        String provider = System.getProperty(Constants.SECURITY_JCE_PROVIDER);
        if (provider != null && (provider.equalsIgnoreCase(Constants.BOUNCY_CASTLE_FIPS_PROVIDER) ||
                provider.equalsIgnoreCase(Constants.BOUNCY_CASTLE_PROVIDER))) {
            return provider;
        }
        return null;
    }
}

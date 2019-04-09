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

import junit.framework.TestCase;
import org.apache.synapse.commons.crypto.CryptoConstants;
import org.apache.synapse.transport.certificatevalidation.crl.CRLCache;
import org.apache.synapse.transport.certificatevalidation.crl.CRLVerifier;import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

public class CRLVerifierTest extends TestCase {

    /**
     * To test CRLVerifier behaviour when a revoked certificate is given, a fake certificate will be created, signed
     * by a fake root certificate. To make our life easy, the CrlDistributionPoint extension will be extracted from
     * the real peer certificate in resources directory and copied to the fake certificate as a certificate extension.
     * So the criDistributionPointURL in the fake certificate will be the same as in the real certificate.
     * The created X509CRL object will be put to CRLCache against the criDistributionPointURL. Since the crl is in the
     * cache, there will NOT be a remote call to the CRL server at criDistributionPointURL.
     * @throws Exception
     */
    public void testRevokedCertificate() throws Exception {

        //Add BouncyCastle as Security Provider.
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        Utils utils = new Utils();
        //Create X509Certificate from the real certificate file in resource folder.
        X509Certificate realPeerCertificate = utils.getRealPeerCertificate();
        //Extract crlDistributionPointUrl from the real peer certificate.
        String crlDistributionPointUrl = getCRLDistributionPointUrl(realPeerCertificate);

        //Create fake CA certificate.
        KeyPair caKeyPair = utils.generateRSAKeyPair();
        X509Certificate fakeCACert = utils.generateFakeRootCert(caKeyPair);

        //Create fake peer certificate signed by the fake CA private key. This will be a revoked certificate.
        KeyPair peerKeyPair = utils.generateRSAKeyPair();
        BigInteger revokedSerialNumber = BigInteger.valueOf(111);
        X509Certificate fakeRevokedCertificate = generateFakePeerCert(revokedSerialNumber, peerKeyPair.getPublic(),
                caKeyPair.getPrivate(), fakeCACert, realPeerCertificate);

        //Create a crl with fakeRevokedCertificate marked as revoked.
        X509CRL x509CRL = createCRL(fakeCACert, caKeyPair.getPrivate(), revokedSerialNumber);

        CRLCache cache = CRLCache.getCache();
        cache.init(5, 5);
        cache.setCacheValue(crlDistributionPointUrl, x509CRL);

        CRLVerifier crlVerifier  = new CRLVerifier(cache);
        RevocationStatus status = crlVerifier.checkRevocationStatus(fakeRevokedCertificate, null);

        //the fake crl we created will be checked if the fake certificate is revoked. So the status should be REVOKED.
        assertTrue(status == RevocationStatus.REVOKED);
    }

    /**
     * This will use Reflection to call getCrlDistributionPoints() private method in CRLVerifier.
     * @param certificate is a certificate with a proper CRLDistributionPoints extension.
     * @return the extracted cRLDistributionPointUrl.
     * @throws Exception
     */
    private String getCRLDistributionPointUrl(X509Certificate certificate) throws Exception {

        CRLVerifier crlVerifier = new CRLVerifier(null);
        // use reflection since getCrlDistributionPoints() is private.
        Class<? extends CRLVerifier> crlVerifierClass = crlVerifier.getClass();
        Method getCrlDistributionPoints = crlVerifierClass.getDeclaredMethod("getCrlDistributionPoints", X509Certificate.class);
        getCrlDistributionPoints.setAccessible(true);

        //getCrlDistributionPoints(..) returns a list of urls. Get the first one.
        List<String> distPoints = (List<String>) getCrlDistributionPoints.invoke(crlVerifier, certificate);
        return distPoints.get(0);
    }

    /**
     * Creates a fake CRL for the fake CA. The fake certificate with the given revokedSerialNumber will be marked
     * as Revoked in the returned CRL.
     * @param caCert the fake CA certificate.
     * @param caPrivateKey private key of the fake CA.
     * @param revokedSerialNumber the serial number of the fake peer certificate made to be marked as revoked.
     * @return the created fake CRL
     * @throws Exception
     */
    public static X509CRL createCRL(X509Certificate caCert, PrivateKey caPrivateKey, BigInteger revokedSerialNumber)
            throws Exception {

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        Date now = new Date();
        X500Name issuer = X500Name.getInstance(PrincipalUtil.getIssuerX509Principal(caCert).getEncoded());
        X509v2CRLBuilder builder = new X509v2CRLBuilder(issuer, new Date());
        builder.addCRLEntry(revokedSerialNumber, new Date(), 0);
        builder.setNextUpdate(new Date(now.getTime() + TestConstants.NEXT_UPDATE_PERIOD));
        builder.addExtension(Extension.cRLDistributionPoints, false,
                extUtils.createAuthorityKeyIdentifier(caCert));
        builder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(1)));
        JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        contentSignerBuilder.setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER);
        X509CRLHolder cRLHolder = builder.build(contentSignerBuilder.build(caPrivateKey));
        JcaX509CRLConverter converter = new JcaX509CRLConverter();
        converter.setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER);
        return converter.getCRL(cRLHolder);
    }

    public X509Certificate generateFakePeerCert(BigInteger serialNumber, PublicKey entityKey,
                                                PrivateKey caKey, X509Certificate caCert, X509Certificate firstCertificate)
            throws Exception {

        Utils utils = new Utils();
        X509v3CertificateBuilder certBuilder = utils.getUsableCertificateBuilder(entityKey, serialNumber);
        certBuilder.copyAndAddExtension(Extension.cRLDistributionPoints, false,
                new JcaX509CertificateHolder(firstCertificate));
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                .find("SHA1WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(caKey.getEncoded()));

        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
        return new JcaX509CertificateConverter().setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER)
                .getCertificate(certificateHolder);
    }

}

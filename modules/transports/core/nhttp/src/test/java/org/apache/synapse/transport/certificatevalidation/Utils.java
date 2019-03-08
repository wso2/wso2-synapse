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

import org.apache.synapse.commons.crypto.CryptoConstants;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Contains utility methods used by the test classes.
 */
public class Utils {


    public X509Certificate generateFakeRootCert(KeyPair pair) throws Exception {

        X500Name subjectDN = new X500Name("CN=Test End Certificate");
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + TestConstants.VALIDITY_PERIOD);
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(subjectDN, BigInteger.valueOf(1),
                notBefore, notAfter, subjectDN, subPubKeyInfo);

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                .find("SHA1WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(pair.getPrivate().getEncoded()));
        X509CertificateHolder certificateHolder = builder.build(contentSigner);

        return new JcaX509CertificateConverter().setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER)
                .getCertificate(certificateHolder);
    }


    public KeyPair generateRSAKeyPair() throws Exception {

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(1024, new SecureRandom());
        return kpGen.generateKeyPair();
    }

    /**
     * CRLVerifierTest and OCSPVerifierTest both will use this method. This has common code for both test classes
     * in creating fake peer certificates.
     * @param peerPublicKey public key of the peer certificate which will be generated.
     * @param serialNumber  serial number of the peer certificate.
     * @return
     */
    public X509v3CertificateBuilder getUsableCertificateBuilder(PublicKey peerPublicKey,
                                                                BigInteger serialNumber) {
        X500Name subjectDN = new X500Name("CN=Test End Certificate");
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + TestConstants.VALIDITY_PERIOD);
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(peerPublicKey.getEncoded());

        return new X509v3CertificateBuilder(
                subjectDN, serialNumber, notBefore, notAfter,
                subjectDN, subPubKeyInfo);
    }

    /**
     * Generate X509Certificate object from the peer certificate file in resources directory.
     * @return the created certificate object.
     * @throws Exception
     */
    public X509Certificate getRealPeerCertificate()throws Exception {
        return createCertificateFromResourceFile(TestConstants.REAL_PEER_CERT);
    }

    /**
     * Create a certificate chain from the certificates in the resources directory.
     * @return created array of certificates.
     * @throws Exception
     */
    public X509Certificate[] getRealCertificateChain() throws Exception {

        X509Certificate peerCert = createCertificateFromResourceFile(TestConstants.REAL_PEER_CERT);
        X509Certificate intermediateCert = createCertificateFromResourceFile(TestConstants.INTERMEDIATE_CERT);
        X509Certificate rootCert = createCertificateFromResourceFile(TestConstants.ROOT_CERT);

        return new X509Certificate[]{ peerCert,intermediateCert,rootCert  };
    }

    /**
     * Generates a fake certificate chain. The array will contain two certificates, the root and the peer.
     * @return the created array of certificates.
     * @throws Exception
     */
    public X509Certificate[] getFakeCertificateChain() throws Exception{

        KeyPair rootKeyPair = generateRSAKeyPair();
        X509Certificate rootCert = generateFakeRootCert(rootKeyPair);
        KeyPair entityKeyPair = generateRSAKeyPair();
        BigInteger entitySerialNum =BigInteger.valueOf(111);

        X509v3CertificateBuilder certBuilder = getUsableCertificateBuilder(entityKeyPair.getPublic(), entitySerialNum);
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                .find("SHA1WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(entityKeyPair.getPrivate().getEncoded()));

        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);
        X509Certificate entityCert = new JcaX509CertificateConverter()
                .setProvider(CryptoConstants.BOUNCY_CASTLE_PROVIDER).getCertificate(certificateHolder);
        return new X509Certificate[]{entityCert, rootCert};
    }

    private X509Certificate createCertificateFromResourceFile(String resourcePath) throws Exception{

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
        File faceBookCertificateFile = new File(this.getClass().getResource(resourcePath).toURI());
        InputStream in = new FileInputStream(faceBookCertificateFile);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(in);
        return certificate;
    }
}

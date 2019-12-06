package org.apache.synapse.mediators.builtin;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;

/**
 * Will retrieve the jwt token from the message request.
 * The jwt will be verified and the claims are saved in the messagecontext.
 */
public class JWTMediator extends AbstractMediator {

    /** The reference to the JWT.*/
    private SignedJWT signedJWT;
    /** The reference to the rsa public key.*/
    private RSAPublicKey rsaPublicKey;
    /** The reference to the ecdsa public key.*/
    private ECPublicKey ecdsaPublickey;
    /** The reference to other key sources.*/
    private JWKSource<SecurityContext> keySource;


    @Override
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);
        Object headers = ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers instanceof Map) {
            Map headersMap = (Map) headers;
            if (headersMap.get("Authorization") == null) {
                if (synLog.isTraceEnabled()) {
                    synLog.traceTrace("No Authorization header found in the message request");
                }
                return false;


            } else {
                String authHeader = (String) headersMap.get("Authorization");
                String credentials = authHeader.substring(6).trim();


                try {
                    signedJWT = SignedJWT.parse(credentials);

                    if (keySource != null) {
                        if (!validation()) {
                            if (synLog.isTraceEnabled()) {
                                synLog.traceTrace("Invalid credentials");
                            }
                            return false;
                        }
                    } else if (rsaPublicKey != null) {
                        if (!rsaValidation()) {
                            if (synLog.isTraceEnabled()) {
                                synLog.traceTrace("Invalid credentials");
                            }
                            return false;
                        }
                    } else if (ecdsaPublickey != null) {
                        if (!ecdsaValidation()) {
                            if (synLog.isTraceEnabled()) {
                                synLog.traceTrace("Invalid credentials");
                            }
                            return false;
                        }
                    }

                    JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

                    Map<String, Object> claimsMap = claimsSet.getClaims();

                    for (Map.Entry<String, Object> stringObjectEntry : claimsMap.entrySet()) {
                        synCtx.setProperty((stringObjectEntry).getKey(), stringObjectEntry.getValue().toString());
                    }


                } catch (ParseException | JOSEException  e) {
                    handleException("Error in validation", synCtx);
                }


            }
        }
        return true;
    }



    private boolean validation()  {
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSAlgorithm expectedJWSAlg = signedJWT.getHeader().getAlgorithm();
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);


        try {
            jwtProcessor.process(signedJWT, null);
        } catch (Exception e) {
            return false;
        }



        return true;

    }

    private boolean rsaValidation()  {
        try {
            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey);
            return signedJWT.verify(verifier);
        } catch (JOSEException e) {
            return false;
        }
    }


    private boolean ecdsaValidation() throws JOSEException {
        JWSVerifier verifier = new ECDSAVerifier(ecdsaPublickey);
        return signedJWT.verify(verifier);
    }




    public void setX509Certificate(String alias) throws CertificateException, IOException,
            InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        String storePassword = "wso2carbon";
        String storePath = System.getProperty("user.dir") + "/repository/resources/security/client-truststore.jks";
        keyStore.load(new FileInputStream(storePath), storePassword.toCharArray());
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

        PublicKey pubKey = certificate.getPublicKey();

        if (pubKey instanceof RSAPublicKey) {
            setRsaPublicKey(pubKey.toString());
        } else if (pubKey instanceof ECPublicKey) {
            setEcdsaPublickey(pubKey.toString());
        }

    }

    public void setRsaPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory kFactory = KeyFactory.getInstance("RSA");

        byte[] rsaKey =  Base64.getDecoder().decode(publicKey);

        X509EncodedKeySpec spec =  new X509EncodedKeySpec(rsaKey);
        rsaPublicKey = (RSAPublicKey) kFactory.generatePublic(spec);


    }




    public void setSharedSecret(String sharedSecret) {
        keySource = new ImmutableSecret<>(sharedSecret.getBytes(Charset.forName("UTF-8")));
    }

    public void setEcdsaPublickey(String publicKey)
            throws InvalidKeySpecException, NoSuchProviderException, NoSuchAlgorithmException {
        KeyFactory factory = KeyFactory.getInstance("ECDSA", "BC");

        byte[] ecdsaKey = Base64.getDecoder().decode(publicKey);

        ecdsaPublickey = (ECPublicKey) factory.generatePublic(new X509EncodedKeySpec(ecdsaKey));

    }

    public void setJwksFile(String jwksFile) throws IOException, ParseException {
        JWKSet jwkSet = JWKSet.load(new File(jwksFile));
        keySource = new ImmutableJWKSet<>(jwkSet);

    }

    public void setJwksRemoteURL(String jwksRemoteurl) throws MalformedURLException {
        keySource = new RemoteJWKSet<>(new URL(jwksRemoteurl));

    }

}

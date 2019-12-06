package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.JWTMediator;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Properties;
import javax.xml.namespace.QName;


/**
 * Factory for {@link JWTMediator} instances.
 */
public class JWTMediatorFactory extends AbstractMediatorFactory {

    /**.
     * This will hold the QName of the jwt mediator element in the xml configuration
     */
    static final QName JWT_Q = new QName("http://ws.apache.org/ns/synapse", "jwt");
    static final QName SHAREDSECRET_ATT = new QName("http://ws.apache.org/ns/synapse", "sharedSecret");
    static final QName X509CERTIFICATE_ATT = new QName("http://ws.apache.org/ns/synapse", "x509Certificate");
    static final QName RSAPUBLICKEY_ATT = new QName("http://ws.apache.org/ns/synapse", "rsaPublicKey");
    static final QName ECDSAPUBLICKEY_ATT = new QName("http://ws.apache.org/ns/synapse", "ecdsaPublickey");
    static final QName JWKSFILE_ATT = new QName("http://ws.apache.org/ns/synapse", "jwksFile");
    static final QName JWKSREMOTEURL_ATT = new QName("http://ws.apache.org/ns/synapse", "jwksRemoteURL");

    public JWTMediatorFactory() {
    }

    protected Mediator createSpecificMediator(OMElement omElement, Properties properties)  {

        //create new JWTMediator
        JWTMediator jwtMediator = new JWTMediator();

        //setup initial settings
        this.processAuditStatus(jwtMediator, omElement);
        OMElement sharedSecretElement = omElement.getFirstChildWithName(SHAREDSECRET_ATT);
        if (sharedSecretElement != null) {
            String sharedSecret = String.valueOf(sharedSecretElement.getText());
            jwtMediator.setSharedSecret(sharedSecret);

        }

        OMElement x509CertificateElement = omElement.getFirstChildWithName(X509CERTIFICATE_ATT);
        if (x509CertificateElement != null) {
            String x509Certificate = String.valueOf(x509CertificateElement.getText());
            try {
                jwtMediator.setX509Certificate(x509Certificate);
            } catch (CertificateException | KeyStoreException | NoSuchProviderException |
                    NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                handleException("Unable to create JWTMediator");
            }

        }
        OMElement rsaPublicKeyElement = omElement.getFirstChildWithName(RSAPUBLICKEY_ATT);
        if (rsaPublicKeyElement != null) {
            String rsaPublicKey = String.valueOf(rsaPublicKeyElement.getText());
            try {
                jwtMediator.setRsaPublicKey(rsaPublicKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                handleException("Unable to create JWTMediator");
            }

        }
        OMElement ecdsaPublickeyElement = omElement.getFirstChildWithName(ECDSAPUBLICKEY_ATT);
        if (ecdsaPublickeyElement != null) {
            String ecdsaPublickey = String.valueOf(ecdsaPublickeyElement.getText());
            try {
                jwtMediator.setEcdsaPublickey(ecdsaPublickey);
            } catch (InvalidKeySpecException | NoSuchProviderException | NoSuchAlgorithmException e) {
                handleException("Unable to create JWTMediator");
            }

        }
        OMElement jwksFileElement = omElement.getFirstChildWithName(JWKSFILE_ATT);
        if (jwksFileElement != null) {
            String jwksFile = String.valueOf(jwksFileElement.getText());
            try {
                jwtMediator.setJwksFile(jwksFile);
            } catch (IOException  | ParseException e) {
                handleException("Unable to create JWTMediator");
            }

        }
        OMElement jwksRemoteURLElement = omElement.getFirstChildWithName(JWKSREMOTEURL_ATT);
        if (jwksRemoteURLElement != null) {
            String jwksRemoteURL = String.valueOf(jwksRemoteURLElement.getText());
            try {
                jwtMediator.setJwksRemoteURL(jwksRemoteURL);
            } catch (MalformedURLException e) {
                handleException("Unable to create JWTMediator");
            }

        }

        return jwtMediator;
    }

    public QName getTagQName() {
        return JWT_Q;
    }
}

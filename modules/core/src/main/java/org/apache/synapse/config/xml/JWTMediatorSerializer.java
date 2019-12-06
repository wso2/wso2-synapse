package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.JWTMediator;


/**
 * Serializer for {@link JWTMediator} instances.
 */
public class JWTMediatorSerializer extends AbstractMediatorSerializer {

    /**.
     * This method will implement the serializeMediator method of the MediatorSerializer interface
     * and implements the serialization of JWTMediator to its configuration
     *
     * @param m Mediator of the type JWTMediator which is subjected to the serialization
     * @return OMElement serialized in to xml from the given parameters
     */

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof JWTMediator)) {
            handleException("Unsupported mediator passed in for serialization : "
                    + m.getType());
        }
        JWTMediator jwtMediator = (JWTMediator) m;

        OMElement jwt = fac.createOMElement("jwt", synNS);
        saveTracingState(jwt, jwtMediator);

        OMElement x509CertificateElement = fac.createOMElement(
                JWTMediatorFactory.X509CERTIFICATE_ATT);
        saveTracingState(x509CertificateElement, jwtMediator);

        OMElement rsaPublicKeyElement = fac.createOMElement(
                JWTMediatorFactory.RSAPUBLICKEY_ATT);
        saveTracingState(rsaPublicKeyElement, jwtMediator);

        OMElement ecdsaPublickeyElement = fac.createOMElement(
                JWTMediatorFactory.ECDSAPUBLICKEY_ATT);
        saveTracingState(ecdsaPublickeyElement, jwtMediator);

        OMElement jwksFileElement = fac.createOMElement(
                JWTMediatorFactory.JWKSFILE_ATT);
        saveTracingState(jwksFileElement, jwtMediator);

        OMElement jwksRemoteURLElement = fac.createOMElement(
                JWTMediatorFactory.JWKSREMOTEURL_ATT);
        saveTracingState(jwksRemoteURLElement, jwtMediator);


        return jwt;
    }

    @Override
    public String getMediatorClassName() {
        return JWTMediator.class.getName();
    }
}

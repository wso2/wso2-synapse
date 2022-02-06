package org.apache.synapse.mediators.opa;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;

import java.util.Iterator;
import java.util.Properties;

import javax.xml.namespace.QName;

public class OPAMediatorFactory extends AbstractMediatorFactory {

    static final QName OPA_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "opa");
    static final QName SERVER_URL_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "serverUrl");
    static final QName TOKEN_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "opaToken");
    static final QName PAYLOAD_GENERATOR_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "payloadGenerator");
    static final QName ADVANCED_PROPERTIES_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "advancedProperties");
    static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    static final QName NAME_Q = new QName("name");



    @Override
    protected Mediator createSpecificMediator(OMElement omElement, Properties properties) {
        OPAMediator opaMediator = new OPAMediator();
        processAuditStatus(opaMediator, omElement);

        OMElement serverUrlElement = omElement.getFirstChildWithName(SERVER_URL_Q);
        if (serverUrlElement != null) {
            opaMediator.setOpaServerUrl(serverUrlElement.getText());
        }

        OMElement opaTokenElement = omElement.getFirstChildWithName(TOKEN_Q);
        if (opaTokenElement != null) {
            opaMediator.setOpaToken(opaTokenElement.getText());
        }

        OMElement payloadGeneratorElement = omElement.getFirstChildWithName(PAYLOAD_GENERATOR_Q);
        if (payloadGeneratorElement != null) {
            opaMediator.setRequestGeneratorClass(payloadGeneratorElement.getText());
        }

        OMElement advancedPropertiesElement = omElement.getFirstChildWithName(ADVANCED_PROPERTIES_Q);
        if (advancedPropertiesElement != null) {
            Iterator parameterIter = advancedPropertiesElement.getChildrenWithName(PARAMETER_Q);
            while (parameterIter.hasNext()) {
                OMElement parameterElement = (OMElement) parameterIter.next();
                OMAttribute nameAtr = parameterElement.getAttribute(NAME_Q);
                if (nameAtr != null) {
                    String parameterName = nameAtr.getAttributeValue();
                    Object parameterValue = parameterElement.getText();

                    if (parameterName != null && parameterValue != null) {
                        opaMediator.addAdvancedProperty(parameterName, parameterValue);
                    }
                } else {
                    throw new SynapseException("Name attribute missing");
                }

            }
        }

        addAllCommentChildrenToList(omElement, opaMediator.getCommentsList());
        return opaMediator;
    }

    @Override
    public QName getTagQName() {

        return OPA_Q;
    }
}

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
    static final QName TOKEN_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "accessToken");
    static final QName POLICY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "policy");
    static final QName Rule_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "rule");
    static final QName PAYLOAD_GENERATOR_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "requestGenerator");
    static final QName ADVANCED_PROPERTIES_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "advancedProperties");
    static final QName PROPERTY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");
    static final QName NAME_Q = new QName("name");



    @Override
    protected Mediator createSpecificMediator(OMElement omElement, Properties properties) {
        OPAMediator opaMediator = new OPAMediator();
        processAuditStatus(opaMediator, omElement);

        OMElement serverUrlElement = omElement.getFirstChildWithName(SERVER_URL_Q);
        if (serverUrlElement != null) {
            opaMediator.setServerUrl(serverUrlElement.getText());
        }

        OMElement opaTokenElement = omElement.getFirstChildWithName(TOKEN_Q);
        if (opaTokenElement != null) {
            opaMediator.setAccessToken(opaTokenElement.getText());
        }

        OMElement policyElement = omElement.getFirstChildWithName(POLICY_Q);
        if (policyElement != null) {
            opaMediator.setPolicy(policyElement.getText());
        }

        OMElement ruleElement = omElement.getFirstChildWithName(Rule_Q);
        if (ruleElement != null) {
            opaMediator.setRule(ruleElement.getText());
        }


        OMElement payloadGeneratorElement = omElement.getFirstChildWithName(PAYLOAD_GENERATOR_Q);
        if (payloadGeneratorElement != null) {
            opaMediator.setRequestGeneratorClassName(payloadGeneratorElement.getText());
        }

        OMElement advancedPropertiesElement = omElement.getFirstChildWithName(ADVANCED_PROPERTIES_Q);
        if (advancedPropertiesElement != null) {
            Iterator parameterIter = advancedPropertiesElement.getChildrenWithName(PROPERTY_Q);
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

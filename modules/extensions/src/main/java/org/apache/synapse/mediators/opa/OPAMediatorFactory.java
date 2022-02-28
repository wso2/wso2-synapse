/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
    static final QName ACCESS_KEY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "accessKey");
    static final QName POLICY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "policy");
    static final QName Rule_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "rule");
    static final QName PAYLOAD_GENERATOR_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "requestGenerator");
    static final QName ADDITIONAL_PARAMETERS_Q =
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "additionalParameters");
    static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "parameter");
    static final QName NAME_Q = new QName("name");

    @Override
    protected Mediator createSpecificMediator(OMElement omElement, Properties properties) {

        OPAMediator opaMediator = new OPAMediator();
        processAuditStatus(opaMediator, omElement);

        OMElement serverUrlElement = omElement.getFirstChildWithName(SERVER_URL_Q);
        if (serverUrlElement != null) {
            opaMediator.setServerUrl(serverUrlElement.getText());
        }

        OMElement opaKeyElement = omElement.getFirstChildWithName(ACCESS_KEY_Q);
        if (opaKeyElement != null) {
            opaMediator.setAccessKey(opaKeyElement.getText());
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

        OMElement advancedPropertiesElement = omElement.getFirstChildWithName(ADDITIONAL_PARAMETERS_Q);
        if (advancedPropertiesElement != null) {
            Iterator parameterIter = advancedPropertiesElement.getChildrenWithName(PARAMETER_Q);
            while (parameterIter.hasNext()) {
                OMElement parameterElement = (OMElement) parameterIter.next();
                OMAttribute nameAtr = parameterElement.getAttribute(NAME_Q);
                if (nameAtr != null) {
                    String parameterName = nameAtr.getAttributeValue();
                    String parameterValue = parameterElement.getText();

                    if (parameterName != null && parameterValue != null) {
                        opaMediator.addAdditionalParameter(parameterName, parameterValue);
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

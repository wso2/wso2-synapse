/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.v2.TriggerError;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Properties;

/**
 * Creates a trigger error mediator through the supplied XML configuration
 * <p/>
 * <pre>
 * &lt;triggererror type="string" (errorMessage="string" | expression="expression")/&gt;
 * </pre>
 */
public class TriggerErrorMediatorFactory extends AbstractMediatorFactory {

    private static final QName ATT_ERROR_MSG = new QName("errorMessage");
    private static final QName ATT_TYPE = new QName("type");
    private static final QName TRIGGER_ERROR_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "triggererror");

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        TriggerError triggerErrorMediator = new TriggerError();
        OMAttribute type = elem.getAttribute(ATT_TYPE);
        OMAttribute errorMsg = elem.getAttribute(ATT_ERROR_MSG);
        OMAttribute expression = elem.getAttribute(ATT_EXPRN);

        if (type == null || type.getAttributeValue().isEmpty()) {
            triggerErrorMediator.setType(SynapseConstants.DEFAULT_ERROR_TYPE);
        } else {
            triggerErrorMediator.setType(type.getAttributeValue());
        }
        if (expression != null) {
            try {
                triggerErrorMediator.setExpression(SynapsePathFactory.getSynapsePath(elem, ATT_EXPRN));
            } catch (JaxenException e) {
                throw new SynapseException("Invalid expression for attribute 'expression' : " +
                        expression.getAttributeValue());
            }
        } else if (errorMsg != null && !errorMsg.getAttributeValue().isEmpty()) {
            triggerErrorMediator.setErrorMsg(errorMsg.getAttributeValue());
        } else {
            triggerErrorMediator.setErrorMsg("Error occurred in the mediation flow");
        }
        return triggerErrorMediator;
    }

    @Override
    public QName getTagQName() {
        return TRIGGER_ERROR_Q;
    }
}

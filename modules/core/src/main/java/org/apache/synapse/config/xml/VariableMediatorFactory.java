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
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.v2.VariableMediator;
import org.jaxen.JaxenException;

import java.util.Properties;
import javax.xml.namespace.QName;

/**
 * Creates a variable mediator through the supplied XML configuration
 * <p/>
 * <pre>
 * &lt;variable name="string" [action=set/remove] (value="literal" | expression="expression") type="string|integer|JSON"/&gt;
 * </pre>
 */
public class VariableMediatorFactory extends AbstractMediatorFactory {

    private static final QName ATT_ACTION = new QName("action");
    private static final QName ATT_TYPE = new QName("type");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        VariableMediator variableMediator = new VariableMediator();
        OMAttribute name = elem.getAttribute(ATT_NAME);
        OMAttribute value = elem.getAttribute(ATT_VALUE);
        OMAttribute expression = elem.getAttribute(ATT_EXPRN);
        OMAttribute action = elem.getAttribute(ATT_ACTION);
        OMAttribute type = elem.getAttribute(ATT_TYPE);

        if (name == null || name.getAttributeValue().isEmpty()) {
            String msg = "The 'name' attribute is required for the configuration of a variable mediator";
            log.error(msg);
            throw new SynapseException(msg);
        } else if ((value == null && expression == null) &&
                !(action != null && "remove".equals(action.getAttributeValue()))) {
            String msg = "'value' or 'expression' attributes is required for a variable mediator when action is SET";
            log.error(msg);
            throw new SynapseException(msg);
        }
        variableMediator.setName(name.getAttributeValue());

        String dataType = null;
        if (type != null) {
            dataType = type.getAttributeValue();
        }

        if (value != null) {
            variableMediator.setValue(value.getAttributeValue(), dataType);
        } else if (expression != null) {
            try {
                variableMediator.setExpression(SynapsePathFactory.getSynapsePath(elem, ATT_EXPRN),
                        dataType);
            } catch (JaxenException e) {
                String msg = "Invalid expression for attribute 'expression' : " +
                        expression.getAttributeValue();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }

        if (action != null && "remove".equals(action.getAttributeValue())) {
            variableMediator.setAction(VariableMediator.ACTION_REMOVE);
        }
        processAuditStatus(variableMediator, elem);
        addAllCommentChildrenToList(elem, variableMediator.getCommentsList());
        return variableMediator;
    }

    public QName getTagQName() {

        return VARIABLE_Q;
    }
}

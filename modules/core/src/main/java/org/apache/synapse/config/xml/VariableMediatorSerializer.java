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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.v2.VariableMediator;

/**
 * <pre>
 * &lt;variable name="string" [action=set/remove] (value="literal" | expression="expression") type="string|integer|JSON"/&gt;
 * </pre>
 */
public class VariableMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof VariableMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        VariableMediator mediator = (VariableMediator) m;
        OMElement variable = fac.createOMElement("variable", synNS);
        saveTracingState(variable, mediator);

        if (mediator.getName() != null) {
            variable.addAttribute(fac.createOMAttribute(
                    "name", nullNS, mediator.getName()));
        } else {
            handleException("Invalid variable mediator. Name is required");
        }

        if (mediator.getValue() != null) {
            variable.addAttribute(fac.createOMAttribute(
                    "value", nullNS, mediator.getValue().toString()));
        } else if (mediator.getExpression() != null) {
            SynapsePathSerializer.serializePath((SynapsePath) mediator.getExpression(),
                    variable, "expression");
        } else if (mediator.getAction() == VariableMediator.ACTION_SET) {
            handleException("Invalid variable mediator. Value or expression is required if " +
                    "action is SET");
        }

        if (mediator.getAction() == VariableMediator.ACTION_REMOVE) {
            variable.addAttribute(fac.createOMAttribute(
                    "action", nullNS, "remove"));
        } else if (mediator.getType() != null) {
            variable.addAttribute(fac.createOMAttribute(
                    "type", nullNS, mediator.getType()));
        }

        serializeComments(variable, mediator.getCommentsList());

        return variable;
    }

    public String getMediatorClassName() {

        return VariableMediator.class.getName();
    }
}

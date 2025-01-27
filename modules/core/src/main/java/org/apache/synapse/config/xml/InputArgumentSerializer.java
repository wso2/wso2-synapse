/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.v2.ext.InputArgument;

import java.util.Map;

/**
 * Serializer for input arguments
 */
public class InputArgumentSerializer {

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();
    private static final OMNamespace nullNS = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");
    private static final String INPUTS = "inputs";
    private static final String ARGUMENT = "argument";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String EXPRESSION = "expression";
    private static final String VALUE = "value";

    public static OMElement serializeInputArguments(Map<String, InputArgument> inputArguments) {

        return serializeInputArguments(inputArguments, false);
    }

    public static OMElement serializeInputArguments(Map<String, InputArgument> inputArguments, boolean includeType) {

        OMElement inputElement = fac.createOMElement(INPUTS, SynapseConstants.SYNAPSE_OMNAMESPACE);
        for (Map.Entry<String, InputArgument> entry : inputArguments.entrySet()) {
            InputArgument inputArgument = entry.getValue();
            OMElement inputArgElement = fac.createOMElement(ARGUMENT, SynapseConstants.SYNAPSE_OMNAMESPACE);
            if (includeType) {
                inputArgElement.addAttribute(fac.createOMAttribute(TYPE, nullNS, inputArgument.getType()));
            }
            inputArgElement.addAttribute(fac.createOMAttribute(NAME, nullNS, inputArgument.getName()));
            if (inputArgument.getExpression() != null) {
                SynapsePathSerializer.serializePath(inputArgument.getExpression(),
                        inputArgument.getExpression().getExpression(), inputArgElement, EXPRESSION);
            } else {
                inputArgElement.addAttribute(fac.createOMAttribute(VALUE, nullNS, inputArgument.getValue().toString()));
            }
            inputElement.addChild(inputArgElement);
        }
        return inputElement;
    }
}

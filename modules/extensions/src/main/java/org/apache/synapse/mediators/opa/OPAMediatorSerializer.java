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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;

import java.util.Map;

public class OPAMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof OPAMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        OPAMediator mediator = (OPAMediator) m;
        OMElement opaElement = fac.createOMElement("opa", synNS);

        saveTracingState(opaElement, mediator);

        if (mediator.getServerUrl() != null) {
            OMElement serverUrlElement = fac.createOMElement(OPAMediatorFactory.SERVER_URL_Q, opaElement);
            serverUrlElement.setText(mediator.getServerUrl());
        } else {
            handleException("Invalid mediator. Server url is required.");
        }

        if (mediator.getAccessKey() != null) {
            OMElement tokenElement = fac.createOMElement(OPAMediatorFactory.ACCESS_KEY_Q, opaElement);
            tokenElement.setText(mediator.getAccessKey());
        }

        if (mediator.getPolicy() != null) {
            OMElement policyElement = fac.createOMElement(OPAMediatorFactory.POLICY_Q, opaElement);
            policyElement.setText(mediator.getPolicy());
        } else {
            handleException("Invalid mediator. Policy is required.");
        }

        if (mediator.getRule() != null) {
            OMElement ruleElement = fac.createOMElement(OPAMediatorFactory.Rule_Q, opaElement);
            ruleElement.setText(mediator.getRule());
        } else {
            handleException("Invalid mediator. Rule is required.");
        }

        if (mediator.getRequestGeneratorClassName() != null) {
            OMElement requestGeneratorElement = fac.createOMElement(OPAMediatorFactory.PAYLOAD_GENERATOR_Q, opaElement);
            requestGeneratorElement.setText(mediator.getRequestGeneratorClassName());
        }

        if (mediator.getAdditionalParameters() != null && !mediator.getAdditionalParameters().isEmpty()) {
            OMElement additionalParametersElement =
                    fac.createOMElement(OPAMediatorFactory.ADDITIONAL_PARAMETERS_Q, opaElement);

            Map additionalParameters = mediator.getAdditionalParameters();
            for (String parameter : mediator.getAdditionalParameters().keySet()) {
                Object parameterValue = additionalParameters.get(parameter);

                OMElement parameterElement = fac.createOMElement(OPAMediatorFactory.PARAMETER_Q, additionalParametersElement);
                parameterElement.addAttribute(fac.createOMAttribute("name", nullNS, parameter));
                parameterElement.setText(String.valueOf(parameterValue));
            }

        }

        return opaElement;
    }

    public String getMediatorClassName() {

        return OPAMediator.class.getName();
    }
}

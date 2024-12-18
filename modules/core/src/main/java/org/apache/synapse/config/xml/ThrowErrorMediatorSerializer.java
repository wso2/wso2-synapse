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
import org.apache.synapse.mediators.v2.ThrowError;

/**
 * <pre>
 * &lt;throwError (type="string") (errorMessage=("string" | expression))/&gt;
 * </pre>
 */
public class ThrowErrorMediatorSerializer extends AbstractMediatorSerializer{

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof ThrowError)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        ThrowError mediator = (ThrowError) m;
        OMElement throwError = fac.createOMElement("throwError", synNS);
        saveTracingState(throwError, mediator);

        if (mediator.getType() != null) {
            throwError.addAttribute(fac.createOMAttribute("type", nullNS, mediator.getType()));
        }
        if (mediator.getErrorMsg() != null) {
            ValueSerializer valueSerializer = new ValueSerializer();
            valueSerializer.serializeValue(mediator.getErrorMsg(), "errorMessage", throwError);
        }

        serializeComments(throwError, mediator.getCommentsList());
        return throwError;
    }

    @Override
    public String getMediatorClassName() {
        return ThrowError.class.getName();
    }
}

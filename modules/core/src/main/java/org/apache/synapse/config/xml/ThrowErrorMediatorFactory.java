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
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.v2.ThrowError;

import javax.xml.namespace.QName;
import java.util.Properties;

/**
 * Creates a throw error mediator through the supplied XML configuration
 * <p/>
 * <pre>
 * &lt;throwError (type="string") (errorMessage=("string" | expression))/&gt;
 * </pre>
 */
public class ThrowErrorMediatorFactory extends AbstractMediatorFactory {

    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String THROW_ERROR = "throwError";
    private static final QName ATT_ERROR_MSG = new QName(ERROR_MESSAGE);
    private static final QName ATT_TYPE = new QName("type");
    private static final QName THROW_ERROR_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, THROW_ERROR);

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        ThrowError throwErrorMediator = new ThrowError();
        OMAttribute type = elem.getAttribute(ATT_TYPE);
        OMAttribute errorMsg = elem.getAttribute(ATT_ERROR_MSG);

        if (type == null || type.getAttributeValue().isEmpty()) {
            throwErrorMediator.setType(SynapseConstants.DEFAULT_ERROR_TYPE);
        } else {
            throwErrorMediator.setType(type.getAttributeValue());
        }
        if (errorMsg == null || errorMsg.getAttributeValue().isEmpty()) {
            throwErrorMediator.setErrorMsg(new Value("Error occurred in the mediation flow"));
        } else {
            ValueFactory msgFactory = new ValueFactory();
            Value generatedMsg = msgFactory.createValue(ERROR_MESSAGE, elem);
            throwErrorMediator.setErrorMsg(generatedMsg);
        }
        return throwErrorMediator;
    }

    @Override
    public QName getTagQName() {
        return THROW_ERROR_Q;
    }
}

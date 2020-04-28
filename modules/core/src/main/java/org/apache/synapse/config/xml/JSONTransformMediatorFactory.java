/**
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.builtin.JSONTransformMediator;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Properties;

/**
 * Mediator Factory for JSON Transform mediator
 */
public class JSONTransformMediatorFactory extends AbstractMediatorFactory {
    private static final Log log = LogFactory.getLog(JSONTransformMediatorFactory.class);
    private static final QName JSON_TRANSFORM_Q = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "jsontransform");
    private static final QName ATT_SCHEMA = new QName("schema");

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        JSONTransformMediator JSONTransformMediator = new JSONTransformMediator();
        processAuditStatus(JSONTransformMediator, elem);
        OMAttribute schema = elem.getAttribute(ATT_SCHEMA);
        if (schema != null) {
            // ValueFactory for creating dynamic or static Value
            ValueFactory keyFac = new ValueFactory();
            // create dynamic or static key based on OMElement
            Value generatedKey = keyFac.createValue("schema", elem);
            JSONTransformMediator.setSchemaKey(generatedKey);
        }
        List<MediatorProperty> mediatorPropertyList = MediatorPropertyFactory.getMediatorProperties(elem);
        if (!mediatorPropertyList.isEmpty()) {
            JSONTransformMediator.addAllProperties(mediatorPropertyList);
        }
        if (schema == null && mediatorPropertyList.isEmpty()) {
            handleException(JSON_TRANSFORM_Q.getLocalPart() +
                    " mediator should contain either a schema or custom properties");
        }
        addAllCommentChildrenToList(elem, JSONTransformMediator.getCommentsList());
        return JSONTransformMediator;
    }

    @Override
    public QName getTagQName() {
        return JSON_TRANSFORM_Q;
    }
}

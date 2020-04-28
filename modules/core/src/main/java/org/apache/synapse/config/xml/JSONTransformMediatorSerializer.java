/**
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.JSONTransformMediator;

/**
 * Serializer for JSON Transform mediator
 */
public class JSONTransformMediatorSerializer extends AbstractMediatorSerializer {
    @Override
    protected OMElement serializeSpecificMediator(Mediator mediator) {
        if (!(mediator instanceof JSONTransformMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + mediator.getType());
        }
        JSONTransformMediator jsonTransformMediator = (JSONTransformMediator) mediator;
        OMElement transformElement = fac.createOMElement("jsontransform", synNS);
        boolean isSchemaEmpty = jsonTransformMediator.getSchemaKey() == null;
        if (!isSchemaEmpty) {
            // Serialize Value using ValueSerializer
            ValueSerializer keySerializer = new ValueSerializer();
            keySerializer.serializeValue(jsonTransformMediator.getSchemaKey(), "schema", transformElement);
        }
        boolean isPropertiesEmpty = jsonTransformMediator.getProperties() == null ||
                jsonTransformMediator.getProperties().isEmpty();
        if (!isPropertiesEmpty) {
            super.serializeProperties(transformElement, jsonTransformMediator.getProperties());
        }
        if (isSchemaEmpty && isPropertiesEmpty) {
            handleException("Invalid JSONTransform mediator. Should either contain schema or properties");
        }
        saveTracingState(transformElement, mediator);
        serializeComments(transformElement, jsonTransformMediator.getCommentsList());
        return transformElement;
    }

    @Override
    public String getMediatorClassName() {
        return JSONTransformMediator.class.getName();
    }
}

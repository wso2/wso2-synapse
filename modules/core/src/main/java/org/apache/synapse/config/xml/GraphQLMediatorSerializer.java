/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.GraphQLMediator;


/**
 * Serializer for {@link GraphQLMediator} instances.
 */
public class GraphQLMediatorSerializer extends AbstractMediatorSerializer {

    /**.
     * This method will implement the serializeMediator method of the MediatorSerializer interface
     * and implements the serialization of GraphQLMediator to its configuration
     *
     * @param mediator Mediator of the type GraphQLMediator which is subjected to the serialization
     * @return OMElement serialized in to xml from the given parameters
     */

    protected OMElement serializeSpecificMediator(Mediator mediator) {

        if (!(mediator instanceof GraphQLMediator)) {
            handleException("Unsupported mediator passed in for serialization : "
                    + mediator.getType());
        }

        GraphQLMediator graphQLMediator = (GraphQLMediator) mediator;

        OMElement graphql = fac.createOMElement("graphql", synNS);
        saveTracingState(graphql, graphQLMediator);

        OMElement runtimeWiringClassNameElement = fac.createOMElement(
                GraphQLMediatorFactory.RUNTIMEWIRING_CLASS_NAME_Q);
        saveTracingState(runtimeWiringClassNameElement , graphQLMediator);

        OMElement schemaPathElement = fac.createOMElement(GraphQLMediatorFactory.SCHEMA_PATH_Q);
        saveTracingState(schemaPathElement , graphQLMediator);

        return graphql;
    }

    public String getMediatorClassName() {
        return GraphQLMediator.class.getName();
    }
}

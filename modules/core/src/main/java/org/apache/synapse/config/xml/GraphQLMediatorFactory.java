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
import org.apache.synapse.SynapseException;

import org.apache.synapse.mediators.builtin.GraphQLMediator;

import java.util.Properties;

import javax.xml.namespace.QName;

/**
 * Factory for {@link GraphQLMediator} instances.
 */
public class GraphQLMediatorFactory extends AbstractMediatorFactory {

    /**.
     * This will hold the QName of the graphql mediator element in the xml configuration
     */

    static final QName GRAPHQL_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "graphql");
    static final  QName SCHEMA_PATH_Q = new QName(
            XMLConfigConstants.SYNAPSE_NAMESPACE, "schemaPath");
    static  final QName RUNTIMEWIRING_CLASS_NAME_Q = new QName(
            XMLConfigConstants.SYNAPSE_NAMESPACE, "runtimeWiringClassName");


    protected Mediator createSpecificMediator(OMElement omElement, Properties properties) {

        //create new GraphQL Mediator
        GraphQLMediator graphQLMediator = new GraphQLMediator();

        //setup initial settings
        processTraceState(graphQLMediator, omElement);

        OMElement runtimeWiringClassNameElement = omElement.getFirstChildWithName(RUNTIMEWIRING_CLASS_NAME_Q);

        if (runtimeWiringClassNameElement != null) {
            String runtimeWiringClassName = String.valueOf(runtimeWiringClassNameElement.getText());
            graphQLMediator.setRuntimeWiringClassName(runtimeWiringClassName);
        } else {
            throw new SynapseException("RuntimeWiring class name element os missing");
        }

        OMElement schemaPathElement = omElement.getFirstChildWithName(SCHEMA_PATH_Q);

        if (schemaPathElement != null) {
            String schemaPath =  String.valueOf(schemaPathElement.getText());
            graphQLMediator.setSchemaPath(schemaPath);

        } else {
            throw new SynapseException("Schema Path element is missing");
        }

        return graphQLMediator;
    }

    public QName getTagQName() {
        return GRAPHQL_Q;
    }
}

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


package org.apache.synapse.mediators.builtin;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.synapse.MessageContext;
import org.apache.synapse.RuntimewiringInterface;
import org.apache.synapse.mediators.AbstractMediator;

import java.io.File;
import java.net.URL;

/**
 * Hold the reference to the graphql api that is created at the startup.
 * The query or mutation or subscription will be executed by the graphql api and return the json response.
 */
public class GraphQLMediator extends AbstractMediator {

    /**The reference to the graphql api.*/
    private GraphQL graphql;

    /** The path of the graphql schema to create the graphql api at the stratup.*/
    private String schemaPath;

    /** The RuntimeWiring class name to access the buildwiring method.*/
    private String runtimeWiringClassName;


    private void init() {
        try {
            URL url = new File(schemaPath).toURI().toURL();
            String sdl = Resources.toString(url, Charsets.UTF_8);
            GraphQLSchema graphQLSchema = buildSchema(sdl);
            this.graphql = GraphQL.newGraphQL(graphQLSchema).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean mediate(MessageContext messageContext) {
        ExecutionResult executionResult = graphql.execute((String) messageContext.getProperty("query"), messageContext);
        messageContext.setProperty("result", new Gson().toJson(executionResult.toSpecification()));
        return true;
    }

    private GraphQLSchema buildSchema(String sdl)
            throws  ClassNotFoundException, IllegalAccessException, InstantiationException {

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);

        Class clazz = this.getClass().getClassLoader().loadClass(runtimeWiringClassName.trim());
        RuntimewiringInterface datafetcher = (RuntimewiringInterface) clazz.newInstance();

        RuntimeWiring runtimeWiring  = datafetcher.buildWiring();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);


    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
        init();
    }

    public void setRuntimeWiringClassName(String runtimeWiringClassName) {
        this.runtimeWiringClassName = runtimeWiringClassName;
    }
}

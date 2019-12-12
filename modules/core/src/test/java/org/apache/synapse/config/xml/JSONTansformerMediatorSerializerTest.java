/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.config.xml;

/**
 * Serializer tests for the JSONTransformMediator
 */
public class JSONTansformerMediatorSerializerTest extends AbstractTestCase {

    private JSONTransformMediatorFactory jsonTransformMediatorFactory;
    private JSONTransformMediatorSerializer jsonTransformMediatorSerializer;

    public JSONTansformerMediatorSerializerTest() {

        super(JSONTansformerMediatorSerializerTest.class.getName());
        jsonTransformMediatorFactory = new JSONTransformMediatorFactory();
        jsonTransformMediatorSerializer = new JSONTransformMediatorSerializer();
    }

    public void testJSONTransformWithSchema() {

        String inputXml = "<jsontransform xmlns=\"http://ws.apache.org/ns/synapse\" schema=\"schema.json\" />";
        assertTrue(serialization(inputXml, jsonTransformMediatorFactory, jsonTransformMediatorSerializer));
        assertTrue(serialization(inputXml, jsonTransformMediatorSerializer));
    }

    public void testJSONTransformWithProperties() {

        String inputXml = "<jsontransform xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<property name=\"check\" value=\"true\" />" +
                "<property name=\"check1\" value=\"false\" />" +
                "</jsontransform>";
        assertTrue(serialization(inputXml, jsonTransformMediatorFactory, jsonTransformMediatorSerializer));
        assertTrue(serialization(inputXml, jsonTransformMediatorSerializer));
    }

    public void testJSONTransformWithSchemaAndProperties() {

        String inputXml = "<jsontransform xmlns=\"http://ws.apache.org/ns/synapse\" schema=\"schema.json\">" +
                "<property name=\"check\" value=\"true\" />" +
                "<property name=\"check1\" value=\"false\" />" +
                "</jsontransform>";
        assertTrue(serialization(inputXml, jsonTransformMediatorFactory, jsonTransformMediatorSerializer));
        assertTrue(serialization(inputXml, jsonTransformMediatorSerializer));
    }
}

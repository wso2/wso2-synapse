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

/**
 * Factory and Serializer tests for the ScatterGatherMediator
 */

public class ScatterGatherMediatorSerializationTest extends AbstractTestCase {

    private ScatterGatherMediatorFactory scatterGatherMediatorFactory;
    private ScatterGatherMediatorSerializer scatterGatherMediatorSerializer;

    public ScatterGatherMediatorSerializationTest() {

        super(ScatterGatherMediatorSerializer.class.getName());
        scatterGatherMediatorFactory = new ScatterGatherMediatorFactory();
        scatterGatherMediatorSerializer = new ScatterGatherMediatorSerializer();
    }

    public void testScatterGatherSerialization() {

        String inputXML = "<scatter-gather xmlns=\"http://ws.apache.org/ns/synapse\" parallel-execution=\"true\">" +
                "<aggregation value-to-aggregate=\"json-eval($)\" /><target><sequence>" +
                "<payloadFactory media-type=\"json\"><format>{                    \"pet\": {                        " +
                "\"name\": \"pet1\",                        \"type\": \"dog\"                    },                    " +
                "\"status\": \"success\"                    }</format><args/></payloadFactory><log level=\"custom\">" +
                "<property name=\"Message\" value=\"==== DONE scatter target 1 ====\"/></log></sequence></target>" +
                "<target><sequence><call><endpoint><http method=\"GET\" uri-template=\"http://localhost:5454/api/pet2\"/>" +
                "</endpoint></call><log level=\"custom\"><property name=\"Message\" value=\"==== DONE scatter target 2 ====\"/>" +
                "</log></sequence></target></scatter-gather>";

        assertTrue(serialization(inputXML, scatterGatherMediatorFactory, scatterGatherMediatorSerializer));
    }
}

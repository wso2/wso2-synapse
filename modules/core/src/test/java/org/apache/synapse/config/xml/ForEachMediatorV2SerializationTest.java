/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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
 * Factory and Serializer tests for the ForEach mediator
 */
public class ForEachMediatorV2SerializationTest extends AbstractTestCase {

    ForEachMediatorFactory foreachMediatorFactory;
    ForEachMediatorV2Serializer foreachMediatorSerializer;

    public ForEachMediatorV2SerializationTest() {

        super(ForEachMediatorV2SerializationTest.class.getName());
        foreachMediatorFactory = new ForEachMediatorFactory();
        foreachMediatorSerializer = new ForEachMediatorV2Serializer();
    }

    public void testForEachMediatorV2_continueWithoutAggregation() throws Exception {

        String inputXML = "<foreach collection=\"${payload.array}\" parallel-execution=\"true\" " +
                "continue-without-aggregation=\"true\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<sequence>" +
                "<log logMessageID=\"false\">" +
                "<message>Processing payload ${payload}</message>" +
                "</log>" +
                "</sequence>" +
                "</foreach>";
        assertTrue(serialization(inputXML, foreachMediatorFactory, foreachMediatorSerializer));
        assertTrue(serialization(inputXML, foreachMediatorSerializer));
    }

    public void testForEachMediatorV2_XML_Variable_Out() throws Exception {

        String inputXML = "<foreach collection=\"${payload.products}\" parallel-execution=\"true\" " +
                "update-original=\"false\" target-variable=\"foreach_out\" result-content-type=\"XML\"" +
                " result-enclosing-element=\"ForEachResult\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<sequence>" +
                "<payloadFactory media-type=\"xml\"><format><data xmlns=\"\">updated</data></format><args /></payloadFactory>" +
                "</sequence>" +
                "</foreach>";
        assertTrue(serialization(inputXML, foreachMediatorFactory, foreachMediatorSerializer));
        assertTrue(serialization(inputXML, foreachMediatorSerializer));
    }

    public void testForEachMediatorV2_JSON() throws Exception {

        String inputXML = "<foreach collection=\"${payload.products}\" parallel-execution=\"true\"" +
                " update-original=\"true\" xmlns=\"http://ws.apache.org/ns/synapse\">" +
                "<sequence>" +
                "<payloadFactory media-type=\"json\">" +
                "<format>{\"data\":\"updated\"}</format><args />" +
                "</payloadFactory>" +
                "</sequence>" +
                "</foreach>";
        assertTrue(serialization(inputXML, foreachMediatorFactory, foreachMediatorSerializer));
        assertTrue(serialization(inputXML, foreachMediatorSerializer));
    }
}

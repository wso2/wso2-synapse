/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;


public class ConditionalRouterMediatorSerializationTest extends AbstractTestCase {
    private ConditionalRouterMediatorSerializer serializer = null;

    private ConditionalRouterMediatorFactory factory = null;

    public ConditionalRouterMediatorSerializationTest() {
        serializer = new ConditionalRouterMediatorSerializer();
        factory = new ConditionalRouterMediatorFactory();
    }

    public void testRouterMediatorSerializationScenarioOne() throws Exception {
        String inputXml = "<conditionalRouter xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                          "<conditionalRoute><condition><equal type=\"url\" value=\"http://localhost:9000.*\" />" +
                          "</condition><target><sequence statisticId=\"86116\">" +
                          "<log statisticId=\"186215\" level=\"full\" /></sequence></target>" +
                          "</conditionalRoute></conditionalRouter>";
        
        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testRouterMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<conditionalRouter xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"41112\">" +
                          "<conditionalRoute><condition><and><equal type=\"url\" value=\"http://localhost:9000.*\" />" +
                          "<match type=\"url\" regex=\"http://localhost:9000.*\" /></and></condition><target>" +
                          "<sequence statisticId=\"251153\"><log statisticId=\"51156\" level=\"full\" />" +
                          "</sequence></target></conditionalRoute></conditionalRouter>";

        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testRouterMediatorSerializationScenarioThree() throws Exception {
        String inputXml = "<conditionalRouter xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"1334919\">" +
                          "<conditionalRoute><condition><or><equal type=\"url\" value=\"http://localhost:9000.*\" />" +
                          "<match type=\"url\" regex=\"http://localhost:9000.*\" /></or></condition><target>" +
                          "<sequence statisticId=\"34938\"><log statisticId=\"334941\" level=\"full\" /></sequence>" +
                          "</target></conditionalRoute></conditionalRouter>";

        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }

    public void testRouterMediatorSerializationScenarioFour() throws Exception {
        String inputXml = "<conditionalRouter xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"406413\">" +
                          "<conditionalRoute><condition><not><equal type=\"url\" value=\"http://localhost:9000.*\" />" +
                          "</not></condition><target><sequence statisticId=\"06431\">" +
                          "<log statisticId=\"406434\" level=\"full\" /></sequence>" +
                          "</target></conditionalRoute></conditionalRouter>";

        assertTrue(serialization(inputXml, factory, serializer));
        assertTrue(serialization(inputXml, serializer));
    }
}

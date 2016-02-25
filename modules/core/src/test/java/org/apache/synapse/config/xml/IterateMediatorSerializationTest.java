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

import org.apache.synapse.SynapseException;

/**
 * Factory and Serializer tests for the IterateMediator
 */
public class IterateMediatorSerializationTest extends AbstractTestCase {

    IterateMediatorFactory iterateMediatorFactory;
    IterateMediatorSerializer iterateMediatorSerializer;

    public IterateMediatorSerializationTest() {
        super(IterateMediatorSerializationTest.class.getName());
        iterateMediatorFactory = new IterateMediatorFactory();
        iterateMediatorSerializer = new IterateMediatorSerializer();
    }

    public void testIterateMediatorSerializationSenarioOne() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" " +
                          "continueParent=\"true\" preservePayload=\"true\" expression=\".\" " +
                          "attachPath=\"get-property('to')\"><target sequence=\"sequenceRef1\" " +
                          "endpoint=\"endpointRef1\"/>" + "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationSenarioOneWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" " +
                          "continueParent=\"true\" preservePayload=\"true\" expression=\".\" " +
                          "attachPath=\"get-property('to')\"><!--Test Comment--><target sequence=\"sequenceRef1\" " +
                          "endpoint=\"endpointRef1\"/>" + "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));

    }

    public void testIterateMediatorSerializationScenarioTwo() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" expression=\".\" statisticId=\"186104\">" +
                          "<target endpoint=\"endpointRef1\"><sequence statisticId=\"186104\">" +
                          "<log statisticId=\"186104\"/></sequence></target>" + "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationScenarioTwoWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\"><!--Test Comment-->" +
                          "<target endpoint=\"endpointRef1\"><sequence statisticId=\"186104\">" +
                          "<!--Test Comment--><log statisticId=\"186104\"/></sequence></target>" +
                          "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationScenarioThree() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\">" +
                          "<target><sequence statisticId=\"186104\"><send statisticId=\"186104\"/></sequence><endpoint>" +
                          "<address statisticId=\"186104\" uri=\"http://testURL2\"/>" +
                          "</endpoint></target></iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationScenarioThreeWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\"><!--Test Comment-->" +
                          "<target><sequence statisticId=\"186104\"><!--Test Comment--><send statisticId=\"186104\"/></sequence><endpoint>" +
                          "<address statisticId=\"186104\" uri=\"http://testURL2\"/>" +
                          "</endpoint></target></iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationScenarioFour() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\">" +
                          "<target soapAction=\"urn:test\" to=\"http://localhost:7777\"><sequence statisticId=\"186104\">" +
                          "<send statisticId=\"186104\"/>" +
                          "</sequence><endpoint><address statisticId=\"186104\" uri=\"http://testURL2\"/></endpoint></target>" +
                          "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));
    }

    public void testIterateMediatorSerializationScenarioFourWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\"><!--Test Comment-->" +
                          "<target soapAction=\"urn:test\" to=\"http://localhost:7777\"><sequence statisticId=\"186104\">" +
                          "<!--Test Comment--><send statisticId=\"186104\"/>" +
                          "</sequence><endpoint><address uri=\"http://testURL2\" statisticId=\"186104\"/></endpoint></target>" +
                          "</iterate>";
        assertTrue(serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer));
        assertTrue(serialization(inputXml, iterateMediatorSerializer));

    }

    public void testIterateMediatorSerializationScenarioFive() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\" expression=\".\" " +
                          "attachPath=\".\" preservePayload=\"false\"><target to=\"http://localhost:7777\">" +
                          "<sequence statisticId=\"186104\"><send statisticId=\"186104\"/></sequence>" +
                          "<endpoint><address statisticId=\"186104\" uri=\"http://testURL2\"/></endpoint>" +
                          "</target></iterate>";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

    public void testIterateMediatorSerializationScenarioFiveWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" expression=\".\" " +
                          "attachPath=\".\" preservePayload=\"false\" statisticId=\"186104\"><!--Test Comment-->" +
                          "<target to=\"http://localhost:7777\">" +
                          "<sequence statisticId=\"186104\"><!--Test Comment--><send/></sequence><endpoint>" +
                          "<address uri=\"http://testURL2\" statisticId=\"186104\"/></endpoint>" +
                          "</target></iterate>";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

    public void testIterateMediatorSerializationScenarioSix() throws Exception {
        String inputXml =
                "<clone xmlns=\"http://ws.apache.org/ns/synapse\" expression=\".\" attachPath=\".\" statisticId=\"186104\">" +
                "<target to=\"http://localhost:7777\"><sequence statisticId=\"186104\"><send statisticId=\"186104\"/></sequence><endpoint>" +
                "<address uri=\"http://testURL2\" statisticId=\"186104\"/></endpoint></target><target soapAction=" +
                "\"urn:test\" sequence=\"sequenceRef2\" endpoint=\"endpointRef2\"/></clone> ";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

    public void testIterateMediatorSerializationScenarioSixWithComments() throws Exception {
        String inputXml =
                "<clone xmlns=\"http://ws.apache.org/ns/synapse\" expression=\".\" attachPath=\".\" statisticId=\"186104\"><!--Test Comment-->" +
                "<target to=\"http://localhost:7777\"><sequence statisticId=\"186104\"><!--Test Comment--><send statisticId=\"186104\"/></sequence><endpoint>" +
                "<address uri=\"http://testURL2\" statisticId=\"186104\"/></endpoint></target><target soapAction=" +
                "\"urn:test\" sequence=\"sequenceRef2\" endpoint=\"endpointRef2\"/></clone> ";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

    public void testIterateMediatorSerializationScenarioSeven() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" expression=\".\" sequential=\"true\" statisticId=\"186104\">" +
                          "<target endpoint=\"endpointRef1\"><sequence statisticId=\"186104\"><log statisticId=\"186104\"/></sequence></target>" + "</iterate>";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

    public void testIterateMediatorSerializationScenarioSevenWithComments() throws Exception {
        String inputXml = "<iterate xmlns=\"http://ws.apache.org/ns/synapse\" " +
                          "statisticId=\"186104\" expression=\".\" sequential=\"true\"><!--Test Comment-->" +
                          "<target endpoint=\"endpointRef1\"><sequence statisticId=\"186104\">" +
                          "<!--Test Comment--><log statisticId=\"186104\"/></sequence></target>" + "</iterate>";
        try {
            serialization(inputXml, iterateMediatorFactory, iterateMediatorSerializer);
            serialization(inputXml, iterateMediatorSerializer);
        } catch (SynapseException syne) {
            assertTrue(true);
        }
    }

}

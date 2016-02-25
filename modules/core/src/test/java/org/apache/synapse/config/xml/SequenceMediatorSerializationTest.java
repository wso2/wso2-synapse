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

/**
 *
 *
 */

public class SequenceMediatorSerializationTest extends AbstractTestCase {

    SequenceMediatorFactory sequenceMediatorFactory;
    SequenceMediatorSerializer sequenceMediatorSerializer;

    public SequenceMediatorSerializationTest() {
        super(SequenceMediatorSerializationTest.class.getName());
        sequenceMediatorFactory = new SequenceMediatorFactory();
        sequenceMediatorSerializer = new SequenceMediatorSerializer();
    }

    public void testSequenceMediatorSerializationSenarioOne() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"namedsequence\" statisticId=\"186104\">" +
                     "<header statisticId=\"186104\" name=\"To\" value=\"http://localhost:9000/services/TestService\"/>" +
                     "<send statisticId=\"186104\"/>" +
                     "</sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioTwo() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" " +
                     "name=\"namedsequence\"  onError=\"ErrorHandler\" statisticId=\"186104\">" +
                     "<header statisticId=\"186104\" name=\"To\" value=\"http://localhost:9000/services/TestService\"/>" +
                     "<send statisticId=\"186104\"/></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioTwoWithDescription() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" " +
                     "name=\"namedsequence\"  onError=\"ErrorHandler\" statisticId=\"186104\">" +
                     "<description>Test documentation</description>" +
                     "<header statisticId=\"186104\" name=\"To\" value=\"http://localhost:9000/services/TestService\"/>" +
                     "<send statisticId=\"186104\"/></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioThree() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" key=\"sequenceone\"></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioThreeWithDescription() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" key=\"sequenceone\">" +
                     "<description>Test description</description></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioFour() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"sequenceone\" " +
                     "onError=\"ErrorHandler\" statisticId=\"186104\"></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioFive() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" key=\"sequenceone\"></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioSix() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"sequenceone\" statisticId=\"186104\"" +
                     " description=\"short description\"></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioOneWithComments() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"namedsequence\" statisticId=\"186104\">" +
                     "<header statisticId=\"186104\" name=\"To\" value=\"http://localhost:9000/services/TestService\"/>" +
                     "<!--Test Comment--><send statisticId=\"186104\"/></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioTwoWithComments() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" " +
                     "name=\"namedsequence\"  onError=\"ErrorHandler\" statisticId=\"186104\">" +
                     "<header statisticId=\"186104\" name=\"To\" value=\"http://localhost:9000/services/TestService\"/>" +
                     "<!--Test Comment--><send statisticId=\"186104\"/></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioFourWithComments() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" " +
                     "name=\"sequenceone\" onError=\"ErrorHandler\" statisticId=\"186104\">" +
                     "<!--Test Comment--></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }

    public void testSequenceMediatorSerializationSenarioSixWithComments() throws Exception {
        String xml = "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"sequenceone\" " +
                     " statisticId=\"186104\" description=\"short description\">" +
                     "<!--Test Comment--></sequence>";
        assertTrue(serialization(xml, sequenceMediatorFactory, sequenceMediatorSerializer));
        assertTrue(serialization(xml, sequenceMediatorSerializer));
    }
}

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

public class OutMediatorSerializationTest extends AbstractTestCase {

    OutMediatorFactory outMediatorFactory;
    OutMediatorSerializer outMediatorSerializer;

    public OutMediatorSerializationTest() {
        super(OutMediatorSerializationTest.class.getName());
        outMediatorFactory = new OutMediatorFactory();
        outMediatorSerializer = new OutMediatorSerializer();
    }

    public void testOutMediatorSerialization() throws Exception {
        String inptXml = " <out xmlns=\"http://ws.apache.org/ns/synapse\"><header name=\"To\" value=\"http://64.124.140.30:9090/soap\"/></out>";
        assertTrue(serialization(inptXml, outMediatorFactory, outMediatorSerializer));
        assertTrue(serialization(inptXml, outMediatorSerializer));
    }

    public void testOutMediatorSerializationWithComment() throws Exception {
        String inptXml = " <out xmlns=\"http://ws.apache.org/ns/synapse\"><header name=\"To\" value=\"http://64.124.140.30:9090/soap\"/><!--Test Comment--></out>";
        assertTrue(serialization(inptXml, outMediatorFactory, outMediatorSerializer));
        assertTrue(serialization(inptXml, outMediatorSerializer));
    }
}

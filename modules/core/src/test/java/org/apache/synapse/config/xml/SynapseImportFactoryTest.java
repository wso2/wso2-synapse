/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements. See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership. The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.util.Properties;

/**
 * Unit tests for SynapseImportFactory class.
 */
public class SynapseImportFactoryTest {

    private final String XML1 = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\"> </property>\n";
    private final String XML2 = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
            "name=\"testName\"> </property>\n";
    private final String XML3 = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
            "name=\"testName\" package=\"testPackage\"> </property>\n";
    private final String XML4 = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
            "name=\"testName\" package=\"testPackage\" status=\"enabled\"> </property>\n";

    /**
     * Test CreateImport method using different XML formats.
     * @throws XMLStreamException
     */
    @Test
    public void testCreateImport() throws XMLStreamException {
        Properties properties = new Properties();
        OMElement element = AXIOMUtil.stringToOM(XML1);
        try {
            SynapseImportFactory.createImport(element,properties);
            Assert.fail("execution successful where exception is expected");
        } catch (Exception ex) {
            Assert.assertEquals("asserting exception class",SynapseException.class,ex.getClass());
            Assert.assertEquals("asserting exception message",
                    "Synapse Import Target Library name is not specified",ex.getMessage());
        }
        element = AXIOMUtil.stringToOM(XML2);
        try {
            SynapseImportFactory.createImport(element,properties);
            Assert.fail("execution successful where exception is expected");
        } catch (Exception ex) {
            Assert.assertEquals("asserting exception class",SynapseException.class,ex.getClass());
            Assert.assertEquals("asserting exception message",
                    "Synapse Import Target Library package is not specified",ex.getMessage());
        }
        element = AXIOMUtil.stringToOM(XML3);
        SynapseImport synapseImport = SynapseImportFactory.createImport(element,properties);
        Assert.assertFalse("status should be false when status data not provided",synapseImport.isStatus());
        element = AXIOMUtil.stringToOM(XML4);
        synapseImport = SynapseImportFactory.createImport(element,properties);
        Assert.assertTrue("status should be enabled as specified in XML",synapseImport.isStatus());
    }
}

/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.namespace.QName;

/**
 * Unit tests for SynapseImportSerializer class.
 */
public class SynapseImportSerializerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test serializeImport method with null value.
     */
    @Test
    public void testSerializeImportNUll() {
        thrown.expect(SynapseException.class);
        thrown.expectMessage("Unsupported Synapse Import passed in for serialization");
        SynapseImportSerializer.serializeImport(null);
    }

    /**
     * Test serializeImport with incomplete synapseImport.
     */
    @Test
    public void testSerializeImportNoLibName() {
        SynapseImport synapseImport = new SynapseImport();
        thrown.expect(SynapseException.class);
        thrown.expectMessage("Invalid Synapse Import. Target Library name is required");
        SynapseImportSerializer.serializeImport(synapseImport);
    }

    /**
     * Test serializeImport with incomplete synapseImport.
     */
    @Test
    public void testSerializeImportNoLibPackage() {
        SynapseImport synapseImport = new SynapseImport();
        synapseImport.setLibName("testLibName");
        thrown.expect(SynapseException.class);
        thrown.expectMessage("Invalid Synapse Import. Target Library package is required");
        SynapseImportSerializer.serializeImport(synapseImport);
    }

    /**
     * Test SerializeImport with valid synapseImport, status disabled.
     */
    @Test
    public void testSerializeImportDisabled() {
        SynapseImport synapseImport = new SynapseImport();
        synapseImport.setLibName("testLibName");
        synapseImport.setLibPackage("testVersion");
        OMElement element = SynapseImportSerializer.serializeImport(synapseImport);
        Assert.assertEquals("asserting OMElement name", "import", element.getLocalName());
        OMAttribute temp = element.getAttribute(new QName("name"));
        Assert.assertEquals("asserting name attribute", "testLibName", temp.getAttributeValue());
        temp = element.getAttribute(new QName("package"));
        Assert.assertEquals("asserting package attribute", "testVersion", temp.getAttributeValue());
        temp = element.getAttribute(new QName("status"));
        Assert.assertEquals("asserting status attribute", "disabled", temp.getAttributeValue());
    }

    /**
     * Test SerializeImport with valid synapseImport, status enabled.
     */
    @Test
    public void testSerializeImportEnabled() {
        SynapseImport synapseImport = new SynapseImport();
        synapseImport.setStatus(true);
        synapseImport.setLibName("testLibName");
        synapseImport.setLibPackage("testVersion");
        OMElement element = SynapseImportSerializer.serializeImport(synapseImport);
        OMAttribute temp = element.getAttribute(new QName("status"));
        Assert.assertEquals("asserting stats attribute", "enabled", temp.getAttributeValue());
    }
}

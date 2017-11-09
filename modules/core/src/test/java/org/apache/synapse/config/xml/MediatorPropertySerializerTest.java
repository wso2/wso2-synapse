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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.MediatorProperty;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for MediatorPropertySerializer.
 */
public class MediatorPropertySerializerTest {

    private final String XML = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" > </property>\n";
    private final String NAME = "testName";
    private final String VALUE = "testValue";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test SerializeMediatorProperties for correct insertion of properties.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testSerializeMediatorProperties() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM(XML);
        List<MediatorProperty> propertyList = new ArrayList<>();
        MediatorProperty property = new MediatorProperty();
        property.setName(NAME);
        property.setValue(VALUE);
        propertyList.add(property);
        MediatorPropertySerializer.serializeMediatorProperties(element, propertyList);
        OMElement firstElement = element.getFirstElement();
        Assert.assertEquals("name of property must be added as an attribute", NAME,
                firstElement.getAttribute(new QName("name")).getAttributeValue());
        Assert.assertEquals("value of property must be added as an attribute", VALUE,
                firstElement.getAttribute(new QName("value")).getAttributeValue());
    }

    /**
     * Test SerializeMediatorProperties for a property without name.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testSerializeWithoutPropertyName() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM(XML);
        List<MediatorProperty> propertyList = new ArrayList<>();
        MediatorProperty property = new MediatorProperty();
        propertyList.add(property);
        thrown.expect(SynapseException.class);
        thrown.expectMessage("Mediator property name missing");
        MediatorPropertySerializer.serializeMediatorProperties(element, propertyList);
    }

    /**
     * Test SerializeMediatorProperties for a property without value.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testSerializeWithoutPropertyValue() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM(XML);
        List<MediatorProperty> propertyList = new ArrayList<>();
        MediatorProperty property = new MediatorProperty();
        property.setName(NAME);
        propertyList.add(property);
        thrown.expect(SynapseException.class);
        thrown.expectMessage("Mediator property must have a literal value or be an expression");
        MediatorPropertySerializer.serializeMediatorProperties(element, propertyList);
    }
}

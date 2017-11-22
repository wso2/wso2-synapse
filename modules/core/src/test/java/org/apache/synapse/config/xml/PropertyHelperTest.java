/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.stream.XMLStreamException;

/**
 * Unit tests for PropertyHelper class.
 */
public class PropertyHelperTest {

    private final String SAMPLE_STRING = "SampleString";
    private final String STATIC_XML = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
            "name=\"stringParameter\" value=\"" + SAMPLE_STRING + "\"> </property>\n";
    private final String NON_STATIC_XML = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
            "name=\"stringParameter\" expression=\"$func:message\"> </property>\n";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test setInstanceProperty method by adding values to a pojo.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testSetInstanceProperty() throws XMLStreamException {

        OMElement element = AXIOMUtil.stringToOM(STATIC_XML);

        PojoClass pojoInstance = new PojoClass();
        String name = "intParameter";
        PropertyHelper.setInstanceProperty(name, "45", pojoInstance);
        Assert.assertEquals("parameter must be set by the method", 45, pojoInstance.getIntParameter());

        name = "stringParameter";
        PropertyHelper.setInstanceProperty(name, "testString", pojoInstance);
        Assert.assertEquals("parameter must be set by the method", "testString", pojoInstance.getStringParameter());

        name = "longParameter";
        PropertyHelper.setInstanceProperty(name, "45", pojoInstance);
        Assert.assertEquals("parameter must be set by the method", 45, pojoInstance.getLongParameter());

        name = "floatParameter";
        PropertyHelper.setInstanceProperty(name, "45.67", pojoInstance);
        Assert.assertEquals("parameter must be set by the method", 45.67f, pojoInstance.getFloatParameter(), 0.001);

        name = "doubleParameter";
        PropertyHelper.setInstanceProperty(name, "45.67", pojoInstance);
        Assert.assertEquals("parameter must be set by the method", 45.67f, pojoInstance.getDoubleParameter(), 0.001);

        name = "omElementParameter";
        PropertyHelper.setInstanceProperty(name, element, pojoInstance);
        Assert.assertEquals("parameter must be set by the method", element, pojoInstance.getOmElementParameter());
    }

    /**
     * Testing setInstanceProperty method with invalid method name.
     */
    @Test
    public void testSetInstancePropertyError() {
        PojoClass pojoInstance = new PojoClass();
        String name = "errorMethodName";
        thrown.expect(SynapseException.class);
        PropertyHelper.setInstanceProperty(name, "45", pojoInstance);
    }


    /**
     * Test setStaticProperty by setting a parameter in pojo using OMElement.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testSetStaticProperty() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM(STATIC_XML);
        PojoClass pojoInstance = new PojoClass();
        PropertyHelper.setStaticProperty(element, pojoInstance);
        Assert.assertEquals(SAMPLE_STRING, pojoInstance.getStringParameter());

        String sampleProp = "SampleProp";
        String input = "<property xmlns:ns2=\"http://org.apache.synapse/xsd\" " +
                "name=\"omElementParameter\"><" + sampleProp + ">SampleValue</" + sampleProp + "></property>\n";
        element = AXIOMUtil.stringToOM(input);
        PropertyHelper.setStaticProperty(element, pojoInstance);
        OMElement result = pojoInstance.getOmElementParameter();
        Assert.assertNotNull("new OMElement should be inserted", result);
        Assert.assertEquals("asserting the localName of OMElement", sampleProp, result.getLocalName());
    }

    /**
     * Test isStaticProperty method using static and non static properties.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testIsStaticProperty() throws XMLStreamException {
        OMElement element = AXIOMUtil.stringToOM(STATIC_XML);
        Assert.assertTrue("should identify as static property", PropertyHelper.isStaticProperty(element));

        element = AXIOMUtil.stringToOM(NON_STATIC_XML);
        Assert.assertFalse("should identify as non-static property", PropertyHelper.isStaticProperty(element));
    }
}

/**
 * Temporary POJO class used in test cases
 */
class PojoClass {

    private int intParameter;
    private String stringParameter;
    private long longParameter;
    private float floatParameter;
    private double doubleParameter;
    private boolean booleanParameter;
    private OMElement omElementParameter;

    public OMElement getOmElementParameter() {
        return omElementParameter;
    }

    public void setOmElementParameter(OMElement omElementParameter) {
        this.omElementParameter = omElementParameter;
    }

    public boolean getBooleanParameter() {
        return booleanParameter;
    }

    public void setBooleanParameter(boolean booleanParameter) {
        this.booleanParameter = booleanParameter;
    }

    public double getDoubleParameter() {
        return doubleParameter;
    }

    public void setDoubleParameter(double doubleParameter) {
        this.doubleParameter = doubleParameter;
    }

    public float getFloatParameter() {
        return floatParameter;
    }

    public void setFloatParameter(float floatParameter) {
        this.floatParameter = floatParameter;
    }

    public long getLongParameter() {
        return longParameter;
    }

    public void setLongParameter(long longParameter) {
        this.longParameter = longParameter;
    }

    public String getStringParameter() {
        return stringParameter;
    }

    public void setStringParameter(String stringParameter) {
        this.stringParameter = stringParameter;
    }

    public int getIntParameter() {
        return intParameter;
    }

    public void setIntParameter(int intParameter) {
        this.intParameter = intParameter;
    }
}

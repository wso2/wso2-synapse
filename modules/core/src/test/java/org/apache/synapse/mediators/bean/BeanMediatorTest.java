/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.mediators.bean;

import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.Value;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for BeanMediator class
 */
public class BeanMediatorTest {

    private static final String PROPERTY_NAME = "testProperty";
    private static final String VAR_NAME = "testVariableName";
    private static final String DESCRIPTION = "testDescription";
    private static final String VALUE_TEXT = "testValue";
    private static final String ATTRIBUTE = "attr";
    private static final String ELEMENT_NAME = "TEST";
    private static final String ERROR_ATTRIBUTE = "errorAttribute";
    private static final String XML = "<" + ELEMENT_NAME + " attr=\"testVariableName\">";
    private static BeanMediator beanMediator = new BeanMediator();
    private static MessageContext messageContext;

    /**
     * Initializing bean mediator by creating an initial bean in messageContext
     */
    @BeforeClass
    public static void init() {
        SynapseEnvironment synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        messageContext = new TestMessageContext();
        beanMediator.setReplace(true);
        beanMediator.setDescription(DESCRIPTION);
        beanMediator.setBreakPoint(true);
        beanMediator.setSkipEnabled(true);
        beanMediator.setMediatorPosition(1);
        beanMediator.setPropertyName(PROPERTY_NAME);
        Value value = new Value(VALUE_TEXT);
        beanMediator.setValue(value);
        Target target = new Target(ATTRIBUTE, TestUtils.createOMElement(XML));
        beanMediator.setTarget(target);
        beanMediator.setAction(BeanMediator.Action.CREATE);
        beanMediator.setClazz(SampleBean.class);
        beanMediator.setVarName(VAR_NAME);
        messageContext.setEnvironment(synapseEnvironment);
        beanMediator.mediate(messageContext);
    }

    /**
     * Test whether bean created in the init method exists
     */
    @Test
    public void testMediateCreate() {
        Assert.assertNotNull("new bean must be created", messageContext.getProperty(VAR_NAME));
    }

    /**
     * Test both setProperty and getProperty ( since ordering matters )
     */
    @Test
    public void testMediateSetAndGetProperty() {
        beanMediator.setVarName(VAR_NAME);
        beanMediator.setAction(BeanMediator.Action.SET_PROPERTY);
        beanMediator.mediate(messageContext);
        SampleBean temp = (SampleBean) messageContext.getProperty(VAR_NAME);
        Assert.assertNotNull("Bean must contain the property", temp.getTestProperty());
        beanMediator.setAction(BeanMediator.Action.GET_PROPERTY);
        beanMediator.mediate(messageContext);
        String text = (String) messageContext.getProperty(VAR_NAME);
        Assert.assertEquals("property should be same as the Value", text, VALUE_TEXT);
    }

    /**
     * Asserting exceptions for calling set property with wrong variable name
     */
    @Test(expected = org.apache.synapse.SynapseException.class)
    public void testSetPropertyException() {
        beanMediator.setVarName("errorName");
        beanMediator.setAction(BeanMediator.Action.SET_PROPERTY);
        beanMediator.mediate(messageContext);
    }

    /**
     * Asserting exceptions for calling get property with wrong variable name
     */
    @Test(expected = org.apache.synapse.SynapseException.class)
    public void testGetPropertyException() {
        beanMediator.setVarName("errorName");
        beanMediator.setAction(BeanMediator.Action.GET_PROPERTY);
        beanMediator.mediate(messageContext);
    }

    /**
     * Asserting exception for creating an erroneous Target
     */
    @Test
    public void testErroneousValue() {
        try {
            Target target = new Target(ERROR_ATTRIBUTE, TestUtils.createOMElement(XML));
            Assert.fail("In case target creation successful without exceptions");
        } catch (Exception ex) {
            String errorMessage = "The '" + ERROR_ATTRIBUTE + "' attribute is required for the element '"
                    + ELEMENT_NAME + "'";
            Assert.assertEquals("comparing exception messages",ex.getMessage(), errorMessage);
        }
    }

    /**
     * Test removing the bean from messageContext
     */
    @AfterClass
    public static void testMediateRemove() {
        beanMediator.setAction(BeanMediator.Action.REMOVE);
        beanMediator.setVarName(VAR_NAME);
        beanMediator.mediate(messageContext);
        Assert.assertNull("bean shouldn't be there after removal", messageContext.getProperty(VAR_NAME));
    }
}

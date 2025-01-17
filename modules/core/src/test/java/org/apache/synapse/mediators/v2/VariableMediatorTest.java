/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators.v2;

import com.google.gson.JsonParser;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.junit.Assert;

public class VariableMediatorTest extends TestCase {

    JsonParser jsonParser = new JsonParser();

    public void testValidDataTypes() throws Exception {

        VariableMediator variableMediatorOne = new VariableMediator();
        variableMediatorOne.setName("nameOne");
        variableMediatorOne.setValue("valueOne", XMLConfigConstants.VARIABLE_DATA_TYPES.STRING.name());

        VariableMediator variableMediatorTwo = new VariableMediator();
        variableMediatorTwo.setName("nameTwo");
        variableMediatorTwo.setValue("25000", XMLConfigConstants.VARIABLE_DATA_TYPES.INTEGER.name());

        VariableMediator variableMediatorThree = new VariableMediator();
        variableMediatorThree.setName("nameThree");
        variableMediatorThree.setValue("123.456", XMLConfigConstants.VARIABLE_DATA_TYPES.DOUBLE.name());

        VariableMediator variableMediatorFour = new VariableMediator();
        variableMediatorFour.setName("nameFour");
        variableMediatorFour.setValue("true", XMLConfigConstants.VARIABLE_DATA_TYPES.BOOLEAN.name());

        VariableMediator variableMediatorFive = new VariableMediator();
        variableMediatorFive.setName("nameFive");
        variableMediatorFive.setValue("1234561223", XMLConfigConstants.VARIABLE_DATA_TYPES.LONG.name());

        VariableMediator variableMediatorSix = new VariableMediator();
        variableMediatorSix.setName("nameSix");
        variableMediatorSix.setValue("{\"nameSix\": 12345}", XMLConfigConstants.VARIABLE_DATA_TYPES.JSON.name());

        VariableMediator variableMediatorSeven = new VariableMediator();
        variableMediatorSeven.setName("nameSeven");
        variableMediatorSeven.setValue("<person><name>John</name><age>30</age></person>",
                XMLConfigConstants.VARIABLE_DATA_TYPES.XML.name());

        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");
        variableMediatorOne.mediate(synCtx);
        variableMediatorTwo.mediate(synCtx);
        variableMediatorThree.mediate(synCtx);
        variableMediatorFour.mediate(synCtx);
        variableMediatorFive.mediate(synCtx);
        variableMediatorSix.mediate(synCtx);
        variableMediatorSeven.mediate(synCtx);

        Object valueOne = synCtx.getVariable("nameOne");
        Object valueTwo = synCtx.getVariable("nameTwo");
        Object valueThree = synCtx.getVariable("nameThree");
        Object valueFour = synCtx.getVariable("nameFour");
        Object valueFive = synCtx.getVariable("nameFive");
        Object valueSix = synCtx.getVariable("nameSix");
        Object valueSeven = synCtx.getVariable("nameSeven");

        assertEquals("valueOne", valueOne);
        assertEquals(new Integer(25000), valueTwo);
        assertEquals(new Double(123.456), valueThree);
        assertEquals(Boolean.TRUE, valueFour);
        assertEquals(new Long(1234561223), valueFive);
        assertEquals(jsonParser.parse("{\"nameSix\": 12345}"), valueSix);
        OMElement xml = SynapseConfigUtils.stringToOM("<person><name>John</name><age>30</age></person>");
        assertEquals(xml.toString(), valueSeven.toString());
    }

    public void testInvalidDataTypes() throws Exception {

        try {
            VariableMediator variableMediatorOne = new VariableMediator();
            variableMediatorOne.setName("nameOne");
            variableMediatorOne.setValue("valueOne", "abc");
            Assert.fail("Expected an Unknown data type exception");
        } catch (SynapseException e) {
            // Assert the exception message
            assertEquals("Unknown type : abc for the variable mediator or the variable value cannot be " +
                            "converted into the specified type.",
                    e.getMessage()
            );
        }

        try {
            VariableMediator variableMediatorOne = new VariableMediator();
            variableMediatorOne.setName("nameOne");
            variableMediatorOne.setValue("valueOne", "SHORT");
            Assert.fail("Expected an Unknown data type exception");
        } catch (SynapseException e) {
            // Assert the exception message
            assertEquals("Unknown type : SHORT for the variable mediator or the variable value cannot be " +
                            "converted into the specified type.",
                    e.getMessage()
            );
        }

        try {
            VariableMediator variableMediatorOne = new VariableMediator();
            variableMediatorOne.setName("nameOne");
            variableMediatorOne.setValue("<person><name>John</name><age>30</age></person>", "OM");
            Assert.fail("Expected an Unknown data type exception");
        } catch (SynapseException e) {
            // Assert the exception message
            assertEquals("Unknown type : OM for the variable mediator or the variable value cannot be " +
                            "converted into the specified type.",
                    e.getMessage()
            );
        }
    }

    public void testExpressions() throws Exception {

        VariableMediator variableMediatorOne = new VariableMediator();
        variableMediatorOne.setName("nameOne");
        variableMediatorOne.setValue("valueOne", XMLConfigConstants.VARIABLE_DATA_TYPES.STRING.name());

        VariableMediator variableMediatorTwo = new VariableMediator();
        variableMediatorTwo.setName("nameTwo");
        variableMediatorTwo.setExpression(new SynapseExpression("vars.nameOne"),
                XMLConfigConstants.VARIABLE_DATA_TYPES.STRING.name());

        VariableMediator variableMediatorThree = new VariableMediator();
        variableMediatorThree.setName("nameThree");
        variableMediatorThree.setExpression(new SynapseExpression("payload.person.age"),
                XMLConfigConstants.VARIABLE_DATA_TYPES.INTEGER.name());

        MessageContext synCtx = TestUtils.
                createLightweightSynapseMessageContext("<person><name>John</name><age>30</age></person>");

        variableMediatorOne.mediate(synCtx);
        variableMediatorTwo.mediate(synCtx);
        variableMediatorThree.mediate(synCtx);

        Object valueOne = synCtx.getVariable("nameOne");
        Object valueTwo = synCtx.getVariable("nameTwo");
        Object valueThree = synCtx.getVariable("nameThree");

        assertEquals("valueOne", valueOne);
        assertEquals("valueOne", valueTwo);
        assertEquals(30, valueThree);
    }
}

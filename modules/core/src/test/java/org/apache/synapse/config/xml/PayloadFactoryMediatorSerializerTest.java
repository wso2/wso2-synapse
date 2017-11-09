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
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.transform.Argument;
import org.apache.synapse.mediators.transform.PayloadFactoryMediator;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This is the test class for PayloadFactoryMediatorSerializer  class.
 */
public class PayloadFactoryMediatorSerializerTest {

    private static final String format = "<p:echoInt xmlns:p=\"http://echo.services.core.carbon.wso2.org\">"
            + "<in>1</in></p:echoInt>";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /**
     * Test SerializeSpecificMediator method with payloadFactory mediator, with no format set.
     */
    @Test
    public void testSerializeSpecificMediator() {
        exception.expect(SynapseException.class);
        exception.expectMessage("Invalid payloadFactory mediator, format is required");
        PayloadFactoryMediatorSerializer serializer = new PayloadFactoryMediatorSerializer();
        serializer.serializeSpecificMediator(new PayloadFactoryMediator());
    }

    /**
     * Test SerializeSpecificMediator method with format added for payloadFactory mediator
     * and assert the format added in OmElement.
     */
    @Test
    public void testSerializeSpecificMediator2() {
        PayloadFactoryMediatorSerializer serializer = new PayloadFactoryMediatorSerializer();
        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        payloadFactoryMediator.setFormat(format);
        OMElement element = serializer.serializeSpecificMediator(payloadFactoryMediator);
        Assert.assertNotNull(element);
        Assert.assertTrue("Format set for Mediator is not serialized", element.getChildren().next().
                toString().contains("<p:echoInt xmlns:p=\"" +
                "http://echo.services.core.carbon.wso2.org\">"));
    }

    /**
     * Test SerializeSpecificMediator method with Dynamic format added for payloadFactory mediator
     * and assert that dynamic format is set.
     */
    @Test
    public void testSerializeSpecificMediator3() {
        PayloadFactoryMediatorSerializer serializer = new PayloadFactoryMediatorSerializer();
        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        payloadFactoryMediator.setFormat(format);
        payloadFactoryMediator.setFormatDynamic(true);
        payloadFactoryMediator.setFormatKey(new Value("testKey"));
        OMElement element = serializer.serializeSpecificMediator(payloadFactoryMediator);
        Assert.assertNotNull(element);
        MediatorFactory mediatorFactory = new PayloadFactoryMediatorFactory();
        Mediator mediator = mediatorFactory.createMediator(element, null);
        Assert.assertTrue("Dynamic Format set for Mediator is not serialized",
                ((PayloadFactoryMediator) mediator).isFormatDynamic());
    }

    /**
     * Test SerializeSpecificMediator method with PathArgument added for payloadFactory mediator
     * and assert that argument is added.
     */
    @Test
    public void testSerializeSpecificMediator4() throws JaxenException {
        PayloadFactoryMediatorSerializer serializer = new PayloadFactoryMediatorSerializer();
        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        Argument argument = new Argument();
        argument.setValue("TestArgument1");
        payloadFactoryMediator.addPathArgument(argument);
        payloadFactoryMediator.setFormat(format);
        OMElement element = serializer.serializeSpecificMediator(payloadFactoryMediator);
        MediatorFactory mediatorFactory = new PayloadFactoryMediatorFactory();
        Mediator mediator = mediatorFactory.createMediator(element, null);
        Assert.assertNotNull(element);
        Assert.assertEquals("Path argument added is not serialized", "TestArgument1",
                ((PayloadFactoryMediator) mediator).getPathArgumentList().get(0).getValue());
    }

    /**
     * Test SerializeSpecificMediator method with PathArgument with expression added for payloadFactory mediator
     * and assert that expression is added.
     */
    @Test
    public void testSerializeSpecificMediator5() throws JaxenException {
        PayloadFactoryMediatorSerializer serializer = new PayloadFactoryMediatorSerializer();
        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        Argument argument = new Argument();
        argument.setExpression(new SynapseXPath("//name"));
        payloadFactoryMediator.addPathArgument(argument);
        payloadFactoryMediator.setFormat(format);
        OMElement element = serializer.serializeSpecificMediator(payloadFactoryMediator);
        MediatorFactory mediatorFactory = new PayloadFactoryMediatorFactory();
        Mediator mediator = mediatorFactory.createMediator(element, null);
        Assert.assertNotNull(element);
        Assert.assertEquals("Expression added for path argument is not serialized", "//name",
                ((PayloadFactoryMediator) mediator).getPathArgumentList().get(0).getExpression().toString()
        );
    }

}


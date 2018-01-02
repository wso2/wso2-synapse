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
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.bean.BeanMediator;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

/**
 * This is the test class for BeanMediatorFactory class.
 */
public class BeanMediatorFactoryTest {

    /**
     * Creating a bean mediator and asserting it is created successfully.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateSpecificMediator() throws XMLStreamException {
        BeanMediatorFactory beanMediatorFactory = new BeanMediatorFactory();
        String inputXML = "<bean action=\"CREATE\" class=\"org.apache.synapse.util.TestTask\" " +
                "var=\"loc\"></bean>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        Mediator mediator = beanMediatorFactory.createSpecificMediator(element, null);
        Assert.assertTrue("BeanMediator is not created successfully.", mediator instanceof
                BeanMediator);
        BeanMediator beanMediator = (BeanMediator) mediator;
        Assert.assertEquals("CREATE action is not set", BeanMediator.Action.CREATE, beanMediator.getAction());
    }

    /**
     * Setting property of BeanMediator and asserting the action.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateSpecificMediator2() throws XMLStreamException {
        BeanMediatorFactory beanMediatorFactory = new BeanMediatorFactory();
        String inputXML = "<bean action=\"SET_PROPERTY\" property=\"latitude\" value=\"{//latitude}\" var=\"loc\" " +
                "xmlns:ns3=\"http://org.apache.synapse/xsd\"             " +
                "xmlns:ns=\"http://org.apache.synapse/xsd\"></bean>\n";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        Mediator mediator = beanMediatorFactory.createSpecificMediator(element, null);
        Assert.assertTrue("BeanMediator is not created successfully.", mediator instanceof
                BeanMediator);
        BeanMediator beanMediator = (BeanMediator) mediator;
        Assert.assertEquals("SET_PROPERTY action is not set", BeanMediator.Action.SET_PROPERTY, beanMediator.getAction());
    }

    /**
     * Getting the property of BeanMediator and asserting the action.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateSpecificMediator3() throws XMLStreamException {
        BeanMediatorFactory beanMediatorFactory = new BeanMediatorFactory();
        String inputXML = "<bean action=\"GET_PROPERTY\" property=\"latitude\" value=\"{//latitude}\" var=\"loc\" " +
                "target=\"target\"   xmlns:ns3=\"http://org.apache.synapse/xsd\"             " +
                "xmlns:ns=\"http://org.apache.synapse/xsd\"></bean>\n";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        Mediator mediator = beanMediatorFactory.createSpecificMediator(element, null);
        Assert.assertTrue("BeanMediator is not created successfully.", mediator instanceof
                BeanMediator);
        BeanMediator beanMediator = (BeanMediator) mediator;
        Assert.assertEquals("GET_PROPERTY action is not set", BeanMediator.Action.GET_PROPERTY, beanMediator.getAction());
    }

    /**
     * Removing the property of BeanMediator and asserting the action.
     *
     * @throws XMLStreamException - XMLStreamException.
     */
    @Test
    public void testCreateSpecificMediator4() throws XMLStreamException {
        BeanMediatorFactory beanMediatorFactory = new BeanMediatorFactory();
        String inputXML = "<bean action=\"REMOVE\" property=\"latitude\" value=\"{//latitude}\" var=\"loc\" " +
                "xmlns:ns3=\"http://org.apache.synapse/xsd\"             " +
                "xmlns:ns=\"http://org.apache.synapse/xsd\"></bean>\n";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        Mediator mediator = beanMediatorFactory.createSpecificMediator(element, null);
        Assert.assertTrue("BeanMediator is not created successfully.", mediator instanceof
                BeanMediator);
        BeanMediator beanMediator = (BeanMediator) mediator;
        Assert.assertEquals("REMOVE action is not set", BeanMediator.Action.REMOVE, beanMediator.getAction());
    }
}


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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.bean.enterprise.EJBMediator;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

/**
 * This is the test class for EJBMediatorFactory.
 */
public class EJBMediatorFactoryTest {

    private static final Log log = LogFactory.getLog(EJBMediatorFactoryTest.class);

    /**
     * Test createSpecificMediator method and checked the returned mediator benStalkName.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testCreateSpecificMediator() throws XMLStreamException {
        EJBMediatorFactory factory = new EJBMediatorFactory();
        String inputXML = "<ejb xmlns=\"http://ws.apache.org/ns/synapse\" beanstalk=\"jack\" class=\"org.apache.synapse"
                + ".mediators.bean.enterprise.EJBMediator\" method=\"setMethod\" target=\"store\" "
                + "jndiName=\"ejb:/EJBDemo/StoreRegisterBean!org.ejb.wso2.test.StoreRegister\">"
                + "<args><arg value=\"{get-property('loc_id')}\"/></args></ejb>";
        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        Mediator mediator = factory.createSpecificMediator(inputElement, null);
        EJBMediatorSerializer serializer = new EJBMediatorSerializer();
        OMElement outputElement = serializer.serializeSpecificMediator(mediator);
        Assert.assertEquals("EJB Mediator with beanstalk name jack is not returned", "jack",
                ((EJBMediator) mediator).getBeanstalkName());
        Assert.assertTrue("Input XML and serialized output XMLs are not same", compare(inputElement, outputElement));
    }

    /**
     * Compare OMElements
     *
     * @param inputElement      - InputOMElement
     * @param serializedElement - OutputOMElement
     * @return - the compared status.
     */
    protected boolean compare(OMElement inputElement, OMElement serializedElement) {
        try {
            assertXMLEqual(inputElement.toString(), serializedElement.toString());
            return true;
        } catch (SAXException | IOException e) {
            log.error(e);
        }
        return false;
    }
}

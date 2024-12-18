/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.mediators.v2.ThrowError;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;

/**
 * This is the test class for ThrowErrorMediatorFactory class.
 */
public class ThrowErrorMediatorFactoryTest {

    /**
     * Test create TriggerError with given XML configuration and asserting it is created.
     *
     * @throws XMLStreamException - XMLStreamException
     */
    @Test
    public void testThrowErrorExp() throws XMLStreamException {
        String inputXML = "<throwError type=\"HTTP:TIMEOUT\" errorMessage=\"{${$.abc}}\"/>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        ThrowErrorMediatorFactory throwErrorMediatorFactory = new ThrowErrorMediatorFactory();
        ThrowError throwError = (ThrowError) throwErrorMediatorFactory.createSpecificMediator(element,null);
        Assert.assertEquals("$.abc", throwError.getErrorMsg().getExpression().toString());
    }

    @Test
    public void testThrowErrorMsg() throws XMLStreamException {
        String inputXML = "<throwError type=\"HTTP:TIMEOUT\" errorMessage=\"string error message\"/>";
        OMElement element = AXIOMUtil.stringToOM(inputXML);
        ThrowErrorMediatorFactory throwErrorMediatorFactory = new ThrowErrorMediatorFactory();
        ThrowError throwError = (ThrowError) throwErrorMediatorFactory.createSpecificMediator(element,null);
        Assert.assertEquals("string error message", throwError.getErrorMsg().getKeyValue());
    }
}

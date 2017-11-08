/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.util;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.junit.Test;

/**
 * Test class for PayloadHelper
 */
public class PayloadHelperTest extends TestCase {
    /**
     * Testing whether the type of payload retrieved successfully
     * @throws Exception
     */
    @Test
    public void testGetPayloadType() throws Exception {
        SOAP11Factory factory = new SOAP11Factory();
        SOAPEnvelope envelope = factory.getDefaultEnvelope();
        OMElement element = AXIOMUtil.stringToOM("<name><value>Test</value></name>");
        envelope.getBody().addChild(element);
        int type = PayloadHelper.getPayloadType(envelope);
        Assert.assertEquals("Payload type is mismatching!", 0, type);
    }

    /**
     * Testing whether xml payload is set successfully
     * @throws Exception
     */
    @Test
    public void testSetXmlPayload() throws Exception {
        SOAP11Factory factory = new SOAP11Factory();
        SOAPEnvelope envelope = factory.getDefaultEnvelope();
        OMElement element = AXIOMUtil.stringToOM("<name><value>Test</value></name>");
        PayloadHelper.setXMLPayload(envelope, element);
        Assert.assertEquals("Couldn't set payload!", "name", envelope.getBody().getFirstElementLocalName());
    }
}
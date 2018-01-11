/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.script;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

import javax.xml.namespace.QName;

/**
 * Test case for inline script mediation with Javascript
 */
public class InlineFunctionTestCase extends MediatorTestCase {

    public InlineFunctionTestCase() {
        loadConfiguration("/mediators/scriptInlineFunctionTestConfig.xml");
    }

    public void testInlineFunction()throws Exception{
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendCustomQuoteRequest("http://localhost:8280/services/scriptMediatorJSInlineTestProxy",
                null, null, "wso2");
        assertNotNull("Response message null", response);
        assertEquals("Local Name does not match", "Code", response.getFirstElement().getLocalName().toString());
        assertEquals("Local Part does not match", "CheckPriceResponse", response.getQName().getLocalPart().toString());
        assertEquals("Text does not match", "wso2", response.getFirstChildWithName
                        (new QName("http://services.samples/xsd", "Code")).getText());
        assertEquals("Local Name does not match", "Price", response.getFirstChildWithName
                        (new QName("http://services.samples/xsd", "Price")).getLocalName());
        assertNotNull("Text does not exist", response.getFirstChildWithName
                        (new QName("http://services.samples/xsd", "Price")).getText());
    }
}

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

package org.apache.synapse.mediators.property;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

import javax.xml.namespace.QName;

/**
 * Test case for Property mediator : HTTP Header case sensitivity
 */
public class HTTPHeaderCaseSensitivityTestCase extends MediatorTestCase {

    public HTTPHeaderCaseSensitivityTestCase() {
        loadConfiguration("/mediators/httpHeaderCaseSensitivityTestConfig.xml");
    }

    /**
     * Test by adding a http header with name "TEST_HEADER" where all the letters are in uppercase
     * @throws AxisFault
     */
    public void testHttpHeaderUpperCase() throws AxisFault {
        StockQuoteSampleClient client = getStockQuoteClient();
        client.addHttpHeader("TEST_HEADER", "uppercase");
        OMElement response = client.sendStandardQuoteRequest("http://localhost:8280/services/httpHeaderTestProxy",
                null, null, "WSO2", null);

        assertNotNull("Response message null", response);
        OMElement returnElement = response.getFirstElement();
        OMElement symbolElement = returnElement.getFirstChildWithName(
                new QName("http://services.samples/xsd", "symbol"));
        assertEquals("Fault, invalid response", "uppercase", symbolElement.getText());
    }

    /**
     * Test by adding a http header with all lowercase letters
     * @throws AxisFault
     */
    public void testHttpHeaderLowerCase() throws AxisFault {
        StockQuoteSampleClient client = getStockQuoteClient();
        client.addHttpHeader("test_header", "lowercase");
        OMElement response = client.sendStandardQuoteRequest("http://localhost:8280/services/httpHeaderTestProxy",
                null, null, "WSO2", null);

        assertNotNull("Response message null", response);
        OMElement returnElement = response.getFirstElement();
        OMElement symbolElement = returnElement.getFirstChildWithName(
                new QName("http://services.samples/xsd", "symbol"));
        assertEquals("Fault, invalid response", "lowercase", symbolElement.getText());
    }

    /**
     * Add a http header with name "test_header" where  the letters are in both upper & lower case
     * @throws AxisFault
     */
    public void testHttpHeaderCombined() throws AxisFault {
        StockQuoteSampleClient client = getStockQuoteClient();
        client.addHttpHeader("Test_Header", "mixed");
        OMElement response = client.sendStandardQuoteRequest("http://localhost:8280/services/httpHeaderTestProxy",
                null, null, "WSO2", null);

        assertNotNull("Response message null", response);
        OMElement returnElement = response.getFirstElement();
        OMElement symbolElement = returnElement.getFirstChildWithName(
                new QName("http://services.samples/xsd", "symbol"));
        assertEquals("Fault, invalid response", "mixed", symbolElement.getText());
    }
}

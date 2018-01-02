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
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;

/**
 * This test case tests whether the setting of properties
 * in the Axis2-client scope is working fine.
 */
public class PropertyIntegrationAxis2ClientScopeTestCase extends MediatorTestCase {

    public PropertyIntegrationAxis2ClientScopeTestCase() {
        loadConfiguration("/mediators/propertyAxis2ClientScopeTestConfig.xml");
    }

    public void testDoubleVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyDoubleAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("Double Property Not Set in the Axis2-client scope!", response.toString().contains("123123.123123"));
    }

    public void testIntVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyIntAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("Integer Property Not Set in the Axis2-client scope!", response.toString().contains("123"));
    }

    public void testStringVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyStringAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("String Property Not Set in the Axis2-client scope!", response.toString().contains("WSO2 Lanka"));
    }

    public void testBooleanVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyBooleanAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("Boolean Property Not Set in the Axis2-client scope!", response.toString().contains("true"));
    }

    public void testFloatVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyFloatAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("Float Property Not Set in the Axis2-client scope!", response.toString().contains("123.123"));
    }

    public void testShortVal() throws Exception {
        StockQuoteSampleClient client = getStockQuoteClient();
        OMElement response = client.sendStandardQuoteRequest(
                "http://localhost:8280/services/propertyShortAxis2ClientTestProxy",
                null, null, "Random Symbol", null);
        assertTrue("Short Property Not Set in the Axis2-client scope!", response.toString().contains("12"));
    }
}
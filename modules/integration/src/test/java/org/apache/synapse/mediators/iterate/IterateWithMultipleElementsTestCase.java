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

package org.apache.synapse.mediators.iterate;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.mediators.clients.AxisOperationClient;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * https://wso2.org/jira/browse/ESBJAVA-2843
 * Iterator mediator test case with multiple elements in payload
 */
public class IterateWithMultipleElementsTestCase extends MediatorTestCase {

    public IterateWithMultipleElementsTestCase() {
        loadConfiguration("/mediators/iterateWithMultipleElementsTestConfig.xml");
    }

    public void testIteratorWithMultipleElement() throws Exception {
        AxisOperationClient operationClient = getAxisOperationClient();
        OMElement response = operationClient.send("http://localhost:8280/services/iterateWithMultipleElementsTestProxy",
                null, createRequestPayload(), "urn:getQuote");
        assertNotNull("Received response is null", response);
        OMElement soapBody = response.getFirstElement();
        Iterator iterator =
                soapBody.getChildrenWithName(new QName("http://services.samples",
                        "getQuoteResponse"));
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            OMElement getQuote = (OMElement) iterator.next();
            assertTrue("Invalid payload received", getQuote.toString().contains("WSO2 Company"));
        }
        assertEquals("Child Element count mismatched", 3, count);
    }

    private OMElement createRequestPayload() {
        SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement payload = fac.createOMElement("getQuotes", omNs);

        for (int i = 0; i < 3; i++) {
            OMElement method = fac.createOMElement("getQuote", omNs);
            OMElement request = fac.createOMElement("request", omNs);
            OMElement symbol = fac.createOMElement("symbol", omNs);
            symbol.addChild(fac.createOMText(request, "WSO2"));
            request.addChild(symbol);
            method.addChild(request);
            payload.addChild(method);
        }

        for (int i = 0; i < 3; i++) {
            OMElement method = fac.createOMElement("dummy", omNs);
            OMElement request = fac.createOMElement("request", omNs);
            OMElement symbol = fac.createOMElement("symbol", omNs);
            symbol.addChild(fac.createOMText(request, "WSO2"));
            request.addChild(symbol);
            method.addChild(request);
            payload.addChild(method);
        }
        return payload;
    }
}

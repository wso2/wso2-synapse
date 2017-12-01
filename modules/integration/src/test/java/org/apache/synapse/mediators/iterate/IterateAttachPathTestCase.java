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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.mediators.MediatorTestCase;
import org.apache.synapse.mediators.clients.AxisOperationClient;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Tests a path that is already in the original request as the attachPath and
 * verify whether iterated messages are appended properly
 *
 */
public class IterateAttachPathTestCase extends MediatorTestCase {

    public IterateAttachPathTestCase() {
        loadConfiguration("/mediators/iterateAttachPathTestConfig.xml");
    }

    public void testAttachPath() throws IOException, XMLStreamException {

        AxisOperationClient client = getAxisOperationClient();
        OMElement response = client.sendMultipleQuoteRequest("http://localhost:8280/services/iterateWithAttachPathTestProxy", null, "WSO2", 2);
        assertNotNull("Response is null", response);
        OMElement soapBody = response.getFirstElement();
        Iterator iterator = soapBody.getChildrenWithName(new QName("http://services.samples",
                        "getQuoteResponse"));
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            OMElement getQuote = (OMElement) iterator.next();
            assertTrue(getQuote.toString().contains("WSO2"));
        }
        assertEquals("Response count mismatched", 2, count);
    }
}

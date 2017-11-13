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

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.endpoints.HTTPEndpoint;
import org.apache.synapse.config.xml.AbstractTestCase;

/**
 * Test class for building and then serializing http endpoint
 */
public class HTTPEndpointSerializerTest extends AbstractTestCase {
    public void test() throws Exception {
        String inputXml = "<endpoint  xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<http uri-template=\"URI Template\" method=\"GET\" />" + "</endpoint>";
        OMElement inputElement = AXIOMUtil.stringToOM(inputXml);
        HTTPEndpoint endpoint = (HTTPEndpoint) HTTPEndpointFactory.getEndpointFromElement(inputElement, true, null);
        OMElement serializedResponse = HTTPEndpointSerializer.getElementFromEndpoint(endpoint);
        assertTrue("Endpoint not serialized!", compare(serializedResponse, inputElement));
    }
}
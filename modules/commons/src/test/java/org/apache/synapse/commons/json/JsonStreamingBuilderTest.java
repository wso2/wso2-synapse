/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.commons.json;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.InputStream;

public class JsonStreamingBuilderTest extends TestCase {
    public void testCase() {
        testInvalidJson(invalidJson);
    }

    private Builder jsonBuilder = Util.newJsonStreamBuilder();
    private static final Log log = LogFactory.getLog(JsonStreamingBuilderTest.class);

    private static final String invalidJson = "{\n" +
            "\"account_number\":\"1234567890\",\n" +
            "\"routing_number\":\"09100001\n" +
            "\"image_type\":\"COMMERCIAL_DEPOSIT\"\n" +
            "}";

    public  void testInvalidJson(String jsonIn) {
        try {
            MessageContext message = Util.newMessageContext();
            InputStream inputStream = Util.newInputStream(jsonIn.getBytes());
            OMElement element  = jsonBuilder.processDocument(inputStream, "application/json", message);
            message.getEnvelope().getBody().addChild(element);
            log.info(message.getEnvelope().getBody().toString());
        } catch (Exception e) {
            assertEquals(e.getMessage(),"Error: could not match input");
        }
    }
}

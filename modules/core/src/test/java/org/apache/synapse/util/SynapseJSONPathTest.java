/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.util;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.util.xpath.SynapseJsonPath;

/**
 * This class is testing dynamic json-path capabilities.
 */
public class SynapseJSONPathTest extends TestCase {

    private static String payload = "{\n" +
            "    \"store\": {\n" +
            "        \"book\": [\n" +
            "            {\n" +
            "                \"category\": \"reference\",\n" +
            "                \"author\": \"Nigel Rees\",\n" +
            "                \"title\": \"Sayings of the Century\",\n" +
            "                \"price\": 8.95\n" +
            "            },\n" +
            "            {\n" +
            "                \"category\": \"fiction\",\n" +
            "                \"author\": \"Evelyn Waugh\",\n" +
            "                \"title\": \"Sword of Honour\",\n" +
            "                \"price\": 12.99\n" +
            "            },\n" +
            "            {\n" +
            "                \"category\": \"fiction\",\n" +
            "                \"author\": \"Herman Melville\",\n" +
            "                \"title\": \"Moby Dick\",\n" +
            "                \"isbn\": \"0-553-21311-3\",\n" +
            "                \"price\": 8.99\n" +
            "            },\n" +
            "            {\n" +
            "                \"category\": \"fiction\",\n" +
            "                \"author\": \"J. R. R. Tolkien\",\n" +
            "                \"title\": \"The Lord of the Rings\",\n" +
            "                \"isbn\": \"0-395-19395-8\",\n" +
            "                \"price\": 22.99\n" +
            "            }\n" +
            "        ],\n" +
            "        \"bicycle\": {\n" +
            "            \"color\": \"red\",\n" +
            "            \"price\": 19.95\n" +
            "        }\n" +
            "    },\n" +
            "    \"expensive\": 10\n" +
            "}";

    public void testDynamicJSONPath() throws Exception {

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        PropertyMediator prop1 = new PropertyMediator();
        prop1.setValue("$.store.bicycle");
        prop1.setName("prop1");
        prop1.setScope(XMLConfigConstants.SCOPE_DEFAULT);
        prop1.mediate(mc);
        SynapseJsonPath synapseJsonPath = new SynapseJsonPath("{$ctx:prop1}");
        Assert.assertEquals("Didn't receive the expected result", "{\"color\":\"red\",\"price\":19.95}",
                synapseJsonPath.stringValueOf(mc));
    }

    public void testDynamicJSONPathWithIntermediateProperties() throws Exception {

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        PropertyMediator prop1 = new PropertyMediator();
        prop1.setValue("store");
        prop1.setName("prop1");
        prop1.setScope(XMLConfigConstants.SCOPE_DEFAULT);
        prop1.mediate(mc);
        SynapseJsonPath synapseJsonPath = new SynapseJsonPath("$.{$ctx:prop1}.book[3].author");
        Assert.assertEquals("Didn't receive the expected result", "J. R. R. Tolkien",
                synapseJsonPath.stringValueOf(mc));
    }
}

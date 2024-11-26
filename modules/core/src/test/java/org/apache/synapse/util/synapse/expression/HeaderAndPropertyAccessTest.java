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
package org.apache.synapse.util.synapse.expression;

import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for header and property access.
 */
public class HeaderAndPropertyAccessTest {

    private Axis2MessageContext synCtx;

    @Before
    public void setUp() throws Exception {
        synCtx = TestUtils.getAxis2MessageContext("<test/>", null);
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("test", "Hello World");
        headersMap.put("toUpper", "HELLO WORLD");
        headersMap.put("te st", "HELLO");
        headersMap.put("numerical", "400");
        headersMap.put("price", "10");
        synCtx.getAxis2MessageContext().setProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
        synCtx.getAxis2MessageContext().setProperty("test", "Sic Mundus");
        synCtx.getAxis2MessageContext().setProperty("test2", "Creatus Est");
        synCtx.getAxis2MessageContext().setProperty("empty", "Thus the world was created");
        synCtx.getAxis2MessageContext().setProperty("category", "biography");
        synCtx.setProperty("phrase", "Now I Am Become Death, the Destroyer of Worlds");
        synCtx.setProperty("selected book", "Animal Farm");
        synCtx.setProperty("null", null);
        synCtx.setProperty("empty", "");
        JsonUtil.getNewJsonPayload(synCtx.getAxis2MessageContext(), "{\n" +
                "  \"store\": {\n" +
                "    \"book\": [\n" +
                "      {\n" +
                "        \"category\": \"reference\",\n" +
                "        \"author\": \"Nigel Rees\",\n" +
                "        \"title\": \"Sayings of the Century\",\n" +
                "        \"price\": 8.95\n" +
                "      },\n" +
                "      {\n" +
                "        \"category\": \"fiction\",\n" +
                "        \"author\": \"Herman Melville\",\n" +
                "        \"title\": \"Moby Dick\",\n" +
                "        \"isbn\": \"0-553-21311-3\",\n" +
                "        \"price\": 8.99\n" +
                "      },\n" +
                "      {\n" +
                "        \"category\": \"fiction\",\n" +
                "        \"author\": \"J.R.R. Tolkien\",\n" +
                "        \"title\": \"The Lord of the Rings\",\n" +
                "        \"isbn\": \"0-395-19395-8\",\n" +
                "        \"price\": 22.99\n" +
                "      },\n" +
                "      {\n" +
                "        \"category\": \"fiction\",\n" +
                "        \"author\": \"Harper Lee\",\n" +
                "        \"title\": \"To Kill a Mockingbird\",\n" +
                "        \"price\": 10.99\n" +
                "      },\n" +
                "      {\n" +
                "        \"category\": \"fiction\",\n" +
                "        \"author\": \"George Orwell\",\n" +
                "        \"title\": \"Animal Farm\",\n" +
                "        \"price\": 7.99\n" +
                "      },\n" +
                "      {\n" +
                "        \"category\": \"biography\",\n" +
                "        \"author\": \"Anne Frank\",\n" +
                "        \"title\": \"The Diary of a Young Girl\",\n" +
                "        \"price\": 6.99\n" +
                "      }\n" +
                "    ],\n" +
                "    \"bicycle\": {\n" +
                "      \"color\": \"red\",\n" +
                "      \"price\": 19.95\n" +
                "    }\n" +
                "  },\n" +
                "  \"expensive\": 10,\n" +
                "  \"selectedCategory\": \"biography\"\n" +
                "}\n", true, true);
    }

    @Test
    public void testTransportHeader() throws Exception {
        SynapseExpression testPath = new SynapseExpression("headers.test");
        Assert.assertEquals("Hello World", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testTransportHeaderWithReservedName() throws Exception {
        SynapseExpression testPath = new SynapseExpression("toLower(headers.[\"toUpper\"])");
        Assert.assertEquals("hello world", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testTransportHeaderInFilter() throws Exception {
        SynapseExpression testPath = new SynapseExpression("$..book[?(@.price > integer(headers.price))].title");
        Assert.assertEquals("[\"The Lord of the Rings\",\"To Kill a Mockingbird\"]", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testTransportHeaderWithSpecialChar() throws Exception {
        SynapseExpression testPath = new SynapseExpression("toLower(headers.[\"te st\"]) + \" World\"");
        Assert.assertEquals("hello World", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testTransportHeaderNotExist() throws Exception {
        SynapseExpression testPath = new SynapseExpression("headers[\"toUpper2\"]");
        Assert.assertNull(testPath.stringValueOf(synCtx));
    }

    @Test
    public void testTransportHeaderNumerical() throws Exception {
        SynapseExpression testPath = new SynapseExpression("integer(headers.numerical)+ 4");
        Assert.assertEquals("404", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testAxis2Header() throws Exception {
        SynapseExpression testPath = new SynapseExpression("attr.axis2.test");
        Assert.assertEquals("Sic Mundus", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testAxis2HeaderWithExpression() throws Exception {
        SynapseExpression testPath = new SynapseExpression("attributes.axis2.test + \" \" " +
                "+ attributes.axis2.test2");
        Assert.assertEquals("Sic Mundus Creatus Est", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testAxis2HeaderInFilter() throws Exception {
        SynapseExpression testPath = new SynapseExpression("$..book[?(@.category == attributes.axis2.category)].title");
        Assert.assertEquals("[\"The Diary of a Young Girl\"]", testPath.stringValueOf(synCtx));
    }
    @Test
    public void testAxis2HeaderWithReservedName() throws Exception {
        SynapseExpression testPath = new SynapseExpression("attributes.axis2.[\"empty\"]");
        Assert.assertEquals("Thus the world was created", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testSynapseHeader() throws Exception {
        SynapseExpression testPath = new SynapseExpression("attributes.synapse.phrase");
        Assert.assertEquals("Now I Am Become Death, the Destroyer of Worlds", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testSynapseHeaderWithFunction() throws Exception {
        SynapseExpression testPath = new SynapseExpression("length(split(attributes.synapse.phrase, \" \"))");
        Assert.assertEquals("9", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testSynapseHeaderInFilter() throws Exception {
        SynapseExpression testPath = new SynapseExpression("$..book[?(@.title == attributes.synapse.[\"selected book\"])].price");
        Assert.assertEquals("[7.99]", testPath.stringValueOf(synCtx));
    }

    @Test
    public void testNonExistingEmptyAndNull() throws Exception {
        SynapseExpression testPath = new SynapseExpression("attributes.synapse.nonExisting");
        Assert.assertNull(testPath.stringValueOf(synCtx));
        testPath = new SynapseExpression("attributes.synapse.[\"null\"]");
        Assert.assertNull(testPath.stringValueOf(synCtx));
        testPath = new SynapseExpression("attributes.synapse[\"empty\"]");
        Assert.assertEquals("", testPath.stringValueOf(synCtx));
    }
}

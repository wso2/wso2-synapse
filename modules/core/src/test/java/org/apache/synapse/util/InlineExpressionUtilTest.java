/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

public class InlineExpressionUtilTest {

    private static String payload = "{\n" +
            "  \"team\": [\n" +
            "    {\n" +
            "      \"name\": \"Alice\",\n" +
            "      \"role\": \"Developer\",\n" +
            "      \"experience\": 3\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bob\",\n" +
            "      \"role\": \"Designer\",\n" +
            "      \"experience\": 5\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Charlie\",\n" +
            "      \"role\": \"Product Manager\",\n" +
            "      \"experience\": 7\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    private static String xmlPayload = "<catalog>\n" +
            "    <book>\n" +
            "        <id>101</id>\n" +
            "        <title>The Great Gatsby</title>\n" +
            "        <author>F. Scott Fitzgerald</author>\n" +
            "        <genre>Fiction</genre>\n" +
            "        <price>10.99</price>\n" +
            "        <publish_date>1925-04-10</publish_date>\n" +
            "    </book>\n" +
            "    <book>\n" +
            "        <id>102</id>\n" +
            "        <title>1984</title>\n" +
            "        <author>George Orwell</author>\n" +
            "        <genre>Dystopian</genre>\n" +
            "        <price>8.99</price>\n" +
            "        <publish_date>1949-06-08</publish_date>\n" +
            "    </book>\n" +
            "    <book>\n" +
            "        <id>103</id>\n" +
            "        <title>To Kill a Mockingbird</title>\n" +
            "        <author>Harper Lee</author>\n" +
            "        <genre>Classic</genre>\n" +
            "        <price>12.99</price>\n" +
            "        <publish_date>1960-07-11</publish_date>\n" +
            "    </book>\n" +
            "</catalog>\n";

    @Before
    public void init() {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new GsonJsonProvider(new GsonBuilder().serializeNulls().create());
            private final MappingProvider mappingProvider = new GsonMappingProvider();

            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    /**
     * Test inline synapse expression template processing with a JSON object.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate1() throws Exception {

        String expected = "Processing payload : {\"name\":\"Alice\",\"role\":\"Developer\",\"experience\":3}";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        String inlineExpression = "Processing payload : ${payload.team[0]}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with a JSON primitive.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate2() throws Exception {

        String expected = "Processing user : Alice";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        String inlineExpression = "Processing user : ${payload.team[0].name}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with non-existing expression.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate3() throws Exception {

        String expected = "Processing user : ";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        String inlineExpression = "Processing user : ${payload.team[0].age}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with variables.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate4() throws Exception {

        String expected = "Processing user with age : 3 lives at {\"no\":110,\"street\":\"Palm Grove\",\"city\":\"Colombo\"}";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        mc.setVariable("age", 3);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("no", 110);
        jsonObject.addProperty("street", "Palm Grove");
        jsonObject.addProperty("city", "Colombo");
        mc.setVariable("address", jsonObject);
        String inlineExpression = "Processing user with age : ${vars.age} lives at ${vars.address}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertFalse("Inline expression content aware should be false", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with multiple expressions.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate5() throws Exception {

        String expected = "Processing user : Alice, role : Developer, experience : 3 years";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        mc.setVariable("role", "Developer");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("level", "3");
        mc.setVariable("experience", jsonObject);
        mc.setProperty("duration", "years");
        String inlineExpression = "Processing user : ${payload.team[0].name}, role : ${vars.role}, " +
                "experience : ${vars.experience.level} ${properties.synapse.duration}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with multiple expressions.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate6() throws Exception {

        String expected = "Processing using endpoint : https://test.wso2.com/, method : get, role : Product Manager";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        mc.setVariable("endpoint", "https://test.wso2.com/");
        mc.setProperty("method", "get");
        String inlineExpression = "Processing using endpoint : ${vars.endpoint}, method : ${properties.synapse.method}, role : ${payload.team[2].role}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with multiple expressions.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate7() throws Exception {

        String expected = "Using endpoint : https://test.wso2.com/ to process book : <book>\n" +
                "        <id>101</id>\n" +
                "        <title>The Great Gatsby</title>\n" +
                "        <author>F. Scott Fitzgerald</author>\n" +
                "        <genre>Fiction</genre>\n" +
                "        <price>10.99</price>\n" +
                "        <publish_date>1925-04-10</publish_date>\n" +
                "    </book>";

        MessageContext mc = TestUtils.getTestContext(xmlPayload);
        mc.setVariable("endpoint", "https://test.wso2.com/");
        String inlineExpression = "Using endpoint : ${vars.endpoint} to process book : ${xpath('//catalog/book[1]')}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }

    /**
     * Test inline synapse expression template processing with multiple expressions.
     */
    @Test
    public void testsInLineSynapseExpressionTemplate8() throws Exception {

        String expected = "Using endpoint : https://test.wso2.com/integration to process sum = 15 and status = true";

        MessageContext mc = TestUtils.getTestContextJson(payload, null);
        mc.setVariable("endpoint", "https://test.wso2.com/");
        mc.setVariable("completed", true);
        String inlineExpression = "Using endpoint : ${vars.endpoint + 'integration'} to process sum = " +
                "${payload.team[0].experience + 12} and status = ${(payload.team[0].experience > 2) && (vars.completed)}";

        boolean isContentAware = InlineExpressionUtil.initInlineSynapseExpressions(inlineExpression, new HashMap<>());
        Assert.assertTrue("Inline expression content aware should be true", isContentAware);

        String result = InlineExpressionUtil.processInLineSynapseExpressionTemplate(mc, inlineExpression, new HashMap<>());
        Assert.assertEquals("Inline expression result mismatch", expected, result);
    }
}

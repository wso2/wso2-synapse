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

package org.apache.synapse.mediators.transform.pfutils;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests for Payload factory Regex Template Processor with inline synapse expressions
 */
@RunWith(Enclosed.class)
public class RegexTemplateProcessorTest extends TestCase {

//    @RunWith(Parameterized.class)
//    public static class PayloadRegexTemplateProcessor {
//
//        private final String template;
//        private final String mediaType;
//        private final String expectedOutput;
//        private final MessageContext messageContext;
//
//        public PayloadRegexTemplateProcessor(String template, String mediaType, String payloadContentType, String payload, String expectedOutput) throws Exception {
//
//            this.template = template;
//            this.mediaType = mediaType;
//            this.expectedOutput = expectedOutput;
//            if (payloadContentType.equals("application/json"))
//                this.messageContext = TestUtils.getTestContextJson(payload, null);
//            else
//                this.messageContext = TestUtils.getTestContext(payload, null);
//        }
//
//        @Parameterized.Parameters
//        public static Collection provideConfigsForPrepareReplacementValueWithEscapeXmlCharsTests() throws Exception {
//
//            return Arrays.asList(new Object[][]{
//                            // Test Case 1: Strings with escape characters
//                            {
//                                    "{\n" +
//                                            "    \"name\": \"${payload.user.name}\",\n" +
//                                            "    \"message\": \"Welcome, ${payload.user.name}!\",\n" +
//                                            "    \"rawString\": \"${payload.rawData}\"\n" +
//                                            "}",
//                                    "json",
//                                    "application/json",
//                                    "{\n" +
//                                            "    \"user\": {\n" +
//                                            "        \"name\": \"John \\\"Doe\\\"\"\n" +
//                                            "    },\n" +
//                                            "    \"rawData\": \"This is a raw \\\\n string with \\\"quotes\\\".\"\n" +
//                                            "}\n",
//                                    "{\n" +
//                                            "    \"name\": \"John \\\"Doe\\\"\",\n" +
//                                            "    \"message\": \"Welcome, John \\\"Doe\\\"!\",\n" +
//                                            "    \"rawString\": \"This is a raw \\\\n string with \\\"quotes\\\".\"\n" +
//                                            "}"
//                            },
//                            // Test Case 2: Numbers and booleans
//                            {
//                                    "{ \"age\": ${payload.user.age}, \"isActive\": ${payload.user.isActive} }",
//                                    "json",
//                                    "application/json",
//                                    "{ \"user\": { \"age\": 30, \"isActive\": true } }",
//                                    "{ \"age\": 30, \"isActive\": true }"
//                            },
//                            // Test Case 3: Arrays and nested objects
//                            {
//                                    "{ \"items\": ${payload.items}, \"firstItem\": \"${payload.items[0].name}\" }",
//                                    "json",
//                                    "application/json",
//                                    "{ \"items\": [{ \"name\": \"item1\" }, { \"name\": \"item2\" }] }",
//                                    "{ \"items\": [{\"name\":\"item1\"},{\"name\":\"item2\"}], \"firstItem\": \"item1\" }"
//                            },
//                            // Test Case 4: Special characters and escaping
//                            {
//                                    "{ \"description\": \"${payload.details}\" }",
//                                    "json",
//                                    "application/json",
//                                    "{ \"details\": \"Line1\\\\nLine2\" }",
//                                    "{ \"description\": \"Line1\\\\nLine2\" }"
//                            },
//                            // Test Case 5: non-existing expression
//                            {
//                                    "{\n" +
//                                            "    \"description\": \"${payload.test1}\",\n" +
//                                            "    \"path\": ${vars.path}\n" +
//                                            "}",
//                                    "json",
//                                    "application/json",
//                                    "{ \"details\": \"Line1\\\\nLine2\" }",
//                                    "{\n" +
//                                            "    \"description\": \"\",\n" +
//                                            "    \"path\": \n" +
//                                            "}"
//                            },
//                            // Test Case 6: XML to JSON
//                            {
//                                    "{\n" +
//                                            "    \"first_name\": \"${xpath('//person/first_name')}\",\n" +
//                                            "    \"last_name\": \"${xpath('//person/last_name')}\",\n" +
//                                            "    \"email\": \"${xpath('//person/contact/email')}\",\n" +
//                                            "    \"is_active\": ${xpath('//person/is_active')},\n" +
//                                            "    \"address\": \"${xpath('//person/address/street')}\",\n" +
//                                            "    \"escapedAddress\": \"${xpath('//person/address/street')}\",\n" +
//                                            "    \"special_characters\": \"${xpath('//person/notes')}\"\n" +
//                                            "}\n",
//                                    "json",
//                                    "application/xml",
//                                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
//                                            "   <soapenv:Header/>\n" +
//                                            "   <soapenv:Body>\n" +
//                                            "      <person>\n" +
//                                            "         <first_name>John</first_name>\n" +
//                                            "         <last_name>Doe</last_name>\n" +
//                                            "         <contact>\n" +
//                                            "            <email>john.doe@example.com</email>\n" +
//                                            "         </contact>\n" +
//                                            "         <is_active>false</is_active>\n" +
//                                            "         <address>\n" +
//                                            "            <street>1234 Elm Street</street>\n" +
//                                            "            <city>Springfield</city>\n" +
//                                            "         </address>\n" +
//                                            "         <notes>Special characters: &amp;*()_+|~=`{}[]:&quot;&lt;&gt;?/'</notes>\n" +
//                                            "      </person>\n" +
//                                            "   </soapenv:Body>\n" +
//                                            "</soapenv:Envelope>\n\n",
//                                    "{\n" +
//                                            "    \"first_name\": \"John\",\n" +
//                                            "    \"last_name\": \"Doe\",\n" +
//                                            "    \"email\": \"john.doe@example.com\",\n" +
//                                            "    \"is_active\": false,\n" +
//                                            "    \"address\": \"1234 Elm Street\",\n" +
//                                            "    \"escapedAddress\": \"1234 Elm Street\",\n" +
//                                            "    \"special_characters\": \"Special characters: &*()_+|~=`{}[]:\"<>?/'\"\n" +
//                                            "}\n"
//                            },
//                            // Test Case 7: XML to XML
//                            {
//                                    "<person>\n" +
//                                            "    <full_name>${xpath('//person/first_name')} ${xpath('//person/last_name')}</full_name>\n" +
//                                            "    <contact_info>\n" +
//                                            "        <email>${xpath('//person/contact/email')}</email>\n" +
//                                            "        <active_status>${xpath('//person/is_active')}</active_status>\n" +
//                                            "    </contact_info>\n" +
//                                            "    <address>\n" +
//                                            "        <street>${xpath('//person/address/street')}</street>\n" +
//                                            "        <city>${xpath('//person/address/city')}</city>\n" +
//                                            "    </address>\n" +
//                                            "    <notes>${xpath('//person/notes')}</notes>\n" +
//                                            "</person>\n",
//                                    "xml",
//                                    "application/xml",
//                                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
//                                            "   <soapenv:Header/>\n" +
//                                            "   <soapenv:Body>\n" +
//                                            "      <person>\n" +
//                                            "         <first_name>John</first_name>\n" +
//                                            "         <last_name>Doe</last_name>\n" +
//                                            "         <contact>\n" +
//                                            "            <email>john.doe@example.com</email>\n" +
//                                            "         </contact>\n" +
//                                            "         <is_active>false</is_active>\n" +
//                                            "         <address>\n" +
//                                            "            <street>1234 Elm Street</street>\n" +
//                                            "            <city>Springfield</city>\n" +
//                                            "         </address>\n" +
//                                            "         <notes>Special characters: &amp;*()_+|~=`{}[]:&quot;&lt;&gt;?/'</notes>\n" +
//                                            "      </person>\n" +
//                                            "   </soapenv:Body>\n" +
//                                            "</soapenv:Envelope>\n",
//                                    "<pfPadding><person>\n" +
//                                            "    <full_name>John Doe</full_name>\n" +
//                                            "    <contact_info>\n" +
//                                            "        <email>john.doe@example.com</email>\n" +
//                                            "        <active_status>false</active_status>\n" +
//                                            "    </contact_info>\n" +
//                                            "    <address>\n" +
//                                            "        <street>1234 Elm Street</street>\n" +
//                                            "        <city>Springfield</city>\n" +
//                                            "    </address>\n" +
//                                            "    <notes>Special characters: &amp;*()_+|~=`{}[]:&quot;&lt;&gt;?/&apos;</notes>\n" +
//                                            "</person>\n</pfPadding>"
//                            },
//                    }
//            );
//        }
//
//        @Test
//        public void testPrepareReplacementValueWithEscapeXmlChars() {
//
//            TemplateProcessor templateProcessor = new RegexTemplateProcessor();
//            String result = templateProcessor.processTemplate(template, mediaType, messageContext);
//            Assert.assertEquals(expectedOutput, result);
//        }
//    }
}

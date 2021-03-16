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
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.transform.pfutils;

import com.google.common.collect.Maps;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.transform.ArgumentDetails;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.apache.synapse.mediators.transform.PayloadFactoryMediator.QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class TemplateProcessorTest {

    private static final String inputPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap" +
            ".org/soap/envelope/\">\n"
            + "   <soapenv:Header/>\n"
            + "   <soapenv:Body>\n"
            + "   </soapenv:Body>\n"
            + "</soapenv:Envelope>  ";

    private static final String JSON_TYPE = "json";
    private static final String XML_TYPE = "xml";

    private static final String X_PATH = "X_PATH";
    private static final String JSON_PATH = "JSON_PATH";

    @RunWith(Parameterized.class)
    public static class PrepareReplacementValue {

        private final String mediaType;
        private final String replacementValue;
        private final Map.Entry<String, ArgumentDetails> replacementEntry;
        private final MessageContext messageContext;

        public PrepareReplacementValue(String mediaType, Map.Entry<String, ArgumentDetails> replacementEntry,
                                       MessageContext messageContext, String replacementValue) {

            this.mediaType = mediaType;
            this.replacementEntry = replacementEntry;
            this.messageContext = messageContext;
            this.replacementValue = replacementValue;
        }

        @Parameterized.Parameters
        public static Collection provideConfigsForPrepareReplacementValueTests() throws Exception {

            MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext(inputPayload);

            // xmlToJson
            Map.Entry<String, ArgumentDetails> xmlToJson = Maps.immutableEntry("<name>john smith</name>",
                    getArgumentDetails(X_PATH, true, false));
            Map.Entry<String, ArgumentDetails> xmlToJsonWithSpecialChars = Maps.immutableEntry("<name>hello\\\\nworld" +
                            "</name>",
                    getArgumentDetails(X_PATH, true, false));

            // jsonToXml
            Map.Entry<String, ArgumentDetails> jsonToXml = Maps.immutableEntry("{\"name\":\"john smith\"}",
                    getArgumentDetails(JSON_PATH, false, false));
            Map.Entry<String, ArgumentDetails> jsonToXmlWithSpecialChars =
                    Maps.immutableEntry("{\"name\":\"hello\\\\nworld\"}",
                            getArgumentDetails(JSON_PATH, true, false));

            // jsonToXml when path evaluates to regular String
            Map.Entry<String, ArgumentDetails> jsonToXmlWithInferReplacementTypeString = Maps.immutableEntry(
                    "{\"name\":\"john smith\"}",
                    getArgumentDetails(JSON_PATH, false, true));
            Map.Entry<String, ArgumentDetails> jsonToXmlWithInferReplacementTypeStringAndSpecialChars =
                    Maps.immutableEntry(
                            "{\"name\":\"hello\\\\nworld\"}",
                            getArgumentDetails(JSON_PATH, false, true));

            // InferReplacementTypeStringAndMediaTypeJson
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson =
                    Maps.immutableEntry(
                            "{\"name\":\"hello\\\\nworld\"}",
                            getArgumentDetails(JSON_PATH, false, true));
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson2 =
                    Maps.immutableEntry(
                            "{\"name\":\"john smith\"}",
                            getArgumentDetails(X_PATH, false, true));
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson3 =
                    Maps.immutableEntry(
                            "{\"name\":\"hello\\\\nworld\"}",
                            getArgumentDetails(X_PATH, false, true));

            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson4 =
                    Maps.immutableEntry(
                            "\"hello\\\\nworld\"",
                            getArgumentDetails(JSON_PATH, false, false));

            // With literal false
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson5 =
                    Maps.immutableEntry(
                            "{\"name\":\"john smith\"}",
                            getArgumentDetails(X_PATH, false, false));
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJson6 =
                    Maps.immutableEntry(
                            "{\"name\":\"hello\\\\nworld\"}",
                            getArgumentDetails(X_PATH, false, false));

            // InferReplacementTypeStringAndMediaTypeJson and QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON set to true
            Map.Entry<String, ArgumentDetails> inferReplacementTypeStringAndMediaTypeJsonWithForce_string_quote =
                    Maps.immutableEntry(
                            "\"hello\\\\nworld\"",
                            getArgumentDetails(JSON_PATH, false, false));
            MessageContext synCtx2 = TestUtils.createLightweightSynapseMessageContext(inputPayload);
            synCtx2.setProperty(QUOTE_STRING_IN_PAYLOAD_FACTORY_JSON, "true");

            return Arrays.asList(new Object[][]{
                    {JSON_TYPE, xmlToJson, synCtx, "{\"name\":\"john smith\"}"},
                    {JSON_TYPE, xmlToJsonWithSpecialChars, synCtx, "{\"name\":\"hello\\\\\\\\\\nworld\"}"},
                    {XML_TYPE, jsonToXml, synCtx, "<name>john smith</name>"},
                    {XML_TYPE, jsonToXmlWithSpecialChars, synCtx, "<name>hello\\\nworld</name>"},
                    {XML_TYPE, jsonToXmlWithInferReplacementTypeString, synCtx, "<name>john smith</name>"},
                    {XML_TYPE, jsonToXmlWithInferReplacementTypeStringAndSpecialChars, synCtx,
                            "<name>hello\\\nworld</name>"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson, synCtx,
                            "{\\\\\"name\\\\\":\\\\\"hello\\\\\\\\nworld\\\\\"}"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson2, synCtx,
                            "{\\\\\"name\\\\\":\\\\\"john smith\\\\\"}"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson3, synCtx,
                            "{\\\\\"name\\\\\":\\\\\"hello\\\\\\\\nworld\\\\\"}"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson4, synCtx, "\\\\\"hello\\\\\\\\nworld\\\\\""},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson5, synCtx, "{\"name\":\"john smith\"}"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJson6, synCtx, "{\"name\":\"hello\\\\nworld\"}"},
                    {JSON_TYPE, inferReplacementTypeStringAndMediaTypeJsonWithForce_string_quote, synCtx2,
                            "\"\\\\\"hello\\\\\\\\nworld\\\\\"\""},
            });
        }

        @Test
        public void testPrepareReplacementValue() {

            TemplateProcessor templateProcessor = new RegexTemplateProcessor();
            assertEquals(replacementValue,
                    templateProcessor.prepareReplacementValue(mediaType, messageContext, replacementEntry));
        }
    }

    @RunWith(Parameterized.class)
    public static class PrepareReplacementValueWithEscapeXmlChars {

        private final String mediaType;
        private final String replacementValue;
        private final Map.Entry<String, ArgumentDetails> replacementEntry;
        private final MessageContext messageContext;

        public PrepareReplacementValueWithEscapeXmlChars(String mediaType,
                                                         Map.Entry<String, ArgumentDetails> replacementEntry,
                                                         MessageContext messageContext, String replacementValue) {

            this.mediaType = mediaType;
            this.replacementEntry = replacementEntry;
            this.messageContext = messageContext;
            this.replacementValue = replacementValue;
        }

        @Parameterized.Parameters
        public static Collection provideConfigsForPrepareReplacementValueWithEscapeXmlCharsTests() throws Exception {

            // json with escapeXmlChars
            Map.Entry<String, ArgumentDetails> jsonWithEscapeXmlChars =
                    Maps.immutableEntry(
                            "{\"name\":\"hello\nworld\"}",
                            getArgumentDetails(JSON_PATH, false, false));

            MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext(inputPayload);

            return Arrays.asList(new Object[][]{
                    {JSON_TYPE, jsonWithEscapeXmlChars, synCtx, "{\"name\":\"hello\\\\nworld\"}"}
            });
        }

        @Test
        public void testPrepareReplacementValueWithEscapeXmlChars() {

            TemplateProcessor templateProcessor = new RegexTemplateProcessor();
            templateProcessor.setEscapeXmlChars(true);
            assertEquals(replacementValue,
                    templateProcessor.prepareReplacementValue(mediaType, messageContext, replacementEntry));
        }
    }

    private static ArgumentDetails getArgumentDetails(String pathType, boolean isXML, boolean isLiteral) {

        ArgumentDetails argumentDetails = new ArgumentDetails();
        argumentDetails.setPathType(pathType);
        argumentDetails.setXml(isXML);
        argumentDetails.setLiteral(isLiteral);
        return argumentDetails;
    }
}

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.axis2.AxisFault;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.synapse.expression.exception.SyntaxErrorListener;
import org.apache.synapse.util.synapse.expression.visitor.ExpressionVisitor;
import org.apache.synapse.util.synapse_expression.ExpressionLexer;
import org.apache.synapse.util.synapse_expression.ExpressionParser;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.jaxen.JaxenException;


import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for testing Synapse Expressions.
 */
public class TestUtils {

    private static Axis2MessageContext synCtx = null;
    private static final String PAYLOAD1 = "{\"name\":\"John\",\"age\":30,\"cars\":[ \"Ford\", " +
            "\"BMW\", \"Fiat\", \"Honda\", \"Lexus\", \"KIA\" ],\"index\":1,\"string\":\" Hello World \"," +
            " \"null\" : null }";
    private static final String PAYLOAD2 = "{\n" +
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
            "}\n";

    private static final JsonObject responseHeaders;

    private static final JsonObject responseAttributes;

    private static final String PAYLOAD3 = "[\"When\",\"my\",\"time\",\"comes\",\"Forget\",\"the\",\"wrong\",\"that\"" +
            ",\"I've\",\"done\"]";
    private static final Map<String, Object> variableMap1;

    private static final Map<String, Object> variableMap2;

    private static final Map<String, Object> variableMap3;

    static {
        variableMap1 = new HashMap<>();
        variableMap1.put("name", "John");
        variableMap1.put("age", 30);
        variableMap1.put("cars", JsonParser.parseString("[\"Ford\",\"BMW\",\"Fiat\",\"Honda\",\"Lexus\",\"KIA\"]").getAsJsonArray());
        variableMap1.put("index", "1");
        variableMap1.put("num1", 10);
        variableMap1.put("num2", 5);
        variableMap1.put("num3", -2.5);
        variableMap1.put("num4", -2.0);
        variableMap1.put("encoded", "V1NPMk1J");
        variableMap1.put("empty", "");
        variableMap2 = new HashMap<>();
        variableMap2.put("json1", PAYLOAD1);
        variableMap2.put("json2", PAYLOAD2);
        variableMap2.put("json3", "[1,2,3,\"abc\"]");
        variableMap3 = new HashMap<>();
        responseHeaders = new JsonObject();
        responseHeaders.addProperty("Content-Type", "application/json");
        responseHeaders.addProperty("Content-Length", "101");
        responseAttributes = new JsonObject();
        responseAttributes.addProperty("statusCode", 201);
        Map<String, Object> result = new HashMap<>();
        result.put("headers", responseHeaders);
        result.put("attributes", responseAttributes);
        result.put("payload", JsonParser.parseString(PAYLOAD2));
        variableMap3.put("fileRead_1", result);

        try {
            synCtx = org.apache.synapse.mediators.TestUtils.getAxis2MessageContext("<test/>", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String evaluateExpression(String expression) {
        return evaluateExpressionWithPayload(expression, 0);
    }

    public static String evaluateExpressionWithPayload(String expression, int payloadId) {
        return evaluateExpressionWithPayloadAndVariables(expression, payloadId, 0);
    }

    public static String evaluateExpressionWithPayloadAndVariables(String expression, int payloadId, int variableMapId) {
        try {
            if (payloadId == 1) {
                JsonUtil.getNewJsonPayload(synCtx.getAxis2MessageContext(), PAYLOAD1, true, true);
            } else if (payloadId == 2) {
                JsonUtil.getNewJsonPayload(synCtx.getAxis2MessageContext(), PAYLOAD2, true, true);
            } else if (payloadId == 3) {
                JsonUtil.getNewJsonPayload(synCtx.getAxis2MessageContext(), PAYLOAD3, true, true);
            }
            if (variableMapId == 1) {
                for (Map.Entry<String, Object> entry : variableMap1.entrySet()) {
                    synCtx.setVariable(entry.getKey(), entry.getValue());
                }
            } else if (variableMapId == 2) {
                for (Map.Entry<String, Object> entry : variableMap2.entrySet()) {
                    synCtx.setVariable(entry.getKey(), entry.getValue());
                }
            } else if (variableMapId == 3) {
                for (Map.Entry<String, Object> entry : variableMap3.entrySet()) {
                    synCtx.setVariable(entry.getKey(), entry.getValue());
                }
            }
            SynapseExpression synapsePath = new SynapseExpression(expression);
            return synapsePath.stringValueOf(synCtx);
        } catch (JaxenException | AxisFault e) {
            throw new RuntimeException(e);
        }
    }

    public static void evaluateWithErrorListener(SyntaxErrorListener errorListener, String expression) {
        CharStream input = CharStreams.fromString(expression);
        ExpressionLexer lexer = new ExpressionLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        // trigger parsing
        ParseTree tree = parser.expression();
        ExpressionVisitor visitor = new ExpressionVisitor();
        visitor.visit(tree);
    }

    /**
     * used in tests to get an Axis2MessageContext with a given payload
     */
    public static void clearMessageContext() {
        try {
            synCtx = org.apache.synapse.mediators.TestUtils.getAxis2MessageContext("<test/>", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

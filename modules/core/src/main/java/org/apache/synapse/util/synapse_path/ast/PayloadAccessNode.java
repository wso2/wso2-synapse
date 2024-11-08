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
package org.apache.synapse.util.synapse_path.ast;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.synapse_path.context.EvaluationContext;
import org.apache.synapse.util.synapse_path.exception.EvaluationException;
import org.apache.synapse.util.synapse_path.utils.ExpressionUtils;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a node in the AST that accesses a value in the payload or variable.
 * Resolve placeholders in the expression and evaluate the expression using JSONPath.
 */
public class PayloadAccessNode implements ExpressionNode {
    private static final Log log = LogFactory.getLog(PayloadAccessNode.class);
    private String expression;
    private final Map<String, ExpressionNode> arguments;

    public enum Type {
        PAYLOAD,
        VARIABLE,
        REGISTRY,
        OBJECT,
        ARRAY
    }

    private final Type type;
    private ExpressionNode predefinedFunctionNode = null;

    public PayloadAccessNode(String expression, Map<String, ExpressionNode> arguments, Type type,
                             ExpressionNode predefinedFunctionNode) {
        this.expression = expression;
        this.arguments = arguments;
        this.type = type;
        this.predefinedFunctionNode = predefinedFunctionNode;
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

    @Override
    public ExpressionResult evaluate(EvaluationContext context) {
        if (expression.startsWith(SynapseConstants.PAYLOAD)) {
            expression = SynapseConstants.PAYLOAD_$ + expression.substring(SynapseConstants.PAYLOAD.length());
        }

        for (Map.Entry<String, ExpressionNode> entry : arguments.entrySet()) {
            Optional.ofNullable(entry.getValue())
                    .map(value -> value.evaluate(context))
                    .ifPresent(result -> processResult(entry, result));
        }

        Object result = null;
        switch (type) {
            case PAYLOAD:
                try {
                    result = context.getPayload();
                    if (result == null) {
                        throw new EvaluationException("Could not find a JSON payload to evaluate the expression: "
                                + expression);
                    }
                    result = JsonPath.parse(context.getPayload().toString()).read(expression);
                } catch (PathNotFoundException e) {
                    // convert jsonPath error to native one
                    throw new EvaluationException(e.getMessage());
                } catch (IOException e) {
                    throw new EvaluationException("Error while parsing payload");
                }
                break;
            case VARIABLE:
                String[] variableAndExpression = ExpressionUtils.extractVariableAndJsonPath(expression);
                Object variable = context.getVariable(variableAndExpression[0]);
                if (variable == null) {
                    throw new EvaluationException("Variable " + variableAndExpression[0] + " is not defined");
                }
                String expressionToEvaluate = variableAndExpression[1];
                // if no expression just return variable
                if (StringUtils.isEmpty(expressionToEvaluate)) {
                    return parseVariable(variable);
                } else {
                    expressionToEvaluate = expressionToEvaluate.startsWith(".") ? "$" + expressionToEvaluate
                            : "$." + expressionToEvaluate;
                    try {
                        result = JsonPath.parse(variable.toString()).read(expressionToEvaluate);
                    } catch (PathNotFoundException e) {
                        // convert jsonPath error to native one
                        throw new EvaluationException(e.getMessage());
                    }
                }
                break;
            case REGISTRY:
                ExpressionResult registryValue = predefinedFunctionNode.evaluate(context);
                try {
                    if (registryValue == null) {
                        throw new EvaluationException("Could not find a JSON payload to evaluate the expression: " + expression);
                    } else if (registryValue.isOMElement()) {
                        throw new EvaluationException("Could not evaluate JSONPath expression: " + expression
                                + " on non-JSON registry value");
                    }
                    expression = expression.startsWith(".") ? "$" + expression : "$." + expression;
                    result = JsonPath.parse(registryValue.asString()).read(expression);
                } catch (PathNotFoundException e) {
                    // convert jsonPath error to native one
                    throw new EvaluationException(e.getMessage());
                }
                break;
            case OBJECT:
                expression = expression.startsWith(".") ? "$" + expression : "$." + expression;
                ExpressionResult objFuncResult = predefinedFunctionNode.evaluate(context);
                try {
                    result = JsonPath.parse(objFuncResult.asJsonElement()).read(expression);
                } catch (PathNotFoundException e) {
                    throw new EvaluationException(e.getMessage());
                }
                break;
            case ARRAY:
                expression = "[" + arguments.get(SynapseConstants.ARRAY).evaluate(context).asString() + "]";
                ExpressionResult arrFuncResult = predefinedFunctionNode.evaluate(context);
                try {
                    result = JsonPath.parse(arrFuncResult.asJsonElement()).read(expression);
                } catch (PathNotFoundException e) {
                    throw new EvaluationException(e.getMessage());
                }
                break;
            default:
                throw new EvaluationException("Unsupported type: " + type);
        }
        if (result instanceof JsonPrimitive) {
            return new ExpressionResult((JsonPrimitive) result);
        } else if (result instanceof JsonNull) {
            // unbox JsonNull to null
            return null;
        } else if (result instanceof JsonElement) {
            return new ExpressionResult((JsonElement) result);
        }
        return null;
    }

    private void processResult(Map.Entry<String, ExpressionNode> entry, ExpressionResult result) {
        String regex = ExpressionUtils.escapeSpecialCharacters(entry.getKey());
        String resultString = result.asString();
        if (result.isString() && !entry.getValue().getClass().equals(FilterExpressionNode.class)) {
            resultString = "\"" + resultString + "\"";
        }
        if (entry.getValue().getClass().equals(ArrayIndexNode.class)) {
            resultString = resultString.replace("\"", "");
        }
        expression = expression.replaceFirst(regex, resultString);
    }

    private ExpressionResult parseVariable(Object variable) {
        try {
            return new ExpressionResult(Integer.parseInt(variable.toString()));
        } catch (NumberFormatException e1) {
            // If integer parsing fails, attempt to parse as double
            try {
                return new ExpressionResult(Double.parseDouble(variable.toString()));
            } catch (NumberFormatException e2) {
                // If double parsing fails, attempt to parse as JSON
                try {
                    JsonElement jsonElement = JsonParser.parseString(variable.toString());
                    return new ExpressionResult(jsonElement);
                } catch (JsonSyntaxException e3) {
                    // If JSON parsing fails, return the variable as a string
                    return new ExpressionResult(variable.toString());
                }
            }
        }
    }
}

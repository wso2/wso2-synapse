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
package org.apache.synapse.util.synapse.expression.ast;

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
import org.apache.axiom.om.OMNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;
import org.apache.synapse.util.synapse.expression.exception.EvaluationException;
import org.apache.synapse.util.synapse.expression.utils.ExpressionUtils;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a node in the AST that accesses a value in the payload or variable.
 * Resolve placeholders in the expression and evaluate the expression using JSONPath.
 */
public class PayloadAccessNode implements ExpressionNode {

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
    private final ExpressionNode predefinedFunctionNode;

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
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) throws EvaluationException {
        if (expression.startsWith(ExpressionConstants.PAYLOAD)) {
            expression = ExpressionConstants.PAYLOAD_$ + expression.substring(ExpressionConstants.PAYLOAD.length());
        }

        for (Map.Entry<String, ExpressionNode> entry : arguments.entrySet()) {
            Optional.ofNullable(entry.getValue())
                    .map(value -> value.evaluate(context, isObjectValue))
                    .ifPresent(result -> processResult(entry, result));
        }

        Object result;
        switch (type) {
            case PAYLOAD:
                try {
                    result = context.getJSONResult(isObjectValue, expression);
                } catch (PathNotFoundException e) {
                    // convert jsonPath error to native one
                    throw new EvaluationException(e.getMessage());
                } catch (IOException e) {
                    throw new EvaluationException("Error while parsing payload");
                } catch (JaxenException e) {
                    throw new EvaluationException("Error while retrieving payload");
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
                    if (ExpressionUtils.isXMLVariable(variable)) {
                        throw new EvaluationException("Could not evaluate JSONPath expression: " + expression
                                + " on non-JSON variable value");
                    }
                    try {
                        if (isObjectValue) {
                            SynapseJsonPath jsonPath = new SynapseJsonPath(expressionToEvaluate);
                            result = jsonPath.evaluate(variable.toString());
                        } else {
                            result = JsonPath.parse(variable.toString()).read(expressionToEvaluate);
                        }
                    } catch (PathNotFoundException e) {
                        // convert jsonPath error to native one
                        throw new EvaluationException(e.getMessage());
                    } catch (JaxenException e) {
                        throw new EvaluationException("Error while parsing the expression: " + expressionToEvaluate);
                    }
                }
                break;
            case REGISTRY:
                ExpressionResult registryValue = predefinedFunctionNode.evaluate(context, isObjectValue);
                try {
                    if (registryValue == null) {
                        throw new EvaluationException("Could not find a JSON payload to evaluate the expression: "
                                + expression);
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
            case ARRAY:
            case OBJECT:
                expression = expression.startsWith(".") ? "$" + expression : "$." + expression;
                ExpressionResult objFuncResult = predefinedFunctionNode.evaluate(context, isObjectValue);
                try {
                    result = JsonPath.parse(objFuncResult.asJsonElement()).read(expression);
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
        } else if (result instanceof String) {
            return new ExpressionResult((String) result);
        } else if (result instanceof Integer) {
            return new ExpressionResult((Integer) result);
        } else if (result instanceof Long) {
            return new ExpressionResult((Long) result);
        } else if (result instanceof Double) {
            return new ExpressionResult((Double) result);
        } else if (result instanceof Boolean) {
            return new ExpressionResult((Boolean) result);
        } else if (result instanceof List) {
            return new ExpressionResult((List) result);
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
        if (variable instanceof List) {
            return new ExpressionResult((List) variable);
        } else if (variable instanceof OMNode) {
            return new ExpressionResult((OMNode) variable);
        } else if (variable instanceof JsonElement) {
            return new ExpressionResult((JsonElement) variable);
        } else {
            try {
                return new ExpressionResult(Integer.parseInt(variable.toString()));
            } catch (NumberFormatException e1) {
                // If integer parsing fails, attempt to parse as double
                try {
                    return new ExpressionResult(Double.parseDouble(variable.toString()));
                } catch (NumberFormatException e2) {
                    // If double parsing fails, attempt to parse as JSON
                    try {
                        if (variable.equals("")) {
                            // avoid converting empty string to NULL value
                            return new ExpressionResult("");
                        }
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
}

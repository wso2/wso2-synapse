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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;

/**
 * Represents a leaf node in the AST that holds a literal value.
 */
public class LiteralNode implements ExpressionNode {
    private final String value;
    private ArgumentListNode parameterList = new ArgumentListNode();

    public enum Type {
        NUMBER,
        STRING,
        BOOLEAN,
        NULL,
        ARRAY
    }

    private final Type type;

    public LiteralNode(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public LiteralNode(ArgumentListNode value, Type type) {
        this.parameterList = value;
        this.type = type;
        this.value = "";
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) {
        switch (type) {
            case NUMBER:
                return parseNumber(value);
            case STRING:
                return new ExpressionResult(value);
            case BOOLEAN:
                return new ExpressionResult(Boolean.parseBoolean(value));
            case NULL:
                return null;
            case ARRAY:
                return parseArray(context, isObjectValue);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private ExpressionResult parseNumber(String value) {
        try {
            return new ExpressionResult(Integer.parseInt(value));
        } catch (NumberFormatException e1) {
            try {
                return new ExpressionResult(Float.parseFloat(value));
            } catch (NumberFormatException e2) {
                try {
                    return new ExpressionResult(Double.parseDouble(value));
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Value " + value + " is not a number");
                }
            }
        }
    }

    private ExpressionResult parseArray(EvaluationContext context, boolean isObjectValue) {
        JsonArray jsonArray = new JsonArray();
        for (ExpressionNode expressionNode : parameterList.getArguments()) {
            ExpressionResult result = expressionNode.evaluate(context, isObjectValue);
            if (result.getType().equals(JsonElement.class)) {
                jsonArray.add(result.asJsonElement());
            } else if (result.isInteger()) {
                jsonArray.add(result.asInt());
            } else if (result.isDouble()) {
                jsonArray.add(result.asDouble());
            } else {
                jsonArray.add(result.asString());
            }
        }
        return new ExpressionResult(jsonArray);
    }
}

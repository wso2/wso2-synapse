/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.expression.impl.ast;


import org.apache.synapse.expression.impl.context.EvaluationContext;
import org.apache.synapse.expression.impl.exception.EvaluationException;

import java.util.function.BiFunction;

/**
 * Represents a binary operation between two nodes in AST.
 */
public class BinaryOperationNode implements ExpressionNode {

    private final ExpressionNode left;
    private final ExpressionNode right;
    private final Operator operator;

    public enum Operator {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MODULO("%"),
        EQUALS("=="),
        NOT_EQUALS("!="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        AND("and"),
        AND_SYMBOL("&&"),
        OR("or"),
        OR_SYMBOL("||");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public static Operator fromString(String symbol) {
            for (Operator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unsupported operator: " + symbol);
        }
    }

    public BinaryOperationNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = Operator.fromString(operator.trim().toLowerCase());
        this.right = right;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) throws EvaluationException {
        ExpressionResult leftValue = left.evaluate(context, isObjectValue);
        ExpressionResult rightValue = right.evaluate(context, isObjectValue);
        if ((leftValue == null || rightValue == null) &&
                (operator != Operator.EQUALS && operator != Operator.NOT_EQUALS)) {
            throw new EvaluationException("Null inputs for " + operator + " operation: " + leftValue
                    + " and " + rightValue);
        }

        switch (operator) {
            case ADD:
                return handleAddition(leftValue, rightValue);
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return handleArithmetic(leftValue, rightValue, operator);
            case EQUALS:
                return handleEquality(leftValue, rightValue);
            case NOT_EQUALS:
                return handleNotEquality(leftValue, rightValue);
            case LESS_THAN:
                return handleComparison(leftValue, rightValue, (a, b) -> a < b);
            case LESS_THAN_OR_EQUAL:
                return handleComparison(leftValue, rightValue, (a, b) -> a <= b);
            case GREATER_THAN:
                return handleComparison(leftValue, rightValue, (a, b) -> a > b);
            case GREATER_THAN_OR_EQUAL:
                return handleComparison(leftValue, rightValue, (a, b) -> a >= b);
            case AND:
            case AND_SYMBOL:
            case OR:
            case OR_SYMBOL:
                return handleLogical(leftValue, rightValue, operator);
            default:
                throw new EvaluationException("Unsupported operator: " + operator + " between "
                        + leftValue.asString() + " and " + rightValue.asString());
        }
    }

    private ExpressionResult handleComparison(ExpressionResult leftValue, ExpressionResult rightValue,
                                              BiFunction<Double, Double, Boolean> comparison) {
        if ((leftValue.isDouble() || leftValue.isInteger()) && (rightValue.isDouble() || rightValue.isInteger())) {
            return new ExpressionResult(comparison.apply(leftValue.asDouble(), rightValue.asDouble()));
        }
        throw new EvaluationException("Comparison between non-numeric values: "
                + leftValue.asString() + " and " + rightValue.asString());
    }

    private ExpressionResult handleEquality(ExpressionResult leftValue, ExpressionResult rightValue) {
        if (leftValue != null && rightValue != null) {
            return new ExpressionResult(leftValue.asString().equals(rightValue.asString()));
        } else if (leftValue == null && rightValue == null) {
            return new ExpressionResult(true);
        }
        return new ExpressionResult(false);
    }

    private ExpressionResult handleNotEquality(ExpressionResult leftValue, ExpressionResult rightValue) {
        if (leftValue != null && rightValue != null) {
            return new ExpressionResult(!leftValue.asString().equals(rightValue.asString()));
        } else if (leftValue == null && rightValue == null) {
            return new ExpressionResult(false);
        }
        return new ExpressionResult(true);
    }

    private ExpressionResult handleLogical(ExpressionResult leftValue, ExpressionResult rightValue, Operator operator) {
        if (leftValue.isBoolean() && rightValue.isBoolean()) {
            BiFunction<Boolean, Boolean, Boolean> logicOperation = operator
                    == Operator.AND || operator == Operator.AND_SYMBOL ? (a, b) -> a && b : (a, b) -> a || b;
            return new ExpressionResult(logicOperation.apply(leftValue.asBoolean(), rightValue.asBoolean()));
        }
        throw new EvaluationException("Logical operation between non-boolean values: "
                + leftValue.asString() + " and " + rightValue.asString());
    }

    private ExpressionResult handleAddition(ExpressionResult leftValue, ExpressionResult rightValue) {
        if (leftValue.isDouble() || rightValue.isDouble()) {
            return new ExpressionResult(leftValue.asDouble() + rightValue.asDouble());
        } else if (leftValue.isInteger() && rightValue.isInteger()) {
            return new ExpressionResult(leftValue.asInt() + rightValue.asInt());
        } else if (leftValue.isString() && rightValue.isString()) {
            return new ExpressionResult(leftValue.asString().concat(rightValue.asString()));
        }
        throw new EvaluationException("Addition between non-numeric values: " + leftValue.asString()
                + " and " + rightValue.asString());
    }

    private ExpressionResult handleArithmetic(ExpressionResult leftValue, ExpressionResult rightValue,
                                              Operator operator) {
        if (!leftValue.isNumeric() || !rightValue.isNumeric()) {
            throw new EvaluationException("Arithmetic operation: " + operator + " between non-numeric values: "
                    + leftValue.asString() + " and " + rightValue.asString());
        }
        boolean isInteger = leftValue.isInteger() && rightValue.isInteger();
        switch (operator) {
            case SUBTRACT:
                return isInteger ? new ExpressionResult(leftValue.asInt() - rightValue.asInt()) :
                        new ExpressionResult(leftValue.asDouble() - rightValue.asDouble());
            case MULTIPLY:
                return isInteger ? new ExpressionResult(leftValue.asInt() * rightValue.asInt()) :
                        new ExpressionResult(leftValue.asDouble() * rightValue.asDouble());
            case DIVIDE:
                return isInteger ? new ExpressionResult(leftValue.asInt() / rightValue.asInt()) :
                        new ExpressionResult(leftValue.asDouble() / rightValue.asDouble());
            case MODULO:
                return isInteger ? new ExpressionResult(leftValue.asInt() % rightValue.asInt()) :
                        new ExpressionResult(leftValue.asDouble() % rightValue.asDouble());
            default:
                throw new EvaluationException("Unsupported operator: " + operator + " between "
                        + leftValue.asString() + " and " + rightValue.asString());
        }
    }
}

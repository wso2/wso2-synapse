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

import org.apache.synapse.util.synapse.expression.context.EvaluationContext;
import org.apache.synapse.util.synapse.expression.exception.EvaluationException;

import java.math.BigDecimal;
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
                return handleComparison(leftValue, rightValue, (a, b) -> a.compareTo(b)<0);
            case LESS_THAN_OR_EQUAL:
                return handleComparison(leftValue, rightValue, (a, b) -> a.compareTo(b)<=0);
            case GREATER_THAN:
                return handleComparison(leftValue, rightValue, (a, b) -> a.compareTo(b)>0);
            case GREATER_THAN_OR_EQUAL:
                return handleComparison(leftValue, rightValue, (a, b) -> a.compareTo(b)>=0);
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
                                              BiFunction<BigDecimal, BigDecimal, Boolean> comparison) {
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
        if (leftValue.isNumeric() && rightValue.isNumeric()) {
            if (leftValue.isDouble() || rightValue.isDouble()) {
                return new ExpressionResult(leftValue.asDouble().add(rightValue.asDouble()));
            } else if (leftValue.isLong() || rightValue.isLong()) {
                return new ExpressionResult(leftValue.asLong() + rightValue.asLong());
            } else {
                try {
                    int result = Math.addExact(leftValue.asInt(), rightValue.asInt());
                    return new ExpressionResult(result);
                } catch (ArithmeticException e) {
                    // handle overflow
                    return new ExpressionResult(leftValue.asLong() + rightValue.asLong());
                }
            }
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
        boolean leftIsDouble = leftValue.isDouble();
        boolean leftIsLong = leftValue.isLong();
        boolean rightIsDouble = rightValue.isDouble();
        boolean rightIsLong = rightValue.isLong();

        // Promote to the highest precision type
        if (leftIsDouble || rightIsDouble) {
            BigDecimal left = leftValue.asDouble();
            BigDecimal right = rightValue.asDouble();
            return performDoubleOperation(left, right, operator);
        } else if (leftIsLong || rightIsLong) {
            long left = leftValue.asLong();
            long right = rightValue.asLong();
            return performLongOperation(left, right, operator);
        } else {
            // Default to int
            int left = leftValue.asInt();
            int right = rightValue.asInt();
            return performIntOperation(left, right, operator);
        }
    }

    private ExpressionResult performDoubleOperation(BigDecimal left, BigDecimal right, Operator operator) {
        switch (operator) {
            case SUBTRACT:
                BigDecimal sum = left.subtract(right);
                return new ExpressionResult(sum.doubleValue());
            case MULTIPLY:
                BigDecimal product = left.multiply(right);
                return new ExpressionResult(product);
            case DIVIDE:
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new EvaluationException("Division by zero");
                }
                BigDecimal quotient = left.divide(right, BigDecimal.ROUND_HALF_UP);
                return new ExpressionResult(quotient.doubleValue());
            case MODULO:
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new EvaluationException("Modulo by zero");
                }
                BigDecimal remainder = left.remainder(right);
                return new ExpressionResult(remainder.doubleValue());
            default:
                throw new EvaluationException("Unsupported operator: " + operator + " between "
                        + left + " and " + right);
        }
    }

    private ExpressionResult performLongOperation(long left, long right, Operator operator) {
        switch (operator) {
            case SUBTRACT:
                return new ExpressionResult(left - right);
            case MULTIPLY:
                return new ExpressionResult(left * right);
            case DIVIDE:
                if (right == 0L) {
                    throw new EvaluationException("Division by zero");
                }
                return new ExpressionResult((double) left / right);
            case MODULO:
                if (right == 0L) {
                    throw new EvaluationException("Modulo by zero");
                }
                return new ExpressionResult(left % right);
            default:
                throw new EvaluationException("Unsupported operator: " + operator + " between "
                        + left + " and " + right);
        }
    }

    private ExpressionResult performIntOperation(int left, int right, Operator operator) {
        switch (operator) {
            case SUBTRACT:
                return new ExpressionResult(left - right);
            case MULTIPLY:
                try {
                    int result = Math.multiplyExact(left, right);
                    return new ExpressionResult(result);
                } catch (ArithmeticException e) {
                    // handle overflow
                    return new ExpressionResult((long) left * (long) right);
                }
            case DIVIDE:
                if (right == 0) {
                    throw new EvaluationException("Division by zero");
                }
                if (left % right == 0) {
                    // Exact division, return as integer
                    return new ExpressionResult(left / right);
                } else {
                    // Division has a fractional part, return as double
                    return new ExpressionResult((double) left / right);
                }
            case MODULO:
                if (right == 0) {
                    throw new EvaluationException("Modulo by zero");
                }
                return new ExpressionResult(left % right);
            default:
                throw new EvaluationException("Unsupported operator: " + operator + " between "
                        + left + " and " + right);
        }
    }
}

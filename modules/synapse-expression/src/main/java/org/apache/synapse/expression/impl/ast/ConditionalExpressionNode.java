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

/**
 * Represents a conditional expression node ( a ? b : c ) in the AST.
 */
public class ConditionalExpressionNode implements ExpressionNode {

    private final ExpressionNode condition;
    private final ExpressionNode trueExpression;
    private final ExpressionNode falseExpression;

    public ConditionalExpressionNode(ExpressionNode condition, ExpressionNode trueExpression,
                                     ExpressionNode falseExpression) {
        this.condition = condition;
        this.trueExpression = trueExpression;
        this.falseExpression = falseExpression;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) throws EvaluationException {
        ExpressionResult conditionResult = condition.evaluate(context, isObjectValue);
        if (conditionResult == null || conditionResult.isNull()) {
            throw new EvaluationException("Condition is null in conditional expression");
        }
        if (conditionResult.isBoolean()) {
            return conditionResult.asBoolean() ? trueExpression.evaluate(context, isObjectValue)
                    : falseExpression.evaluate(context, isObjectValue);
        } else {
            throw new EvaluationException("Condition is not a boolean in conditional expression");
        }
    }
}

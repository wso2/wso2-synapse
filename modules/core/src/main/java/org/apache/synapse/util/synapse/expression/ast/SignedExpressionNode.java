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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.util.synapse.expression.context.EvaluationContext;

/**
 * Represents a node in the AST that holds a signed expression. ex: ( -var.num1 )
 */
public class SignedExpressionNode implements ExpressionNode {
    private static final Log log = LogFactory.getLog(SignedExpressionNode.class);
    private final ExpressionNode expression;
    private final boolean signed;

    public SignedExpressionNode(ExpressionNode expression, boolean signed) {
        this.expression = expression;
        this.signed = signed;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context, boolean isObjectValue) {
        ExpressionResult result = expression.evaluate(context, isObjectValue);
        if (result == null || result.isNull()) {
            throw new IllegalStateException("Cannot negate a null value");
        }
        if (signed) {
            if (result.isInteger()) {
                return new ExpressionResult(-result.asInt());
            } else if (result.isDouble()) {
                return new ExpressionResult(-result.asDouble());
            }
            throw new IllegalStateException("Cannot negate a non-numeric value : " + result.asString());
        }
        return result;
    }
}

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

import org.apache.synapse.util.synapse_path.context.EvaluationContext;
import org.apache.synapse.util.synapse_path.utils.ExpressionUtils;

import java.util.Map;

/**
 * Represents a json-path filter expression node in the AST.
 */
public class FilterExpressionNode implements ExpressionNode {


    private String expression;
    private final Map<String, ExpressionNode> arguments;

    public FilterExpressionNode(String expression, Map<String, ExpressionNode> arguments) {
        this.expression = expression;
        this.arguments = arguments;
    }

    /**
     * return the formatted JSONPath filter expression.
     * Not evaluating here.
     */
    @Override
    public ExpressionResult evaluate(EvaluationContext context) {
        for (Map.Entry<String, ExpressionNode> entry : arguments.entrySet()) {
            if (entry.getValue() != null) {
                ExpressionResult result = entry.getValue().evaluate(context);
                if (result != null) {
                    String regex = ExpressionUtils.escapeSpecialCharacters(entry.getKey());
                    String resultString = result.asString();
                    if (result.isString()) {
                        resultString = "\"" + resultString + "\"";
                    }
                    expression = expression.replaceFirst(regex, resultString);
                }
            }
        }
        //TODO: Need to stop adding "?" for expressions like $..book[(@.length-1)].title. But not handling this for
        // now since its not even working in json-path.
        return new ExpressionResult("?" + expression);
    }
}

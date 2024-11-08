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

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the index of an array in AST.
 */
public class ArrayIndexNode implements ExpressionNode {
    private final List<ExpressionNode> indexArray;
    private final char separator;

    public ArrayIndexNode(ArgumentListNode arguments, char separator) {
        this.indexArray = arguments.getArguments();
        this.separator = separator;
    }

    @Override
    public ExpressionResult evaluate(EvaluationContext context) {
        List<String> indexList = new ArrayList<>();
        for (ExpressionNode index : indexArray) {
            if (index == null) {
                indexList.add("");
                continue;
            }
            ExpressionResult result = index.evaluate(context);
            if (result != null) {
                indexList.add(result.asString());
            }
        }
        return new ExpressionResult(String.join(String.valueOf(separator), indexList));
    }
}

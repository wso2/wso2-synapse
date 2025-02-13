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

import org.apache.synapse.util.synapse.expression.exception.EvaluationException;
import org.apache.synapse.util.synapse.expression.exception.SyntaxErrorListener;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for syntax errors in the Synapse Expressions.
 */
public class SyntaxErrorsTest {

    SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();

    @Test
    public void testValidExpressions() {
        TestUtils.evaluateWithErrorListener(syntaxErrorListener,"5 > 3");
        Assert.assertFalse(syntaxErrorListener.hasErrors());
    }

    @Test (expected = EvaluationException.class)
    public void testOperationError() {
        TestUtils.evaluateWithErrorListener(syntaxErrorListener,"5 >> 3");
    }

    @Test (expected = EvaluationException.class)
    public void testInvalidExpressionByLength() {
        TestUtils.evaluateExpression("payload.products[0].stock + abc");
    }
}

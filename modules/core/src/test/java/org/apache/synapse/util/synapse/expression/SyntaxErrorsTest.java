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

import org.apache.synapse.util.synapse.expression.exception.SyntaxError;
import org.apache.synapse.util.synapse.expression.exception.SyntaxErrorListener;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

    @Test
    public void testOperationError() {
        TestUtils.evaluateWithErrorListener(syntaxErrorListener,"5 >> 3");
        Assert.assertTrue("Should throw an error", syntaxErrorListener.hasErrors());

        List<SyntaxError> errors = syntaxErrorListener.getErrors();
        Assert.assertEquals(1, errors.size());

        // Assert details of the error
        SyntaxError error = errors.get(0);
        Assert.assertTrue(error.getMessage().contains("no viable alternative at input '5>>'"));
        Assert.assertEquals(3, error.getCharPositionInLine());

        syntaxErrorListener.clearErrors();
    }
}

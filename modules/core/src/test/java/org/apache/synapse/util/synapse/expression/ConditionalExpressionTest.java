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

import org.apache.synapse.SynapseConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for conditional (if-else) expressions.
 */
public class ConditionalExpressionTest {
    @Test
    public void testIfElse() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("true ? true : false"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("false ? true : false"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("false ? true : false ? false : true"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("true ? false ? false : true : false"));
        Assert.assertEquals("true", TestUtils.evaluateExpression(" 5 > 4 ? (false ? false : true) : false"));
        Assert.assertEquals("5", TestUtils.evaluateExpression(" 5.2 > 5 ? 5 : 4"));
        Assert.assertEquals(SynapseConstants.UNKNOWN, TestUtils.evaluateExpression("\"bla\"? true : 123"));
        Assert.assertEquals(SynapseConstants.UNKNOWN, TestUtils.evaluateExpression("45 == (  5 + 34 ? true : 456)"));
        Assert.assertEquals(SynapseConstants.UNKNOWN,
                TestUtils.evaluateExpressionWithPayload("45 == (  $[\"null\"] ? true : 456)",1));
        Assert.assertEquals("[22.99]", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "var.num1 > var.num2 ? $..book[?(@.author =~ /.*Tolkien/i)].price " +
                        ": $..book[(@.\"length\"-1)].title", 2,1));
        Assert.assertEquals(SynapseConstants.UNKNOWN, TestUtils.evaluateExpression("null == $[\"null\"] ? 123 : 456"));
        Assert.assertEquals(SynapseConstants.UNKNOWN, TestUtils.evaluateExpressionWithPayload(
                "$[\"null\"] ? 123 : 456",1));
        Assert.assertEquals("123", TestUtils.evaluateExpressionWithPayload(
                "$[\"null\"] == null ? 123 : 456",1));
        Assert.assertEquals(SynapseConstants.UNKNOWN, TestUtils.evaluateExpressionWithPayload("null ? 4 : 5",1));
    }
}

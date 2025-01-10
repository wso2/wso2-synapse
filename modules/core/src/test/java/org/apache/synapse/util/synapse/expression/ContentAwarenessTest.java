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
package org.apache.synapse.util.synapse.expression;

import org.apache.synapse.util.xpath.SynapseExpression;
import org.jaxen.JaxenException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for content awareness in the Synapse Expressions.
 */
public class ContentAwarenessTest {

    @Test
    public void testPayloadAccess() throws JaxenException {
        SynapseExpression synapsePath = new SynapseExpression("payload");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("$");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("payload.name");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("$.name");
        Assert.assertTrue(synapsePath.isContentAware());
    }

    @Test
    public void testComplexUsages() throws JaxenException {
        SynapseExpression synapsePath = new SynapseExpression("length(payload)");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("5 + length(payload)");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("5 + length(payload[\"payload\"])");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("$..book[?(@.category==payload.category)]");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("$..book[?(@.category==\"payload.category\")]");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.books[?(@.category==payload.category)]");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.books[?(@.category==$.category)]");
        Assert.assertTrue(synapsePath.isContentAware());
    }

    @Test
    public void testWithXpath() throws JaxenException {
        SynapseExpression synapsePath = new SynapseExpression("xpath(\"$ctx:name\")");
        Assert.assertFalse(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("xpath(\"//student\")");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("xpath(\"/student\")");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("xpath(\"//*\") + vars.a$bc");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("xpath(\"$ctx:bla\") + $.age");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.num1 + vars.[\"payload\"] + xpath(\"//num3\")");
        Assert.assertTrue(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.num1 + vars.[\"payload\"] + xpath(\"$ctx:num3\")");
        Assert.assertFalse(synapsePath.isContentAware());
    }

    @Test
    public void testNegativeCases() throws JaxenException {
        SynapseExpression synapsePath = new SynapseExpression("length(vars.abc)");
        Assert.assertFalse(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars[\"payload\"]");
        Assert.assertFalse(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("5 + var[\"payload\"].age");
        Assert.assertFalse(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.a$.bc");
        Assert.assertFalse(synapsePath.isContentAware());
        synapsePath = new SynapseExpression("vars.books[?(@.category==\"payload.category\")]");
        Assert.assertFalse(synapsePath.isContentAware());
    }
}

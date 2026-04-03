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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the xpath() predefined function, including the dot-notation extension
 * that allows accessing XML payloads stored inside connector response variables.
 *
 * <p>Connector responses are Map-typed variables with keys "payload", "headers",
 * and "attributes". The syntax xpath('//expr', 'varName.payload') evaluates the
 * XPath expression against the OMElement stored at map.get("payload").
 *
 * <p>Test data (set up in TestUtils.variableMap4):
 * <ul>
 *   <li>personVar.payload — &lt;person&gt;&lt;name&gt;John&lt;/name&gt;...&lt;/person&gt;</li>
 *   <li>orderVar.payload  — &lt;order id="ORD-001"&gt;&lt;item quantity="3"&gt;...&lt;/item&gt;&lt;/order&gt;</li>
 *   <li>nsVar.payload     — &lt;ns:employee xmlns:ns="http://example.com"&gt;...&lt;/ns:employee&gt;</li>
 *   <li>plainXmlVar       — OMElement (not a Map) — used for error-path testing</li>
 * </ul>
 */
public class XPathFunctionTest {

    // -------------------------------------------------------------------------
    // Text extraction
    // -------------------------------------------------------------------------

    @Test
    public void testTextExtractionFromConnectorPayload() {
        Assert.assertEquals("John",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//name/text()', 'personVar.payload')", 0, 4));
    }

    @Test
    public void testNumericTextFromConnectorPayload() {
        Assert.assertEquals("30",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//age/text()', 'personVar.payload')", 0, 4));
    }

    @Test
    public void testNestedElementTextFromConnectorPayload() {
        Assert.assertEquals("Colombo",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//address/city/text()', 'personVar.payload')", 0, 4));
        Assert.assertEquals("Sri Lanka",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//address/country/text()', 'personVar.payload')", 0, 4));
    }

    // -------------------------------------------------------------------------
    // Attribute access
    // -------------------------------------------------------------------------

    @Test
    public void testRootAttributeFromConnectorPayload() {
        Assert.assertEquals("ORD-001",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('/order/@id', 'orderVar.payload')", 0, 4));
    }

    @Test
    public void testChildAttributeFromConnectorPayload() {
        Assert.assertEquals("3",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//item/@quantity', 'orderVar.payload')", 0, 4));
    }

    @Test
    public void testDecimalTextFromConnectorPayload() {
        Assert.assertEquals("9.99",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//price/text()', 'orderVar.payload')", 0, 4));
    }

    // -------------------------------------------------------------------------
    // Composition with other functions
    // -------------------------------------------------------------------------

    @Test
    public void testXPathResultUsedInStringFunction() {
        Assert.assertEquals("JOHN",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "toUpper(xpath('//name/text()', 'personVar.payload'))", 0, 4));
    }

    @Test
    public void testXPathResultUsedInConcatenation() {
        Assert.assertEquals("Order: ORD-001",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "\"Order: \" + xpath('/order/@id', 'orderVar.payload')", 0, 4));
    }

    @Test
    public void testXPathResultUsedInConditionalExpression() {
        Assert.assertEquals("found",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//name/text()', 'personVar.payload') == \"John\" ? \"found\" : \"not found\"",
                        0, 4));
    }

    // -------------------------------------------------------------------------
    // Namespace-aware XPath
    // -------------------------------------------------------------------------

    @Test
    public void testNamespacedElementAccess() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ns", "http://example.com");
        Assert.assertEquals("Jane",
                TestUtils.evaluateExpressionWithNamespaces(
                        "xpath('//ns:name/text()', 'nsVar.payload')", 4, namespaces));
    }

    @Test
    public void testNamespacedNestedElementAccess() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ns", "http://example.com");
        Assert.assertEquals("Engineering",
                TestUtils.evaluateExpressionWithNamespaces(
                        "xpath('//ns:department/text()', 'nsVar.payload')", 4, namespaces));
    }

    // -------------------------------------------------------------------------
    // No-match XPath (should return empty string, not an error)
    // -------------------------------------------------------------------------

    @Test
    public void testXPathNoMatchReturnsEmpty() {
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//nonexistent/text()', 'personVar.payload')", 0, 4));
    }

    @Test
    public void testXPathNoMatchAttributeReturnsEmpty() {
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('/person/@missingAttr', 'personVar.payload')", 0, 4));
    }

    // -------------------------------------------------------------------------
    // Error paths — all should return "" (EvaluationException caught upstream)
    // -------------------------------------------------------------------------

    @Test
    public void testUndefinedVariableReturnsEmpty() {
        // "ghost" variable does not exist in variableMap4
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//name/text()', 'ghost.payload')", 0, 4));
    }

    @Test
    public void testVariableNotAMapReturnsEmpty() {
        // plainXmlVar is an OMElement, not a Map
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//value/text()', 'plainXmlVar.payload')", 0, 4));
    }

    @Test
    public void testFieldNotAnOMElementReturnsEmpty() {
        // personVar.headers is a plain HashMap, not an OMElement
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//name/text()', 'personVar.headers')", 0, 4));
    }

    @Test
    public void testEmptyVariableNameReturnsEmpty() {
        Assert.assertEquals("",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//name/text()', '')", 0, 4));
    }

    // -------------------------------------------------------------------------
    // Regression — existing two-argument xpath() with a plain XML variable
    // -------------------------------------------------------------------------

    @Test
    public void testTwoArgXPathOnPlainXmlVariable() {
        // plainXmlVar is a top-level OMElement (not a connector response Map);
        // the two-arg xpath() with no dot in the variable name must still work via $var: path
        Assert.assertEquals("42",
                TestUtils.evaluateExpressionWithPayloadAndVariables(
                        "xpath('//value/text()', 'plainXmlVar')", 0, 4));
    }
}

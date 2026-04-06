/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml.endpoints;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.AbstractTestCase;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.ResolvingEndpoint;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.Properties;

/**
 * Tests for ResolvingEndpointFactory and ResolvingEndpointSerializer.
 *
 * Covers the bug fix for GitHub issue #4574: key-expression with the new
 * ${...} Synapse Expression syntax was rejected with an XPathSyntaxException
 * because the factory was unconditionally using SynapseXPathFactory instead
 * of the polymorphic SynapsePathFactory.
 */
public class ResolvingEndpointSerializationTest extends AbstractTestCase {

    private static final String SYNAPSE_NS = "xmlns=\"http://ws.apache.org/ns/synapse\"";

    // -------------------------------------------------------------------------
    // Factory parse tests
    // -------------------------------------------------------------------------

    /**
     * Issue #4574 — the primary bug scenario.
     * A key-expression using ${...} syntax must be parsed without error and
     * must produce a SynapseExpression (not a SynapseXPath).
     */
    public void testParseNewExpressionSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS + " key-expression=\"${params.functionParams.Endpoint}\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());

        assertNotNull("Endpoint must not be null", endpoint);
        assertTrue("Must be a ResolvingEndpoint", endpoint instanceof ResolvingEndpoint);

        SynapsePath expr = ((ResolvingEndpoint) endpoint).getKeyExpression();
        assertNotNull("keyExpression must not be null", expr);
        assertTrue("keyExpression must be a SynapseExpression for ${...} syntax",
                expr instanceof SynapseExpression);
        assertEquals("Inner expression must equal the payload path",
                "params.functionParams.Endpoint", expr.getExpression());
    }

    /**
     * Legacy XPath syntax ($url:myKey style) must still be accepted without regression.
     */
    public void testParseLegacyXPathSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS + " key-expression=\"get-property('myEndpointKey')\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());

        assertNotNull(endpoint);
        assertTrue(endpoint instanceof ResolvingEndpoint);

        SynapsePath expr = ((ResolvingEndpoint) endpoint).getKeyExpression();
        assertNotNull(expr);
        assertTrue("Legacy XPath expression must produce a SynapseXPath", expr instanceof SynapseXPath);
    }

    /**
     * A named (non-anonymous) ResolvingEndpoint with new expression syntax.
     */
    public void testParseNamedEndpointWithNewExpressionSyntax() throws Exception {
        String inputXML = "<endpoint name=\"myResolvingEP\" " + SYNAPSE_NS
                + " key-expression=\"${vars.targetEndpoint}\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, false, new Properties());

        assertNotNull(endpoint);
        assertTrue(endpoint instanceof ResolvingEndpoint);
        assertEquals("myResolvingEP", endpoint.getName());

        SynapsePath expr = ((ResolvingEndpoint) endpoint).getKeyExpression();
        assertTrue(expr instanceof SynapseExpression);
        assertEquals("vars.targetEndpoint", expr.getExpression());
    }

    // -------------------------------------------------------------------------
    // Serializer round-trip tests
    // -------------------------------------------------------------------------

    /**
     * Round-trip: parse ${...} expression, serialize, and verify the attribute
     * value is preserved as ${...}.
     */
    public void testRoundTripNewExpressionSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS
                + " key-expression=\"${params.functionParams.Endpoint}\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
        OMElement serialized = EndpointSerializer.getElementFromEndpoint(endpoint);

        assertNotNull(serialized);
        String keyExprAttr = serialized.getAttributeValue(
                new javax.xml.namespace.QName("key-expression"));
        assertNotNull("key-expression attribute must be present after serialization", keyExprAttr);
        assertEquals("${params.functionParams.Endpoint}", keyExprAttr);
    }

    /**
     * Round-trip: parse legacy XPath expression, serialize, and verify value is preserved.
     */
    public void testRoundTripLegacyXPathSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS
                + " key-expression=\"get-property('myEndpointKey')\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
        OMElement serialized = EndpointSerializer.getElementFromEndpoint(endpoint);

        assertNotNull(serialized);
        String keyExprAttr = serialized.getAttributeValue(
                new javax.xml.namespace.QName("key-expression"));
        assertNotNull(keyExprAttr);
        assertEquals("get-property('myEndpointKey')", keyExprAttr);
    }

    /**
     * Round-trip: parse a multi-level ${...} expression (e.g. nested property access).
     */
    public void testRoundTripNestedNewExpressionSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS
                + " key-expression=\"${vars.ep}\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
        OMElement serialized = EndpointSerializer.getElementFromEndpoint(endpoint);

        String keyExprAttr = serialized.getAttributeValue(
                new javax.xml.namespace.QName("key-expression"));
        assertEquals("${vars.ep}", keyExprAttr);
    }

    // -------------------------------------------------------------------------
    // Negative / edge-case tests
    // -------------------------------------------------------------------------

    /**
     * An invalid XPath expression (not ${...}) must still throw SynapseException
     * so that deployment errors surface clearly.
     */
    public void testInvalidXPathThrowsSynapseException() {
        String inputXML = "<endpoint " + SYNAPSE_NS + " key-expression=\"$$invalid!!\"/>";
        OMElement inputElement = createOMElement(inputXML);
        try {
            EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
            fail("Expected SynapseException for invalid expression");
        } catch (SynapseException e) {
            // expected
        }
    }

    /**
     * Path type of the parsed SynapseExpression must be SYNAPSE_EXPRESSIONS_PATH.
     */
    public void testPathTypeForNewExpressionSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS + " key-expression=\"${vars.myKey}\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
        SynapsePath expr = ((ResolvingEndpoint) endpoint).getKeyExpression();

        assertEquals(SynapsePath.SYNAPSE_EXPRESSIONS_PATH, expr.getPathType());
        assertEquals("vars.myKey", expr.getExpression());
    }

    /**
     * Path type of the parsed legacy XPath must be X_PATH.
     */
    public void testPathTypeForLegacyXPathSyntax() throws Exception {
        String inputXML = "<endpoint " + SYNAPSE_NS + " key-expression=\"get-property('ep')\"/>";
        OMElement inputElement = createOMElement(inputXML);

        Endpoint endpoint = EndpointFactory.getEndpointFromElement(inputElement, true, new Properties());
        SynapsePath expr = ((ResolvingEndpoint) endpoint).getKeyExpression();

        assertEquals(SynapsePath.X_PATH, expr.getPathType());
    }
}

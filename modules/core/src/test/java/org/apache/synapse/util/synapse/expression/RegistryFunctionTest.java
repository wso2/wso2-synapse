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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.registry.SimpleInMemoryRegistry;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for the registry() predefined function in Synapse Expressions.
 * Covers issue #4436: text/plain registry resources must not be base64-decoded.
 */
public class RegistryFunctionTest {

    private static final String KEY_PLAIN_TEXT = "resources:txt/hello.txt";
    private static final String KEY_PLAIN_TEXT_WITH_SPACES = "resources:txt/hello-spaces.txt";
    private static final String KEY_PLAIN_TEXT_WITH_NEWLINES = "resources:txt/multiline.txt";
    private static final String KEY_BINARY = "resources:json/config.json";
    private static final String KEY_MISSING = "resources:txt/nonexistent.txt";

    private Axis2MessageContext synCtx;
    private SimpleInMemoryRegistry registry;

    @Before
    public void setUp() throws Exception {
        Map<String, OMNode> data = new HashMap<>();

        // Plain text resource (text/plain) — created via createOMText(String), isBinary() == false
        data.put(KEY_PLAIN_TEXT, OMAbstractFactory.getOMFactory().createOMText("Hello from registry"));

        // Plain text with spaces — the exact scenario that triggers the bug (space == 0x20)
        data.put(KEY_PLAIN_TEXT_WITH_SPACES,
                OMAbstractFactory.getOMFactory().createOMText("Hello from registry text resource"));

        // Plain text with newlines
        data.put(KEY_PLAIN_TEXT_WITH_NEWLINES,
                OMAbstractFactory.getOMFactory().createOMText("line1\nline2\nline3"));

        // Binary (non-text/plain) resource — created via createOMText(DataHandler, true), isBinary() == true
        // Simulates application/json stored as a base64-encoded binary OMText node
        String jsonContent = "{\"key\":\"value\"}";
        byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(jsonBytes, "application/json"));
        data.put(KEY_BINARY, OMAbstractFactory.getOMFactory().createOMText(dataHandler, true));

        registry = new SimpleInMemoryRegistry(data, 8000L);

        synCtx = org.apache.synapse.mediators.TestUtils.getAxis2MessageContext("<test/>", null);
        synCtx.getConfiguration().setRegistry(registry);
    }

    @After
    public void tearDown() {
        // Reset the shared registry to avoid state leaking into other tests
        synCtx.getConfiguration().setRegistry(null);
    }

    // -------------------------------------------------------------------------
    // Bug scenario: text/plain content must be returned as-is (not base64-decoded)
    // -------------------------------------------------------------------------

    @Test
    public void testPlainTextRegistryResource() throws Exception {
        // "Hello from registry" — no spaces, would actually be valid base64 by coincidence,
        // but the content check confirms the correct path was taken.
        SynapseExpression expr = new SynapseExpression("registry(\"" + KEY_PLAIN_TEXT + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("Hello from registry", result);
    }

    @Test
    public void testPlainTextWithSpacesRegistryResource() throws Exception {
        // "Hello from registry text resource" contains spaces (0x20), which is an invalid
        // base64 character and causes IllegalArgumentException before the fix.
        SynapseExpression expr = new SynapseExpression("registry(\"" + KEY_PLAIN_TEXT_WITH_SPACES + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("Hello from registry text resource", result);
    }

    @Test
    public void testPlainTextWithNewlinesRegistryResource() throws Exception {
        SynapseExpression expr = new SynapseExpression("registry(\"" + KEY_PLAIN_TEXT_WITH_NEWLINES + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("line1\nline2\nline3", result);
    }

    // -------------------------------------------------------------------------
    // Binary (non-text/plain) path must continue to work correctly
    // -------------------------------------------------------------------------

    @Test
    public void testBinaryRegistryResourceDecodesCorrectly() throws Exception {
        // Binary OMText is stored with isBinary() == true; its getText() returns a base64 string.
        // The fix must still decode it correctly.
        SynapseExpression expr = new SynapseExpression("registry(\"" + KEY_BINARY + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("{\"key\":\"value\"}", result);
    }

    // -------------------------------------------------------------------------
    // Missing registry key must return empty string (existing behaviour)
    // -------------------------------------------------------------------------

    @Test
    public void testMissingRegistryResourceReturnsEmpty() throws Exception {
        SynapseExpression expr = new SynapseExpression("registry(\"" + KEY_MISSING + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("", result);
    }

    // -------------------------------------------------------------------------
    // Expression composition: registry value used in string concatenation
    // -------------------------------------------------------------------------

    @Test
    public void testPlainTextRegistryResourceInExpression() throws Exception {
        SynapseExpression expr = new SynapseExpression(
                "\"file:\" + registry(\"" + KEY_PLAIN_TEXT_WITH_SPACES + "\")");
        String result = expr.stringValueOf(synCtx);
        Assert.assertEquals("file:Hello from registry text resource", result);
    }

    // -------------------------------------------------------------------------
    // Helper: minimal in-memory DataSource backed by a byte array
    // -------------------------------------------------------------------------

    private static class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final String contentType;

        ByteArrayDataSource(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream();
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "ByteArrayDataSource";
        }
    }
}

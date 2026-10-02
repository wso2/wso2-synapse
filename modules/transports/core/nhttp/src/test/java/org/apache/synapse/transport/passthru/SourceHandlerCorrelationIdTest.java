/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfigPNames;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.regex.Pattern;

/**
 * Tests correlation ID resolution in {@link SourceHandler#setCorrelationId(NHttpServerConnection)}.
 */
public class SourceHandlerCorrelationIdTest {

    private static final String HEADER = PassThroughConstants.CORRELATION_DEFAULT_HEADER;
    private static final String REGEX = "^[a-zA-Z0-9\\-_.:]{1,60}$";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final HttpContext httpContext = new BasicHttpContext();
    private HttpRequest request;
    private final NHttpServerConnection conn = Mockito.mock(NHttpServerConnection.class);

    @After
    public void tearDown() {
        System.clearProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_REGEX);
    }

    private SourceHandler createHandler(String regex) {
        if (regex != null) {
            System.setProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_REGEX, regex);
        }
        Mockito.when(conn.getContext()).thenReturn(httpContext);
        return new SourceHandler(Mockito.mock(SourceConfiguration.class));
    }

    /**
     * Simulates a new request arriving on the same (keep-alive) connection.
     */
    private void newRequest(String activityId) {
        request = new BasicHttpRequest("GET", "/test");
        if (activityId != null) {
            request.addHeader(HEADER, activityId);
        }
        Mockito.when(conn.getHttpRequest()).thenReturn(request);
    }

    private Object correlationId() {
        return httpContext.getAttribute(CorrelationConstants.CORRELATION_ID);
    }

    private Object systemGenerated() {
        return httpContext.getAttribute(CorrelationConstants.SYSTEM_GENERATED_CORRELATION_ID);
    }

    @Test
    public void testSystemGeneratedFlagResetOnKeepAliveConnection() {
        SourceHandler handler = createHandler(null);

        newRequest(null);
        handler.setCorrelationId(conn);
        Assert.assertEquals(Boolean.TRUE, systemGenerated());

        newRequest("foo");
        handler.setCorrelationId(conn);
        Assert.assertEquals("foo", correlationId());
        Assert.assertEquals(Boolean.FALSE, systemGenerated());
    }

    @Test
    public void testAnyValueAcceptedWhenValidationNotConfigured() {
        SourceHandler handler = createHandler(null);
        newRequest("INJECT<?>");
        handler.setCorrelationId(conn);
        Assert.assertEquals("INJECT<?>", correlationId());
        Assert.assertEquals("INJECT<?>", request.getFirstHeader(HEADER).getValue());
    }

    @Test
    public void testValidValueRetained() {
        SourceHandler handler = createHandler(REGEX);
        newRequest("abc-123");
        handler.setCorrelationId(conn);
        Assert.assertEquals("abc-123", correlationId());
        Assert.assertEquals(Boolean.FALSE, systemGenerated());
        Assert.assertEquals("abc-123", request.getFirstHeader(HEADER).getValue());
    }

    @Test
    public void testInvalidValueReplaced() {
        SourceHandler handler = createHandler(REGEX);
        assertReplaced(handler, "INJECT<?>");
        assertReplaced(handler, "");
        assertReplaced(handler, new String(new char[61]).replace('\0', 'a'));
    }

    @Test
    public void testInvalidRegexDisablesValidation() {
        SourceHandler handler = createHandler("[");
        newRequest("INJECT<?>");
        handler.setCorrelationId(conn);
        Assert.assertEquals("INJECT<?>", correlationId());
    }

    @Test
    public void testAbsentHeaderGeneratesId() {
        SourceHandler handler = createHandler(REGEX);
        newRequest(null);
        handler.setCorrelationId(conn);
        Assert.assertTrue(UUID_PATTERN.matcher((String) correlationId()).matches());
        Assert.assertEquals(Boolean.TRUE, systemGenerated());
        Assert.assertEquals(correlationId(), request.getFirstHeader(HEADER).getValue());
    }

    private void assertReplaced(SourceHandler handler, String activityId) {
        newRequest(activityId);
        handler.setCorrelationId(conn);
        String id = (String) correlationId();
        Assert.assertTrue("Expected generated UUID for '" + activityId + "' but got " + id,
                UUID_PATTERN.matcher(id).matches());
        Assert.assertEquals(Boolean.TRUE, systemGenerated());
        Assert.assertEquals(1, request.getHeaders(HEADER).length);
        Assert.assertEquals(id, request.getFirstHeader(HEADER).getValue());
    }
}

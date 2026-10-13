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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.transport.passthru.config.PassThroughConfigPNames;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.PassThroughTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * Tests correlation ID resolution in {@link SourceHandler#resolveCorrelationId(NHttpServerConnection)}.
 */
public class SourceHandlerCorrelationIdTest {

    private static final String HEADER = PassThroughConstants.CORRELATION_DEFAULT_HEADER;
    private static final String REGEX = "^[a-zA-Z0-9\\-_.:]{1,60}$";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final HttpContext httpContext = new BasicHttpContext();
    private HttpRequest request;
    private final NHttpServerConnection conn = Mockito.mock(NHttpServerConnection.class);
    private final SourceConfiguration sourceConfiguration = Mockito.mock(SourceConfiguration.class);

    /**
     * Creating a {@link SourceHandler} initializes the {@link PassThroughConfiguration} singleton, which reads
     * passthru-http.properties only once, at class load. Since this happens before other tests set the test
     * configuration location, the singleton is rebuilt here so that it does not leak into other tests.
     */
    @AfterClass
    public static void resetPassThroughConfiguration() throws Exception {
        PassThroughTestUtils.getPassThroughConfiguration(); // sets the test configuration location
        Constructor<PassThroughConfiguration> constructor = PassThroughConfiguration.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Field instance = PassThroughConfiguration.class.getDeclaredField("_instance");
        instance.setAccessible(true);
        instance.set(null, constructor.newInstance());
    }

    @After
    public void tearDown() {
        System.clearProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_REGEX);
        System.clearProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_ACTION);
    }

    private SourceHandler createHandler(String regex) {
        return createHandler(regex, null);
    }

    private SourceHandler createHandler(String regex, String action) {
        if (regex != null) {
            System.setProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_REGEX, regex);
        }
        if (action != null) {
            System.setProperty(PassThroughConfigPNames.ACTIVITY_ID_HEADER_VALIDATION_ACTION, action);
        }
        Mockito.when(conn.getContext()).thenReturn(httpContext);
        return new SourceHandler(sourceConfiguration);
    }

    /**
     * Simulates a new request arriving on the same (keep-alive) connection.
     */
    private void newRequest(String... activityIds) {
        request = new BasicHttpRequest("GET", "/test");
        if (activityIds != null) {
            for (String activityId : activityIds) {
                request.addHeader(HEADER, activityId);
            }
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

        newRequest((String[]) null);
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
        newRequest((String[]) null);
        handler.setCorrelationId(conn);
        Assert.assertTrue(UUID_PATTERN.matcher((String) correlationId()).matches());
        Assert.assertEquals(Boolean.TRUE, systemGenerated());
        Assert.assertEquals(correlationId(), request.getFirstHeader(HEADER).getValue());
    }

    @Test
    public void testDuplicateHeadersRemovedWhenFirstValueValid() {
        SourceHandler handler = createHandler(REGEX);
        newRequest("abc-123", "INJECT<?>");
        handler.setCorrelationId(conn);
        Assert.assertEquals("abc-123", correlationId());
        Assert.assertEquals(Boolean.FALSE, systemGenerated());
        Assert.assertEquals(1, request.getHeaders(HEADER).length);
        Assert.assertEquals("abc-123", request.getFirstHeader(HEADER).getValue());
    }

    @Test
    public void testDuplicateHeadersRemovedWhenFirstValueInvalid() {
        SourceHandler handler = createHandler(REGEX);
        assertReplaced(handler, "INJECT<1>", "INJECT<2>");
        assertReplaced(handler, "INJECT<?>", "abc-123");
    }

    @Test
    public void testInvalidValueRegeneratedWhenActionExplicitlyRegenerate() {
        SourceHandler handler = createHandler(REGEX, "regenerate");
        newRequest("INJECT<?>");
        Assert.assertTrue(handler.resolveCorrelationId(conn));
        assertGenerated();
    }

    @Test
    public void testUnknownActionDefaultsToRegenerate() {
        SourceHandler handler = createHandler(REGEX, "foo");
        newRequest("INJECT<?>");
        Assert.assertTrue(handler.resolveCorrelationId(conn));
        assertGenerated();
    }

    @Test
    public void testRejectActionIgnoredWhenValidationNotConfigured() {
        SourceHandler handler = createHandler(null, "reject");
        newRequest("INJECT<?>");
        Assert.assertTrue(handler.resolveCorrelationId(conn));
        Assert.assertEquals("INJECT<?>", correlationId());
        Assert.assertEquals(Boolean.FALSE, systemGenerated());
    }

    @Test
    public void testRejectActionRejectsInvalidValue() {
        SourceHandler handler = createHandler(REGEX, "REJECT");
        newRequest("INJECT<?>");
        Assert.assertFalse(handler.resolveCorrelationId(conn));
        // A correlation ID is still generated so that the rejection can be correlated in logs.
        assertGenerated();
    }

    @Test
    public void testRejectActionAcceptsValidOrAbsentValue() {
        SourceHandler handler = createHandler(REGEX, "reject");
        newRequest("abc-123", "INJECT<?>");
        Assert.assertTrue(handler.resolveCorrelationId(conn));
        Assert.assertEquals("abc-123", correlationId());
        Assert.assertEquals(Boolean.FALSE, systemGenerated());
        Assert.assertEquals(1, request.getHeaders(HEADER).length);

        newRequest((String[]) null);
        Assert.assertTrue(handler.resolveCorrelationId(conn));
        assertGenerated();
    }

    @Test
    public void testRequestWithInvalidValueRejected() throws Exception {
        PassThroughTransportMetricsCollector metrics = Mockito.mock(PassThroughTransportMetricsCollector.class);
        SourceHandler handler = createRejectingHandler(metrics);
        newRequest("INJECT<?>");

        handler.requestReceived(conn);

        assertRejected(metrics);
        Mockito.verify(sourceConfiguration, Mockito.never()).getWorkerPool();
    }

    /**
     * Subclasses (e.g. inbound endpoint handlers) call {@link SourceHandler#setCorrelationId(NHttpServerConnection)}
     * followed by {@link SourceHandler#getSourceRequest(NHttpServerConnection)}, and stop processing the request if
     * the latter returns null.
     */
    @Test
    public void testRequestWithInvalidValueRejectedWhenSettingCorrelationId() throws Exception {
        PassThroughTransportMetricsCollector metrics = Mockito.mock(PassThroughTransportMetricsCollector.class);
        SourceHandler handler = createRejectingHandler(metrics);
        newRequest("INJECT<?>");

        handler.setCorrelationId(conn);

        assertRejected(metrics);
        Assert.assertNull(handler.getSourceRequest(conn));
        // The connection is closed gracefully, so that the response is sent, rather than being shut down.
        Mockito.verify(sourceConfiguration, Mockito.never()).getSourceConnections();
        Assert.assertEquals(ProtocolState.CLOSED, SourceContext.getState(conn));
    }

    @Test
    public void testApplyCorrelationIdReturnsFalseWhenRejected() throws Exception {
        PassThroughTransportMetricsCollector metrics = Mockito.mock(PassThroughTransportMetricsCollector.class);
        SourceHandler handler = createRejectingHandler(metrics);
        newRequest("abc-123");
        Assert.assertTrue(handler.applyCorrelationId(conn));
        Mockito.verify(conn, Mockito.never()).submitResponse(Mockito.any(HttpResponse.class));

        newRequest("INJECT<?>");
        Assert.assertFalse(handler.applyCorrelationId(conn));
        assertRejected(metrics);
    }

    private SourceHandler createRejectingHandler(PassThroughTransportMetricsCollector metrics) {
        Mockito.when(sourceConfiguration.getMetrics()).thenReturn(metrics);
        Mockito.when(sourceConfiguration.getHttpParams()).thenReturn(new BasicHttpParams());
        Mockito.when(sourceConfiguration.getHttpProcessor()).thenReturn(Mockito.mock(HttpProcessor.class));
        SourceHandler handler = createHandler(REGEX, "reject");
        SourceContext.create(conn, ProtocolState.REQUEST_READY, sourceConfiguration);
        return handler;
    }

    private void assertRejected(PassThroughTransportMetricsCollector metrics) throws Exception {
        ArgumentCaptor<HttpResponse> response = ArgumentCaptor.forClass(HttpResponse.class);
        // Input must be suspended before the response is submitted, so that the request body is not passed to
        // inputReady, which would shut down the connection before the response is sent.
        InOrder inOrder = Mockito.inOrder(conn);
        inOrder.verify(conn).suspendInput();
        inOrder.verify(conn).submitResponse(response.capture());
        inOrder.verify(conn).close();
        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getValue().getStatusLine().getStatusCode());
        Assert.assertEquals("Bad Request", response.getValue().getStatusLine().getReasonPhrase());
        Assert.assertEquals(HTTP.CONN_CLOSE, response.getValue().getFirstHeader(HTTP.CONN_DIRECTIVE).getValue());
        Assert.assertEquals(ProtocolState.CLOSED, SourceContext.getState(conn));
        Mockito.verify(metrics).incrementFaultsReceiving();
    }

    private void assertGenerated() {
        Assert.assertTrue(UUID_PATTERN.matcher((String) correlationId()).matches());
        Assert.assertEquals(Boolean.TRUE, systemGenerated());
        Assert.assertEquals(1, request.getHeaders(HEADER).length);
        Assert.assertEquals(correlationId(), request.getFirstHeader(HEADER).getValue());
    }

    private void assertReplaced(SourceHandler handler, String... activityIds) {
        String activityId = activityIds[0];
        newRequest(activityIds);
        handler.setCorrelationId(conn);
        String id = (String) correlationId();
        Assert.assertTrue("Expected generated UUID for '" + activityId + "' but got " + id,
                UUID_PATTERN.matcher(id).matches());
        Assert.assertEquals(Boolean.TRUE, systemGenerated());
        Assert.assertEquals(1, request.getHeaders(HEADER).length);
        Assert.assertEquals(id, request.getFirstHeader(HEADER).getValue());
    }
}

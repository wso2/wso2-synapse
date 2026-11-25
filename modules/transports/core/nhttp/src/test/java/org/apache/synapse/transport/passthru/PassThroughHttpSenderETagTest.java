/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.passthru;

import junit.framework.TestCase;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.util.BufferFactory;
import org.apache.synapse.transport.passthru.util.ControlledByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.wso2.caching.digest.DigestGenerator;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests HTTP ETag revalidation (If-None-Match -> 304 Not Modified) in
 * {@link PassThroughHttpSender#submitResponse(MessageContext)}.
 *
 * The 304 decision must be taken before the response body is serialized into the pipe. If it is taken later
 * (after the worker thread has started writing a body larger than the pipe buffer), the 304 response has no
 * entity, the pipe is never consumed and the worker thread blocks until the socket timeout.
 */
public class PassThroughHttpSenderETagTest extends TestCase {

    private static final String ETAG = "\"sample-etag-1\"";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private PassThroughHttpSender sender;
    private NHttpServerConnection conn;
    private SourceConfiguration sourceConfiguration;
    private SourceRequest sourceRequest;
    private Map<String, String> requestHeaders;
    private MockedStatic<SourceContext> sourceContextMock;

    @Before
    public void setUp() {
        sender = new PassThroughHttpSender();

        IOSession session = Mockito.mock(IOSession.class);
        conn = new DefaultNHttpServerConnection(session, 12);

        sourceConfiguration = Mockito.mock(SourceConfiguration.class);
        BufferFactory bufferFactory = Mockito.mock(BufferFactory.class);
        Mockito.when(bufferFactory.getBuffer()).thenReturn(new ControlledByteBuffer(ByteBuffer.allocate(1024)));
        Mockito.when(sourceConfiguration.getBufferFactory()).thenReturn(bufferFactory);

        requestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sourceRequest = Mockito.mock(SourceRequest.class);
        Mockito.when(sourceRequest.getHeaders()).thenReturn(requestHeaders);
        Mockito.when(sourceRequest.getRequest()).thenReturn(new BasicHttpRequest("GET", "/test/resource"));

        sourceContextMock = Mockito.mockStatic(SourceContext.class);
        sourceContextMock.when(() -> SourceContext.getRequest(conn)).thenReturn(sourceRequest);
        sourceContextMock.when(() -> SourceContext.getState(conn)).thenReturn(ProtocolState.REQUEST_DONE);
        sourceContextMock.when(() -> SourceContext.get(any())).thenReturn(Mockito.mock(SourceContext.class));
    }

    @After
    public void tearDown() {
        if (sourceContextMock != null) {
            sourceContextMock.close();
        }
    }

    /**
     * Bug scenario: HTTP_ETAG is enabled, the ETag is generated from the response and the client sends a
     * matching If-None-Match. The response must be turned into a body-less 304 before any body is written.
     */
    @Test
    public void testMatchingIfNoneMatchWithGeneratedETagSends304WithoutBody() throws Exception {
        DigestGenerator digestGenerator = Mockito.mock(DigestGenerator.class);
        Mockito.when(digestGenerator.getDigest(any(MessageContext.class))).thenReturn("sample-etag-1");
        setField(sender, "digestGenerator", digestGenerator);

        MessageContext msgContext = createResponseMessageContext(null);
        msgContext.setProperty(PassThroughConstants.HTTP_ETAG_ENABLED, true);
        requestHeaders.put(HttpHeaders.IF_NONE_MATCH, ETAG);

        sender.submitResponse(msgContext);

        SourceResponse sourceResponse = captureSourceResponse();
        assertNotModifiedWithoutBody(msgContext, sourceResponse);
    }

    /**
     * Negative case: If-None-Match does not match the ETag, the full 200 response must be sent.
     */
    @Test
    public void testNonMatchingIfNoneMatchKeepsOriginalResponse() throws Exception {
        MessageContext msgContext = createResponseMessageContext(ETAG);
        msgContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        requestHeaders.put(HttpHeaders.IF_NONE_MATCH, "\"does-not-match\"");

        sender.submitResponse(msgContext);

        assertOriginalResponse(msgContext, captureSourceResponse(), HttpStatus.SC_OK);
    }

    /**
     * A backend 202 response sets SC_ACCEPTED, which takes precedence over HTTP_SC when the status is determined.
     * A matching If-None-Match must still produce a 304 response.
     */
    @Test
    public void testBackendAcceptedResponseWithMatchingIfNoneMatchSends304() throws Exception {
        MessageContext msgContext = createResponseMessageContext(ETAG);
        msgContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        msgContext.setProperty(PassThroughConstants.SC_ACCEPTED, Boolean.TRUE);
        requestHeaders.put(HttpHeaders.IF_NONE_MATCH, ETAG);

        sender.submitResponse(msgContext);

        assertNotModifiedWithoutBody(msgContext, captureSourceResponse());
    }

    /**
     * A backend 202 response with a non matching If-None-Match must be sent as 202.
     */
    @Test
    public void testBackendAcceptedResponseWithNonMatchingIfNoneMatchKeeps202() throws Exception {
        MessageContext msgContext = createResponseMessageContext(ETAG);
        msgContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        msgContext.setProperty(PassThroughConstants.SC_ACCEPTED, Boolean.TRUE);
        requestHeaders.put(HttpHeaders.IF_NONE_MATCH, "\"does-not-match\"");

        sender.submitResponse(msgContext);

        assertOriginalResponse(msgContext, captureSourceResponse(), HttpStatus.SC_ACCEPTED);
    }

    private MessageContext createResponseMessageContext(String eTag) throws Exception {
        MessageContext msgContext = new MessageContext();
        msgContext.setEnvelope(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope());
        msgContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION, conn);
        msgContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION, sourceConfiguration);

        Map<String, String> transportHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        transportHeaders.put(HTTP.CONTENT_TYPE, JSON_CONTENT_TYPE);
        transportHeaders.put(HTTP.CONTENT_LEN, "20000");
        transportHeaders.put("X-Custom-Header", "custom-value");
        if (eTag != null) {
            transportHeaders.put(HttpHeaders.ETAG, eTag);
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, transportHeaders);
        return msgContext;
    }

    private SourceResponse captureSourceResponse() {
        ArgumentCaptor<SourceResponse> captor = ArgumentCaptor.forClass(SourceResponse.class);
        sourceContextMock.verify(() -> SourceContext.setResponse(eq(conn), captor.capture()));
        return captor.getValue();
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void assertNotModifiedWithoutBody(MessageContext msgContext, SourceResponse sourceResponse)
            throws Exception {
        assertEquals("Response status", HttpStatus.SC_NOT_MODIFIED, getField(sourceResponse, "status"));
        assertEquals("Response reason phrase", "Not Modified", getField(sourceResponse, "statusLine"));
        assertEquals("ETag header must be retained", ETAG, sourceResponse.getHeader(HttpHeaders.ETAG));
        assertEquals("Non entity headers must be retained", "custom-value",
                sourceResponse.getHeader("X-Custom-Header"));
        assertNull("Content-Type must not be sent with 304", sourceResponse.getHeader(HTTP.CONTENT_TYPE));
        assertNull("Content-Length must not be sent with 304", sourceResponse.getHeader(HTTP.CONTENT_LEN));
        assertNull("Transfer-Encoding must not be sent with 304",
                sourceResponse.getHeader(HTTP.TRANSFER_ENCODING));
        assertTrue("Response must be marked as having no entity body",
                msgContext.isPropertyTrue(PassThroughConstants.NO_ENTITY_BODY));
        assertNull("No pipe must be created to serialize a body for a 304 response",
                msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE));
    }

    private void assertOriginalResponse(MessageContext msgContext, SourceResponse sourceResponse,
                                        int expectedStatus) throws Exception {
        assertEquals("Response status", expectedStatus, getField(sourceResponse, "status"));
        assertNotNull("Content-Type must be sent with the response body",
                sourceResponse.getHeader(HTTP.CONTENT_TYPE));
        assertFalse("Response must not be marked as having no entity body",
                msgContext.isPropertyTrue(PassThroughConstants.NO_ENTITY_BODY));
        assertNotNull("Response body must be serialized to a pipe as usual",
                msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE));
    }
}

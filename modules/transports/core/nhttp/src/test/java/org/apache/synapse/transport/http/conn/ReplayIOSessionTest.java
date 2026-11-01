/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.http.conn;

import org.apache.axis2.context.MessageContext;
import org.apache.http.nio.reactor.IOSession;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.TargetContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the capture logic in {@link ReplayIOSession}: which outgoing writes are recorded and
 * exactly which bytes end up in the replay record.
 */
public class ReplayIOSessionTest {

    private static final String REPLAY_ENABLED_PROPERTY = "ENABLE_REPLAY_TRANSACTION";
    private static final long WAIT_MILLIS = 5000;
    // Long enough for the worker threads (50ms poll interval) to pick up anything that was queued.
    private static final long QUIET_PERIOD_MILLIS = 300;

    private ReplayTestUtils.CapturingReplayWriter writer;
    private IOSession session;
    private ByteChannel channel;
    private MessageContext messageContext;

    @Before
    public void setUp() throws Exception {
        writer = new ReplayTestUtils.CapturingReplayWriter();
        ReplayTestUtils.installDispatcher(writer);

        messageContext = new MessageContext();
        messageContext.setMessageID("urn:uuid:replay-test");
        TargetContext targetContext = Mockito.mock(TargetContext.class);
        Mockito.when(targetContext.getRequestMsgCtx()).thenReturn(messageContext);

        channel = Mockito.mock(ByteChannel.class);
        session = Mockito.mock(IOSession.class);
        Mockito.when(session.channel()).thenReturn(channel);
        Mockito.when(session.getAttribute(TargetContext.CONNECTION_INFORMATION)).thenReturn(targetContext);
    }

    @After
    public void tearDown() throws Exception {
        ReplayTestUtils.resetDispatcher();
    }

    /**
     * Makes the mocked socket channel accept at most {@code maxBytesPerWrite} bytes per call, the way
     * a non-blocking channel does when the socket buffer is full.
     */
    private void channelAccepts(int maxBytesPerWrite) throws Exception {
        Mockito.when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer src = invocation.getArgument(0);
            int written = Math.min(maxBytesPerWrite, src.remaining());
            src.position(src.position() + written);
            return written;
        });
    }

    private void enableReplay(String httpMethod) {
        messageContext.setProperty("HTTP_METHOD", httpMethod);
        messageContext.setProperty(REPLAY_ENABLED_PROPERTY, Boolean.TRUE);
    }

    private int write(ReplayIOSession replaySession, ByteBuffer src) throws Exception {
        return replaySession.channel().write(src);
    }

    private void assertNothingCaptured() throws Exception {
        TimeUnit.MILLISECONDS.sleep(QUIET_PERIOD_MILLIS);
        Assert.assertTrue("No replay record expected but found " + writer.records.size(),
                writer.records.isEmpty());
    }

    private static ReplayRecord recordWithChunkOrder(List<ReplayRecord> records, int chunkOrder) {
        for (ReplayRecord record : records) {
            if (Integer.valueOf(chunkOrder).equals(record.getMetadata().get(PassThroughConstants.CHUNK_ORDER_PROPERTY))) {
                return record;
            }
        }
        Assert.fail("No replay record with chunk order " + chunkOrder);
        return null;
    }

    @Test
    public void testCapturesOnlyTheBytesWrittenToTheSocket() throws Exception {
        enableReplay("POST");
        channelAccepts(8);
        byte[] payload = "POST /orders HTTP/1.1\r\n\r\n{}".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer src = ByteBuffer.wrap(payload);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        Assert.assertEquals(8, write(replaySession, src));
        Assert.assertEquals(8, write(replaySession, src));

        Assert.assertEquals("Capturing must not change the caller's buffer position", 16, src.position());
        List<ReplayRecord> records = writer.awaitRecords(2, WAIT_MILLIS);
        Assert.assertEquals(2, records.size());
        Assert.assertArrayEquals(Arrays.copyOfRange(payload, 0, 8), recordWithChunkOrder(records, 1).getData());
        Assert.assertArrayEquals(Arrays.copyOfRange(payload, 8, 16), recordWithChunkOrder(records, 2).getData());
        for (ReplayRecord record : records) {
            Assert.assertEquals("urn:uuid:replay-test", record.getMessageId());
        }
    }

    @Test
    public void testChunkOrderIsTrackedOnTheMessageContext() throws Exception {
        enableReplay("PUT");
        channelAccepts(4);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");
        ByteBuffer src = ByteBuffer.wrap(new byte[12]);

        write(replaySession, src);
        write(replaySession, src);
        write(replaySession, src);

        Assert.assertEquals(3, messageContext.getProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY));
        List<ReplayRecord> records = writer.awaitRecords(3, WAIT_MILLIS);
        Assert.assertEquals(3, records.size());
        recordWithChunkOrder(records, 1);
        recordWithChunkOrder(records, 2);
        recordWithChunkOrder(records, 3);
    }

    @Test
    public void testMetadataContainsMessageContextProperties() throws Exception {
        enableReplay("POST");
        messageContext.setProperty("API_NAME", "OrdersAPI");
        channelAccepts(64);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        write(replaySession, ByteBuffer.wrap("body".getBytes(StandardCharsets.US_ASCII)));

        ReplayRecord record = writer.awaitRecords(1, WAIT_MILLIS).get(0);
        Assert.assertEquals("OrdersAPI", record.getMetadata().get("API_NAME"));
        Assert.assertEquals("POST", record.getMetadata().get("HTTP_METHOD"));
        Assert.assertEquals(Boolean.TRUE, record.getMetadata().get(REPLAY_ENABLED_PROPERTY));
        Assert.assertEquals(1, record.getMetadata().get(PassThroughConstants.CHUNK_ORDER_PROPERTY));
    }

    @Test
    public void testAllWriteMethodsAreCapturedCaseInsensitively() throws Exception {
        channelAccepts(64);
        String[] methods = {"POST", "PUT", "PATCH", "DELETE", "post", "Patch"};
        for (String method : methods) {
            messageContext.removeProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY);
            enableReplay(method);
            write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1}));
        }
        Assert.assertEquals(methods.length, writer.awaitRecords(methods.length, WAIT_MILLIS).size());
    }

    @Test
    public void testNonWriteMethodsAreNotCaptured() throws Exception {
        channelAccepts(64);
        for (String method : new String[]{"GET", "HEAD", "OPTIONS", "CONNECT"}) {
            enableReplay(method);
            ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");
            Assert.assertEquals(1, write(replaySession, ByteBuffer.wrap(new byte[]{1})));
        }
        assertNothingCaptured();
        Assert.assertNull(messageContext.getProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY));
    }

    @Test
    public void testMissingHttpMethodIsNotCaptured() throws Exception {
        channelAccepts(64);
        messageContext.setProperty(REPLAY_ENABLED_PROPERTY, Boolean.TRUE);
        write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1}));
        assertNothingCaptured();
    }

    @Test
    public void testNotCapturedWhenReplayPropertyIsAbsent() throws Exception {
        channelAccepts(64);
        messageContext.setProperty("HTTP_METHOD", "POST");
        Assert.assertEquals(1, write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1})));
        assertNothingCaptured();
    }

    @Test
    public void testNotCapturedWhenReplayPropertyIsFalse() throws Exception {
        channelAccepts(64);
        messageContext.setProperty("HTTP_METHOD", "POST");
        messageContext.setProperty(REPLAY_ENABLED_PROPERTY, Boolean.FALSE);
        write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1}));
        assertNothingCaptured();
    }

    @Test
    public void testNotCapturedWhenSessionHasNoTargetContext() throws Exception {
        channelAccepts(64);
        enableReplay("POST");
        Mockito.when(session.getAttribute(TargetContext.CONNECTION_INFORMATION)).thenReturn(null);
        ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3});

        Assert.assertEquals("The write must still go through to the socket", 3,
                write(new ReplayIOSession(session, "http-outgoing-1"), src));
        assertNothingCaptured();
    }

    @Test
    public void testUnknownMessageIdIsUsedWhenMessageIdIsMissing() throws Exception {
        channelAccepts(64);
        enableReplay("POST");
        messageContext.setMessageID(null);

        write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1}));

        Assert.assertEquals("UNKNOWN", writer.awaitRecords(1, WAIT_MILLIS).get(0).getMessageId());
    }

    @Test
    public void testReadIsDelegatedAndNotCaptured() throws Exception {
        enableReplay("POST");
        Mockito.when(channel.read(Mockito.any(ByteBuffer.class))).thenReturn(5);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        Assert.assertEquals(5, replaySession.channel().read(ByteBuffer.allocate(16)));
        Mockito.verify(channel).read(Mockito.any(ByteBuffer.class));
        assertNothingCaptured();
    }

    @Test
    public void testSessionAndChannelOperationsAreDelegated() throws Exception {
        Mockito.when(session.getStatus()).thenReturn(IOSession.ACTIVE);
        Mockito.when(session.getSocketTimeout()).thenReturn(1234);
        Mockito.when(channel.isOpen()).thenReturn(true);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        Assert.assertEquals(IOSession.ACTIVE, replaySession.getStatus());
        Assert.assertEquals(1234, replaySession.getSocketTimeout());
        Assert.assertTrue(replaySession.channel().isOpen());
        replaySession.setAttribute("key", "value");
        Mockito.verify(session).setAttribute("key", "value");
        replaySession.setEventMask(1);
        Mockito.verify(session).setEventMask(1);
        replaySession.channel().close();
        Mockito.verify(channel).close();
        replaySession.close();
        Mockito.verify(session).close();
    }

    @Test
    public void testWriteSucceedsWhenDispatcherCannotBeInitialized() throws Exception {
        // No writer class is configured, so creating the dispatcher fails.
        ReplayTestUtils.resetDispatcher();
        enableReplay("POST");
        channelAccepts(64);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        for (int i = 0; i < 3; i++) {
            ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
            Assert.assertEquals("The backend write must succeed even if replay capture fails", 4,
                    write(replaySession, src));
            Assert.assertEquals(4, src.position());
        }
    }

    @Test
    public void testWriteSucceedsWhenWriterClassInitializationFails() throws Exception {
        ReplayTestUtils.resetDispatcher();
        java.util.Properties props = ReplayTestUtils.dispatcherProperties();
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY,
                ReplayTestUtils.BrokenStaticInitWriter.class.getName());
        try {
            enableReplay("POST");
            channelAccepts(64);
            ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");
            for (int i = 0; i < 2; i++) {
                Assert.assertEquals(3, write(replaySession, ByteBuffer.wrap(new byte[]{1, 2, 3})));
            }
        } finally {
            props.remove(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY);
        }
    }

    @Test
    public void testZeroByteWriteIsNotCaptured() throws Exception {
        enableReplay("POST");
        channelAccepts(0);
        ReplayIOSession replaySession = new ReplayIOSession(session, "http-outgoing-1");

        Assert.assertEquals(0, write(replaySession, ByteBuffer.wrap(new byte[]{1, 2, 3})));

        assertNothingCaptured();
        Assert.assertNull("A zero byte write must not advance the chunk order",
                messageContext.getProperty(PassThroughConstants.CHUNK_ORDER_PROPERTY));
    }

    @Test
    public void testWriteSucceedsWhenReplayPropertyIsNotBoolean() throws Exception {
        channelAccepts(64);
        messageContext.setProperty("HTTP_METHOD", "POST");
        // A property mediator without type="BOOLEAN" sets a String value
        messageContext.setProperty(REPLAY_ENABLED_PROPERTY, "true");

        Assert.assertEquals(3, write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1, 2, 3})));
        assertNothingCaptured();
    }

    @Test
    public void testWriteSucceedsWhenRequestMessageContextIsMissing() throws Exception {
        channelAccepts(64);
        TargetContext targetContext = Mockito.mock(TargetContext.class);
        Mockito.when(session.getAttribute(TargetContext.CONNECTION_INFORMATION)).thenReturn(targetContext);

        Assert.assertEquals(2, write(new ReplayIOSession(session, "http-outgoing-1"), ByteBuffer.wrap(new byte[]{1, 2})));
        assertNothingCaptured();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullSessionIsRejected() {
        new ReplayIOSession(null, "http-outgoing-1");
    }
}

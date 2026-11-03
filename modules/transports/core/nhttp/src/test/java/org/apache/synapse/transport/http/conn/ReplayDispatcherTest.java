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

import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link ReplayDispatcher}: writer class loading, default configuration, asynchronous
 * delivery to the writer and dropping of records when the buffer is full.
 */
public class ReplayDispatcherTest {

    private static final long WAIT_MILLIS = 10000;

    private Properties props;

    /**
     * Writer that is loaded by class name through {@link ReplayDispatcher#getInstance()}.
     */
    public static class ConfiguredWriter implements ReplayDataWriter {

        static final List<ReplayRecord> RECORDS = new CopyOnWriteArrayList<>();

        public ConfiguredWriter() {
        }

        @Override
        public void write(ReplayRecord replayRecord) {
            RECORDS.add(replayRecord);
        }

        @Override
        public void close() {
        }
    }

    /**
     * Writer without a no-argument constructor, which the dispatcher cannot create.
     */
    public static class NoDefaultConstructorWriter implements ReplayDataWriter {

        public NoDefaultConstructorWriter(String unused) {
        }

        @Override
        public void write(ReplayRecord replayRecord) {
        }

        @Override
        public void close() {
        }
    }

    @Before
    public void setUp() throws Exception {
        ReplayTestUtils.resetDispatcher();
        props = ReplayTestUtils.dispatcherProperties();
        props.remove(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY);
        ConfiguredWriter.RECORDS.clear();
    }

    @After
    public void tearDown() throws Exception {
        props.remove(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY);
        ReplayTestUtils.resetDispatcher();
    }

    private void assertGetInstanceFails() {
        try {
            ReplayDispatcher.getInstance();
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void testGetInstanceFailsWhenWriterClassIsNotConfigured() {
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceFailsWhenWriterClassIsEmpty() {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, "");
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceFailsWhenWriterClassDoesNotExist() {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, "org.example.MissingWriter");
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceFailsWhenClassIsNotAReplayDataWriter() {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, String.class.getName());
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceFailsWhenWriterHasNoDefaultConstructor() {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY,
                NoDefaultConstructorWriter.class.getName());
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceFailsWhenWriterClassInitializationFails() {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY,
                ReplayTestUtils.BrokenStaticInitWriter.class.getName());
        assertGetInstanceFails();
        // The failure is remembered, so the next call fails fast with the same exception type
        assertGetInstanceFails();
    }

    @Test
    public void testInitializationFailureIsNotRetried() {
        assertGetInstanceFails();
        // The configuration is only read at startup, so a failed initialization is not retried.
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, ConfiguredWriter.class.getName());
        assertGetInstanceFails();
    }

    @Test
    public void testGetInstanceLoadsConfiguredWriterOnce() throws Exception {
        props.setProperty(PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, ConfiguredWriter.class.getName());

        ReplayDispatcher first = ReplayDispatcher.getInstance();
        ReplayDispatcher second = ReplayDispatcher.getInstance();

        Assert.assertSame(first, second);
        Field writerField = ReplayDispatcher.class.getDeclaredField("replayDataWriter");
        writerField.setAccessible(true);
        Assert.assertTrue(writerField.get(first) instanceof ConfiguredWriter);

        first.addReplayRecord("id-1", Collections.emptyMap(), "data".getBytes(StandardCharsets.UTF_8));
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (ConfiguredWriter.RECORDS.isEmpty() && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.assertEquals(1, ConfiguredWriter.RECORDS.size());
        Assert.assertEquals("id-1", ConfiguredWriter.RECORDS.get(0).getMessageId());
    }

    @Test
    public void testDefaultConfigurationWhenPropertiesAreAbsent() throws Exception {
        Assert.assertEquals(10000, ReplayTestUtils.staticIntField("REPLAY_MAX_BUFFER_SIZE"));
        Assert.assertEquals(100, ReplayTestUtils.staticIntField("REPLAY_LOG_DROP_FREQUENCY"));
        Assert.assertEquals(50, ReplayTestUtils.staticIntField("REPLAY_BUFFER_POLL_INTERVAL"));
        Assert.assertEquals(4, ReplayTestUtils.staticIntField("CORE_POOL_SIZE"));
        Assert.assertEquals(8, ReplayTestUtils.staticIntField("MAX_POOL_SIZE"));
        Field keepAlive = ReplayDispatcher.class.getDeclaredField("KEEP_ALIVE_TIME_MILLIS");
        keepAlive.setAccessible(true);
        Assert.assertEquals(30000L, keepAlive.getLong(null));
    }

    @Test
    public void testWorkerThreadsAreNamedDaemonThreads() throws Exception {
        ReplayTestUtils.installDispatcher(new ReplayTestUtils.CapturingReplayWriter());
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        Set<Thread> workers = new HashSet<>();
        while (workers.isEmpty() && System.currentTimeMillis() < deadline) {
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.getName().startsWith("ReplayTransaction-Writer-")) {
                    workers.add(thread);
                }
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        Assert.assertFalse("Replay worker threads were not started", workers.isEmpty());
        for (Thread worker : workers) {
            Assert.assertTrue("Replay worker threads must not block JVM shutdown", worker.isDaemon());
        }
    }

    @Test
    public void testRecordsAreDeliveredToWriter() throws Exception {
        ReplayTestUtils.CapturingReplayWriter writer = new ReplayTestUtils.CapturingReplayWriter();
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);

        for (int i = 0; i < 50; i++) {
            dispatcher.addReplayRecord("id-" + i, Collections.singletonMap("index", i),
                    ("chunk-" + i).getBytes(StandardCharsets.UTF_8));
        }

        List<ReplayRecord> records = writer.awaitRecords(50, WAIT_MILLIS);
        Assert.assertEquals(50, records.size());
        Set<String> ids = new HashSet<>();
        for (ReplayRecord record : records) {
            ids.add(record.getMessageId());
            int index = (Integer) record.getMetadata().get("index");
            Assert.assertEquals("chunk-" + index, new String(record.getData(), StandardCharsets.UTF_8));
        }
        Assert.assertEquals("Every record must be written exactly once", 50, ids.size());
    }

    @Test
    public void testByteBufferVariantCopiesWrittenBytesFromCurrentPosition() throws Exception {
        ReplayTestUtils.CapturingReplayWriter writer = new ReplayTestUtils.CapturingReplayWriter();
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);
        ByteBuffer buffer = ByteBuffer.wrap("0123456789".getBytes(StandardCharsets.US_ASCII));
        buffer.position(3);

        dispatcher.addReplayRecord(buffer, 4, "id", Collections.emptyMap());

        Assert.assertEquals("3456", new String(writer.awaitRecords(1, WAIT_MILLIS).get(0).getData(),
                StandardCharsets.US_ASCII));
    }

    @Test
    public void testWorkerKeepsRunningAfterWriterIOException() throws Exception {
        ReplayTestUtils.IOExceptionOnceReplayWriter writer = new ReplayTestUtils.IOExceptionOnceReplayWriter();
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);

        dispatcher.addReplayRecord("failed", null, new byte[]{1});
        TimeUnit.MILLISECONDS.sleep(300);
        dispatcher.addReplayRecord("after-failure", null, new byte[]{2});

        List<ReplayRecord> records = writer.awaitRecords(1, WAIT_MILLIS);
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("after-failure", records.get(0).getMessageId());
    }

    @Test
    public void testWorkersKeepRunningAfterWriterRuntimeException() throws Exception {
        int workers = ReplayTestUtils.staticIntField("CORE_POOL_SIZE");
        // One failure per worker: if a failure stopped the worker that hit it, no worker would be left.
        ReplayTestUtils.RuntimeExceptionReplayWriter writer = new ReplayTestUtils.RuntimeExceptionReplayWriter(workers);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);

        for (int i = 0; i < workers; i++) {
            dispatcher.addReplayRecord("failed-" + i, null, new byte[]{1});
        }
        TimeUnit.MILLISECONDS.sleep(500);
        dispatcher.addReplayRecord("after-failures", null, new byte[]{2});

        List<ReplayRecord> records = writer.awaitRecords(1, WAIT_MILLIS);
        Assert.assertEquals("Records must still be written after writer failures", 1, records.size());
        Assert.assertEquals("after-failures", records.get(0).getMessageId());
    }

    @Test
    public void testWorkersKeepRunningAfterWriterLinkageError() throws Exception {
        int workers = ReplayTestUtils.staticIntField("CORE_POOL_SIZE");
        ReplayTestUtils.LinkageErrorReplayWriter writer = new ReplayTestUtils.LinkageErrorReplayWriter(workers);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);

        for (int i = 0; i < workers; i++) {
            dispatcher.addReplayRecord("failed-" + i, null, new byte[]{1});
        }
        TimeUnit.MILLISECONDS.sleep(500);
        dispatcher.addReplayRecord("after-failures", null, new byte[]{2});

        List<ReplayRecord> records = writer.awaitRecords(1, WAIT_MILLIS);
        Assert.assertEquals("Records must still be written after writer linkage errors", 1, records.size());
    }

    @Test
    public void testRecordsAreDroppedWithoutBlockingWhenBufferIsFull() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        ReplayTestUtils.CapturingReplayWriter blockingWriter = new ReplayTestUtils.CapturingReplayWriter() {
            @Override
            public void write(ReplayRecord replayRecord) throws java.io.IOException {
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.write(replayRecord);
            }
        };
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(blockingWriter);
        int capacity = ReplayTestUtils.staticIntField("REPLAY_MAX_BUFFER_SIZE");
        int workers = ReplayTestUtils.staticIntField("CORE_POOL_SIZE");
        int extra = 250;
        int total = capacity + workers + extra;

        long start = System.nanoTime();
        try {
            for (int i = 0; i < total; i++) {
                dispatcher.addReplayRecord("id-" + i, null, new byte[]{1});
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            long dropped = ReplayTestUtils.droppedCount(dispatcher);

            // Each worker can hold at most one record while blocked, so between "extra" and
            // "extra + workers" records must have been rejected by the full queue.
            Assert.assertTrue("Dropped " + dropped + ", expected at least " + extra, dropped >= extra);
            Assert.assertTrue("Dropped " + dropped + ", expected at most " + (extra + workers),
                    dropped <= extra + workers);
            Assert.assertTrue("Adding records must not block on a full buffer (took " + elapsedMillis + "ms)",
                    elapsedMillis < WAIT_MILLIS);

            release.countDown();
            int accepted = (int) (total - dropped);
            Assert.assertEquals("All accepted records must be written once the writer recovers",
                    accepted, blockingWriter.awaitRecords(accepted, WAIT_MILLIS * 3).size());
        } finally {
            release.countDown();
        }
    }

    /**
     * Writer that counts close calls and can be held inside write() until released.
     */
    private static class ClosableWriter extends ReplayTestUtils.CapturingReplayWriter {

        final AtomicInteger closeCount = new AtomicInteger();
        final CountDownLatch release;
        final long writeDelayMillis;

        ClosableWriter(CountDownLatch release, long writeDelayMillis) {
            this.release = release;
            this.writeDelayMillis = writeDelayMillis;
        }

        @Override
        public void write(ReplayRecord replayRecord) throws java.io.IOException {
            try {
                if (release != null) {
                    release.await();
                }
                if (writeDelayMillis > 0) {
                    TimeUnit.MILLISECONDS.sleep(writeDelayMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            super.write(replayRecord);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    @Test
    public void testWorkersDoNotCloseWriterWhileRunning() throws Exception {
        ClosableWriter writer = new ClosableWriter(null, 0);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);
        try {
            dispatcher.addReplayRecord("msg-1", Collections.emptyMap(), new byte[]{1});
            Assert.assertEquals(1, writer.awaitRecords(1, WAIT_MILLIS).size());
            TimeUnit.MILLISECONDS.sleep(200);
            Assert.assertEquals("Writer must stay open while the dispatcher runs", 0, writer.closeCount.get());
        } finally {
            dispatcher.shutdown(WAIT_MILLIS);
        }
    }

    @Test
    public void testShutdownWritesQueuedRecordsAndClosesWriterOnce() throws Exception {
        ClosableWriter writer = new ClosableWriter(null, 5);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);
        int count = 100;
        for (int i = 0; i < count; i++) {
            dispatcher.addReplayRecord("msg-" + i, Collections.emptyMap(), new byte[]{(byte) i});
        }

        dispatcher.shutdown(WAIT_MILLIS);

        Assert.assertEquals("All queued records must be written before the writer is closed",
                count, writer.records.size());
        Assert.assertEquals("Writer must be closed exactly once", 1, writer.closeCount.get());
        dispatcher.shutdown(WAIT_MILLIS);
        Assert.assertEquals("A second shutdown must have no effect", 1, writer.closeCount.get());
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().startsWith("ReplayTransaction-Writer-") && thread.isAlive()) {
                thread.join(WAIT_MILLIS);
            }
        }
    }

    @Test
    public void testRecordsAreNotAcceptedAfterShutdown() throws Exception {
        ClosableWriter writer = new ClosableWriter(null, 0);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);
        dispatcher.shutdown(WAIT_MILLIS);

        dispatcher.addReplayRecord("late", Collections.emptyMap(), new byte[]{1});
        dispatcher.addReplayRecord(ByteBuffer.wrap(new byte[]{1, 2}), 2, "late-buffer", Collections.emptyMap());

        Assert.assertEquals("Records must not be queued after shutdown", 0, ReplayTestUtils.queueSize(dispatcher));
        TimeUnit.MILLISECONDS.sleep(200);
        Assert.assertTrue(writer.records.isEmpty());
        Assert.assertEquals(0, ReplayTestUtils.droppedCount(dispatcher));
    }

    @Test
    public void testShutdownIsBoundedWhenWriterIsBlocked() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        ClosableWriter writer = new ClosableWriter(release, 0);
        ReplayDispatcher dispatcher = ReplayTestUtils.installDispatcher(writer);
        try {
            dispatcher.addReplayRecord("blocked", Collections.emptyMap(), new byte[]{1});
            TimeUnit.MILLISECONDS.sleep(200);

            long start = System.nanoTime();
            dispatcher.shutdown(300);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            Assert.assertTrue("Shutdown must return after the timeout, took " + elapsedMillis + " ms",
                    elapsedMillis < 3000);
            Assert.assertEquals(1, writer.closeCount.get());
        } finally {
            release.countDown();
        }
    }
}

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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared helpers for the replay transaction unit tests. The replay dispatcher is a lazily created
 * singleton whose configuration is read once from passthru-http.properties, so the tests reach into
 * it reflectively to install a capturing writer and to reset it afterwards.
 */
final class ReplayTestUtils {

    private ReplayTestUtils() {
    }

    /**
     * Writer that keeps every record it receives in memory.
     */
    static class CapturingReplayWriter implements ReplayDataWriter {

        final List<ReplayRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void write(ReplayRecord replayRecord) throws IOException {
            records.add(replayRecord);
        }

        @Override
        public void close() {
        }

        List<ReplayRecord> awaitRecords(int expected, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (records.size() < expected && System.currentTimeMillis() < deadline) {
                TimeUnit.MILLISECONDS.sleep(10);
            }
            return new ArrayList<>(records);
        }
    }

    /**
     * Creates a dispatcher through its private constructor (which also starts its worker threads)
     * and installs it as the singleton instance.
     */
    static ReplayDispatcher installDispatcher(ReplayDataWriter writer) throws Exception {
        Constructor<ReplayDispatcher> constructor =
                ReplayDispatcher.class.getDeclaredConstructor(ReplayDataWriter.class);
        constructor.setAccessible(true);
        ReplayDispatcher dispatcher = constructor.newInstance(writer);
        setSingleton(dispatcher);
        return dispatcher;
    }

    static void resetDispatcher() throws Exception {
        Field instance = ReplayDispatcher.class.getDeclaredField("instance");
        instance.setAccessible(true);
        ReplayDispatcher current = (ReplayDispatcher) instance.get(null);
        if (current != null) {
            current.shutdown(10000);
        }
        setSingleton(null);
        for (Field field : ReplayDispatcher.class.getDeclaredFields()) {
            if ("initializationFailed".equals(field.getName())) {
                field.setAccessible(true);
                field.setBoolean(null, false);
            }
        }
    }

    static void setSingleton(ReplayDispatcher dispatcher) throws Exception {
        Field instance = ReplayDispatcher.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, dispatcher);
    }

    static Properties dispatcherProperties() throws Exception {
        Field props = ReplayDispatcher.class.getDeclaredField("props");
        props.setAccessible(true);
        return (Properties) props.get(null);
    }

    static long droppedCount(ReplayDispatcher dispatcher) throws Exception {
        Field dropped = ReplayDispatcher.class.getDeclaredField("droppedCount");
        dropped.setAccessible(true);
        return ((AtomicLong) dropped.get(dispatcher)).get();
    }

    static int queueSize(ReplayDispatcher dispatcher) throws Exception {
        Field queue = ReplayDispatcher.class.getDeclaredField("replayQueue");
        queue.setAccessible(true);
        return ((java.util.Collection<?>) queue.get(dispatcher)).size();
    }

    static int staticIntField(String name) throws Exception {
        Field field = ReplayDispatcher.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    /**
     * Writer whose class initialization fails, as it would when a static initializer throws. Loading it raises
     * ExceptionInInitializerError the first time and NoClassDefFoundError afterwards, both LinkageErrors.
     */
    public static class BrokenStaticInitWriter implements ReplayDataWriter {

        static {
            failInitialization();
        }

        private static void failInitialization() {
            throw new IllegalStateException("simulated static initializer failure");
        }

        public BrokenStaticInitWriter() {
        }

        @Override
        public void write(ReplayRecord replayRecord) {
        }

        @Override
        public void close() {
        }
    }

    /**
     * Writer that throws a NoClassDefFoundError for the first given number of records, as a writer with a missing
     * optional dependency would.
     */
    static class LinkageErrorReplayWriter extends CapturingReplayWriter {

        private final AtomicLong calls = new AtomicLong();
        private final long failures;

        LinkageErrorReplayWriter(long failures) {
            this.failures = failures;
        }

        @Override
        public void write(ReplayRecord replayRecord) throws IOException {
            if (calls.incrementAndGet() <= failures) {
                throw new NoClassDefFoundError("org/example/MissingDependency");
            }
            super.write(replayRecord);
        }
    }

    /**
     * Writer that throws a RuntimeException for the first given number of records, used to check that an unexpected
     * writer failure does not stop the worker threads.
     */
    static class RuntimeExceptionReplayWriter extends CapturingReplayWriter {

        private final AtomicLong calls = new AtomicLong();
        private final long failures;

        RuntimeExceptionReplayWriter(long failures) {
            this.failures = failures;
        }

        @Override
        public void write(ReplayRecord replayRecord) throws IOException {
            if (calls.incrementAndGet() <= failures) {
                throw new IllegalStateException("simulated writer bug");
            }
            super.write(replayRecord);
        }
    }

    /**
     * Writer that throws an IOException for the first record only, used to check that a failed
     * write does not stop the worker thread.
     */
    static class IOExceptionOnceReplayWriter extends CapturingReplayWriter {

        private final AtomicLong calls = new AtomicLong();

        @Override
        public void write(ReplayRecord replayRecord) throws IOException {
            if (calls.incrementAndGet() == 1) {
                throw new IOException("simulated write failure");
            }
            super.write(replayRecord);
        }
    }
}

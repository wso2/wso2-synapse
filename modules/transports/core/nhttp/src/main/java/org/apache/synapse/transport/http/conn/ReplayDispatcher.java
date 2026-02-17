/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.transport.passthru.PassThroughConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Singleton manager class responsible for buffering replay data and asynchronously writing it
 * to a configured ReplayDataWriter.
 * A dedicated background thread consumes records from the queue and writes them using the
 * ReplayDataWriter implementation configured dynamically through properties.
 * This class is thread-safe and supports graceful shutdown by closing the writer and stopping the background thread.
 */
public class ReplayDispatcher {
    private static final Log log = LogFactory.getLog(ReplayDispatcher.class);
    private static volatile ReplayDispatcher instance;
    // Counter tracking the number of dropped replay records due to queue overflow
    private final AtomicLong droppedCount = new AtomicLong(0);
    // Writer instance that handles actual replay data persistence
    private final ReplayDataWriter replayDataWriter;
    private static final String PROPERTY_FILE = "passthru-http.properties";
    // Loaded configuration properties
    private static final Properties props = MiscellaneousUtil.loadProperties(PROPERTY_FILE);
    // Maximum size of the replay record queue
    private static final int REPLAY_MAX_BUFFER_SIZE = Integer.parseInt(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_MAX_BUFFER_SIZE_KEY, "10000"));
    // Frequency at which dropped message logs are emitted
    private static final int REPLAY_LOG_DROP_FREQUENCY = Integer.parseInt(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_LOG_DROP_FREQUENCY_KEY, "100"));
    // Poll interval in milliseconds for replay queue when empty
    private static final int REPLAY_BUFFER_POLL_INTERVAL = Integer.parseInt(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_BUFFER_POLL_INTERVAL_MS_KEY, "50"));
    // Core number of threads to keep in the pool, even if idle;
    private static final int CORE_POOL_SIZE = Integer.parseInt(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_WORKER_CORE_POOL_SIZE_KEY, "4"));
    // Maximum number of threads allowed in the pool
    private static final int MAX_POOL_SIZE = Integer.parseInt(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_WORKER_MAX_POOL_SIZE_KEY, "8"));
    // Time in milliseconds that excess idle threads (beyond core size) will wait before terminating
    private static final long KEEP_ALIVE_TIME_MILLIS = Long.parseLong(
            MiscellaneousUtil.getProperty(props, PassThroughConstants.REPLAY_WORKER_KEEP_ALIVE_TIME_MS_KEY, "30000"));

    private final BlockingQueue<ReplayRecord> replayQueue = new LinkedBlockingQueue<>(REPLAY_MAX_BUFFER_SIZE);

    /**
     * Private constructor for singleton. Initializes the replay writer and starts the background worker thread.
     *
     * @param replayDataWriter an implementation of ReplayDataWriter used for persisting replay records
     */
    private ReplayDispatcher(ReplayDataWriter replayDataWriter) {
        this.replayDataWriter = replayDataWriter;
        log.info("Initializing OverflowBufferManager with buffer size: " + REPLAY_MAX_BUFFER_SIZE);
        startReplayWorker();
    }

    /**
     * Returns the singleton instance of the ReplayDispatcher, creating it if necessary.
     * Dynamically instantiates the writer class configured via properties. Throws an
     * IllegalStateException for missing or invalid configuration.
     *
     * @return singleton instance of this dispatcher
     */
    public static ReplayDispatcher getInstance() {
        if (instance == null) {
            synchronized (ReplayDispatcher.class) {
                if (instance == null) {
                    try {
                        String writerClassName = MiscellaneousUtil.getProperty(props,
                                PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY, null);

                        if (writerClassName == null || writerClassName.isEmpty()) {
                            throw new IllegalStateException(
                                    "Required config '" + PassThroughConstants.REPLAY_TRANSACTION_WRITER_CLASS_KEY
                                            + "' missing");
                        }
                        // Dynamically load and instantiate writer
                        Class<?> writerClass = Class.forName(writerClassName);
                        if (!ReplayDataWriter.class.isAssignableFrom(writerClass)) {
                            throw new IllegalStateException(
                                    "Configured class " + writerClassName + " does not implement ReplayDataWriter");
                        }
                        ReplayDataWriter writerInstance;
                        try {
                            writerInstance = (ReplayDataWriter) writerClass.getConstructor().newInstance();
                        } catch (Exception e) {
                            log.fatal("Failed to instantiate ReplayDataWriter with no-arg constructor", e);
                            throw new IllegalStateException("Failed to instantiate ReplayDataWriter", e);
                        }
                        instance = new ReplayDispatcher(writerInstance);
                        log.info("ReplayDispatcher initialized with custom writer class: " + writerClassName);
                    } catch (Exception e) {
                        log.fatal("Failed to initialize ReplayDispatcher dynamically", e);
                        throw new IllegalStateException("Failed to initialize ReplayDispatcher", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Adds a replay record to the internal buffer queue for later asynchronous writing.
     * If the queue is full, drops the message and logs drop occurrences at configured frequency.
     *
     * @param messageId unique identifier of the message
     * @param metadata  metadata map describing contextual information of the message
     * @param data      raw byte array of the message content
     */
    public void addReplayRecord(String messageId, Map<String, Object> metadata, byte[] data) {
        boolean accepted = replayQueue.offer(new ReplayRecord(messageId, metadata, data));
        if (!accepted) {
            long dropped = droppedCount.incrementAndGet();
            if (dropped % REPLAY_LOG_DROP_FREQUENCY == 0) {
                log.warn("Replay buffer overflow: dropped " + dropped + " messages so far; " +
                        "Latest dropped messageID: " + messageId);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Replay buffer overflow: dropped message with ID: " + messageId);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Buffer accepted message with ID: " + messageId + ". Current buffer size: "
                        + replayQueue.size());
            }
        }
    }

    /**
     * Convenience method to wrap data in ByteBuffer and delegate to addReplayRecord(String, byte[], Map).
     *
     * @param dataBuffer    byte buffer containing the raw message data
     * @param byteWritten   number of bytes written to the buffer
     * @param messageId     unique message identifier
     * @param metadata      metadata map for contextual information
     */
    public void addReplayRecord(ByteBuffer dataBuffer, int byteWritten, String messageId, Map<String, Object> metadata) {
        byte[] data = new byte[byteWritten];
        dataBuffer.get(data);
        addReplayRecord(messageId, metadata, data);
    }

    /**
     * Starts the background thread pool which asynchronously writes buffered replay records using
     * the configured ReplayDataWriter.
     * The worker threads run until the queue is drained.
     */
    private void startReplayWorker() {
        // ExecutorService managing a configurable pool of worker threads for asynchronously processing replay records
        ExecutorService replayWorkerThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
                KEEP_ALIVE_TIME_MILLIS, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            private final AtomicLong threadIndex = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "ReplayTransaction-Writer-" + threadIndex.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });

        for (int i = 0; i < ReplayDispatcher.CORE_POOL_SIZE; i++) {
            replayWorkerThreadPool.submit(() -> {
                while (!replayQueue.isEmpty()) {
                    ReplayRecord replayRecord = replayQueue.poll();
                    if (replayRecord != null) {
                        try {
                            replayDataWriter.write(replayRecord);
                            if (log.isDebugEnabled()) {
                                log.debug("Successfully wrote buffer data for messageID: " + replayRecord.getMessageId());
                            }
                        } catch (IOException e) {
                            log.error("Failed to write buffered data for messageID: " + replayRecord.getMessageId(), e);
                        }
                    } else {
                        // No data, prevent tight spinning
                        try {
                            Thread.sleep(REPLAY_BUFFER_POLL_INTERVAL);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            if (log.isDebugEnabled()) {
                                log.debug("ReplayTransaction-BufferDataWriter thread interrupted during sleep");
                            }
                            // Exit loop if interrupted while stopping
                            break;
                        }
                    }
                }
                try {
                    replayDataWriter.close();
                    log.info("ReplayTransaction-BufferDataWriter closed BufferDataWriter cleanly");
                } catch (IOException e) {
                    log.error("Failed to close BufferDataWriter during thread termination", e);
                }
            });
        }
    }
}

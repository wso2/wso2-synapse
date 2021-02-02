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
package org.apache.synapse.transport.nhttp;

import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.annotation.Contract;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.ContentOutputBuffer;
import org.apache.http.nio.util.ExpandableBuffer;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@link ContentOutputBuffer} interface that can be
 * shared by multiple threads, usually the I/O dispatch of an I/O reactor and
 * a worker thread.
 * <p>
 * The I/O dispatch thread is expected to transfer data from the buffer to
 *   {@link ContentEncoder} by calling {@link #produceContent(ContentEncoder)}.
 * <p>
 * The worker thread is expected to write data to the buffer by calling
 * {@link #write(int)}, {@link #write(byte[], int, int)} or {@link #writeCompleted()}
 * <p>
 * In case of an abnormal situation or when no longer needed the buffer must be
 * shut down using {@link #shutdown()} method.
 *
 * Please note that {@link org.apache.http.nio.util.SharedOutputBuffer} class was copied here renamed as
 * NhttpSharedOutputBuffer from httpcore-nio in order to fix
 * https://github.com/wso2/product-ei/issues/1367, without having to do an API change in httpcore-nio component.
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class NhttpSharedOutputBuffer extends ExpandableBuffer implements ContentOutputBuffer {

    private final ReentrantLock lock;
    private final Condition condition;

    private volatile IOControl ioctrl;
    private volatile boolean shutdown = false;
    private volatile boolean endOfStream = false;
    private int timeout = 0;

    /**
     * Boolean state to identify whether write condition await interrupted or timeout exceeded.
     * Normal behaviour should be the interruption of the await. If something went wrong and the write condition
     * did not got any notification to allow write to the buffer, the await will timeout after socket timeout
     * value specified in the nttp.properties file.
     * "true" - if interrupted
     * "false"- if time out exceeds
     */
    private boolean awaitInterrupted = true;

    public NhttpSharedOutputBuffer(final int buffersize, final IOControl ioctrl, final ByteBufferAllocator allocator,
            int timeout) {
        super(buffersize, allocator);
        Args.notNull(ioctrl, "I/O content control");
        this.ioctrl = ioctrl;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
        this.timeout = timeout;
    }


    @Deprecated
    public NhttpSharedOutputBuffer(final int buffersize, final IOControl ioctrl, final ByteBufferAllocator allocator) {
        super(buffersize, allocator);
        Args.notNull(ioctrl, "I/O content control");
        this.ioctrl = ioctrl;
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }


    public NhttpSharedOutputBuffer(final int buffersize, final ByteBufferAllocator allocator) {
        super(buffersize, allocator);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    public NhttpSharedOutputBuffer(final int buffersize) {
        this(buffersize, HeapByteBufferAllocator.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        if (this.shutdown) {
            return;
        }
        this.lock.lock();
        try {
            clear();
            this.endOfStream = false;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasData() {
        this.lock.lock();
        try {
            return super.hasData();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() {
        this.lock.lock();
        try {
            return super.available();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {
        this.lock.lock();
        try {
            return super.capacity();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        this.lock.lock();
        try {
            return super.length();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public int produceContent(final ContentEncoder encoder) throws IOException {
        return produceContent(encoder, null);
    }


    public int produceContent(final ContentEncoder encoder, final IOControl ioctrl) throws IOException {
        if (this.shutdown) {
            return -1;
        }
        this.lock.lock();
        try {
            if (ioctrl != null) {
                this.ioctrl = ioctrl;
            }
            setOutputMode();
            int bytesWritten = 0;
            if (super.hasData()) {
                bytesWritten = encoder.write(this.buffer);
                if (encoder.isCompleted()) {
                    this.endOfStream = true;
                }
            }
            if (!super.hasData()) {
                // No more buffered content
                // If at the end of the stream, terminate
                if (this.endOfStream && !encoder.isCompleted()) {
                    encoder.complete();
                }
                if (!this.endOfStream) {
                    // suspend output events
                    if (this.ioctrl != null) {
                        this.ioctrl.suspendOutput();
                    }
                }
            }
            this.condition.signalAll();
            return bytesWritten;
        } finally {
            this.lock.unlock();
        }
    }

    public void close() {
        shutdown();
    }

    public void shutdown() {
        if (this.shutdown) {
            return;
        }
        this.shutdown = true;
        this.lock.lock();
        try {
            this.condition.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (b == null) {
            return;
        }
        int pos = off;
        this.lock.lock();
        try {
            Asserts.check(!this.shutdown && !this.endOfStream, "Buffer already closed for writing");
            // Set buffer to write mode
            setInputMode();
            int remaining = len;
            while (remaining > 0) {
                // At the moment buffer is in the write mode. So position equals to capacity.
                // if position >= limit(= capacity) -> true i.e. buffer is full
                // If this buffer is full, that means the data should be written to output buffer (Need to flush).
                if (!this.buffer.hasRemaining()) {
                    flushContent();
                    // If awaitInterrupted was set to false, that means something went wrong(timeout happened)
                    // therefore, we clear the buffer and stop the flow with an exception.
                    if (!awaitInterrupted) {
                        this.buffer.clear();
                        throw new IOException("Output buffer write time out exceeded");
                    }
                    // Set buffer to write mode. (Inside flushContent method, buffer was set to read mode)
                    setInputMode();
                }
                final int chunk = Math.min(remaining, this.buffer.remaining());
                this.buffer.put(b, pos, chunk);
                remaining -= chunk;
                pos += chunk;
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void write(final byte[] b) throws IOException {
        if (b == null) {
            return;
        }
        write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void write(final int b) throws IOException {
        this.lock.lock();
        try {
            Asserts.check(!this.shutdown && !this.endOfStream, "Buffer already closed for writing");
            // Set buffer to write mode
            setInputMode();
            // At the moment buffer is in the write mode. So position equals to capacity.
            // if position >= limit(= capacity) -> true i.e. buffer is full
            // If this buffer is full, that means the data should be written to output buffer (Need to flush).
            if (!this.buffer.hasRemaining()) {
                flushContent();
                // If "awaitInterrupted" was set to false, that means something went wrong(timeout happened)
                // therefore, we clear the buffer and stop the flow with an exception.
                if (!awaitInterrupted) {
                    this.buffer.clear();
                    throw new IOException("Output buffer write time out exceeded");
                }
                // Set buffer to write mode. (Inside flushContent method, buffer was set to read mode)
                setInputMode();
            }
            this.buffer.put((byte)b);
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
    }

    /**
     * Flushes the content in the buffer if any.
     *
     * @throws IOException Thrown if the thread was interrupted while flushing
     */
    private void flushContent() throws IOException {
        this.lock.lock();
        try {
            try {
                // hasData sets the buffer to read mode and calls hasRemaining method and returns the result.
                // if position (= initially 0, incremented as data is read) < limit(= how much bytes have been written
                // to the buffer) -> true i.e. there is data to be read
                while (super.hasData()) {
                    if (this.shutdown) {
                        throw new InterruptedIOException("Output operation aborted");
                    }
                    // Request event notifications to be triggered when the underlying
                    // channel is ready for output operations.
                    if (this.ioctrl != null) {
                        this.ioctrl.requestOutput();
                    }
                    // Ask the thread which is reading this buffer to wait for a notification to
                    // write data out
                    awaitInterrupted = this.condition.await(timeout, TimeUnit.MILLISECONDS);
                    // If socket timeout happens before the thread is notified, then we don't care whether there is
                    // data or not, but honor the socket timeout config and break the loop
                    if (!awaitInterrupted) {
                        break;
                    }
                }
            } catch (final InterruptedException ex) {
                throw new IOException("Interrupted while flushing the content buffer");
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeCompleted() throws IOException {
        this.lock.lock();
        try {
            if (this.endOfStream) {
                return;
            }
            this.endOfStream = true;
            if (this.ioctrl != null) {
                this.ioctrl.requestOutput();
            }
        } finally {
            this.lock.unlock();
        }
    }

}

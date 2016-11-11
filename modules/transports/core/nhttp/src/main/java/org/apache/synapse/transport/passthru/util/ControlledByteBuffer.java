/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.util;


import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ControlledByteBuffer {
    private ByteBuffer byteBuffer;

    public boolean isInputMode() {
        return inputMode.get();
    }

    private AtomicBoolean inputMode = new AtomicBoolean(true);

    public ControlledByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }

    public boolean setInputMode() {
        return this.inputMode.compareAndSet(false, true);
    }

    public boolean setOutputMode() {
        return this.inputMode.compareAndSet(true, false);
    }

    public void forceSetInputMode() {
        this.inputMode = new AtomicBoolean(true);
    }

    public void flip() {
        this.byteBuffer.flip();
    }

    public void clear() {
        this.byteBuffer.clear();
    }

    public void compact() {
        this.byteBuffer.compact();
    }

    public int position() {
        return this.byteBuffer.position();
    }

    public int capacity() {
        return this.byteBuffer.capacity();
    }

    public void put(byte b) {
        this.byteBuffer.put(b);
    }

    public void putInt(int value) {
        this.byteBuffer.putInt(value);
    }

    public ByteBuffer put(byte[] src, int offset, int length) {
        return this.byteBuffer.put(src, offset, length);
    }

    public boolean hasRemaining() {
        return this.byteBuffer.hasRemaining();
    }

    public byte get() {
        return this.byteBuffer.get();
    }

    public ByteBuffer get(byte[] dst, int offset, int length) {
        return this.byteBuffer.get(dst, offset, length);
    }

    public int remaining() {
        return this.byteBuffer.remaining();
    }
}
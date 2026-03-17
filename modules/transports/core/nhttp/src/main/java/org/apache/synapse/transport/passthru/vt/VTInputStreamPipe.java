/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru.vt;

import java.io.InputStream;

/**
 * A simple wrapper that exposes a plain {@link InputStream} as a "Pipe" object
 * that can be set on the message context property
 * {@code PassThroughConstants.PASS_THROUGH_PIPE}.
 * <p>
 * In the existing NIO transport, the {@code Pipe} class uses NIO buffers and
 * async producers/consumers. In the VT transport, the body is simply the
 * socket's input stream and can be consumed directly in a blocking fashion.
 * </p>
 */
public class VTInputStreamPipe {

    private final InputStream inputStream;

    public VTInputStreamPipe(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Get the underlying input stream.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public String toString() {
        return "VTInputStreamPipe{" + inputStream + "}";
    }
}

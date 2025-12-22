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

import java.io.IOException;

/**
 * Defines the contract for writing replay data within the HTTP transport layer.
 * <p>
 * Implementations of this interface handle the persistence of replay records—such as
 * request data into various storage mediums, which may include file systems,
 * in-memory buffers, or other destinations.
 * </p>
 *
 * <p>
 * This interface acts as an extension point so that custom data writers can be plugged in
 * without altering the core replay dispatching mechanism.
 * </p>
 *
 * @see ReplayRecord for structure details of replay data.
 */
public interface ReplayDataWriter {

    /**
     * Writes a replay record containing message data and associated metadata.
     * Implementations are responsible for defining how and where the record
     * is persisted.
     *
     * @param replayRecord the {@link ReplayRecord} instance to be written
     * @throws IOException if any I/O error occurs during the write operation
     */
    void write(ReplayRecord replayRecord) throws IOException;

    /**
     * Closes this writer and releases any underlying system resources such as
     * file streams or buffers.
     * <p>This method is called during transport shutdown to ensure proper
     * resource cleanup.</p>
     *
     * @throws IOException if an error occurs while closing underlying resources
     */
    void close() throws IOException;
}


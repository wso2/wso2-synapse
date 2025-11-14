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

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single replay record that encapsulates message content and
 * associated metadata for replay purposes.
 *
 * @see ReplayDataWriter for writing and persisting replay records.
 */
public class ReplayRecord {

    /** Unique identifier for this message record. */
    private final String messageId;

    /** Metadata key-value pairs describing contextual properties of the replayed message. */
    private final Map<String, Object> metadata;

    /** Raw binary representation of the message content (headers, payload, etc.). */
    private final byte[] data;

    /**
     * Constructs a new replay record instance containing the unique message ID,
     * metadata, and raw data of the message.
     *
     * @param messageId a unique identifier for the replayed message; may not be null
     * @param metadata  map containing message context metadata; may be null if not applicable
     * @param data      byte array containing the message payload; may be null or empty
     */
    public ReplayRecord(String messageId, Map<String, Object> metadata, byte[] data) {
        this.messageId = messageId;
        this.data = (data != null) ? data.clone() : null;  // clone for data safety

        if (metadata != null) {
            // Wrap map unmodifiable to prevent changes from this instance,
            // but does not protect if original metadata map is mutated externally
            this.metadata = Collections.unmodifiableMap(metadata);
        } else {
            this.metadata = Collections.emptyMap();
        }
    }

    /**
     * Returns the unique identifier associated with this replay record.
     *
     * @return message ID string
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns metadata associated with this record.
     * The metadata includes contextual information such as API name, version,
     * transport headers, or other attributes recorded at replay capture time.
     *
     * @return metadata map, or null if no metadata is available
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns the raw binary message data contained in this record.
     *
     * @return byte array of message data, or null if not set
     */
    public byte[] getData() {
        return data != null ? data.clone() : null;
    }
}

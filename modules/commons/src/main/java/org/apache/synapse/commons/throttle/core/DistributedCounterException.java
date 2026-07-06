/*
*  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 LLC. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.apache.synapse.commons.throttle.core;

/**
 * Unchecked exception thrown by {@link DistributedCounterManager} operations.
 * Carries a {@link Kind} that classifies the failure so callers can make
 * fine-grained decisions (retry strategy, log level, alert routing) without
 * coupling the Synapse layer to any specific backing store or client library.
 *
 * <p>The convenience method {@link #isTransient()} derives the transient/permanent
 * split from {@link Kind}, keeping existing callers compatible.
 */
public class DistributedCounterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Classifies the nature of the distributed-counter failure in
     * infrastructure-neutral terms.
     */
    public enum Kind {
        /** The remote host is unreachable, the connection was refused or timed out, or the socket was closed. */
        CONNECTION_FAILURE,

        /** The connection pool is exhausted; no resource became available within the configured wait limit. */
        POOL_EXHAUSTED,

        /** An atomic transaction returned an unexpected result (null or insufficient responses). */
        TRANSACTION_ABORT,

        /** The remote store rejected the command or returned a structurally unexpected response. */
        SERVER_ERROR
    }

    private final Kind kind;

    public DistributedCounterException(String message, Throwable cause, Kind kind) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Returns the structured failure kind for fine-grained handling.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns {@code true} if this exception represents a transient infrastructure
     * failure ({@link Kind#CONNECTION_FAILURE} or {@link Kind#POOL_EXHAUSTED}) that
     * may succeed on retry. Returns {@code false} for permanent errors.
     */
    public boolean isTransient() {
        return kind == Kind.CONNECTION_FAILURE || kind == Kind.POOL_EXHAUSTED;
    }
}

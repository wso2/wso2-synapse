/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package org.apache.synapse.message.store.impl.jdbc;

import org.apache.synapse.MessageContext;
import org.apache.synapse.message.MessageConsumer;

import java.util.Map;

/**
 * JDBC Store Consumer
 */
public class JDBCConsumer implements MessageConsumer {
    /**
     * Store for the consumer
     */
    private JDBCMessageStore store;

    /**
     * Id of the consumer
     */
    private String idString;

    /**
     * Store current message index processing
     */
    private Long currentMessageIndex;

    /**
     * Initialize consumer
     *
     * @param store - JDBC message store
     */
    public JDBCConsumer(JDBCMessageStore store) {
        this.store = store;
    }

    /**
     * Select and return the first element in current table
     *
     * @return - Select and return the first element from the table
     */
    @Override
    public MessageContext receive() {
        // Message will get peeked from the table
        Map<Long, MessageContext> msgs = store.peek();
        if (msgs != null) {
            Map.Entry<Long, MessageContext> firstEntry = msgs.entrySet().iterator().next();
            currentMessageIndex = firstEntry.getKey();
            return firstEntry.getValue();
        } else {
            return null;
        }
    }

    /**
     * Ack on success message sending by processor
     *
     * @return Success of removing
     */
    @Override
    public boolean ack() {
        // Message will be removed at this point
        if (store.remove(currentMessageIndex)) {
            store.dequeued();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Cleanup the consumer
     *
     * @return Success of cleaning
     */
    @Override
    public boolean cleanup() {
        currentMessageIndex = 0L;
        return true;
    }

    /**
     * Set consumer id
     *
     * @param id ID
     */
    @Override
    public void setId(int id) {
        idString = "[" + store.getName() + "-C-" + id + "]";
    }

    /**
     * Get consumer id
     *
     * @return idString - Consumer identifier
     */
    @Override
    public String getId() {
        if (idString == null) {
            return "[unknown-consumer]";
        }
        return idString;
    }
}

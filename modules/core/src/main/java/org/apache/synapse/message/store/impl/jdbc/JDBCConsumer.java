/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.message.store.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.MessageConsumer;

/**
 * JDBC Store Consumer
 */
public class JDBCConsumer implements MessageConsumer {

    /**
     * Logger for the class
     */
    private static final Log logger = LogFactory.getLog(JDBCConsumer.class.getName());

    /**
     * Store for the consumer
     */
    private JDBCMessageStore store;

    /**
     * Id of the consumer
     */
    private String consumerId;

    /**
     * Store current message index processing
     */
    private String currentMessageId;

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
        MessageContext msg = null;
        try {
            msg = store.peek();
            if (msg != null) {
                currentMessageId = msg.getMessageID();
            }
        } catch (SynapseException e) {
            logger.error("Can't receive message ", e);
        }
        return msg;
    }

    /**
     * Ack on success message sending by processor
     *
     * @return Success of removing
     */
    @Override
    public boolean ack() {
        // Message will be removed at this point
        MessageContext msg = store.remove(currentMessageId);
        if (msg != null) {
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
        currentMessageId = null;
        return true;
    }

    /**
     * Set consumer id
     *
     * @param id ID
     */
    @Override
    public void setId(int id) {
        consumerId = "[" + store.getName() + "-C-" + id + "]";
    }

    /**
     * Get consumer id
     *
     * @return consumerId - Consumer identifier
     */
    @Override
    public String getId() {
        if (consumerId == null) {
            return "[unknown-consumer]";
        }
        return consumerId;
    }
}

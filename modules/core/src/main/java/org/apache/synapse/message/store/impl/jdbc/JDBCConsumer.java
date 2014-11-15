/**
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
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
    private String idString;

    /**
     * Initialize consumer
     *
     * @param store - JDBC message store
     */
    public JDBCConsumer(JDBCMessageStore store) {
        if (store == null) {
            logger.error("Cannot initialize.");
            return;
        }
        this.store = store;
    }

    /**
     * Select and return the first element in current table
     *
     * @return - Select and return the first element from the table
     */
    @Override
    public MessageContext receive() {
        // Message is completely removed from the table
        return store.poll();
    }

    /**
     * Ack on success message sending by processor
     *
     * @return
     */
    @Override
    public boolean ack() {
        // Message is already removed at this point
        return true;
    }

    /**
     * Cleanup the consumer
     *
     * @return
     */
    @Override
    public boolean cleanup() {
        // Nothing to cleanup from consumer side
        return true;
    }

    /**
     * Set consumer id
     *
     * @param i ID
     */
    @Override
    public void setId(int i) {
        idString = "[" + store.getName() + "-C-" + i + "]";
    }

    /**
     * Get consumer id
     *
     * @return
     */
    @Override
    public String getId() {
        if (idString == null) {
            return "[unknown-consumer]";
        }
        return idString;
    }
}

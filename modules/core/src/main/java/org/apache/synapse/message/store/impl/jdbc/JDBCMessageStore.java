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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.impl.jdbc.message.StorableMessage;
import org.apache.synapse.message.store.impl.jdbc.util.JDBCStorableMessageHelper;
import org.apache.synapse.message.store.impl.jdbc.util.JDBCUtil;
import org.apache.synapse.message.store.impl.jdbc.util.Statement;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JDBC Store class
 */
@SuppressWarnings("WeakerAccess")
public class JDBCMessageStore extends AbstractMessageStore {
    /**
     * Message Utility class used to provide utilities to do processing
     */
    private JDBCUtil jdbcUtil;

    /**
     * Logger for the class
     */
    private static final Log logger = LogFactory.getLog(JDBCMessageStore.class.getName());

    /**
     * Locks for clearing the store
     */
    private final ReentrantLock removeLock = new ReentrantLock();
    private final ReentrantLock cleanUpOfferLock = new ReentrantLock();

    /**
     * Initializes the JDBC Message Store
     *
     * @param synapseEnvironment SynapseEnvironment for the store
     */
    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        logger.debug("Initializing JDBC Message Store");
        super.init(synapseEnvironment);

        logger.debug("Initializing Datasource and Properties");
        jdbcUtil = new JDBCUtil();
        jdbcUtil.buildDataSource(parameters);

        JDBCStorableMessageHelper.setSynapseEnvironment(synapseEnvironment);
    }

    /**
     * @see org.apache.synapse.message.store.MessageStore#getProducer()
     */
    @Override
    public MessageProducer getProducer() {
        JDBCProducer producer = new JDBCProducer(this);
        producer.setId(nextProducerId());
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created a new JDBC Message Producer.");
        }
        return producer;
    }

    /**
     * @see org.apache.synapse.message.store.MessageStore#getConsumer()
     */
    @Override
    public MessageConsumer getConsumer() {
        JDBCConsumer consumer = new JDBCConsumer(this);
        consumer.setId(nextConsumerId());
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " created a new JDBC Message Consumer.");
        }
        return consumer;
    }

    /**
     * Set JDBC store parameters
     *
     * @param parameters - List of parameters to set
     */
    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);

        // Rebuild utils after setting new parameters
        if (jdbcUtil != null) {
            jdbcUtil.buildDataSource(parameters);
        }
    }

    /**
     * Process a given Statement object
     *
     * @param stmt - Statement to process
     * @return - Results as a List of MessageContexts
     */
    private Map<Long, MessageContext> processStatementWithResult(Statement stmt) {
        Map<Long, MessageContext> map = new HashMap<Long, MessageContext>();

        // Execute the prepared statement, and return list of messages as an ArrayList
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            ps = jdbcUtil.getPreparedStatement(stmt);
            int index = 1;
            for (Object param : stmt.getParameters()) {
                if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(index, (Integer) param);
                }
                index++;
            }
            con = ps.getConnection();
            rs = ps.executeQuery();
            while (rs.next()) {
                final Object msgObj;
                final Long msgIndex;
                try {
                    msgObj = rs.getObject("message");
                    msgIndex = rs.getLong("indexId");
                } catch (SQLException e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                                 " against DataSource : " + jdbcUtil.getDSName(), e);
                    break;
                }
                if (msgObj != null) {
                    try {
                        // Convert back to MessageContext and add to list
                        ObjectInputStream ios =
                                new ObjectInputStream(new ByteArrayInputStream((byte[]) msgObj));
                        Object msg = ios.readObject();
                        if (msg instanceof StorableMessage) {
                            StorableMessage jdbcMsg = (StorableMessage) msg;
                            map.put(msgIndex, JDBCStorableMessageHelper.createMessageContext(jdbcMsg));
                        }
                    } catch (Exception e) {
                        logger.error("Error reading object input stream", e);
                    }
                } else {
                    logger.error("Retrieved Object is null");
                }
            }
        } catch (SQLException e) {
            logger.error("Processing Statement failed : " + stmt.getRawStatement() +
                         " against DataSource : " + jdbcUtil.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return map;
    }

    /**
     * Process statements that do not give a ResultSet
     *
     * @param stmnt - Statement to process
     * @return - Success or Failure of the process
     */
    private boolean processStatementWithoutResult(Statement stmnt) {
        Connection con = null;
        boolean result = false;
        PreparedStatement ps = null;

        try {
            ps = jdbcUtil.getPreparedStatement(stmnt);
            int index = 1;
            for (Object param : stmnt.getParameters()) {
                if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else if (param instanceof StorableMessage) {
                    ps.setObject(index, param);
                }
                index++;
            }
            con = ps.getConnection();
            ps.execute();
            result = true;
        } catch (SQLException e) {
            logger.error("Processing Statement failed : " + stmnt.getRawStatement() +
                         " against DataSource : " + jdbcUtil.getDSName(), e);
            result = false;
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    logger.error("Error while closing prepared statement", e);
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    logger.error("Error while closing connection", e);
                }
            }
        }
        return result;
    }

    /**
     * Destroy Resources allocated
     */
    @Override
    public void destroy() {
        super.destroy();
        jdbcUtil = null;
    }

    /**
     * Add a message to the end of the table. If fetching success return true else false
     *
     * @param messageContext message to insert
     * @return -  success/failure of fetching
     */
    public synchronized boolean offer(MessageContext messageContext) {
        if (messageContext == null) {
            logger.error("Message is null, can't offer into database");
            return false;
        }
        try {
            cleanUpOfferLock.lock();
            StorableMessage persistentMessage =
                    JDBCStorableMessageHelper.createStorableMessage(messageContext);
            String msgId = persistentMessage.getAxis2Message().getMessageID();
            final Statement stmt =
                    new Statement("INSERT INTO " + jdbcUtil.getTableName() + " (msg_id,message) VALUES (?,?)");
            stmt.addParameter(msgId);
            stmt.addParameter(persistentMessage);
            return processStatementWithoutResult(stmt);
        } catch (Exception e) {
            logger.error("Error while creating StorableMessage", e);
            return false;
        } finally {
            cleanUpOfferLock.unlock();
        }
    }

    /**
     * Select and return the first element in current table
     *
     * @return - Select and return the first element from the table
     */
    public Map<Long, MessageContext> peek() {
        final Statement stmt =
                new Statement("SELECT indexId,message FROM " + jdbcUtil.getTableName() + " WHERE indexId=(SELECT min(indexId) from " + jdbcUtil.getTableName() + ")");
        Map<Long, MessageContext> result = processStatementWithResult(stmt);
        if (!result.isEmpty()) {
            return result;
        } else {
            logger.debug("No first element found !");
            return null;
        }
    }

    /**
     * Remove the given element from table
     *
     * @param index - Message Index
     * @return - success/failure of removing the element
     */
    public boolean remove(long index) {
        try {
            removeLock.lock();
            if (index != 0) {
                final Statement stmt =
                        new Statement("DELETE FROM " + jdbcUtil.getTableName() + " WHERE indexId=?");
                stmt.addParameter(Long.toString(index));
                return processStatementWithoutResult(stmt);
            }
        } catch (Exception ie) {
            logger.error("Message Cleanup lock released unexpectedly," +
                         "Message count value might show a wrong value ," +
                         "Restart the system to re sync the message count", ie);
        } finally {
            removeLock.unlock();
        }
        return false;
    }

    /**
     * Removes the first element from table
     *
     * @return MessageContext - first message context
     * @throws java.util.NoSuchElementException
     */
    @Override
    public MessageContext remove() throws NoSuchElementException {
        long minIdx = getMinTableIndex();
        MessageContext messageContext = peek().entrySet().iterator().next().getValue();
        boolean result = remove(minIdx);
        if (result) {
            return messageContext;
        } else {
            throw new NoSuchElementException("First element not found and remove failed !");
        }
    }

    /**
     * Delete all entries from table
     */
    @Override
    public void clear() {
        try {
            logger.warn(nameString() + "deleting all entries");
            removeLock.lock();
            cleanUpOfferLock.lock();
            Statement stmt = new Statement("DELETE FROM " + jdbcUtil.getTableName());
            processStatementWithoutResult(stmt);
        } catch (Exception ie) {
            logger.error("Acquiring lock failed !", ie);
        } finally {
            removeLock.unlock();
            cleanUpOfferLock.unlock();
        }
    }

    /**
     * Remove the message with given msg_id
     *
     * @param msgId - message ID
     * @return - removed message context
     */
    @Override
    public MessageContext remove(String msgId) {
        MessageContext result = null;
        try {
            removeLock.lock();
            result = get(msgId);
            Statement stmt = new Statement("DELETE FROM " + jdbcUtil.getTableName() + " WHERE msg_id=?");
            stmt.addParameter(msgId);
            processStatementWithoutResult(stmt);
        } catch (Exception ie) {
            logger.error("Acquiring lock failed !", ie);
        } finally {
            removeLock.unlock();
        }
        return result;
    }

    /**
     * Get the message at given position
     *
     * @param position - position of the message , starting value is 0
     * @return Message Context of position th row or if failed return null
     */
    @Override
    public MessageContext get(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Index:" + position + " out of table bound");
        }
        if (!this.getParameters().get(JDBCMessageStoreConstants.JDBC_CONNECTION_DRIVER).
                equals("com.mysql.jdbc.Driver")) {
            throw new UnsupportedOperationException("Only support in MYSQL");
        }
        // Gets the minimum value of the sub-table which contains indexId values greater than given position ('position' has minimum of 0 while indexId has minimum of 1)
        Statement stmt = new Statement("SELECT indexId,message FROM " + jdbcUtil.getTableName() + " ORDER BY indexId ASC LIMIT ?,1 ");
        stmt.addParameter(position);
        List<MessageContext> result = new ArrayList<MessageContext>(processStatementWithResult(stmt).values());
        if (result.size() > 0) {
            return result.get(0);
        } else {
            return null;
        }
    }

    /**
     * Get all messages in the table
     *
     * @return - List containing all message contexts in the store
     */
    @Override
    public List<MessageContext> getAll() {
        if (logger.isDebugEnabled()) {
            logger.debug(nameString() + " retrieving all messages from the store.");
        }
        Statement stmt = new Statement("SELECT indexId,message FROM " + jdbcUtil.getTableName());
        return new ArrayList<MessageContext>(processStatementWithResult(stmt).values());
    }

    /**
     * Return the first element with given msg_id
     *
     * @param msgId - Message ID
     * @return - returns the first result found else null
     */
    @Override
    public MessageContext get(String msgId) {
        Statement stmt = new Statement("SELECT indexId,message FROM " + jdbcUtil.getTableName() + " WHERE msg_id=?");
        stmt.addParameter(msgId);
        List<MessageContext> result = new ArrayList<MessageContext>(processStatementWithResult(stmt).values());
        if (result.size() > 0) {
            return result.get(0);
        } else {
            return null;
        }
    }

    /**
     * Return number of messages in the store
     *
     * @return size - Number of messages
     */
    @Override
    public int size() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        int size = 0;
        Statement stmt = new Statement("SELECT COUNT(*) FROM " + jdbcUtil.getTableName());
        try {
            ps = jdbcUtil.getPreparedStatement(stmt);
            con = ps.getConnection();
            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    size = rs.getInt(1);
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                                 " against DataSource : " + jdbcUtil.getDSName(), e);
                    break;
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                         " against DataSource : " + jdbcUtil.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return size;
    }

    /**
     * Get the maximum indexId value in the current table
     *
     * @return - maximum index value
     */
    private long getMinTableIndex() {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        long index = 0;
        Statement stmt = new Statement("SELECT min(indexId) FROM " + jdbcUtil.getTableName());
        try {
            ps = jdbcUtil.getPreparedStatement(stmt);
            con = ps.getConnection();
            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    index = rs.getLong(1);
                } catch (Exception e) {
                    logger.error("No Max indexId found in : " + jdbcUtil.getDSName(), e);
                    return 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Processing Statement failed : " + stmt.getRawStatement() +
                         " against DataSource : " + jdbcUtil.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return index;
    }

    /**
     * Get the store's name
     *
     * @return - name of the store
     */
    private String nameString() {
        return "Store [" + getName() + "]";
    }

    /**
     * Close all ResultSet related things
     *
     * @param con - Connection to close
     * @param ps  - PreparedStatement to close
     * @param rs  - ResultSet to close
     */
    private void close(Connection con, PreparedStatement ps, ResultSet rs) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.error("Error while closing prepared statement", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("Error while closing result set", e);
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                logger.error("Error while closing connection", e);
            }
        }
    }
}

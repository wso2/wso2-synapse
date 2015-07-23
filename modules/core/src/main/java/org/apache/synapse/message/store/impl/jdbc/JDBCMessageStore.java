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
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.impl.jdbc.message.StorableMessage;
import org.apache.synapse.message.store.impl.jdbc.util.JDBCConfiguration;
import org.apache.synapse.message.store.impl.jdbc.util.JDBCMessageConverter;
import org.apache.synapse.message.store.impl.jdbc.util.Statement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JDBC Store class
 */
public class JDBCMessageStore extends AbstractMessageStore {
    /**
     * Message Utility class used to provide utilities to do processing
     */
    private JDBCConfiguration jdbcConfiguration;

    /**
     * Logger for the class
     */
    private static final Log logger = LogFactory.getLog(JDBCMessageStore.class.getName());

    /**
     * Locks for clearing the store
     */
    private final ReentrantLock removeLock = new ReentrantLock();
    private final ReentrantLock cleanUpOfferLock = new ReentrantLock();
    private final AtomicBoolean cleaningFlag = new AtomicBoolean(false);

    /**
     * Initializes the JDBC Message Store
     *
     * @param synapseEnvironment SynapseEnvironment for the store
     */
    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing JDBC Message Store");
        }
        super.init(synapseEnvironment);

        if (logger.isDebugEnabled()) {
            logger.debug("Initializing Datasource and Properties");
        }
        jdbcConfiguration = new JDBCConfiguration();
        jdbcConfiguration.buildDataSource(parameters);

        JDBCMessageConverter.setSynapseEnvironment(synapseEnvironment);
    }

    /**
     * @see org.apache.synapse.message.store.MessageStore#getProducer()
     */
    @Override
    public MessageProducer getProducer() {
        JDBCProducer producer = new JDBCProducer(this);
        producer.setId(nextProducerId());
        if (logger.isDebugEnabled()) {
            logger.debug(getNameString() + " created a new JDBC Message Producer.");
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
            logger.debug(getNameString() + " created a new JDBC Message Consumer.");
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
        if (jdbcConfiguration != null) {
            jdbcConfiguration.buildDataSource(parameters);
        }
    }

    /**
     * Process a given Statement object
     *
     * @param stmt - Statement to process
     * @return - Results as a List of MessageContexts
     */
    private MessageContext processResultingStatement(Statement stmt) throws SynapseException {
        MessageContext resultMsg = null;

        // Execute the prepared statement, and return list of messages as an ArrayList
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            int index = 1;
            for (Object param : stmt.getParameters()) {
                if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(index, (Integer) param);
                }
                index++;
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                byte[] msgObj;
                try {
                    msgObj = rs.getBytes("message");
                } catch (SQLException e) {
                    throw new SynapseException("Error executing statement : " + stmt.getRawStatement() +
                                               " against DataSource : " + jdbcConfiguration.getDSName(), e);
                }
                if (msgObj != null) {
                    ObjectInputStream ios = null;
                    try {
                        // Convert back to MessageContext and add to list
                        ios = new ObjectInputStream(new ByteArrayInputStream(msgObj));
                        Object msg = ios.readObject();
                        if (msg instanceof StorableMessage) {
                            StorableMessage jdbcMsg = (StorableMessage) msg;
                            resultMsg = JDBCMessageConverter.createMessageContext(jdbcMsg);
                        }
                    } catch (Exception e) {
                        throw new SynapseException("Error reading object input stream", e);
                    } finally {
                        try {
                            ios.close();
                        } catch (IOException e) {
                            logger.error("Error while closing object input stream", e);
                        }
                    }
                } else {
                    throw new SynapseException("Retrieved Object is null");
                }
            }
        } catch (SQLException e) {
            throw new SynapseException("Processing Statement failed : " + stmt.getRawStatement() +
                                       " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return resultMsg;
    }

    /**
     * Process statements that do not give a ResultSet
     *
     * @param stmnt - Statement to process
     * @return - Success or Failure of the process
     */
    private boolean processNonResultingStatement(Statement stmnt) throws SynapseException {
        Connection con = null;
        boolean result = false;
        PreparedStatement ps = null;

        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmnt.getRawStatement());
            int index = 1;
            for (Object param : stmnt.getParameters()) {
                if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else if (param instanceof StorableMessage) {
                    //Serialize the object into byteArray and update the statement
                    ps.setBytes(index, serialize(param));
                }
                index++;
            }
            ps.execute();
            result = true;
        } catch (SQLException e) {
            throw new SynapseException("Processing Statement failed : " + stmnt.getRawStatement() +
                                       " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } catch(IOException ex) {
            throw new SynapseException("Processing Statement failed : " + stmnt.getRawStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), ex);
        }
        finally {
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

    public byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    /**
     * Destroy Resources allocated
     */
    @Override
    public void destroy() {
        super.destroy();
        jdbcConfiguration = null;
    }

    /**
     * Add a message to the end of the table. If fetching success return true else false
     *
     * @param messageContext message to insert
     * @return -  success/failure of fetching
     */
    public boolean store(MessageContext messageContext) throws SynapseException {
        if (messageContext == null) {
            logger.error("Message is null, can't store into database");
            return false;
        }
        boolean cleaningState = false;
        try {
            if (cleaningFlag.get()) {
                try {
                    cleanUpOfferLock.lock();
                    cleaningState = true;
                } catch (Exception e) {
                    logger.error("Message Cleanup lock released unexpectedly", e);
                }
            }
            StorableMessage persistentMessage =
                    JDBCMessageConverter.createStorableMessage(messageContext);
            String msgId = persistentMessage.getAxis2Message().getMessageID();
            Statement stmt =
                    new Statement("INSERT INTO " + jdbcConfiguration.getTableName() + " (msg_id,message) VALUES (?,?)");
            stmt.addParameter(msgId);
            stmt.addParameter(persistentMessage);
            return processNonResultingStatement(stmt);
        } catch (Exception e) {
            throw new SynapseException("Error while creating StorableMessage", e);
        } finally {
            if (cleaningState) {
                cleanUpOfferLock.unlock();
            }
        }
    }

    /**
     * Select and return the first element in current table
     *
     * @return - Select and return the first element from the table
     */
    public MessageContext peek() throws SynapseException {
        Statement stmt =
                new Statement("SELECT message FROM " + jdbcConfiguration.getTableName() +
                              " WHERE indexId=(SELECT min(indexId) from " + jdbcConfiguration.getTableName() + ")");
        MessageContext msg = null;

        try {
            msg = processResultingStatement(stmt);
        } catch (SynapseException se) {
            throw new SynapseException("Error while peek the message", se);
        }
        return msg;
    }

    /**
     * Removes the first element from table
     *
     * @return MessageContext - first message context
     * @throws java.util.NoSuchElementException
     */
    @Override
    public MessageContext remove() throws NoSuchElementException {
        MessageContext messageContext = peek();
        messageContext = remove(messageContext.getMessageID());
        if (messageContext != null) {
            return messageContext;
        } else {
            throw new NoSuchElementException("First element not found and remove failed !");
        }
    }

    /**
     * Remove the message with given msg_id
     *
     * @param msgId - message ID
     * @return - removed message context
     */
    @Override
    public MessageContext remove(String msgId) throws SynapseException {
        MessageContext result = null;
        boolean cleaningState = false;
        try {
            if (cleaningFlag.get()) {
                try {
                    removeLock.lock();
                    cleaningState = true;
                } catch (Exception ie) {
                    logger.error("Message Cleanup lock released unexpectedly", ie);
                }
            }
            result = get(msgId);
            Statement stmt = new Statement("DELETE FROM " + jdbcConfiguration.getTableName() + " WHERE msg_id=?");
            stmt.addParameter(msgId);
            processNonResultingStatement(stmt);
        } catch (Exception e) {
            throw new SynapseException("Removing message with id = " + msgId + " failed !", e);
        } finally {
            if (cleaningState) {
                removeLock.unlock();
            }
        }
        return result;
    }

    /**
     * Delete all entries from table
     */
    @Override
    public void clear() {
        try {
            logger.warn(getNameString() + "deleting all entries");
            removeLock.lock();
            cleanUpOfferLock.lock();
            cleaningFlag.set(true);
            Statement stmt = new Statement("DELETE FROM " + jdbcConfiguration.getTableName());
            processNonResultingStatement(stmt);
        } catch (Exception e) {
            logger.error("Clearing store failed !", e);
        } finally {
            cleaningFlag.set(false);
            removeLock.unlock();
            cleanUpOfferLock.unlock();
        }
    }

    /**
     * Get the message at given position
     * Only can be done with MYSQL, and no use-case in current implementation
     *
     * @param position - position of the message , starting value is 0
     * @return Message Context of position th row or if failed return null
     */
    @Override
    public MessageContext get(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Index:" + position + " out of table bound");
        }
        // Gets the minimum value of the sub-table which contains indexId values greater than given position ('position' has minimum of 0 while indexId has minimum of 1)
        Statement stmt = new Statement("SELECT message FROM " + jdbcConfiguration.getTableName() + " ORDER BY indexId ASC LIMIT ?,1 ");
        stmt.addParameter(position);
        return processResultingStatement(stmt);
    }

    /**
     * Get all messages in the table
     *
     * @return - List containing all message contexts in the store
     */
    @Override
    public List<MessageContext> getAll() {
        if (logger.isDebugEnabled()) {
            logger.debug(getNameString() + " retrieving all messages from the store.");
        }
        Statement stmt = new Statement("SELECT message FROM " + jdbcConfiguration.getTableName());
        MessageContext result = processResultingStatement(stmt);
        if (result != null) {
            List<MessageContext> msgs = new ArrayList<MessageContext>();
            msgs.add(result);
            return msgs;
        } else {
            return null;
        }
    }

    /**
     * Return the first element with given msg_id
     *
     * @param msgId - Message ID
     * @return - returns the first result found else null
     */
    @Override
    public MessageContext get(String msgId) {
        Statement stmt = new Statement("SELECT indexId,message FROM " + jdbcConfiguration.getTableName() + " WHERE msg_id=?");
        stmt.addParameter(msgId);
        return processResultingStatement(stmt);
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
        Statement stmt = new Statement("SELECT COUNT(*) FROM " + jdbcConfiguration.getTableName());
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(stmt.getRawStatement());
            con = ps.getConnection();
            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    size = rs.getInt(1);
                } catch (Exception e) {
                    logger.error("Error executing statement : " + stmt.getRawStatement() +
                                 " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing statement : " + stmt.getRawStatement() +
                         " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return size;
    }

    /**
     * Get the store's name
     *
     * @return - name of the store
     */
    private String getNameString() {
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

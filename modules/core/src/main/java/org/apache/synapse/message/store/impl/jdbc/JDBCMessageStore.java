/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.MessageProducer;
import org.apache.synapse.message.store.AbstractMessageStore;
import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;
import org.apache.synapse.message.store.impl.jdbc.util.JDBCConfiguration;
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
import java.util.HashMap;
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
    protected static final String MESSAGE_COLUMN_NAME = "message";

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

//        JDBCMessageConverter.setSynapseEnvironment(synapseEnvironment);
    }

    protected JDBCConfiguration getJdbcConfiguration() {
        return jdbcConfiguration;
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
     * Process a given SQL statement.
     *
     * @param statement - Statement to process.
     * @return - MessageContext which will hold the content of the message.
     */
    private MessageContext getResultMessageContextFromDatabase(Statement statement) throws SynapseException {
        List<Map> processedRows = getProcessedRows(statement);
        final int firstRowIndex = 0;
        MessageContext messageContext = null;
        if (processedRows.size() > 0) {
            Map messageContentRow = processedRows.get(firstRowIndex);
            messageContext = (MessageContext) messageContentRow.get(MESSAGE_COLUMN_NAME);
            if(logger.isDebugEnabled()){
                logger.debug("Number of rows processed:"+processedRows+" calling the statement "
                        +statement.getStatement());
                logger.debug("Message content with mid:"+messageContext.getMessageID()+" will be returned");
            }
        }
        return messageContext;
    }

    /**
     * Will return the list of processed message rows.
     *
     * @param statement the statement executed in the DB.
     * @return the rows which contains the column data wrapped inside a map.
     */
    protected List<Map> getProcessedRows(Statement statement) {
        Connection con = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        List<Map> elements;
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(statement.getStatement());
            int index = 1;
            for (Object param : statement.getParameters()) {
                if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else if (param instanceof Long) {
                    ps.setLong(index, (Long) param);
                } else if (param instanceof Integer) {
                    ps.setInt(index, (Integer) param);
                }
                index++;
            }
            rs = ps.executeQuery();
            elements = statement.getResult(rs);
        } catch (SQLException e) {
            throw new SynapseException("Processing Statement failed : " + statement.getStatement() +
                    " against DataSource : " + jdbcConfiguration.getDSName(), e);
        } finally {
            close(con, ps, rs);
        }
        return elements;
    }

    /**
     * Will convert the byte[] message to store-able message.
     *
     * @param msgObj serialized message read from the database.
     * @return converted message context.
     */
    protected MessageContext deserializeMessage(byte[] msgObj) {
        MessageContext messageContext = null;
        if (msgObj != null) {
            ObjectInputStream ios = null;
            try {
                // Convert back to MessageContext and add to list
                ios = new ObjectInputStream(new ByteArrayInputStream(msgObj));
                Object msg = ios.readObject();
                if (msg instanceof StorableMessage) {
                    StorableMessage jdbcMsg = (StorableMessage) msg;
                    org.apache.axis2.context.MessageContext axis2Mc = this.newAxis2Mc();
                    MessageContext synapseMc = this.newSynapseMc(axis2Mc);
                    messageContext = MessageConverter.toMessageContext(jdbcMsg, axis2Mc, synapseMc);
                }
            } catch (IOException e) {
                throw new SynapseException("Error reading object input stream", e);
            } catch (ClassNotFoundException e) {
                throw new SynapseException("Could not find the class", e);
            } finally {
                if (ios != null) {
                    closeStream(ios);
                }
            }
        } else {
            throw new SynapseException("Retrieved Object is null");
        }
        return messageContext;
    }

    /**
     * Closes the object stream after completing the DB operations.
     *
     * @param ios stream which should be closed.
     */
    private void closeStream(ObjectInputStream ios) {
        try {
            ios.close();
        } catch (IOException e) {
            logger.error("Error while closing object input stream", e);
        }
    }

    private org.apache.axis2.context.MessageContext newAxis2Mc() {
        return ((Axis2SynapseEnvironment) synapseEnvironment)
                .getAxis2ConfigurationContext().createMessageContext();
    }

    private org.apache.synapse.MessageContext newSynapseMc(
            org.apache.axis2.context.MessageContext msgCtx) {
        SynapseConfiguration configuration = synapseEnvironment.getSynapseConfiguration();
        return new Axis2MessageContext(msgCtx, configuration, synapseEnvironment);
    }

    /**
     * On database update failure tries to rollback
     *
     * @param connection database connection
     * @param task       explanation of the task done when the rollback was triggered
     */
    private void rollback(Connection connection, String task) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                logger.warn("Rollback failed on " + task, e);
            }
        }
    }

    /**
     * Process statements that do not give a ResultSet
     *
     * @param statements - Statement to process
     * @return - Success or Failure of the process
     */
    private boolean processNonResultingStatement(List<Statement> statements) throws SynapseException {
        Connection connection = null;
        boolean result;
        PreparedStatement preparedStatement = null;
        try {
            connection = jdbcConfiguration.getConnection();
            connection.setAutoCommit(false);
            for(Statement statement : statements) {
                preparedStatement = connection.prepareStatement(statement.getStatement());
                int index = 1;
                for (Object param : statement.getParameters()) {
                    if (param instanceof String) {
                        preparedStatement.setString(index, (String) param);
                    } else if (param instanceof Long) {
                        preparedStatement.setLong(index, (Long) param);
                    } else if (param instanceof StorableMessage) {
                        //Serialize the object into byteArray and update the statement
                        preparedStatement.setBytes(index, serialize(param));
                    }
                    index++;
                }
                if(logger.isDebugEnabled()){
                    logger.debug("Executing statement:"+preparedStatement);
                }
                preparedStatement.execute();
            }
            connection.commit();
            result = true;
        } catch (SQLException | IOException e) {
            rollback(connection,"deleting message");
            throw new SynapseException("Processing Statement failed against DataSource : "
                    + jdbcConfiguration.getDSName(), e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.error("Error while closing prepared statement", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
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
            ArrayList<Statement> statements = new ArrayList<>();
            Statement statement = getStoreMessageStatement(messageContext, null);
            statements.add(statement);
            return processNonResultingStatement(statements);
        } catch (Exception e) {
            throw new SynapseException("Error while creating StorableMessage", e);
        } finally {
            if (cleaningState) {
                cleanUpOfferLock.unlock();
            }
        }
    }

    /**
     * <p>
     * Generates the statement to store message in database.
     * </p>
     * <p>
     * If the sequence id is specified the corresponding sequence id will be stored, sequence id will be specified if
     * re-sequence message store is being used. In other times this value will be null.
     * </p>
     *
     * @param messageContext the content of the message.
     * @param sequenceId        the sequence id of the message (optional).
     * @return SQL statement for insertion of value to store.
     * @throws StoreException at an event there's an exception when generating the statement.
     * @see org.apache.synapse.message.store.impl.resequencer.ResequenceMessageStore
     */
    protected Statement getStoreMessageStatement(MessageContext messageContext, Long sequenceId) throws StoreException {
        StorableMessage persistentMessage = MessageConverter.toStorableMessage(messageContext);
        String msgId = persistentMessage.getAxis2message().getMessageID();
        Statement statement;
        if (null == sequenceId) {
            String insertMessageStatement = "INSERT INTO " + jdbcConfiguration.getTableName()
                    + " (msg_id,message) VALUES (?,?)";
            statement = new Statement(insertMessageStatement) {
                @Override
                public List<Map> getResult(ResultSet resultSet) {
                    throw new UnsupportedOperationException();
                }
            };
            statement.addParameter(msgId);
            statement.addParameter(persistentMessage);
        } else {
            String insertMessageWithSequenceIdStatement =
                    "INSERT INTO " + jdbcConfiguration.getTableName() + " (msg_id,seq_id,message) VALUES (?,?,?)";
            statement = new Statement(insertMessageWithSequenceIdStatement) {
                @Override
                public List<Map> getResult(ResultSet resultSet) {
                    throw new UnsupportedOperationException();
                }
            };
            statement.addParameter(msgId);
            statement.addParameter(sequenceId);
            statement.addParameter(persistentMessage);
        }
        return statement;
    }

    /**
     * Select and return the first element in current table
     *
     * @return - Select and return the first element from the table
     */
    public MessageContext peek() throws SynapseException {
        MessageContext msg;
        try {
        Statement statement = new Statement("SELECT message FROM " + jdbcConfiguration.getTableName() +
                        " WHERE indexId=(SELECT min(indexId) from " + jdbcConfiguration.getTableName() + ")") {
                    @Override
                    public List<Map> getResult(ResultSet resultSet) throws SQLException {
                        return messageContentResultSet(resultSet, this.getStatement());
                    }
                };
            msg = getResultMessageContextFromDatabase(statement);
        } catch (SynapseException se) {
            throw new SynapseException("Error while peek the message", se);
        }
        return msg;
    }

    /**
     * Removes the first element from table
     *
     * @return MessageContext - first message context
     * @throws NoSuchElementException if there was not element to be removed.
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
        MessageContext result;
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
            List<Statement> statements = removeMessageStatement(msgId);
            processNonResultingStatement(statements);
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
     * Statement to remove the message once a response is received.
     *
     * @param msgId message id of the statement which should be removed.
     * @return the sql remove message statement.
     */
    protected List<Statement> removeMessageStatement(String msgId) {
        ArrayList<Statement> statements = new ArrayList<>();
        Statement statement = new Statement("DELETE FROM " + jdbcConfiguration.getTableName() + " WHERE msg_id=?") {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };
        statement.addParameter(msgId);
        statements.add(statement);
        return statements;
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
            Statement statement = new Statement("DELETE FROM " + jdbcConfiguration.getTableName()) {
                @Override
                public List<Map> getResult(ResultSet resultSet) throws SQLException {
                    throw new UnsupportedOperationException();
                }
            };
            List<Statement> statements = new ArrayList<>();
            statements.add(statement);
            processNonResultingStatement(statements);
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
        // Gets the minimum value of the sub-table which contains indexId values greater than given position
        // ('position' has minimum of 0 while indexId has minimum of 1)
        Statement statement = new Statement("SELECT message FROM " + jdbcConfiguration.getTableName()
                + " ORDER BY indexId ASC LIMIT ?,1 ") {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return messageContentResultSet(resultSet, this.getStatement());
            }
        };
        statement.addParameter(position);
        return getResultMessageContextFromDatabase(statement);
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
        Statement statement = new Statement("SELECT message FROM " + jdbcConfiguration.getTableName()) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return messageContentResultSet(resultSet, this.getStatement());
            }
        };
        MessageContext result = getResultMessageContextFromDatabase(statement);
        if (result != null) {
            List<MessageContext> msgs = new ArrayList<>();
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
        Statement statement = new Statement("SELECT indexId,message FROM " + jdbcConfiguration.getTableName()
                + " WHERE msg_id=?") {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return messageContentResultSet(resultSet, this.getStatement());
            }
        };
        statement.addParameter(msgId);
        return getResultMessageContextFromDatabase(statement);
    }

    /**
     *<p>
     * Return the messages corresponding to the provided statement.
     *</p>
     *
     * @param resultSet the result-set obtained from the statement.
     * @param statement the SQL statement results are obtained for.
     * @return the content of the messages.
     * @throws SQLException during an error encountered when accessing the database.
     */
    protected List<Map> messageContentResultSet(ResultSet resultSet, String statement) throws SQLException {
        ArrayList<Map> elements = new ArrayList<>();
        while (resultSet.next()) {
            try {
                HashMap<String, Object> rowData = new HashMap<>();
                byte[] msgObj = resultSet.getBytes(MESSAGE_COLUMN_NAME);
                MessageContext responseMessageContext = deserializeMessage(msgObj);
                rowData.put(MESSAGE_COLUMN_NAME, responseMessageContext);
                elements.add(rowData);
            } catch (SQLException e) {
                String message = "Error executing statement : " + statement + " against DataSource : "
                        + jdbcConfiguration.getDSName();
                throw new SynapseException(message, e);
            }
        }
        return elements;
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
        Statement statement = new Statement("SELECT COUNT(*) FROM " + jdbcConfiguration.getTableName()) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return messageContentResultSet(resultSet, this.getStatement());
            }
        };
        try {
            con = jdbcConfiguration.getConnection();
            ps = con.prepareStatement(statement.getStatement());
            rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    size = rs.getInt(1);
                } catch (Exception e) {
                    logger.error("Error executing statement : " + statement.getStatement() +
                            " against DataSource : " + jdbcConfiguration.getDSName(), e);
                    break;
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing statement : " + statement.getStatement() +
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

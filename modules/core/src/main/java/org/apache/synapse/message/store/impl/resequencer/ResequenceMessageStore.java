/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.synapse.message.store.impl.resequencer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.message.store.impl.jdbc.JDBCMessageStore;
import org.apache.synapse.message.store.impl.jdbc.StoreException;
import org.apache.synapse.message.store.impl.jdbc.util.Statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * This represents the store which will allow to re-sequence messages.
 * </p>
 *
 * @see JDBCMessageStore
 */
public class ResequenceMessageStore extends JDBCMessageStore {
    /**
     * Logger for the class
     */
    private static final Log log = LogFactory.getLog(ResequenceMessageStore.class);

    /**
     * Number of milliseconds for a second
     */
    private static final int MILLISECONDS = 1000;

    /**
     * The value of the next sequence id which should be processed.
     */
    private long nextSequenceId = 0;

    /**
     * The xpath expression evaluated to identify sequence.
     */
    private SynapsePath xPath;

    /**
     * Maximum number of time the sequence should wait for gap detection.
     */
    private long gapTimeoutInterval;

    /**
     * The number of counts elapsed as waiting for gap.
     */
    private long nextElapsedTime;

    /**
     * <p>
     * Indicates whether the processor was started.
     * </p>
     * <p>
     * This will be used when clustering, if the processor has not started before, the message will be retrieved from
     * last processed id table.
     * </p>
     */
    private boolean hasStarted = false;

    /**
     * <p>
     * There could be situations where the sequence id could duplicate, hence there will be a unique message id
     * maintained.
     * </p>
     * <p>
     * Will co-relate between the message-id and the sequence-id.
     * </p>
     * key - message id value.
     * value - sequence id value.
     */
    private ConcurrentHashMap<String, Long> sequenceIdMapper = new ConcurrentHashMap<>();


    /**
     * <p>
     * Returns the start id indicated in the DB.
     * </p>
     *
     * @param resultSet the result returned from the DB query.
     * @return the result serialized into DB.
     * @throws SQLException if an error is encountered while accessing the DB.
     */
    private List<Map> startIdSelectionResult(ResultSet resultSet) throws SQLException {
        ArrayList<Map> elements = new ArrayList<>();
        while (resultSet.next()) {
            HashMap<String, Object> rowData = new HashMap<>();
            long sequenceId = resultSet.getLong(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN);
            rowData.put(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN, sequenceId);
            elements.add(rowData);
            if (log.isDebugEnabled()) {
                log.debug("DB returned " + sequenceId + " as the result");
            }
        }
        return elements;
    }

    /**
     * <p>
     * This method should be called at the start of the store.
     * </p>
     * <p>
     * Will read from the DB and identify the id which should be processed.
     * </p>
     *
     * @return the start id of the store.
     */
    private long readStartId() {
        Long sequenceId = 0L;
        final int minimumRowCount = 0;
        String storeName = this.getName();
        final String lastProcessIdSelectStatement = "SELECT "+ResequenceMessageStoreConstants.SEQ_ID+" FROM " +
                ResequenceMessageStoreConstants.LAST_PROCESS_ID_TABLE_NAME + " WHERE " +
                ResequenceMessageStoreConstants.STATEMENT_COLUMN + "=" + "\"" + storeName + "\"";

        Statement statement = new Statement(lastProcessIdSelectStatement) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return startIdSelectionResult(resultSet);
            }
        };
        List<Map> processedRows = getProcessedRows(statement);
        if (processedRows.size() > minimumRowCount) {
            final int firstIndex = 0;
            Map processedRowMap = processedRows.get(firstIndex);
            sequenceId = (Long) processedRowMap.get(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN);
            log.info("Starting sequence id recorded as:" + sequenceId);
        }
        return sequenceId;
    }

    /**
     * Initializes the sequence xPath value.
     *
     * @param parameters the list of parameters defined in the configuration.
     */
    private void initResequenceParams(Map<String, Object> parameters) {
        xPath = (SynapsePath) parameters.get(ResequenceMessageStoreConstants.SEQUENCE_NUMBER_XPATH);
        gapTimeoutInterval = Integer.parseInt((String) parameters.get(ResequenceMessageStoreConstants
                .MAX_NUMBER_OF_WAITING_COUNT));
        //Convert the gap time into milliseconds
        if (gapTimeoutInterval >= 0) {
            gapTimeoutInterval = gapTimeoutInterval * MILLISECONDS;
            nextElapsedTime = System.currentTimeMillis() + gapTimeoutInterval;
            if (log.isDebugEnabled()) {
                log.debug("Resequencer initialized with xpath:" + xPath.expression + ",the waiting count configured:"
                        + gapTimeoutInterval);
            }
        } else {
            nextElapsedTime = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameters(Map<String, Object> parameters) {
        initResequenceParams(parameters);
        super.setParameters(parameters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        super.init(synapseEnvironment);
        nextSequenceId = readStartId() + 1;
        if (log.isDebugEnabled()) {
            log.debug("Next sequence which will be processed:" + nextSequenceId);
        }
    }

    /**
     * Extracts the sequence id from the message context.
     *
     * @param message the message context.
     * @return sequence id of the message.
     */
    private Long getMessageSequenceId(MessageContext message) throws StoreException {
        String sequenceIdValue;
        sequenceIdValue = xPath.stringValueOf(message);
        if (log.isDebugEnabled()) {
            log.debug("Sequence id extracted from the incoming message " + message.getMessageID() + " is:"
                    + sequenceIdValue);
        }
        return Long.parseLong(sequenceIdValue);
    }

    /**
     * Will get the next message belonging to a sequence.
     *
     * @return the next message in the sequence.
     */
    private MessageContext getNextMessage() {
        MessageContext msg = null;
        final int firstRowIndex = 0;
        try {
            String tableName = getJdbcConfiguration().getTableName();
            String selectMessageStatement = "SELECT message FROM " + tableName + " WHERE "
                    + ResequenceMessageStoreConstants.SEQ_ID + "= ?";
            Statement statement = new Statement(selectMessageStatement) {
                @Override
                public List<Map> getResult(ResultSet resultSet) throws SQLException {
                    return messageContentResultSet(resultSet, this.getStatement());
                }
            };
            statement.addParameter(nextSequenceId);
            List<Map> processedRows = getProcessedRows(statement);
            if (!processedRows.isEmpty()) {
                msg = getMessageContext(processedRows, firstRowIndex);
                nextSequenceId++;
                if (log.isTraceEnabled()) {
                    log.trace("Message with id " + msg.getMessageID() + " returned for sequence " + nextSequenceId);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Sequences not returned from DB, next sequence will be:" + nextSequenceId);
                }
            }
        } catch (SynapseException ex) {
            throw new SynapseException("Error while peek the message", ex);
        }
        return msg;
    }

    /**
     * <p>
     * Remove message statement.
     * </p>
     * <p>
     * When re-sequenced we need to maintain the last processed id along with the removal. So that at an event the node
     * crashes we know where to start from.
     * </p>
     * <p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected List<Statement> removeMessageStatement(String msgId) {
        Long messageSequenceId = sequenceIdMapper.remove(msgId);
        String messageStoreName = this.getName();
        if (messageSequenceId == null) {
            log.error("The message with id " + msgId + " is not tracked within the memory.");
        }
        ArrayList<Statement> statements = new ArrayList<>();
        final String deleteMessageStatement = "DELETE FROM " + getJdbcConfiguration().getTableName()
                + " WHERE msg_id=?";
        final String insertLastProcessIdStatement = "INSERT INTO " +
                ResequenceMessageStoreConstants.LAST_PROCESS_ID_TABLE_NAME
                + " (statement,seq_id) VALUES (?,?) ON DUPLICATE KEY UPDATE seq_id = ?";
        Statement sequenceIdUpdateStatement = new Statement(insertLastProcessIdStatement) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };
        Statement deleteMessage = new Statement(deleteMessageStatement) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };
        deleteMessage.addParameter(msgId);
        sequenceIdUpdateStatement.addParameter(messageStoreName);
        sequenceIdUpdateStatement.addParameter(messageSequenceId);
        sequenceIdUpdateStatement.addParameter(messageSequenceId);
        statements.add(deleteMessage);
        statements.add(sequenceIdUpdateStatement);
        if (log.isDebugEnabled()) {
            log.debug("Removing message with id:" + msgId + " and last process id:" + messageSequenceId);
        }
        return statements;
    }

    /**
     * Identify the message context from the processed rows.
     *
     * @param processedRows the processed row value.
     * @return the message context represented in the corresponding row.
     */
    private MessageContext getMessageContext(List<Map> processedRows, int rowIndex) {
        Map map = processedRows.get(rowIndex);
        return (MessageContext) map.get(MESSAGE_COLUMN_NAME);
    }

    /**
     * Get the sequence id belonging to the column.
     *
     * @param processedRows the list of processed rows.
     * @param rowIndex      the index of the row which should be retrieved.
     * @return the sequence id of the message.
     */
    private long getSequenceId(List<Map> processedRows, int rowIndex) {
        Map map = processedRows.get(rowIndex);
        return (long) map.get(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN);
    }

    /**
     * <p>
     * Gets message with minimum sequence id.
     * </p>
     *
     * @param resultSet the results returned from the query.
     * @param statement statement which is executed to obtain the results.
     * @return message which has the minimum sequence id.
     * @throws SQLException if an error is returned from the db while obtaining the sequence id value.
     */
    private List<Map> getMessageWithMinimumId(ResultSet resultSet, String statement) throws SQLException {
        ArrayList<Map> elements = new ArrayList<>();
        while (resultSet.next()) {
            try {
                HashMap<String, Object> rowData = new HashMap<>();
                byte[] msgObj = resultSet.getBytes(MESSAGE_COLUMN_NAME);
                MessageContext responseMessageContext = deserializeMessage(msgObj);
                rowData.put(MESSAGE_COLUMN_NAME, responseMessageContext);
                long sequenceId = resultSet.getLong(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN);
                rowData.put(ResequenceMessageStoreConstants.SEQUENCE_ID_COLUMN, sequenceId);
                elements.add(rowData);
            } catch (SQLException e) {
                String message = "Error executing statement : " + statement + " against " + "DataSource : "
                        + getJdbcConfiguration().getDSName();
                throw new SynapseException(message, e);
            }
        }
        return elements;
    }

    /**
     * <p>
     * Retrieve the minimum sequence number of the available sequence ids in the DB
     * </p>
     * <p>
     * This operation should call when there's a gap and a timeout occurs.
     * </p>
     * <p>
     * <b>Note : </b> This operation would reset the "nextSequenceId" to the minimum sequence id generated from the
     * DB.
     * </p>
     *
     * @return the message context of the next sequence
     */
    private MessageContext getMessageWithMinimumSequence() {
        String tableName = getJdbcConfiguration().getTableName();
        String selectMinimumSequenceIdStatement = "SELECT message,seq_id FROM " + tableName + " WHERE "
                + ResequenceMessageStoreConstants.SEQ_ID + "=(SELECT min("
                + ResequenceMessageStoreConstants.SEQ_ID + ")" + " from " + tableName + ")";
        Statement stmt = new Statement(selectMinimumSequenceIdStatement) {
            @Override
            public List<Map> getResult(ResultSet resultSet) throws SQLException {
                return getMessageWithMinimumId(resultSet, this.getStatement());
            }
        };
        MessageContext msg = null;
        final int firstRowIndex = 0;
        try {
            List<Map> processedRows = getProcessedRows(stmt);
            if (!processedRows.isEmpty()) {
                msg = getMessageContext(processedRows, firstRowIndex);
                long sequenceId = getSequenceId(processedRows, firstRowIndex);
                nextSequenceId = sequenceId + 1;
                if (log.isTraceEnabled()) {
                    log.trace("Message with id " + msg.getMessageID() + " returned as the minimum, the minimum " +
                            "sequence " + "will be marked as " + nextSequenceId);
                }
            }
        } catch (SynapseException ex) {
            throw new SynapseException("Error while peek the message", ex);
        }
        return msg;
    }

    /**
     * <p>
     * Stores message in database by providing the correct sequence id.
     * </p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected Statement getStoreMessageStatement(MessageContext context, Long sequenceId) throws StoreException {
        Statement storeMessageStatement;
        sequenceId = getMessageSequenceId(context);
        storeMessageStatement = super.getStoreMessageStatement(context, sequenceId);
        return storeMessageStatement;
    }

    /**
     * <p>
     * Specifies whether the store should wait instead of processing the message.
     * </p>
     * <p>
     * This is called if a gap is being detected. The condition would be if the maximum number of peeks have breached.
     * </p>
     *
     * @return true if the processor should wait.
     */
    private boolean shouldWait() {
        long currentTime = System.currentTimeMillis();
        return nextElapsedTime < 0 || currentTime <= nextElapsedTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageContext peek() throws SynapseException {
        MessageContext msg;
        if(!hasStarted){
             nextSequenceId = readStartId() + 1;
             hasStarted = true;
        }
        msg = getNextMessage();
        if (null == msg && !shouldWait()) {
            msg = getMessageWithMinimumSequence();
        }
        if (null != msg) {
            long currentSequenceId = nextSequenceId - 1;
            String messageId = msg.getMessageID();
            sequenceIdMapper.put(messageId, currentSequenceId);
            if (nextElapsedTime > 0) {
                nextElapsedTime = System.currentTimeMillis() + gapTimeoutInterval;
            }
            if (log.isDebugEnabled()) {
                log.debug("Message with sequence " + currentSequenceId + " and message id " + messageId
                        + " will be returned to the processor.");
                log.debug("Next elapsed time would be marked as:"+ nextElapsedTime);
            }
        }
        return msg;
    }
}

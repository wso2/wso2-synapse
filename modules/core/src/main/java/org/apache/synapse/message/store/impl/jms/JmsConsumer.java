/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.message.store.impl.jms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.MessageConsumer;
import org.apache.synapse.message.StoreForwardException;
import org.apache.synapse.message.processor.MessageProcessorConstants;
import org.apache.synapse.message.store.Constants;
import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

public class JmsConsumer implements MessageConsumer {

    private static final Log logger = LogFactory.getLog(JmsConsumer.class.getName());

    private Connection connection;

    private Session session;

    private javax.jms.MessageConsumer consumer;

    private JmsStore store;

    private String idString;

    private boolean isInitialized;

    /** Holds the last message read from the message store. */
    private CachedMessage cachedMessage;

    /** Did last receive() call cause an error? */
    private boolean isReceiveError;

    /**
     * Constructor for JMS consumer
     *
     * @param store JMSStore associated to this JMS consumer
     */
    public JmsConsumer(JmsStore store) {
        if (store == null) {
            logger.error("Cannot initialize.");
            return;
        }
        this.store = store;
        cachedMessage = new CachedMessage();
        isReceiveError = false;
        isInitialized = true;
    }

    public MessageContext receive() {

        boolean connectionSuccess = checkAndTryConnect();

        if (!connectionSuccess) {
            throw new SynapseException(idString + "Error while connecting to JMS provider. "
                    + MessageProcessorConstants.STORE_CONNECTION_ERROR);
        }

        try {
            Message message = consumer.receive(1000);
            if (message == null) {
                return null;
            }
            if (!(message instanceof ObjectMessage)) {
                logger.warn("JMS Consumer " + getId() + " did not receive a javax.jms.ObjectMessage");
                //we just discard this message as we only store Object messages via JMS Message store
                message.acknowledge();
                return null;
            }
            ObjectMessage msg = (ObjectMessage) message;
            String messageId = msg.getStringProperty(Constants.OriginalMessageID);
            if (!(msg.getObject() instanceof StorableMessage)) {
                logger.warn("JMS Consumer " + getId() + " did not receive a valid message.");
                message.acknowledge();
                return null;
            }

            //create a ,essage context back from the stored message
            StorableMessage storableMessage = (StorableMessage) msg.getObject();
            org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
            MessageContext synapseMc = store.newSynapseMc(axis2Mc);
            synapseMc = MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);

            //cache the message
            updateCache(message, synapseMc, messageId, false);

            if (logger.isDebugEnabled()) {
                logger.debug(getId() + " Received MessageId:" + messageId + " priority:" + message.getJMSPriority());
            }

            return synapseMc;

        } catch (JMSException e) {
            logger.error("Cannot fetch messages from Store " + store.getName());
            updateCache(null, null, "", true);
            cleanup();
            /* try connecting and receiving again. Try to connect will happen configured number of times
            and give up with a SynapseException */
            return receive();
        }
    }

    public boolean ack() {
        boolean result = cachedMessage.ack();
        if (result) {
            store.dequeued();
        }
        return result;
    }

    public boolean cleanup() throws SynapseException {
        if (logger.isDebugEnabled()) {
            logger.debug(getId() + " cleaning up...");
        }
        try {
            store.cleanup(connection, session);
            return true;
        } catch (JMSException e) {
            throw new SynapseException("Error while connecting to store to close created connections. JMS provider "
                    + "might not be accessible" + store.getName(), e);
        } finally {
            connection = null;
            session = null;
            consumer = null;
        }
    }

    public boolean isAlive() {
        try {
            session.getAcknowledgeMode(); /** No straight forward way to check session availability */
        } catch (JMSException e) {
            return false;
        }

        return true;
    }

    public Connection getConnection() {
        return connection;
    }

    public JmsConsumer setConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public Session getSession() {
        return session;
    }

    public JmsConsumer setSession(Session session) {
        this.session = session;
        return this;
    }

    public javax.jms.MessageConsumer getConsumer() {
        return consumer;
    }

    public JmsConsumer setConsumer(javax.jms.MessageConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setId(int id) {
        idString = "[" + store.getName() + "-C-" + id + "]";
    }

    public void setStringId(String idString) {
        this.idString = idString;
    }

    public String getId() {
        return getIdAsString();
    }

    private String getIdAsString() {
        if (idString == null) {
            return "[unknown-consumer]";
        }
        return idString;
    }

    /**
     * Check if connection, session and consumer is created successfully, if not try to connect
     *
     * @return true if connection to JMS provider is successfully made
     */
    private boolean checkAndTryConnect() {

        boolean connectionSuccess = false;

        if (consumer != null && session != null && connection != null) {
            connectionSuccess = true;
        } else {
            try {
                reconnect();
                connectionSuccess = true;
            } catch (StoreForwardException | SynapseException e) {
                logger.error("Error while connecting to JMS store and initializing consumer", e);
            }
        }
        return connectionSuccess;
    }

    private void writeToFileSystem() {
    }

    private void updateCache(Message message, MessageContext synCtx, String messageId, boolean receiveError) {
        isReceiveError = receiveError;
        cachedMessage.setMessage(message);
        cachedMessage.setMc(synCtx);
        cachedMessage.setId(messageId);
    }

    private void reconnect() throws StoreForwardException {
        logger.info("Trying to reconnect to JMS store " + store.getName());
        JmsConsumer consumer = (JmsConsumer) store.getConsumer();
        logger.info("Successfully connected to JMS store " + store.getName());
        if (consumer.getConsumer() == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(getId() + " could not reconnect to the broker.");
            }
        }
        connection = consumer.getConnection();
        session = consumer.getSession();
        this.consumer = consumer.getConsumer();
        if (logger.isDebugEnabled()) {
            logger.debug(getId() + " ===> " + consumer.getId());
        }
    }

    private final class CachedMessage {
        private Message message = null;
        private MessageContext mc = null;
        private String id = "";

        public CachedMessage setMessage(Message message) {
            this.message = message;
            return this;
        }

        /**
         * Acknowledge the message
         *
         * @return true if ack is processed successfully. If there was some issue,
         * we call recover on session will return false
         */
        public boolean ack() {
            try {
                if (message != null) {
                    message.acknowledge();
                }
            } catch (javax.jms.IllegalStateException e) {
                logger.warn("JMS Session is in an illegal state. Recovering session.");

                try {
                    getSession().recover();
                    logger.warn("JMS Session recovered.");
                } catch (JMSException e1) {
                    logger.error("Error occurred while recovering session: "
                            + e.getLocalizedMessage(), e);
                    return false;
                }
                return false;
            } catch (JMSException e) {
                logger.error(getId() + " cannot ack last read message. Error:"
                        + e.getLocalizedMessage(), e);
                return false;
            }
            return true;
        }

        public Message getMessage() {
            return message;
        }

        public CachedMessage setMc(MessageContext mc) {
            this.mc = mc;
            return this;
        }

        public CachedMessage setId(String id) {
            this.id = id;
            return this;
        }

        public String getId() {
            return id;
        }
    }
}

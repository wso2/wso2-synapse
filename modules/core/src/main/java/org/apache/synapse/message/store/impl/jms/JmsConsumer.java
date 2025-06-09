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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

public class JmsConsumer implements MessageConsumer {

    private static final Log logger = LogFactory.getLog(JmsConsumer.class.getName());

    private javax.jms.Connection javaxConnection;
    private jakarta.jms.Connection jakartaConnection;

    private javax.jms.Session javaxSession;
    private jakarta.jms.Session jakartaSession;

    private javax.jms.MessageConsumer javaxConsumer;
    private jakarta.jms.MessageConsumer jakartaConsumer;

    private JmsStore store;

    private String idString;

    private boolean isInitialized;

    /** Holds the last message read from the message store. */
    private CachedMessage cachedMessage;

    /** Did last receive() call cause an error? */
    private boolean isReceiveError;

    /**
     * Boolean to store if the message processor is alive
     */
    private boolean isAlive;

    private boolean isVersion31;

    /**
     * Constructor for JMS consumer
     *
     * @param store JMSStore associated to this JMS consumer
     */
    public JmsConsumer(JmsStore store, boolean isVersion31) {
        if (store == null) {
            logger.error("Cannot initialize.");
            return;
        }
        this.store = store;
        this.isVersion31 = isVersion31;
        cachedMessage = new CachedMessage();
        isReceiveError = false;
        isInitialized = true;
        isAlive = true;
    }

    public MessageContext receive() {

        if (isAlive) {
            if (isVersion31) {
                return getMessageContextForJakartaMessage();
            } else {
                return getMessageContextForJavaxMessage();
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Trying to receive messages from a consumer that is not alive. Id: " + getId()
                        + ", store: " + store.getName());
            }
            return null;
        }
    }

    private MessageContext getMessageContextForJavaxMessage() {
        boolean connectionSuccess = checkAndTryConnectForJavax();

        if (!connectionSuccess) {
            throw new SynapseException(idString + "Error while connecting to JMS provider. "
                    + MessageProcessorConstants.STORE_CONNECTION_ERROR);
        }

        try {
            Message message = javaxConsumer.receive(1000);
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

            //create a message context back from the stored message
            StorableMessage storableMessage = (StorableMessage) msg.getObject();
            org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
            MessageContext synapseMc = store.newSynapseMc(axis2Mc);
            synapseMc = MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);

            //cache the message
            updateJavaxCache(message, synapseMc, messageId, false);

            if (logger.isDebugEnabled()) {
                logger.debug(getId() + " Received MessageId:" + messageId + " priority:" + message.getJMSPriority());
            }

            return synapseMc;
        } catch (JMSException e) {
            logger.error("Cannot fetch messages from Store " + store.getName());
            updateJavaxCache(null, null, "", true);
            cleanup();
            /* try connecting and receiving again. Try to connect will happen configured number of times
            and give up with a SynapseException */
            return receive();
        }
    }

    private MessageContext getMessageContextForJakartaMessage() {
        boolean connectionSuccess = checkAndTryConnectForJakarta();

        if (!connectionSuccess) {
            throw new SynapseException(idString + "Error while connecting to JMS provider. "
                    + MessageProcessorConstants.STORE_CONNECTION_ERROR);
        }

        try {
            jakarta.jms.Message message = jakartaConsumer.receive(1000);
            if (message == null) {
                return null;
            }
            if (!(message instanceof jakarta.jms.ObjectMessage)) {
                logger.warn("JMS Consumer " + getId() + " did not receive a javax.jms.ObjectMessage");
                //we just discard this message as we only store Object messages via JMS Message store
                message.acknowledge();
                return null;
            }
            jakarta.jms.ObjectMessage msg = (jakarta.jms.ObjectMessage) message;
            String messageId = msg.getStringProperty(Constants.OriginalMessageID);
            if (!(msg.getObject() instanceof StorableMessage)) {
                logger.warn("JMS Consumer " + getId() + " did not receive a valid message.");
                message.acknowledge();
                return null;
            }

            //create a message context back from the stored message
            StorableMessage storableMessage = (StorableMessage) msg.getObject();
            org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
            MessageContext synapseMc = store.newSynapseMc(axis2Mc);
            synapseMc = MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);

            //cache the message
            updateJakartaCache(message, synapseMc, messageId, false);

            if (logger.isDebugEnabled()) {
                logger.debug(getId() + " Received MessageId:" + messageId + " priority:" + message.getJMSPriority());
            }

            return synapseMc;
        } catch (jakarta.jms.JMSException e) {
            logger.error("Cannot fetch messages from Store " + store.getName());
            updateJavaxCache(null, null, "", true);
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
        if (isVersion31) {
            try {
                store.cleanupJakarta(jakartaConnection, jakartaSession);
                return true;
            } catch (jakarta.jms.JMSException e) {
                throw new SynapseException("Error while connecting to store to close created connections. JMS provider "
                        + "might not be accessible " + store.getName() + " "
                        + MessageProcessorConstants.STORE_CONNECTION_ERROR, e);
            } finally {
                jakartaConnection = null;
                jakartaSession = null;
                jakartaConsumer = null;
            }
        } else {
            try {
                store.cleanupJavax(javaxConnection, javaxSession);
                return true;
            } catch (javax.jms.JMSException e) {
                throw new SynapseException("Error while connecting to store to close created connections. JMS provider "
                        + "might not be accessible " + store.getName() + " "
                        + MessageProcessorConstants.STORE_CONNECTION_ERROR, e);
            } finally {
                javaxConnection = null;
                javaxSession = null;
                javaxConsumer = null;
            }
        }
    }

    public boolean isAlive() {

        if (isAlive) {
            if (isVersion31) {
                try {
                    jakartaSession.getAcknowledgeMode(); /** No straight forward way to check session availability */
                } catch(jakarta.jms.JMSException e){
                    return false;
                }
            } else {
                try {
                    javaxSession.getAcknowledgeMode(); /** No straight forward way to check session availability */
                } catch(javax.jms.JMSException e){
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    public javax.jms.Connection getJavaxConnection() {
        return javaxConnection;
    }

    public JmsConsumer setJavaxConnection(javax.jms.Connection connection) {
        this.javaxConnection = connection;
        return this;
    }

    public jakarta.jms.Connection getJakartaConnection() {
        return jakartaConnection;
    }

    public JmsConsumer setJakartaConnection(jakarta.jms.Connection connection) {
        this.jakartaConnection = connection;
        return this;
    }

    public javax.jms.Session getJavaxSession() {
        return javaxSession;
    }

    public JmsConsumer setJavaxSession(javax.jms.Session session) {
        this.javaxSession = session;
        return this;
    }

    public jakarta.jms.Session getJakartaSession() {
        return jakartaSession;
    }

    public JmsConsumer setJakartaSession(jakarta.jms.Session session) {
        this.jakartaSession = session;
        return this;
    }

    public javax.jms.MessageConsumer getJavaxConsumer() {
        return javaxConsumer;
    }

    public JmsConsumer setJavaxConsumer(javax.jms.MessageConsumer consumer) {
        this.javaxConsumer = consumer;
        return this;
    }

    public jakarta.jms.MessageConsumer getJakartaConsumer() {
        return jakartaConsumer;
    }

    public JmsConsumer setJakartaConsumer(jakarta.jms.MessageConsumer consumer) {
        this.jakartaConsumer = consumer;
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
    private boolean checkAndTryConnectForJavax() {

        boolean connectionSuccess = false;

        if (javaxConsumer != null && javaxSession != null && javaxConnection != null) {
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

    /**
     * Check if connection, session and consumer is created successfully, if not try to connect
     *
     * @return true if connection to JMS provider is successfully made
     */
    private boolean checkAndTryConnectForJakarta() {

        boolean connectionSuccess = false;

        if (jakartaConsumer != null && jakartaSession != null && jakartaConnection != null) {
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

    private void updateJavaxCache(javax.jms.Message message, MessageContext synCtx, String messageId, boolean receiveError) {
        isReceiveError = receiveError;
        cachedMessage.setMessage(message);
        cachedMessage.setMc(synCtx);
        cachedMessage.setId(messageId);
    }

    private void updateJakartaCache(jakarta.jms.Message message, MessageContext synCtx, String messageId, boolean receiveError) {
        isReceiveError = receiveError;
        cachedMessage.setJakartaMessage(message);
        cachedMessage.setMc(synCtx);
        cachedMessage.setId(messageId);
    }

    private void reconnect() throws StoreForwardException {
        logger.info("Trying to reconnect to JMS store " + store.getName());
        JmsConsumer consumer = (JmsConsumer) store.getConsumer();
        logger.info("Successfully connected to JMS store " + store.getName());
        if (consumer.getJavaxConsumer() == null && consumer.getJakartaConsumer() == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(getId() + " could not reconnect to the broker.");
            }
        }
        if (isVersion31) {
            jakartaConnection = consumer.getJakartaConnection();
            jakartaSession = consumer.getJakartaSession();
            this.jakartaConsumer = consumer.getJakartaConsumer();
        } else {
            javaxConnection = consumer.getJavaxConnection();
            javaxSession = consumer.getJavaxSession();
            this.javaxConsumer = consumer.getJavaxConsumer();
        }
        if (logger.isDebugEnabled()) {
            logger.debug(getId() + " ===> " + consumer.getId());
        }
    }

    public boolean reInitialize() {
        // To keep the existing behaviour, return false
        return false;
    }

    private final class CachedMessage {
        private javax.jms.Message javaxMessage = null;
        private jakarta.jms.Message jakartaMessage = null;
        private MessageContext mc = null;
        private String id = "";

        public CachedMessage setMessage(Message message) {
            this.javaxMessage = message;
            return this;
        }

        public CachedMessage setJakartaMessage(jakarta.jms.Message message) {
            this.jakartaMessage = message;
            return this;
        }

        /**
         * Acknowledge the message
         *
         * @return true if ack is processed successfully. If there was some issue,
         * we call recover on session will return false
         */
        public boolean ack() {
            if (jakartaMessage != null) {
                return ackJakartaMessage();
            } else {
                return ackJavaxMessage();
            }
        }

        private boolean ackJavaxMessage() {
            try {
               javaxMessage.acknowledge();
            } catch (javax.jms.IllegalStateException e) {
                logger.warn("JMS Session is in an illegal state. Recovering session.");

                try {
                    getJavaxSession().recover();
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

        private boolean ackJakartaMessage() {
            try {
                jakartaMessage.acknowledge();
            } catch (jakarta.jms.IllegalStateException e) {
                logger.warn("JMS Session is in an illegal state. Recovering session.");

                try {
                    getJakartaSession().recover();
                    logger.warn("JMS Session recovered.");
                } catch (jakarta.jms.JMSException e1) {
                    logger.error("Error occurred while recovering session: "
                            + e.getLocalizedMessage(), e);
                    return false;
                }
                return false;
            } catch (jakarta.jms.JMSException e) {
                logger.error(getId() + " cannot ack last read message. Error:"
                        + e.getLocalizedMessage(), e);
                return false;
            }
            return true;
        }

        public Message getMessage() {
            return javaxMessage;
        }

        public jakarta.jms.Message getJakartaMessage() {
            return jakartaMessage;
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

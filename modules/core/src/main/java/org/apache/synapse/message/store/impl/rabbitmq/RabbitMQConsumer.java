/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.message.store.impl.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.message.MessageConsumer;

import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

/**
 * The message consumer responsible for read a message from the queue and set it into the message context
 */
public class RabbitMQConsumer implements MessageConsumer {

    private static final Log log = LogFactory.getLog(RabbitMQConsumer.class.getName());

    private Connection connection;
    private Channel channel;
    private RabbitMQStore store;
    private String queueName;
    private String idString;
    private CachedMessage cachedMessage; // Holds the last message read from the message store

    public RabbitMQConsumer(RabbitMQStore store) {
        if (store == null) {
            log.error("Cannot initialize consumer: " + getId());
            return;
        }
        this.store = store;
        cachedMessage = new CachedMessage();
    }

    /**
     * Get a single message from the queue and deserialize for set into the message context
     *
     * @return the {@link MessageContext} with received message
     */
    @Override
    public MessageContext receive() {
        if (isAlive()) {
            GetResponse delivery = null;
            try {
                delivery = channel.basicGet(queueName, false);
                if (delivery != null) {
                    StorableMessage storableMessage = deserializeMessage(delivery);
                    org.apache.axis2.context.MessageContext axis2Mc = store.newAxis2Mc();
                    MessageContext synapseMc = store.newSynapseMc(axis2Mc);
                    synapseMc = MessageConverter.toMessageContext(storableMessage, axis2Mc, synapseMc);
                    updateCache(delivery, delivery.getProps().getMessageId());
                    if (log.isDebugEnabled()) {
                        log.debug(getId() + " Received MessageId: " + delivery.getProps().getMessageId());
                    }
                    return synapseMc;
                }
            } catch (ShutdownSignalException | IOException e) {
                log.error(getId() + " connection error when receiving messages.", e);
                cleanup();
            } catch (ClassNotFoundException e) {
                log.error(getId() + "unable to read the stored message.", e);
                try {
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException ex) {
                    log.error(getId() + "unable to acknowledge the stored message.", e);
                }
            }
        } else {
            log.warn("The connection and channel to the RabbitMQ broker are unhealthy.");
            cleanup();
            setConnection(store.createConnection());
            setChannel(store.createChannel(connection));
        }
        return null;
    }

    /**
     * Deserialize the message taken from the queue
     *
     * @param delivery the message received from the broker
     * @return a {@link StorableMessage} object
     * @throws IOException
     */
    private StorableMessage deserializeMessage(GetResponse delivery) throws IOException, ClassNotFoundException {
        StorableMessage storableMessage;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(delivery.getBody());
             ObjectInput objectInput = new ObjectInputStream(inputStream)) {
            storableMessage = (StorableMessage) objectInput.readObject();
        }
        return storableMessage;
    }

    /**
     * Acknowledge the message upon successful backend invocation
     *
     * @return whether message is successfully acknowledge or not
     */
    @Override
    public boolean ack() {
        boolean result = cachedMessage.ack();
        if (result) {
            store.dequeued();
        }
        return result;
    }

    /**
     * Cleanup the {@link Connection} and it's related resources
     *
     * @return whether cleanup successful or not
     */
    @Override
    public boolean cleanup() {
        if (connection != null) {
            connection.abort();
        }
        channel = null;
        connection = null;
        return true;
    }

    /**
     * Check availability of connectivity with the message store
     *
     * @return {@code true} if connection available, {@code false} otherwise.
     */
    @Override
    public boolean isAlive() {
        return (connection != null && connection.isOpen()) && (channel != null && channel.isOpen());
    }

    /**
     * Get ID of this RabbitMQ consumer
     *
     * @return the ID
     */
    @Override
    public String getId() {
        return idString;
    }

    /**
     * Set ID of this RabbitMQ consumer
     *
     * @param id ID
     */
    @Override
    public void setId(int id) {
        idString = "[" + store.getName() + "-C-" + id + "]";
    }

    /**
     * Set the {@link Connection} object
     *
     * @param connection a {@link Connection} object
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Set the {@link Channel} object
     *
     * @param channel a {@link Channel} object
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * Set the queue name
     *
     * @param queueName the queue name
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * Update the cached message with the message received from the queue
     *
     * @param delivery  the received message from the queue
     * @param messageId the message id
     */
    private void updateCache(GetResponse delivery, String messageId) {
        cachedMessage.setMessage(delivery);
        cachedMessage.setId(messageId);
    }

    /**
     * This is used to store the last received message
     * if the message is successfully sent to the endpoint, ack will sent to the store
     * in order to delete message from the queue
     * <p/>
     * In RabbitMQ message ack should be using the same channel which was consumed the message
     * There for the consumed channel will also stored without closing until the message is ackd
     */
    private final class CachedMessage {
        private GetResponse message = null;
        private String id = "";

        public void setMessage(GetResponse message) {
            this.message = message;
        }

        public boolean ack() {
            if (message != null && channel != null && channel.isOpen()) {
                try {
                    channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                    return true;
                } catch (IOException e) {
                    log.error(getId() + " cannot ack last read message. Error: " + e.getLocalizedMessage(), e);
                    return false;
                }
            }
            return false;
        }

        public GetResponse getMessage() {
            return message;
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


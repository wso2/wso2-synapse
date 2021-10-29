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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.message.MessageProducer;

import org.apache.synapse.message.store.impl.commons.MessageConverter;
import org.apache.synapse.message.store.impl.commons.StorableMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeoutException;

/**
 * The message producer responsible to store message into RabbitMQ queue
 */
public class RabbitMQProducer implements MessageProducer {

    private static final Log log = LogFactory.getLog(RabbitMQProducer.class.getName());

    private static final int DEFAULT_PRIORITY = 0;
    private Connection connection;
    private RabbitMQStore store;
    private String routingKey;
    private String exchangeName;
    private boolean isInitialized = false;
    private String idString; // ID of the MessageProducer
    private boolean publisherConfirmsEnabled;
    private Channel channel;

    /**
     * The RabbitMQ producer
     *
     * @param store the {@link RabbitMQStore} object
     */
    public RabbitMQProducer(RabbitMQStore store) {
        if (store == null) {
            log.error("Cannot initialize producer: " + getId());
            return;
        }
        this.store = store;
        isInitialized = true;
    }

    /**
     * Store the given message into the queue and return whether the operation success or not
     *
     * @param synCtx Message to be saved.
     * @return {@code true} if storing of the message is successful, {@code false} otherwise.
     */
    @Override
    public boolean storeMessage(MessageContext synCtx) {
        if (synCtx == null) {
            return false;
        }
        if (connection == null) {
            log.error(getId() + " cannot proceed. RabbitMQ Connection is null. Ignored MessageId: " +
                    synCtx.getMessageID());
            return false;
        }
        boolean result = false;
        try {
            if (publisherConfirmsEnabled) {
                channel.confirmSelect();
            }
            StorableMessage storableMessage = MessageConverter.toStorableMessage(synCtx);
            final byte[] message = serializeMessage(storableMessage);
            final AMQP.BasicProperties basicProperties = getBasicProperties(synCtx, storableMessage);
            publishMessage(channel, exchangeName, routingKey, basicProperties, message);
            if (publisherConfirmsEnabled) {
                result = channel.waitForConfirms();
            } else {
                result = true;
            }
            if (log.isDebugEnabled()) {
                log.debug(getId() + ". Stored MessageId: " + synCtx.getMessageID());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            String errorMsg = getId() + ". Ignored MessageId: " + synCtx.getMessageID() + ". " +
                    "Could not store message to store [" + store.getName() + "]. " +
                    "Error:" + e.getLocalizedMessage();
            log.error(errorMsg, e);
            channel = null;
            connection = null;
        }
        store.enqueued();
        return result;
    }

    /**
     * Serialize the message to store in the queue
     *
     * @param storableMessage the {@link StorableMessage} object
     * @return serialize message as byte array
     * @throws IOException
     */
    private byte[] serializeMessage(StorableMessage storableMessage) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = new ObjectOutputStream(outputStream);
        objectOutput.writeObject(storableMessage);
        return outputStream.toByteArray();
    }

    /**
     * Build AMQP basic properties from the message context
     *
     * @param synCtx          the {@link MessageContext} object
     * @param storableMessage the {@link StorableMessage} object
     * @return AMQP basic properties
     */
    private AMQP.BasicProperties getBasicProperties(MessageContext synCtx, StorableMessage storableMessage) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
        builder.messageId(synCtx.getMessageID());
        builder.deliveryMode(MessageProperties.MINIMAL_PERSISTENT_BASIC.getDeliveryMode());
        builder.priority(storableMessage.getPriority(DEFAULT_PRIORITY));
        return builder.build();
    }

    /**
     * Perform basic publish
     *
     * @param channel         the channel
     * @param exchangeName    the exchange to publish the message to
     * @param routingKey      the routing key
     * @param basicProperties other properties for the message
     * @param messageBody     the message body
     * @throws IOException
     */
    private void publishMessage(Channel channel, String exchangeName, String routingKey,
                                AMQP.BasicProperties basicProperties, byte[] messageBody) throws IOException {
        if (StringUtils.isNotEmpty(exchangeName)) {
            channel.basicPublish(exchangeName, routingKey, basicProperties, messageBody);
        } else {
            channel.basicPublish("", routingKey, basicProperties, messageBody);
        }
    }

    /**
     * Used to close the channel opened in this object instance.
     * This should be called after the end of each call on storeMessage method
     * But instead of this, try with resources will close the channel
     */
    @Override
    public boolean cleanup() {
        return true;
    }

    /**
     * Get ID of this RabbitMQ producer
     *
     * @return the ID
     */
    @Override
    public String getId() {
        return idString;
    }

    /**
     * Set ID of this RabbitMQ producer
     *
     * @param id ID
     */
    @Override
    public void setId(int id) {
        idString = "[" + store.getName() + "-P-" + id + "]";
    }

    /**
     * Set the routing key bind with the exchange
     *
     * @param routingKey the message routing key
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    /**
     * Set the exchange name to publish the message
     *
     * @param exchangeName the exchange to publish the message to
     */
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
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
     * Set the publisher confirm enabled or not
     *
     * @param publisherConfirmsEnabled publsher confirm enabled or not
     */
    public void setPublisherConfirmsEnabled(boolean publisherConfirmsEnabled) {
        this.publisherConfirmsEnabled = publisherConfirmsEnabled;
    }

    /**
     * Verify to whether producer was initialized
     *
     * @return is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

}
